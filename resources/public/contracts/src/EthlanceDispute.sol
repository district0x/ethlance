pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";
import "./EthlanceComment.sol";
import "./EthlanceFeedback.sol";
import "proxy/Forwarder.sol";       // target(EthlanceComment)
import "proxy/SecondForwarder.sol"; // target(EthlanceFeedback)


/// @title Represents a Employer / Candidate work dispute
contract EthlanceDispute {
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
    uint public dispute_index;
    
    uint public date_created;

    uint public date_updated;

    // When date_resolved is set >0, this reflects completion
    uint public date_resolved;

    // Title of dispute, describing the reason for the dispute
    string public reason;

    // The amount that the employer did receive as a result of
    // resolution, with a token address for the type of ERC20 token.
    uint public employer_resolution_amount;
    address public employer_resolution_token;

    // The amount that the candidate did receive as a result of
    // resolution, with a token address for the type of ERC20 token.
    uint public candidate_resolution_amount;
    address public candidate_resolution_token;

    // The amount that the arbiter did receive as a result of the
    // resolution, with a token address for the type of ERC20 token.
    uint public arbiter_resolution_amount;
    address public arbiter_resolution_token;

    // The entity that constructed contract
    address public owner;

    // The EthlanceWorkContract reference.
    EthlanceWorkContract public work_instance;
    
    /// @dev Forwarder Constructor
    /// @param _work_instance The EthlanceWorkContract parent instance for this Dispute.
    /// @param _reason Short string defining the reason for the dispute
    /// @param metahash A structure IPFS data structure defined by a
    /// hash string. The hash is stored as the employer or the
    /// candidate depending on is_employer_request.
    function construct(EthlanceWorkContract _work_instance,
		       uint _dispute_index,
		       string calldata _reason,
		       string calldata metahash,
		       bool is_employer_request) external {
	require(owner == address(0), "EthlanceDispute contract already constructed.");
	owner = msg.sender;

	work_instance = _work_instance;
	dispute_index = _dispute_index;
	reason = _reason;
	date_created = now;
	date_updated = now;

	// Fire off comment with provided metahash
	if (is_employer_request) {
	    createComment(work_instance.store_instance().employer_address(), metahash);
	}
	else {
	    createComment(work_instance.candidate_address(), metahash);
	}

	// Fire off event
	fireEvent("DisputeCreated");
    }


    function updateDateUpdated() private {
	date_updated = now;
    }

    
    /// @dev Resolves the dispute between the employer and the
    /// candidate, and pays the employer and the candidate's the given
    /// amounts.
    /// @param _employer_amount The amount of tokens to pay the employer for resolution.
    /// @param _employer_token The token address of the type of token, set to 0x0 for ETH.
    /// @param _candidate_amount The amount of tokens to pay the candidate for resolution.
    /// @param _candidate_token The token address of the type of token, set to 0x0 for ETH.
    /// @param _arbiter_amount The amount of tokens to pay the arbiter for resolution.
    /// @param _arbiter_token The token address of the type of token, set to 0x0 for ETH.
    function resolve(uint _employer_amount,
		     address _employer_token,
		     uint _candidate_amount,
		     address _candidate_token,
		     uint _arbiter_amount,
		     address _arbiter_token) external {
	require(work_instance.store_instance().accepted_arbiter() == msg.sender,
		"Only the accepted arbiter can resolve a dispute.");
	require(date_resolved == 0, "This dispute has already been resolved.");
	work_instance.resolveDispute(_employer_amount, _employer_token,
				     _candidate_amount, _candidate_token,
				     _arbiter_amount, _arbiter_token);
	employer_resolution_amount = _employer_amount;
	employer_resolution_token = _employer_token;
	candidate_resolution_amount = _candidate_amount;
	candidate_resolution_token = _candidate_token;
	arbiter_resolution_amount = _arbiter_amount;
	arbiter_resolution_token = _arbiter_token;
	date_resolved = now;
	updateDateUpdated();

	fireEvent("DisputeResolved");
    }

    
    /// @dev Returns true if the current dispute is resolved.
    function isResolved() external view returns(bool) {
	return date_resolved != 0;
    }


    /// @dev Fire events specific to the dispute.
    /// @param event_name Unique to give the fired event
    function fireEvent(string memory event_name) private {
	uint[] memory event_data = new uint[](3);
	event_data[0] = work_instance.job_index();
	event_data[1] = work_instance.work_index();
	event_data[2] = dispute_index;

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
    /// on the given dispute.
    function addComment(string calldata metahash) external {
	createComment(msg.sender, metahash);
    }


    /// @dev Place a comment on the dispute linked to the given user
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
	index[2] = dispute_index;
	index[3] = registry.getCommentCount(address(this)) - 1;

	// Construct the comment contract
	comment.construct(user_address,
			  user_type,
			  metahash,
			  EthlanceComment.CommentType.Dispute,
			  index);
    }


    /// @dev Leave feedback for the arbiter in the given Dispute.
    /*
      Notes:

      - Only the candidate and the employer can leave feedback on a dispute.
     */
    function leaveFeedback(uint rating, string calldata metahash) external {
	uint user_type = getUserType(msg.sender);
	require(user_type == CANDIDATE_TYPE || user_type == EMPLOYER_TYPE,
		"Only the candidate or the employer, can leave feedback for the arbiter.");
	
	address to_user_address = work_instance.store_instance().accepted_arbiter();
	uint to_user_type = ARBITER_TYPE;

	// Check and create forwarded feedback contract instance
	EthlanceFeedback feedback;
	if (!registry.hasFeedback(address(this))) {
	    SecondForwarder fwd = new SecondForwarder(); // Proxy Contract
	                                                 // target(EthlanceFeedback)

	    feedback = EthlanceFeedback(address(fwd));

	    // Permit Feedback to fire registry events
	    registry.permitDispatch(address(fwd));
	    
	    // Add feedback to the registry feedback listing
	    registry.pushFeedback(address(this), address(feedback));
	    
	    // Construct the feedback contract
	    feedback.construct(address(this),
			       work_instance.store_instance().job_index(),
			       work_instance.work_index());
	}
	else {
	    feedback = EthlanceFeedback(registry.getFeedbackByAddress(address(this)));
	}

	feedback.update(msg.sender,
			to_user_address,
			user_type,
			to_user_type,
			metahash,
			rating);
    }
}
