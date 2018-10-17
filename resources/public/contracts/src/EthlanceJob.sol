pragma solidity ^0.4.24;

import "./EthlanceJobInvoice.sol";
import "./EthlanceJobDispute.sol";
import "./EthlanceRegistry.sol";
import "./EthlanceJobToken.sol";
import "proxy/MutableForwarder.sol";

/// @title Job Contracts to tie candidates, employers, and arbiters to
/// an agreement.
contract EthlanceJob is  EthlanceJobToken,
                         EthlanceJobInvoice,
                         EthlanceJobDispute
{
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

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

    /// Each of employer, candidate, and arbiter have solo privileged
    /// IPFS metahash stores.
    struct MetaHashStore {
	string employer_hash;
	string candidate_hash;
	string arbiter_hash;
    }

    //
    // Members
    //

    address public accepted_arbiter;
    address public accepted_candidate;

    /// Bid Option Enumeration
    // 0 - Hourly Rate
    // 1 - Fixed Price
    // 2 - Annual Salary
    uint8 public bid_option;

    // Datetime of job contract creation
    uint public date_created;

    // Datetime of the last time the job contract was updated.
    uint public date_updated;

    // Datetime of job contract obtaining an accepted_candidate and an
    // accepted_arbiter.
    uint public date_started;

    // Datetime of job contract finishing
    uint public date_finished;
    
    // Employer who created the job contract
    address public employer_address;

    // Estimated amount of time to finish the contract (in seconds)
    uint public estimated_length_seconds;

    // If true, additionally include ether as a token to pay out.
    bool public include_ether_token;

    // If true, job contract is a bounty contract
    bool public is_bounty;

    // If true, only employers can request candidates and arbiters
    bool public is_invitation_only;

    // IPFS MetaHashes for additional job contract data.
    MetaHashStore public metahash_store;

    // If it is a bounty, the reward value to gain from completing
    // the contract.
    uint public reward_value; // wei units (?)

    //
    // Collections
    //

    // Arbiter Requests
    ArbiterRequest[] public arbiter_request_listing;
    mapping(address => uint) public arbiter_request_mapping;

    // Candidate Requests
    CandidateRequest[] public candidate_request_listing;
    mapping(address => uint) public candidate_request_mapping;


    /// @dev Forwarder Constructor
    function construct(address _employer_address,
		       uint8 _bid_option,
		       uint _estimated_length_seconds,
		       bool _include_ether_token,
		       bool _is_bounty,
		       bool _is_invitation_only,
		       string _employer_metahash,
		       uint _reward_value)
	external {
	require(registry.checkFactoryPrivilege(msg.sender),
		"You are not privileged to carry out construction.");

	// Satisfy our inherited classes
	setInvoiceEventDispatcher(registry);
	setDisputeEventDispatcher(registry);

	// Main members
	bid_option = _bid_option;
	date_created = now;
	date_updated = now;
	employer_address = _employer_address;
	estimated_length_seconds = _estimated_length_seconds;
	include_ether_token = _include_ether_token;
	is_bounty = _is_bounty;
	is_invitation_only = _is_invitation_only;
	metahash_store.employer_hash = _employer_metahash;
	reward_value = _reward_value;
    }

    //
    // Methods
    //


    /// @dev Based on filled 
    /// @return Status Code
    function getStatus()
	public 
        returns(uint) {
	return 0;
    }


    /// @dev Update the datetime of the job contract.
    function updateDateUpdated()
	private {
	date_updated = now;
    }


    /// @dev Update the employer's metahash
    /// @param _metahash The new metahash
    function updateEmployerMetahash(string _metahash)
	public
        isEmployer(msg.sender) {
	metahash_store.employer_hash = _metahash;
	updateDateUpdated();
    }


    /// @dev Update the candidate's metahash
    /// @param _metahash The new metahash
    function updateCandidateMetahash(string _metahash)
	public
        isAcceptedCandidate(msg.sender) {
	metahash_store.candidate_hash = _metahash;
	updateDateUpdated();
    }


    /// @dev Update the arbiter's metahash
    /// @param _metahash The new metahash
    function updateArbiterMetahash(string _metahash)
	public
        isAcceptedArbiter(msg.sender) {
	metahash_store.arbiter_hash = _metahash;
	updateDateUpdated();
    }


    /// @dev Fire events specific to the job contract
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string event_name, uint[] event_data) private {
	registry.fireEvent(event_name, version, event_data);
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
    }


    /// @dev Set the accepted candidate
    /// @param candidate_address User address of the accepted candidate.
    function setAcceptedCandidate(address candidate_address)
	private {
	accepted_candidate = candidate_address;

	// The contract starts when both the accepted arbiter and the
	// accepted candidate roles are filled.
	if (accepted_arbiter != 0) {
	    date_started = now;
	}
    }


    /// @dev Add a candidate to the candidate request listing.
    /// @param candidate_address The user address of the candidate.
    /*

      Functionality changes based on who is requesting the candidate,
      and the status of the requested candidate.

      Case 1: Employer requests a Candidate. (msg.sender == employer_address)

      Case 2: Candidate requests himself. (msg.sender == candidate_address)

      accepted_candidate is set if:

      - employer had already requested the candidate, and the candidate requests the job contract
      - candidate requests himself, and the employer requests the same candidate.

     */
    function requestCandidate(address candidate_address)
	public {
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
	public {
	require(arbiter_address != employer_address,
		"Employer cannot be the arbiter of his own job contract.");
	require(msg.sender == employer_address || msg.sender == arbiter_address,
		"Only an employer can request a arbiter, only an arbiter can request themselves.");

	// Locals
	uint request_index;
	bool is_employer_request;

	//
	// Handle case where arbiter is requesting the job contract.
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
	// Handle case where employer is requesting a arbiter for the job contract.
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

 
    //
    // Modifiers
    //

    
    /// @dev Checks if it is the employer of the job contract.
    /// @param _address The user address of the employer.
    modifier isEmployer(address _address) {
	require(employer_address == _address,
		"Given user is not the employer.");
	_;
    }

   
    /// @dev Checks if it is the accepted candidate of the job contract.
    /// @param _address The user address of the accepted candidate.
    modifier isAcceptedCandidate(address _address) {
	require(accepted_candidate == _address,
		"Given user is not the accepted candidate.");
	_;
    }


    /// @dev Checks if it is the accepted arbiter of the job contract
    /// @param _address The user address of the accepted arbiter.
    modifier isAcceptedArbiter(address _address) {
	require(accepted_arbiter == _address,
		"Given user is not the accepted arbiter.");
	_;
    }
}
