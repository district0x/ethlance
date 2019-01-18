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
    // Constants
    //

    // User Type Constants
    uint public constant GUEST_TYPE = 0;
    uint public constant EMPLOYER_TYPE = 1;
    uint public constant CANDIDATE_TYPE = 2;
    uint public constant ARBITER_TYPE = 3;


    //
    // Structs
    //
    struct Feedback {
	// Address of the user leaving feedback
	address from_user_address;
	
	// Address of the user receiving feedback
	address to_user_address;

	// User Role of the user leaving feedback
	uint from_user_type;

	// User Role of the user receiving feedback
	uint to_user_type;

	// Additional Metadata attached to the contract via IPFS
	string metahash;
	
	// Feedback, 0-5 Star Rating
	uint rating;

	// Latest feedback update
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
    function update(address from_user_address,
		    address to_user_address,
		    uint from_user_type,
		    uint to_user_type,
		    string calldata metahash,
		    uint rating) external {
	require(msg.sender == owner, "Must be contract owner to update feedback.");
	require(rating <= 5, "Rating must be a value between 0 and 5.");

	Feedback memory feedback = Feedback({
	    from_user_address: from_user_address,
	    to_user_address: to_user_address,
	    from_user_type: from_user_type,
            to_user_type: to_user_type,
	    metahash: metahash,
            rating: rating,
	    date_updated: now
	});

	// It's a user that has chosen to leave new feedback.
	if (feedback_mapping[from_user_address] > 0) {
	    feedback_listing[feedback_mapping[from_user_address]-1] = feedback;
	}
	// New user leaving feedback.
	else {
	    feedback_listing.push(feedback);
	    feedback_mapping[from_user_address] = feedback_listing.length;
	}
    }


    /// @dev Get the number of feedback entries
    function getFeedbackCount() public view returns(uint) {
	return feedback_listing.length;
    }


    /// @dev Get the feedback at the given index.
    function getFeedbackByIndex(uint _index)
	public view
	returns(address from_user_address,
		address to_user_address,
		uint from_user_type,
		uint to_user_type,
		string memory metahash,
		uint rating,
		uint date_updated) {
	require(_index < feedback_listing.length, "Given index is out of bounds.");
	Feedback memory feedback = feedback_listing[_index];
	
	from_user_address = feedback.from_user_address;
	to_user_address = feedback.to_user_address;
	from_user_type = feedback.from_user_type;
	to_user_type = feedback.to_user_type;
	metahash = feedback.metahash;
	rating = feedback.rating;
	date_updated = feedback.date_updated;
    }
}
