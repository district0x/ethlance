pragma solidity ^0.5.0;

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
    address payable public accepted_arbiter;

    /// Bid Option Enumeration
    // 0 - Hourly Rate
    // 1 - Fixed Price
    // 2 - Annual Salary
    // 3 - Bounty
    uint8 public bid_option;
    uint8 public constant BID_OPTION_BOUNTY = 3;

    // Datetime of job contract creation
    uint public date_created;

    // Datetime of the last time the job contract was updated.
    uint public date_updated;

    // Datetime of job contract finishing
    uint public date_finished;
    
    // Employer assigned to the given JobStore.
    address payable public employer_address;

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

    // Work Contract Listing
    address[] public work_contract_listing;
    mapping(address => bool) public work_contract_mapping;
    

    function construct(address payable _employer_address,
		       uint8 _bid_option,
		       uint _estimated_length_seconds,
		       bool _include_ether_token,
		       bool _is_invitation_only,
		       string memory _metahash,
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
    function fireEvent(string memory event_name, uint[] memory event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }


    /// @dev Set the accepted arbiter for the current Job Wagon.
    /// @param arbiter_address User address of the accepted arbiter.
    function setAcceptedArbiter(address payable arbiter_address)
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
    /*
      
      Only the employer or the candidate can request a work
      contract. Contract status upon construction depends on who
      requests a work contract, or whether the Job is bounty-based.

     */
    function requestWorkContract(address payable candidate_address)
	public {
	require(registry.isRegisteredUser(candidate_address),
		"Given address is not a registered user.");
	require(msg.sender == employer_address || msg.sender == candidate_address,
		"ERROR: The employer can request a work contract for a candidate. The candidate can request a work contract for himself.");
	require(work_contract_mapping[candidate_address] == false,
		"Candidate already has a work contract created.");
	require(employer_address != candidate_address,
		"Employer cannot work on his own Job.");

	// Create the forwarded contract, and place in the work listing.
	Forwarder fwd = new Forwarder(); // Proxy Contract with
	                               // target(EthlanceWorkContract)
	EthlanceWorkContract workContract = EthlanceWorkContract(address(fwd));
	work_contract_listing.push(address(workContract));
	work_contract_mapping[candidate_address] = true;

	// Determine if it's an employer or a candidate request
	bool is_employer_request = false;
	if (msg.sender == employer_address) {
	    is_employer_request = true;
	}

	// Construct the work contract.
	workContract.construct(this, candidate_address, is_employer_request);
	
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
    function requestArbiter(address payable arbiter_address)
	public {
	require(registry.isRegisteredUser(arbiter_address),
		"Given address is not a registered user.");
	require(accepted_arbiter == address(0), "Arbiter already accepted.");
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


    /// @dev Get the current number of work contracts
    /// @return The number of work contracts in the job store.
    function getWorkContractCount()
	public view returns(uint) {
	return work_contract_listing.length;
    }


    /// @dev Returns the WorkContract address at the given index.
    /// @param index The index of the Work Contract to be retrieved.
    /// @return The address of the EthlanceWorkContract.
    function getWorkContractByIndex(uint index)
	public view returns(address) {
	require(index < work_contract_listing.length, "Given index is out of bounds.");
	return work_contract_listing[index];
    }

    
    /// @dev Returns true if the given _work_contract instance resides
    /// in the current job_store.
    function isWorkContract(address _work_contract) private returns(bool) {
	for (uint i = 0; i < work_contract_listing.length; i++) {
	    if (address(work_contract_listing[i]) == _work_contract) {
		return true;
	    }
	}
	return false;
    }

    
    /// @dev Main function for resolving a dispute between an employer and a candidate.
    /// @param employer_amount Amount to give the employer for dispute resolution.
    function resolveDispute(uint employer_amount,
			    address employer_token,
			    uint candidate_amount,
			    address candidate_token,
			    uint arbiter_amount,
			    address arbiter_token,
			    address payable candidate_address) external {
	require(employer_token == address(0) &&
		candidate_token == address(0) &&
		arbiter_token == address(0),
		"ERC20 Tokens are not implemented.");
	
	//FIXME: safemath, ERC20 compatible
	uint total_payout = employer_amount + candidate_amount + arbiter_amount;
	if (address(this).balance < total_payout) {
	    revert("Work Contract balance does not satify resolution payout.");
	}
	
	employer_address.transfer(employer_amount);
	candidate_address.transfer(candidate_amount);
	accepted_arbiter.transfer(arbiter_amount);
    }


    /// @dev Main function for paying an invoice, propagated up from EthlanceInvoice.
    /// @param candidate_address The address of the person acquiring the payout.
    /// @param amount_paid The amount paid to the given candidate_address in Wei.
    /*
      Notes:

      - This function is a propagation from EthlanceWorkContract -->
        EthlanceInvoice. Access rights should be reflected in each
        step.
     */
    function payInvoice(address payable candidate_address, uint amount_paid) external {
	require(isWorkContract(msg.sender), "Only a work contract has permission to transfer from the job store.");
	candidate_address.transfer(amount_paid);
    }

    
    /// @dev Main function for paying an invoice with an ERC20 token, propagated from EthlanceInvoice.
    /// @param candidate_address The address of the person acquiring the payout.
    /// @param amount_paid The amount paid to the given candidate_address based on the ERC20 token contract amount.
    /// @param token_address Address of the ERC20 token contract.
    function payInvoice(address candidate_address, uint amount_paid, address token_address) external {
	revert("Not Implemented");
    }


    /// @dev Main method for funding ethereum to the given JobStore.
    /*
      Notes:

      - Anyone can fund a JobStore with ether as long as the employer
        has included the ether token (include_ether_token)

     */
    function fund() external payable {
	if (!include_ether_token) {
	    revert("Given JobStore is not ethereum fundable.");
	}
    }


    /// @dev Overload for funding the JobStore with an ERC20 token
    /// @param token_address Address of the ERC20 token contract.
    /// @param amount The amount of tokens to transfer to the current JobStore contract.
    /*
      Notes:

      - The token_address ERC20 token contract requires pre-approval
        from the person funding the contract. This will require a
        strictly client-side interaction for this pre-approval.
     */
    function fundToken(address token_address, uint amount) external {
	revert("Not Implemented");
    }
}
