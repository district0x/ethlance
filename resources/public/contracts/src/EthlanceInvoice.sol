pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";
import "./EthlanceComment.sol";
import "proxy/Forwarder.sol";


/// @title Represents a Candidate Invoice for work
contract EthlanceInvoice {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);


    //
    // Constants
    //
    uint public constant GUEST_TYPE = 0;
    uint public constant EMPLOYER_TYPE = 1;
    uint public constant CANDIDATE_TYPE = 2;
    uint public constant ARBITER_TYPE = 3;


    //
    // Members
    //
    uint public invoice_index;

    uint public date_created;
    
    uint public date_updated;

    // When date_paid is set >0, this reflects completion
    uint public date_paid;
    
    //FIXME: needs to be more flexible for other currency types
    //defined in the contract.

    // In Wei, the amount that needs to be paid to the candidate(employee)
    uint public amount_requested;
    
    // In Wei, the amount actually paid by the employer to the candidate(employee)
    uint public amount_paid;

    // The EthlanceWorkContract reference.
    EthlanceWorkContract public work_instance;  
    
    /// @dev Forwarder Constructor
    function construct(EthlanceWorkContract _work_instance,
		       uint _invoice_index,
		       uint _amount_requested,
		       string calldata metahash) external {
	// TODO: authenticate
	work_instance = _work_instance;
	invoice_index = _invoice_index;
	amount_requested = _amount_requested;
	date_created = now;
	date_updated = now;

	// Fire off comment with provided metahash
	createComment(work_instance.candidate_address(), metahash);

	// Fire off event
	fireEvent("InvoiceCreated");
    }

    
    function updateDateUpdated() private {
	date_updated = now;
    }


    /// @dev Pays the given invoice the the amount of `_amount_paid`.
    /// @param _amount_paid The amount to pay the invoice in Wei.
    function pay(uint _amount_paid) external {
	require(work_instance.store_instance().employer_address() == msg.sender,
		"Only the employer can pay an invoice.");
	require(date_paid == 0, "Given invoice has already been paid.");

	work_instance.payInvoice(_amount_paid);
	amount_paid = _amount_paid;
	date_paid = now;
	updateDateUpdated();

	// Fire off event
	fireEvent("InvoicePaid");
    }

    
    /// @dev Returns true if the invoice has been paid.
    function isInvoicePaid() external view returns(bool) {
	return date_paid > 0;
    }


    /// @dev Fire events specific to the invoice.
    /// @param event_name Unique to give the fired event
    function fireEvent(string memory event_name) private {
	uint[] memory event_data = new uint[](3);
	event_data[0] = work_instance.job_index();
	event_data[1] = work_instance.work_index();
	event_data[2] = invoice_index;

	registry.fireEvent(event_name, version, event_data);
    }


    /// @dev Returns the user type, either CANDIDATE_TYPE,
    /// EMPLOYER_TYPE, ARBITER_TYPE, or GUEST_TYPE for the given
    /// address.
    function getUserType(address _address) private returns (uint) {
	if (_address == work_instance.candidate_address()) {
	    return CANDIDATE_TYPE;
	}
	else if (_address == work_instance.store_instance().employer_address()) {
	    return EMPLOYER_TYPE;
	}
	else if (_address == work_instance.store_instance().accepted_arbiter()) {
	    return ARBITER_TYPE;
	}
	else {
	    return GUEST_TYPE;
	}
    }


    /// @dev Public function for authorized users to create comments
    /// on the given invoice.
    function addComment(string calldata metahash) external {
	createComment(msg.sender, metahash);
    }


    /// @dev Place a comment on the invoice linked to the given user
    /// address
    function createComment(address user_address, string memory metahash) private {
	uint user_type = getUserType(user_address);
	require(user_type != GUEST_TYPE,
		"Only the candidate, employer, and arbiter can comment.");
	
	// Create the forwarded contract
        Forwarder fwd = new Forwarder(); // Proxy Contract
                                         // target(EthlanceComment)
        EthlanceComment comment = EthlanceComment(address(fwd));

	// Permit Comment to fire registry events
	registry.permitDispatch(address(fwd));

	// Add comment to the registry comment listing
	registry.pushComment(address(this), address(comment));

	uint[4] memory index;
	index[0] = work_instance.store_instance().job_index();
	index[1] = work_instance.work_index();
	index[2] = invoice_index;
	index[3] = registry.getCommentCount(address(this)) - 1;

	// Construct the comment contract
	comment.construct(msg.sender,
			  user_type,
			  metahash,
			  EthlanceComment.CommentType.Invoice,
			  index);
    }
}
