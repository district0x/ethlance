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

  - EthlanceFeedback objects exist in the WorkContract, and the
    Dispute. Although they are stored separately, they are usually
    unionized when stored outside of the smart contracts. This is why
    there is no dispute_index key present during construction.
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

	// When the feedback was created
	uint date_created;

	// Latest feedback update
	uint date_updated;
    }


    //
    // Members
    //
    address public owner;
    mapping(address => uint) feedback_mapping;
    Feedback[] feedback_listing;

    uint public job_index;
    uint public work_index;

    
    /// @dev Forwarder Constructor
    /// @param _owner The owning address for this feedback {EthlanceWorkContract, EthlanceDispute}
    /// @param _job_index The EthlanceJobStore Index for the feedback
    /// @param _work_index The EthlanceWorkContract Index for the feedback
    function construct(address _owner, uint _job_index, uint _work_index) public {
	require(owner != address(0), "EthlanceFeedback contract was already constructed");
	owner = _owner;
	job_index = _job_index;
	work_index = _work_index;
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
	    date_created: now,
	    date_updated: now
	});


	uint feedback_id = feedback_mapping[from_user_address];
	uint feedback_index; // minus one of feedback ID

	// It's an existing user that has chosen to leave updated feedback.
	if (feedback_id > 0) {
	    feedback_index = feedback_id - 1;
	    feedback_listing[feedback_index] = feedback;
	    fireEvent("FeedbackUpdated", feedback_index);
	}
	// New user leaving feedback.
	else {
	    feedback_listing.push(feedback);

	    feedback_id = feedback_listing.length;
	    feedback_index = feedback_id - 1;
	    
	    feedback_mapping[from_user_address] = feedback_id;
	    fireEvent("FeedbackCreated", feedback_index);
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
                uint date_created,
		uint date_updated) {
	require(_index < feedback_listing.length, "Given index is out of bounds.");
	Feedback memory feedback = feedback_listing[_index];
	
	from_user_address = feedback.from_user_address;
	to_user_address = feedback.to_user_address;
	from_user_type = feedback.from_user_type;
	to_user_type = feedback.to_user_type;
	metahash = feedback.metahash;
	rating = feedback.rating;
	date_created = feedback.date_created;
	date_updated = feedback.date_updated;
    }


    /// @dev Fire events specific to the feedback.
    /// @param event_name Unique name to give the fired event
    /// @param _index The feedback index of the feedback object throwing the event.
    /*
      Notes:

      - EthlanceFeedback is a store of feedback objects
     */
    function fireEvent(string memory event_name, uint _index) private {
	uint[] memory event_data = new uint[](3);
	event_data[0] = job_index;
	event_data[1] = work_index;
	event_data[2] = _index;

	registry.fireEvent(event_name, version, event_data);
    }
}
