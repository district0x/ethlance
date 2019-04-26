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
  uint public disputeIndex;
    
  uint public dateCreated;

  uint public dateUpdated;

  // When dateResolved is set >0, this reflects completion
  uint public dateResolved;

  // Title of dispute, describing the reason for the dispute
  string public reason;

  // The amount that the employer did receive as a result of
  // resolution, with a token address for the type of ERC20 token.
  uint public employerResolutionAmount;
  address public employerResolutionToken;

  // The amount that the candidate did receive as a result of
  // resolution, with a token address for the type of ERC20 token.
  uint public candidateResolutionAmount;
  address public candidateResolutionToken;

  // The amount that the arbiter did receive as a result of the
  // resolution, with a token address for the type of ERC20 token.
  uint public arbiterResolutionAmount;
  address public arbiterResolutionToken;

  // The entity that constructed contract
  address public owner;

  // The EthlanceWorkContract reference.
  EthlanceWorkContract public workInstance;
    
  /// @dev Forwarder Constructor
  /// @param _workInstance The EthlanceWorkContract parent instance for this Dispute.
  /// @param _reason Short string defining the reason for the dispute
  /// @param metahash A structure IPFS data structure defined by a
  /// hash string. The hash is stored as the employer or the
  /// candidate depending on is_employer_request.
  function construct(EthlanceWorkContract _workInstance,
                     uint _disputeIndex,
                     string calldata _reason,
                     string calldata metahash,
                     bool is_employer_request) external {
    require(owner == address(0), "EthlanceDispute contract already constructed.");
    owner = msg.sender;

    workInstance = _workInstance;
    disputeIndex = _disputeIndex;
    reason = _reason;
    dateCreated = now;
    dateUpdated = now;

    // Fire off comment with provided metahash
    if (is_employer_request) {
      createComment(workInstance.store_instance().employer_address(), metahash);
    }
    else {
      createComment(workInstance.candidate_address(), metahash);
    }

    // Fire off event
    fireEvent("DisputeCreated");
  }


  function updateDateUpdated() private {
    dateUpdated = now;
  }

    
  /// @dev Resolves the dispute between the employer and the
  /// candidate, and pays the employer and the candidate's the given
  /// amounts.
  /// @param _employer_amount The amount of tokens to pay the employer for resolution.
  /// @param _employerToken The token address of the type of token, set to 0x0 for ETH.
  /// @param _candidateAmount The amount of tokens to pay the candidate for resolution.
  /// @param _candidateToken The token address of the type of token, set to 0x0 for ETH.
  /// @param _arbiterAmount The amount of tokens to pay the arbiter for resolution.
  /// @param _arbiterToken The token address of the type of token, set to 0x0 for ETH.
  function resolve(uint _employerAmount,
                   address _employerToken,
                   uint _candidateAmount,
                   address _candidateToken,
                   uint _arbiterAmount,
                   address _arbiterToken) external {
    require(workInstance.store_instance().accepted_arbiter() == msg.sender,
            "Only the accepted arbiter can resolve a dispute.");
    require(dateResolved == 0, "This dispute has already been resolved.");
    workInstance.resolveDispute(_employerAmount, _employerToken,
                                 _candidateAmount, _candidateToken,
                                 _arbiterAmount, _arbiterToken);
    employerResolutionAmount = _employerAmount;
    employerResolutionToken = _employerToken;
    candidateResolutionAmount = _candidateAmount;
    candidateResolutionToken = _candidateToken;
    arbiterResolutionAmount = _arbiterAmount;
    arbiterResolutionToken = _arbiterToken;
    dateResolved = now;
    updateDateUpdated();

    fireEvent("DisputeResolved");
  }

    
  /// @dev Returns true if the current dispute is resolved.
  function isResolved() external view returns(bool) {
    return dateResolved != 0;
  }


  /// @dev Fire events specific to the dispute.
  /// @param eventName Unique to give the fired event
  function fireEvent(string memory eventName) private {
    uint[] memory eventData = new uint[](3);
    eventData[0] = workInstance.job_index();
    eventData[1] = workInstance.work_index();
    eventData[2] = disputeIndex;

    registry.fireEvent(eventName, version, eventData);
  }


  /// @dev Returns the user type, either CANDIDATE_TYPE,
  /// EMPLOYER_TYPE, ARBITER_TYPE, or GUEST_TYPE for the given
  /// address.
  function getUserType(address _address) private returns (uint) {
    if (_address == workInstance.candidate_address()) {
      return CANDIDATE_TYPE;
    }
    else if (_address == workInstance.store_instance().employer_address()) {
      return EMPLOYER_TYPE;
    }
    else if (_address == workInstance.store_instance().accepted_arbiter()) {
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
    index[0] = workInstance.store_instance().job_index();
    index[1] = workInstance.work_index();
    index[2] = disputeIndex;
    index[3] = registry.getCommentCount(address(this)) - 1;

    // Construct the comment contract
    comment.construct(userAddress,
                      userType,
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
    uint userType = getUserType(msg.sender);
    require(userType == CANDIDATE_TYPE || userType == EMPLOYER_TYPE,
            "Only the candidate or the employer, can leave feedback for the arbiter.");
  
    address toUserAddress = workInstance.store_instance().accepted_arbiter();
    uint toUserType = ARBITER_TYPE;

    // Check and create forwarded feedback contract instance
    EthlanceFeedback feedback;
    if (!registry.hasFeedback(address(workInstance))) {
      SecondForwarder fwd = new SecondForwarder(); // Proxy Contract
                                                   // target(EthlanceFeedback)

      feedback = EthlanceFeedback(address(fwd));

      // Permit Feedback to fire registry events
      registry.permitDispatch(address(fwd));
      
      // Add feedback to the registry feedback listing
      registry.pushFeedback(address(workInstance), address(feedback));
      
      // Construct the feedback contract
      feedback.construct(address(workInstance),
                         workInstance.store_instance().job_index(),
                         workInstance.work_index());
    }
    else {
      feedback = EthlanceFeedback(registry.getFeedbackByAddress(address(workInstance)));
    }

    feedback.update(msg.sender,
                    toUserAddress,
                    userType,
                    toUserType,
                    metahash,
                    rating);
  }
}
