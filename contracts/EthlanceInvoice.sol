pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";
import "./EthlanceComment.sol";
import "./proxy/Forwarder.sol";


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
  uint public invoiceIndex;

  uint public dateCreated;
    
  uint public dateUpdated;

  // When datePaid is set >0, this reflects completion
  uint public datePaid;
    
  //FIXME: needs to be more flexible for other currency types
  //defined in the contract.

  // In Wei, the amount that needs to be paid to the candidate(employee)
  uint public amountRequested;
    
  // In Wei, the amount actually paid by the employer to the candidate(employee)
  uint public amountPaid;

  // The entity that constructed the contract
  address public owner;

  // The EthlanceWorkContract reference.
  EthlanceWorkContract public workInstance;
    
  /// @dev Forwarder Constructor
  function construct(EthlanceWorkContract _workInstance,
                     uint _invoiceIndex,
                     uint _amountRequested,
                     string calldata metahash) external {
    require(owner == address(0), "EthlanceInvoice contract already constructed.");
    owner = msg.sender;

    workInstance = _workInstance;
    invoiceIndex = _invoiceIndex;
    amountRequested = _amountRequested;
    dateCreated = now;
    dateUpdated = now;

    // Fire off comment with provided metahash
    createComment(workInstance.candidateAddress(), metahash);

    // Fire off event
    fireEvent("InvoiceCreated");
  }

    
  function updateDateUpdated() private {
    dateUpdated = now;
  }


  /// @dev Pays the given invoice the the amount of `_amountPaid`.
  /// @param _amountPaid The amount to pay the invoice in Wei.
  function pay(uint _amountPaid) external {
    require(workInstance.storeInstance().employerAddress() == msg.sender,
            "Only the employer can pay an invoice.");
    require(datePaid == 0, "Given invoice has already been paid.");

    workInstance.payInvoice(_amountPaid);
    amountPaid = _amountPaid;
    datePaid = now;
    updateDateUpdated();

    // Fire off event
    fireEvent("InvoicePaid");
  }

    
  /// @dev Returns true if the invoice has been paid.
  function isInvoicePaid() external view returns(bool) {
    return datePaid > 0;
  }


  /// @dev Fire events specific to the invoice.
  /// @param event_name Unique to give the fired event
  function fireEvent(string memory event_name) private {
    uint[] memory event_data = new uint[](3);
    event_data[0] = workInstance.jobIndex();
    event_data[1] = workInstance.workIndex();
    event_data[2] = invoiceIndex;

    registry.fireEvent(event_name, version, event_data);
  }


  /// @dev Returns the user type, either CANDIDATE_TYPE,
  /// EMPLOYER_TYPE, ARBITER_TYPE, or GUEST_TYPE for the given
  /// address.
  function getUserType(address _address) private returns (uint) {
    if (_address == workInstance.candidateAddress()) {
      return CANDIDATE_TYPE;
    }
    else if (_address == workInstance.storeInstance().employerAddress()) {
      return EMPLOYER_TYPE;
    }
    else if (_address == workInstance.storeInstance().acceptedArbiter()) {
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
  function createComment(address userAddress, string memory metahash) private {
    uint userType = getUserType(userAddress);
    require(userType != GUEST_TYPE,
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
    index[0] = workInstance.storeInstance().jobIndex();
    index[1] = workInstance.workIndex();
    index[2] = invoiceIndex;
    index[3] = registry.getCommentCount(address(this)) - 1;

    // Construct the comment contract
    comment.construct(userAddress,
                      userType,
                      metahash,
                      EthlanceComment.CommentType.Invoice,
                      index);
  }
}
