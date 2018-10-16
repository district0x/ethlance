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
    }

    /// Represents a particular candidate requesting, or being
    /// requested by an employer for a job contract.
    struct CandidateRequest {
	bool is_employer_request;
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

    // IPFS Hash for additional job contract data.
    string public metahash_ipfs;

    // If it is a bounty, the reward value to gain from completing
    // the contract.
    uint public reward_value; // wei units (?)

    //
    // Collections
    //

    // Arbiter Requests
    ArbiterRequest[] public arbiter_request_listing;

    // Candidate Requests
    CandidateRequest[] public candidate_request_listing;

    /// @dev Forwarder Constructor
    function construct(address _employer_address,
		       uint8 _bid_option,
		       uint _estimated_length_seconds,
		       bool _include_ether_token,
		       bool _is_bounty,
		       bool _is_invitation_only,
		       string _metahash_ipfs,
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
	metahash_ipfs = _metahash_ipfs;
	reward_value = _reward_value;
    }

    //
    // Methods
    //


    /// @dev Update the last time the job was updated.
    function updateDateUpdated()
	private {
	date_updated = now;
    }


    /// @dev Update the job contract metahash
    /// @param _metahash The new metahash
    function updateMetahash(string _metahash)
	public {
	// FIXME: authorization
	metahash_ipfs = _metahash;
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
    /// @param arbiter_address Address of the accepted arbiter.
    function setAcceptedArbiter(address arbiter_address)
	public
	//FIXME: has to be employer
    {
	accepted_arbiter = arbiter_address;
    }


    /// @dev Set the accepted candidate
    /// @param candidate_address The accepted candidate
    function setAcceptedCandidate(address candidate_address)
	public
    {
	accepted_candidate = candidate_address;
    }


    /// @dev Add a candidate to the candidate request listing.
    /// @param candidate_address The user address of the candidate.
    function requestCandidate(address candidate_address)
	public {
	
    }

    
    /// @dev Add an arbiter to the arbiter request listing.
    /// @param arbiter_address The user address of the arbiter.
    function requestArbiter(address arbiter_address)
	public {
	
    }

    
}
