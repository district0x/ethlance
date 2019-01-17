pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";

/// @title Represents feedback for a given contract.
/*
  Notes:

  - EthlanceFeedback is setup to be composable.

  - Feedback contract is owned by the contract that constructs it,
    which then decides on who can leave feedback.

  - Feedback can only be left once per a user address, and can be
    updated from then on.
 */
contract EthlanceFeedback {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Structs
    //
    struct Feedback {
	address user_address;
	string metahash;
	uint rating; // 0-5 Star Rating
	uint date_updated;
    }


    //
    // Members
    //
    address owner;
    mapping(address => uint) feedback_mapping;
    Feedback[] feedback_listing;

    
    /// @dev Forwarder Constructor
    function construct(address _owner) public {
	owner = _owner;
    }


    /// @dev Update the given feedback.
    /*
      Notes:

      - Feedback can only be updated by the owner, this method should
        be superceded by the owner contract.
     */
    function update(address user_address, string calldata metahash, uint rating) external {
	require(msg.sender == owner, "Must be contract owner to update feedback.");
	require(rating <= 5, "Rating must be a value between 0 and 5.");

	Feedback memory feedback = Feedback(user_address, metahash, rating, now);

	// It's a user that has chosen to leave new feedback.
	if (feedback_mapping[user_address] > 0) {
	    feedback_listing[feedback_mapping[user_address]-1] = feedback;
	}
	// New user leaving feedback.
	else {
	    feedback_listing.push(feedback);
	    feedback_mapping[user_address] = feedback_listing.length;
	}
    }


    /// @dev Get the number of feedback entries
    function getFeedbackCount() external returns(uint) {
	return feedback_listing.length;
    }


    /// @dev Get the feedback at the given index.
    function getFeedbackByIndex(uint _index)
	external
	returns(address user_address,
		string memory metahash,
		uint rating,
		uint date_updated) {
	require(_index < feedback_listing.length, "Given index is out of bounds.");
	Feedback memory feedback = feedback_listing[_index];
	
	user_address = feedback.user_address;
	metahash = feedback.metahash;
	rating = feedback.rating;
	date_updated = feedback.date_updated;
    }
}
