pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";
import "./proxy/Forwarder.sol";

/// @title Create Job Contracts as an assigned Employer as a group of
/// identical contracts.
contract EthlanceJobStore {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Structures
    //

    /// Represents a particular arbiter requesting, or being requested
    /// by an employer for a job contract.
    struct ArbiterRequest {
	bool is_employer_request;
	address arbiter_address;
    }

    //
    // Members
    //

    // The Accepted Arbiter assigned to the Jobs within the Job Store.
    address public accepted_arbiter;

    /// Bid Option Enumeration
    // 0 - Hourly Rate
    // 1 - Fixed Price
    // 2 - Annual Salary
    // 3 - Bounty
    uint8 public bid_option;

    // Datetime of job contract creation
    uint public date_created;

    // Datetime of the last time the job contract was updated.
    uint public date_updated;

    // Datetime of job contract finishing
    uint public date_finished;
    
    // Employer assigned to the given JobStore.
    address public employer_address;

    // Estimated amount of time to finish the contract (in seconds)
    uint public estimated_length_seconds;

    // If true, additionally include ether as a token to pay out.
    bool public include_ether_token;
    // TODO: token_address_listing

    // If true, only employers can request candidates and arbiters
    bool public is_invitation_only;

    // Additional Job Information stored in IPFS Metahash
    string public metahash;

    // The reward value for a completed bounty
    uint public reward_value;

    //
    // Collections
    //

    // Arbiter Requests
    ArbiterRequest[] public arbiter_request_listing;
    mapping(address => uint) public arbiter_request_mapping;

    // Job Worker Listing
    address[] public job_worker_listing;
    mapping(address => uint) public job_worker_mapping;
    

    function construct(address _employer_address,
		       uint8 _bid_option,
		       uint _estimated_length_seconds,
		       bool _include_ether_token,
		       bool _is_invitation_only,
		       string _metahash,
		       uint _reward_value)
      public {
	employer_address = _employer_address;
	bid_option = _bid_option;
	estimated_length_seconds = _estimated_length_seconds;
	include_ether_token = _include_ether_token;
	is_invitation_only = _is_invitation_only;
	metahash = _metahash;
	reward_value = _reward_value;
    }


    /// @dev Fire events specific to the work contract
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string event_name, uint[] event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }


    /// @dev Set the accepted arbiter for the current Job Wagon.
    /// @param arbiter_address User address of the accepted arbiter.
    function setAcceptedArbiter(address arbiter_address)
	private {
	accepted_arbiter = arbiter_address;

	// The contract starts when both the accepted arbiter and the
	// accepted candidate roles are filled.
	//if (accepted_candidate != 0) {
	//    date_started = now;
	//}

	// Fire off event
	uint[] memory event_data = new uint[](1);
	event_data[0] = EthlanceUser(registry.getUserByAddress(arbiter_address)).user_id();
	fireEvent("JobArbiterAccepted", event_data);
    }


    /// @dev Request and create a pending contract between the Candidate and the Employer
    /// @param candidate_address The user address of the Candidate.
    function requestWorkContract(address candidate_address)
	public {
	
	
    }

    
    /// @dev Add an arbiter to the arbiter request listing.
    /// @param arbiter_address The user address of the arbiter.
    /*

      Functionality changes based on who is requesting the arbiter,
      and the status of the requested arbiter.

      Case 1: Employer requests a Arbiter. (msg.sender == employer_address)

      Case 2: Arbiter requests himself. (msg.sender == arbiter_address)

      accepted_arbiter is set if:

      - employer had already requested the arbiter, and the arbiter requests the job contract
      - arbiter requests himself, and the employer requests the same arbiter.

     */
    function requestArbiter(address arbiter_address)
	public
	//isRegisteredUser(arbiter_address)
    {
	require(accepted_arbiter == 0, "Arbiter already accepted.");
	//require(arbiter_address != accepted_candidate,
	//	"Accepted Candidate cannot be an Accepted Arbiter");
	require(arbiter_address != employer_address,
		"Employer cannot be the arbiter of his own job contract.");
	require(msg.sender == employer_address || msg.sender == arbiter_address,
		"Only an employer can request an arbiter, only an arbiter can request themselves.");

	// Locals
	uint request_index;
	bool is_employer_request;

	//
	// Handle case where an arbiter is requesting the job contract.
	//

	if (msg.sender == arbiter_address) {
	    // No previous request, so create a new Arbiter Request
	    if (arbiter_request_mapping[arbiter_address] == 0) {
		arbiter_request_listing.push(ArbiterRequest(false, arbiter_address));
		arbiter_request_mapping[arbiter_address] = arbiter_request_listing.length;
		return;
	    }

	    // Was a previous request, check if an employer requested this arbiter
	    request_index = arbiter_request_mapping[arbiter_address] - 1;
	    is_employer_request = arbiter_request_listing[request_index].is_employer_request;
	    
	    // If this arbiter was already requested by the employer, we have our accepted arbiter
	    if (is_employer_request) {
		setAcceptedArbiter(arbiter_address);
		return;
	    }

	    // Otherwise, we revert, since this arbiter already made a request
	    revert("Arbiter has already made a request");
	    return;
	}

	//
	// Handle case where employer is requesting an arbiter for the job contract.
	//

	// No previous request, so create a new Arbiter Request
	if (arbiter_request_mapping[arbiter_address] == 0) {
	    arbiter_request_listing.push(ArbiterRequest(true, arbiter_address));
	    arbiter_request_mapping[arbiter_address] = arbiter_request_listing.length;
	    return;
	}

	// Was a previous request, check if a arbiter already requested this job.
	request_index = arbiter_request_mapping[arbiter_address] - 1;
	is_employer_request = arbiter_request_listing[request_index].is_employer_request;

	// If this arbiter already requested this job, we have our accepted arbiter
	if (!is_employer_request) {
	    setAcceptedArbiter(arbiter_address);
	    return;
	}

	// Otherwise, we revert, since the employer already requested this arbiter
	revert("Employer has already requested this arbiter.");
	return;
    }


    /// @dev Returns the number of requested arbiters for this job contract
    /// @return The number of requested arbiters.
    function getRequestedArbiterCount()
	public view returns(uint) {
	return arbiter_request_listing.length;
    }

    
    /// @dev Get the arbiter request data in the arbiter request listing.
    /// @param index The index of the ArbiterRequest within the listing.
    /// @return 2-element tuple containing the arbiter data.
    function getRequestedArbiterByIndex(uint index)
	public view returns (bool is_employer_request,
			     address arbiter_address) {
	require(index < arbiter_request_listing.length,
		"Given index out of bounds.");
	ArbiterRequest memory arbiterRequest = arbiter_request_listing[index];
	is_employer_request = arbiterRequest.is_employer_request;
	arbiter_address = arbiterRequest.arbiter_address;
    }

}
