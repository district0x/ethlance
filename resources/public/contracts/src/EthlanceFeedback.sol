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
		address fromUserAddress;
	
		// Address of the user receiving feedback
		address toUserAddress;

		// User Role of the user leaving feedback
		uint fromUserType;

		// User Role of the user receiving feedback
		uint toUserType;

		// Additional Metadata attached to the contract via IPFS
		string metahash;
	
		// Feedback, 0-5 Star Rating
		uint rating;

		// When the feedback was created
		uint dateCreated;

		// Latest feedback update
		uint dateUpdated;
	}


	//
	// Members
	//
	address public owner;
	mapping(address => uint) feedbackMapping;
	Feedback[] feedbackListing;

	uint public jobIndex;
	uint public workIndex;

    
	/// @dev Forwarder Constructor
	/// @param _owner The owning address for this feedback {EthlanceWorkContract, EthlanceDispute}
	/// @param _jobIndex The EthlanceJobStore Index for the feedback
	/// @param _workIndex The EthlanceWorkContract Index for the feedback
	function construct(address _owner, uint _jobIndex, uint _workIndex) public {
		require(owner == address(0), "EthlanceFeedback contract was already constructed");
		owner = _owner;
		jobIndex = _jobIndex;
		workIndex = _workIndex;
	}


	/// @dev Update the given feedback.
	/*
		Notes:

		- Feedback can only be updated by the owner, this method should
		be superceded by the owner contract.
	*/
	function update(address fromUserAddress,
									address toUserAddress,
									uint fromUserType,
									uint toUserType,
									string calldata metahash,
									uint rating) external {
		//require(msg.sender == owner, "Must be contract owner to update feedback.");
		require(rating <= 5, "Rating must be a value between 0 and 5.");

		Feedback memory feedback = Feedback({
	    fromUserAddress: fromUserAddress,
					toUserAddress: toUserAddress,
					fromUserType: fromUserType,
					toUserType: toUserType,
					metahash: metahash,
					rating: rating,
					dateCreated: now,
					dateUpdated: now
					});


		uint feedbackId = feedbackMapping[fromUserAddress];
		uint feedbackIndex; // minus one of feedback ID

		// It's an existing user that has chosen to leave updated feedback.
		if (feedbackId > 0) {
	    feedbackIndex = feedbackId - 1;
	    feedbackListing[feedbackIndex] = feedback;
	    fireEvent("FeedbackUpdated", feedbackIndex);
		}
		// New user leaving feedback.
		else {
	    feedbackListing.push(feedback);

	    feedbackId = feedbackListing.length;
	    feedbackIndex = feedbackId - 1;
	    
	    feedbackMapping[fromUserAddress] = feedbackId;
	    fireEvent("FeedbackCreated", feedbackIndex);
		}
	}


	/// @dev Get the number of feedback entries
	function getFeedbackCount() public view returns(uint) {
		return feedbackListing.length;
	}


	/// @dev Get the feedback at the given index.
	function getFeedbackByIndex(uint _index)
		public view
		returns(address fromUserAddress,
						address toUserAddress,
						uint fromUserType,
						uint toUserType,
						string memory metahash,
						uint rating,
						uint dateCreated,
						uint dateUpdated) {
		require(_index < feedbackListing.length, "Given index is out of bounds.");
		Feedback memory feedback = feedbackListing[_index];
	
		fromUserAddress = feedback.fromUserAddress;
		toUserAddress = feedback.toUserAddress;
		fromUserType = feedback.fromUserType;
		toUserType = feedback.toUserType;
		metahash = feedback.metahash;
		rating = feedback.rating;
		dateCreated = feedback.dateCreated;
		dateUpdated = feedback.dateUpdated;
	}


	/// @dev Fire events specific to the feedback.
	/// @param eventName Unique name to give the fired event
	/// @param _index The feedback index of the feedback object throwing the event.
	/*
		Notes:

		- EthlanceFeedback is a store of feedback objects
	*/
	function fireEvent(string memory eventName, uint _index) private {
		uint[] memory eventData = new uint[](3);
		eventData[0] = jobIndex;
		eventData[1] = workIndex;
		eventData[2] = _index;

		registry.fireEvent(eventName, version, eventData);
	}
}
