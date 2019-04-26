pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";


/// @title Represents a comment created by a given party
/*
  Notes:

  - Each comment contract stores previous revisions of a comment.
*/
contract EthlanceComment {
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
	// Enums
  //
  enum CommentType {
    WorkContract,
    Invoice,
    Dispute
  }
    

  //
  // Members
  //
  address public owner;
  uint public userType;
  address public userAddress;
  uint public dateCreated;
  uint public dateUpdated;
  string[] public metahashListing;
  CommentType public commentType;
  uint[4] public commentIndex;


  /// @dev Forwarder Constructor
  function construct(address _userAddress,
                     uint _userType,
                     string calldata _metahash,
                     CommentType _commentType,
                     uint[4] calldata _commentIndex)
  
    external {
    require(owner == address(0), "EthlanceComment contract was already constructed");
    require(_userType <= ARBITER_TYPE, "Unknown User Type");

    owner = msg.sender;
    userAddress = _userAddress;
    userType = _userType;
    metahashListing.push(_metahash);
    dateCreated = now;
    dateUpdated = now;
    commentType = _commentType;
    commentIndex = _commentIndex;

    fireEvent("CommentCreated");
  }

    
  /// @dev Update the given comment with a revision, replaces the latest metahash.
  function update(string calldata _metahash) external {
    require(msg.sender == userAddress, "Only the original user can edit the comment.");
    metahashListing.push(_metahash);
    dateUpdated = now;
    fireEvent("CommentUpdated");
  }


  /// @dev Get the number of comment revisions.
  function getCount() public view returns(uint) {
    return metahashListing.length;
  }


  /// @dev Get the comment or comment revision at the given index
  function getRevisionByIndex(uint _index) public view returns(string memory) {
    require(_index < metahashListing.length, "Given index is out of bounds.");
    return metahashListing[_index];
  }


  /// @dev Get the comment or latest comment revision.
  function getLast() public view returns(string memory) {
    return metahashListing[metahashListing.length - 1];
  }


  /// @dev Fire events specific to the dispute.
  /// @param event_name Unique to give the fired event
  function fireEvent(string memory event_name) private {
    uint[] memory event_data = new uint[](4);
    event_data[0] = commentIndex[0];
    event_data[1] = commentIndex[1];
    event_data[2] = commentIndex[2];
    event_data[3] = commentIndex[3];

    registry.fireEvent(event_name, version, event_data);
  }


  /// @dev Returns the comment sub-index
  /*
    For Work Contract:
    [0] --> Job Index
    [1] --> Work Contract Index
    [2] --> Comment Index

    For Invoice:
    [0] --> Job Index
    [1] --> Work Contract Index
    [2] --> Invoice Index
    [3] --> Comment Index

    For Dispute:
    [0] --> Job Index
    [1] --> Work Contract Index
    [2] --> Dispute Index
    [3] --> Comment Index
  */
  function getIndex(uint index) public view returns(uint) {
    require(index <= 4, "Index out of bounds");
    return commentIndex[index];
  }
}
