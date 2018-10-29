pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceUserFactory.sol";
import "./EthlanceUser.sol";
import "./EthlanceJobStore.sol";
import "./EthlanceDispute.sol";
import "./EthlanceInvoice.sol";
import "proxy/MutableForwarder.sol";
import "proxy/Forwarder.sol";
import "proxy/SecondForwarder.sol";


/// @title Job Contracts to tie candidates, employers, and arbiters to
/// an agreement.
contract EthlanceWorkContract {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Structures
    //

    // Nothing here yet.

    //
    // Members
    //

    // The status of contract with respect to the employer and the
    // candidate's interactions.
    //
    // Notes:
    //
    // - The overall status of the contract will also be reflected
    //   with respect to any open disputes.
    //
    // Status Codes:
    // -----------
    // 0 -> Initial
    // --
    // 1 -> Open Candidate Request
    // 2 -> Open Employer Request
    // 3 -> Open Bounty
    // --
    // 4 -> Accepted
    // 5 -> Rejected
    // --
    // 6 -> In Progress
    // 7 -> On Hold
    // --
    // 8 -> Finished
    // 9 -> Cancelled
    uint public contract_status;
    uint public constant CONTRACT_STATUS_OPEN_CANDIDATE_REQUEST = 1;
    uint public constant CONTRACT_STATUS_OPEN_EMPLOYER_REQUEST = 2;
    uint public constant CONTRACT_STATUS_OPEN_BOUNTY = 3;
    uint public constant CONTRACT_STATUS_ACCEPTED = 4;
    uint public constant CONTRACT_STATUS_REJECTED = 5;
    uint public constant CONTRACT_STATUS_IN_PROGRESS = 6;
    uint public constant CONTRACT_STATUS_ON_HOLD = 7;
    uint public constant CONTRACT_STATUS_FINISHED = 8;
    uint public constant CONTRACT_STATUS_CANCELLED = 9;
    

    // The EthlanceJobStore contains additional data about our
    // contract.
    EthlanceJobStore public store_instance;

    // The candidate linked to this contract
    address public candidate_address;

    uint public date_created;
    uint public date_updated;

    //
    // Collections
    //

    // Stores a listing of appended data by the employer, candidate,
    // and arbiter. Each listing allows us to isolate malicious intent
    // between parties.
    string[] public employer_metahash_listing;
    string[] public candidate_metahash_listing;
    string[] public arbiter_metahash_listing;
    
    // Invoice Listing
    address[] public invoice_listing;
    
    // Dispute Listing
    address[] public dispute_listing;


    /// @dev Forwarder Constructor
    function construct(EthlanceJobStore _store_instance,
		       address _candidate_address,
		       bool is_employer_request)
	external {
	// require(registry.checkFactoryPrivilege(msg.sender), "You are not privileged to carry out construction.");

	// Main members
	store_instance = _store_instance;
	candidate_address = _candidate_address;
	date_created = now;
	date_updated = now;

	// Update the contract status based on bounty, or employer request
	if (store_instance.bid_option() == store_instance.BID_OPTION_BOUNTY()) {
	    contract_status = CONTRACT_STATUS_OPEN_BOUNTY;
	}
	else if (is_employer_request) {
	    contract_status = CONTRACT_STATUS_OPEN_EMPLOYER_REQUEST;
	}
	else {
	    contract_status = CONTRACT_STATUS_OPEN_CANDIDATE_REQUEST;
	}
    }

    //
    // Methods
    //

    /// @dev Update the datetime of the job contract.
    function updateDateUpdated()
	private {
	date_updated = now;
    }


    /// @dev Update the employer's metahash
    /// @param _metahash The new metahash
    function appendEmployerMetahash(string _metahash)
	public
        isEmployer(msg.sender) {
	//emit UpdatedEmployerMetahash(metahash_store.employer_hash, _metahash);
	employer_metahash_listing.push(_metahash);
	updateDateUpdated();
    }


    /// @dev Update the candidate's metahash
    /// @param _metahash The new metahash
    function appendCandidateMetahash(string _metahash)
	public
        //isAcceptedCandidate(msg.sender)
    {
	//emit UpdatedCandidateMetahash(metahash_store.candidate_hash, _metahash);
	candidate_metahash_listing.push(_metahash);
	updateDateUpdated();
    }


    /// @dev Update the arbiter's metahash
    /// @param _metahash The new metahash
    function appendArbiterMetahash(string _metahash)
	public
        //isAcceptedArbiter(msg.sender)
    {
	//emit UpdatedArbiterMetahash(metahash_store.arbiter_hash, _metahash);
	arbiter_metahash_listing.push(_metahash);
	updateDateUpdated();
    }


    /// @dev Fire events specific to the work contract
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string event_name, uint[] event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }

    //
    // Modifiers
    //
    
    /// @dev Checks if it is the employer of the job contract.
    /// @param _address The user address of the employer.
    modifier isEmployer(address _address) {
	require(store_instance.employer_address() == _address,
		"Given user is not the employer.");
	_;
    }

   




}
