pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceUserFactory.sol";
import "./EthlanceUser.sol";
import "./EthlanceJobStore.sol";
import "proxy/MutableForwarder.sol";


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

    // The EthlanceJobStore contains additional data about our
    // contract.
    EthlanceJobStore public store_instance;

    // The candidate linked to this contract
    address public accepted_candidate;

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
    function construct(EthlanceJobStore _store_instance, bool is_employer_request)
	external {
	// require(registry.checkFactoryPrivilege(msg.sender), "You are not privileged to carry out construction.");

	// Main members
	store_instance = _store_instance;
	date_created = now;
	date_updated = now;
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
        //isEmployer(msg.sender)
    {
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
	require(store_instance.accepted_arbiter() == _address,
		"Given user is not the accepted arbiter.");
	_;
    }


    /// @dev Checks if the given address is a registered user
    modifier isRegisteredUser(address _address) {
	require(registry.getUserByAddress(_address) != 0,
		"Given address is not a registered user.");
	_;
    }


    /// @dev Checks if the given address is a registered employer
    modifier isRegisteredEmployer(address _address) {
	var (is_registered) = EthlanceUser(registry.getUserByAddress(_address)).getEmployerData();
	require(is_registered,
		"Given address is not a registered employer.");
	_;
    }


    /// @dev Checks if the given address is a registered candidate
    modifier isRegisteredCandidate(address _address) {
	var (is_registered,,) = EthlanceUser(registry.getUserByAddress(_address)).getCandidateData();
	require(is_registered,
		"Given address is not a registered candidate.");
	_;
    }


    /// @dev Checks if the given address is a registered arbiter
    modifier isRegisteredArbiter(address _address) {
	var (is_registered,,,) = EthlanceUser(registry.getUserByAddress(_address)).getArbiterData();
	require(is_registered,
		"Given address is not a registered arbiter.");
	_;
    }
}
