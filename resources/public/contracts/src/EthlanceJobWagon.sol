pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";

/// @title Create Job Contracts as an assigned Employer as a group of
/// identical contracts.
contract EthlanceJobWagon {
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

    /// Represents a particular candidate requesting, or being
    /// requested by an employer for a job contract.
    struct CandidateRequest {
	bool is_employer_request;
	address candidate_address;
    }

    //
    // Members
    //

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
    
    // Employer who created the job contract
    address public employer_address;

    // Estimated amount of time to finish the contract (in seconds)
    uint public estimated_length_seconds;

    // If true, additionally include ether as a token to pay out.
    bool public include_ether_token;

    // If true, only employers can request candidates and arbiters
    bool public is_invitation_only;

    // Job wagon has a certain number of worker contracts in circulation
    uint worker_max_contracts;

    //
    // Collections
    //

    // Arbiter Requests
    ArbiterRequest[] public arbiter_request_listing;
    mapping(address => uint) public arbiter_request_mapping;

    // Candidate Requests
    CandidateRequest[] public candidate_request_listing;
    mapping(address => uint) public candidate_request_mapping;

    // Job Worker Listing
    address[] public job_worker_listing;

    
    function construct(string _metahash)
      public {
	
    }

    /// @dev Set the accepted arbiter
    /// @param arbiter_address User address of the accepted arbiter.
    function setAcceptedArbiter(address arbiter_address)
	private {
	accepted_arbiter = arbiter_address;

	// The contract starts when both the accepted arbiter and the
	// accepted candidate roles are filled.
	if (accepted_candidate != 0) {
	    date_started = now;
	}

	// Fire off event
	uint[] memory event_data = new uint[](1);
	event_data[0] = EthlanceUser(registry.getUserByAddress(arbiter_address)).user_id();
	fireEvent("JobArbiterAccepted", event_data);
    }


    /// @dev Set the accepted candidate
    /// @param candidate_address User address of the accepted candidate.
    function setAcceptedCandidate(address candidate_address, uint _worker_index)
	private {
	accepted_candidate = candidate_address;

	// The contract starts when both the accepted arbiter and the
	// accepted candidate roles are filled.
	if (accepted_arbiter != 0) {
	    date_started = now;
	}

	// Fire off event
	uint[] memory event_data = new uint[](1);
	event_data[0] = EthlanceUser(registry.getUserByAddress(candidate_address)).user_id();
	fireEvent("JobCandidateAccepted", event_data);
    }


    /// @dev Add a candidate to the candidate request listing.
    /// @param candidate_address The user address of the candidate.
    /*

      Functionality changes based on who is requesting the candidate,
      and the status of the requested candidate.

      Case 1: Employer requests a Candidate. (msg.sender == employer_address)

      Case 2: Candidate requests himself. (msg.sender == candidate_address)

      accepted_candidate is set if:

      - employer had already requested the candidate, and then the candidate requests the job contract
      - candidate requests himself, and then the employer requests the same candidate.

     */
    function requestCandidate(address candidate_address)
	public
	isRegisteredUser(candidate_address)
        isRegisteredCandidate(candidate_address) {
	require(accepted_candidate == 0, "Candidate already accepted.");
	require(candidate_address != accepted_arbiter,
		"Accepted Arbiter cannot be an Accepted Candidate.");
	require(candidate_address != employer_address,
		"Employer cannot be the candidate of his own job contract.");
	require(msg.sender == employer_address || msg.sender == candidate_address,
		"Only an employer can request a candidate, only a candidate can request themselves.");

	// Locals
	uint request_index;
	bool is_employer_request;

	//
	// Handle case where candidate is requesting the job contract.
	//

	if (msg.sender == candidate_address) {
	    // No previous request, so create a new Candidate Request
	    if (candidate_request_mapping[candidate_address] == 0) {
		candidate_request_listing.push(CandidateRequest(false, candidate_address));
		candidate_request_mapping[candidate_address] = candidate_request_listing.length;
		return;
	    }

	    // Was a previous request, check if an employer requested this candidate
	    request_index = candidate_request_mapping[candidate_address] - 1;
	    is_employer_request = candidate_request_listing[request_index].is_employer_request;
	    
	    // If this candidate was already requested by the employer, we have our accepted candidate
	    if (is_employer_request) {
		setAcceptedCandidate(candidate_address);
		return;
	    }

	    // Otherwise, we revert, since this candidate already made a request
	    revert("Candidate has already made a request");
	    return;
	}

	//
	// Handle case where employer is requesting a candidate for the job contract.
	//

	// No previous request, so create a new Candidate Request
	if (candidate_request_mapping[candidate_address] == 0) {
	    candidate_request_listing.push(CandidateRequest(true, candidate_address));
	    candidate_request_mapping[candidate_address] = candidate_request_listing.length;
	    return;
	}

	// Was a previous request, check if a candidate already requested this job.
	request_index = candidate_request_mapping[candidate_address] - 1;
	is_employer_request = candidate_request_listing[request_index].is_employer_request;

	// If this candidate already requested this job, we have our accepted candidate
	if (!is_employer_request) {
	    setAcceptedCandidate(candidate_address);
	    return;
	}

	// Otherwise, we revert, since the employer already requested this candidate
	revert("Employer has already requested this candidate.");
	return;
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
	isRegisteredUser(arbiter_address)
	isRegisteredArbiter(arbiter_address) {
	require(accepted_arbiter == 0, "Arbiter already accepted.");
	require(arbiter_address != accepted_candidate,
		"Accepted Candidate cannot be an Accepted Arbiter");
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

 
    /// @dev Returns the number of requested candidates for this job contract
    /// @return The number of requested candidates
    function getRequestedCandidateCount()
	public view returns(uint) {
	return candidate_request_listing.length;
    }


    /// @dev Get the candidate request data in the candidate request listing.
    /// @param index The index of the CandidateRequest within the listing.
    /// @return 2-element tuple containing the candidate data.
    function getRequestedCandidateByIndex(uint index)
	public view returns(bool is_employer_request,
			    address candidate_address) {
	require(index < candidate_request_listing.length,
		"Given index out of bounds.");
	CandidateRequest memory candidateRequest = candidate_request_listing[index];
	is_employer_request = candidateRequest.is_employer_request;
	candidate_address = candidateRequest.candidate_address;
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
