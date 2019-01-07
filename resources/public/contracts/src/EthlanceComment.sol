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
    // Members
    //
    uint public user_type;
    address public user_address;
    uint public date_created;
    uint public date_updated;
    string[] public metahash_listing;


    /// @dev Forwarder Constructor
    function construct(address _user_address, uint _user_type, string calldata _metahash) external {
	// TODO: authenticate
	require(_user_type <= ARBITER_TYPE, "Unknown User Type");
	user_address = _user_address;
	user_type = _user_type;
	metahash_listing.push(_metahash);
	date_created = now;
	date_updated = now;
    }

    
    /// @dev Update the given comment with a revision, replaces the latest metahash.
    function update(string calldata _metahash) external {
	require(msg.sender == user_address, "Only the original user can edit the comment.");
	metahash_listing.push(_metahash);
	date_updated = now;
    }


    /// @dev Get the number of comment revisions.
    function getCount() external returns(uint) {
	return metahash_listing.length;
    }


    /// @dev Get the comment or comment revision at the given index
    function getRevisionByIndex(uint _index) external returns(string memory) {
	require(_index < metahash_listing.length, "Given index is out of bounds.");
	return metahash_listing[_index];
    }


    /// @dev Get the comment or latest comment revision.
    function getLast() external returns(string memory) {
	return metahash_listing[metahash_listing.length - 1];
    }
}
