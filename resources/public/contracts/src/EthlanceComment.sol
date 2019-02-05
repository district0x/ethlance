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
    uint public user_type;
    address public user_address;
    uint public date_created;
    uint public date_updated;
    string[] public metahash_listing;
    CommentType public comment_type;
    uint[4] public comment_index;


    /// @dev Forwarder Constructor
    function construct(address _user_address,
		       uint _user_type,
		       string calldata _metahash,
		       CommentType _comment_type,
		       uint[4] calldata _comment_index)
	
	external {
	require(owner == address(0), "EthlanceComment contract was already constructed");
	require(_user_type <= ARBITER_TYPE, "Unknown User Type");

	owner = msg.sender;
	user_address = _user_address;
	user_type = _user_type;
	metahash_listing.push(_metahash);
	date_created = now;
	date_updated = now;
	comment_type = _comment_type;
	comment_index = _comment_index;

	fireEvent("CommentCreated");
    }

    
    /// @dev Update the given comment with a revision, replaces the latest metahash.
    function update(string calldata _metahash) external {
	require(msg.sender == user_address, "Only the original user can edit the comment.");
	metahash_listing.push(_metahash);
	date_updated = now;
	fireEvent("CommentUpdated");
    }


    /// @dev Get the number of comment revisions.
    function getCount() public view returns(uint) {
	return metahash_listing.length;
    }


    /// @dev Get the comment or comment revision at the given index
    function getRevisionByIndex(uint _index) public view returns(string memory) {
	require(_index < metahash_listing.length, "Given index is out of bounds.");
	return metahash_listing[_index];
    }


    /// @dev Get the comment or latest comment revision.
    function getLast() public view returns(string memory) {
	return metahash_listing[metahash_listing.length - 1];
    }


    /// @dev Fire events specific to the dispute.
    /// @param event_name Unique to give the fired event
    function fireEvent(string memory event_name) private {
	uint[] memory event_data = new uint[](4);
	event_data[0] = comment_index[0];
	event_data[1] = comment_index[1];
	event_data[2] = comment_index[2];
	event_data[3] = comment_index[3];

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
	return comment_index[index];
    }
}
