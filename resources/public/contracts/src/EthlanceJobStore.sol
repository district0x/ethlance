pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";
import "./EthlanceTokenStore.sol";
import "./proxy/Forwarder.sol";        // target(EthlanceWorkContract)
import "./proxy/SecondForwarder.sol";  // target(EthlanceTokenStore)

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
    bool isEmployerRequest;
    uint dateRequested;
    address arbiterAddress;
  }

  //
  // Members
  //

  // The job index.
  uint public jobIndex;

  // The Accepted Arbiter assigned to the Jobs within the Job Store.
  address payable public acceptedArbiter;

  /// Bid Option Enumeration
  // 0 - Hourly Rate
  // 1 - Fixed Price
  // 2 - Annual Salary
  // 3 - Bounty
  uint8 public bidOption;
  uint8 public constant BID_OPTION_BOUNTY = 3;

  // Datetime of job contract creation
  uint public dateCreated;

  // Datetime of the last time the job contract was updated.
  uint public dateUpdated;

  // Datetime of job contract finishing
  uint public dateFinished;
    
  // Employer assigned to the given JobStore.
  address payable public employerAddress;

  // Estimated amount of time to finish the contract (in seconds)
  uint public estimatedLengthSeconds;

  // If true, additionally include ether as a token to pay out.
  bool public includeEtherToken;
  // TODO: tokenAddress_listing

  // If true, only employers can request candidates and arbiters
  bool public isInvitationOnly;

  // Additional Job Information stored in IPFS Metahash
  string public metahash;

  // The reward value for a completed bounty
  uint public rewardValue;

  // Token Storage for ERC20 tokens
  EthlanceTokenStore public tokenStore;

  //
  // Collections
  //

  // Arbiter Requests
  ArbiterRequest[] public arbiterRequestListing;
  mapping(address => uint) public arbiterRequestMapping;

  // Work Contract Listing
  address[] public workContractListing;
  mapping(address => bool) public workContractMapping;
    

  function construct(uint _jobIndex,
                     address payable _employerAddress,
                     uint8 _bidOption,
                     uint _estimatedLengthSeconds,
                     bool _includeEtherToken,
                     bool _isInvitationOnly,
                     string calldata _metahash,
                     uint _rewardValue)
    external {
    jobIndex = _jobIndex;
    employerAddress = _employerAddress;
    bidOption = _bidOption;
    estimatedLengthSeconds = _estimatedLengthSeconds;
    includeEtherToken = _includeEtherToken;
    isInvitationOnly = _isInvitationOnly;
    metahash = _metahash;
    rewardValue = _rewardValue;

    // Construct TokenStore forwarder
    SecondForwarder fwd = new SecondForwarder(); // Proxy Contract
    // target(EthlanceTokenStore)

    tokenStore = EthlanceTokenStore(address(fwd));

    registry.permitDispatch(address(fwd));
  
    tokenStore.construct();
  }


  /// @dev update job store's IPFS metahash
  function updateMetahash(string calldata _metahash) external {
    require(msg.sender == employerAddress, "Only the employer can update the metahash.");
    metahash = _metahash;
  }


  /// @dev Fire events specific to the work contract
  /// @param eventName Unique to give the fired event
  /// @param eventData Additional event data to include in the
  /// fired event.
  function fireEvent(string memory eventName, uint[] memory eventData) private {
    registry.fireEvent(eventName, version, eventData);
  }


  /// @dev Set the accepted arbiter for the current Job Wagon.
  /// @param arbiterAddress User address of the accepted arbiter.
  function setAcceptedArbiter(address payable arbiterAddress)
    private {
    acceptedArbiter = arbiterAddress;

    // Fire off event
    uint[] memory eventData = new uint[](2);
    eventData[0] = jobIndex;
    eventData[1] = EthlanceUser(registry.getUserByAddress(arbiterAddress)).userId();
    fireEvent("JobArbiterAccepted", eventData);
  }


  /// @dev Request and create a pending contract between the Candidate and the Employer
  /// @param candidateAddress The user address of the Candidate.
  /*
      
    Only the employer or the candidate can request a work
    contract. Contract status upon construction depends on who
    requests a work contract, or whether the Job is bounty-based.

  */
  function requestWorkContract(address payable candidateAddress)
    public {
    require(registry.isRegisteredUser(candidateAddress),
            "Given address is not a registered user.");
    require(msg.sender == employerAddress || msg.sender == candidateAddress,
            "ERROR: The employer can request a work contract for a candidate. The candidate can request a work contract for himself.");
    require(workContractMapping[candidateAddress] == false,
            "Candidate already has a work contract created.");
    require(employerAddress != candidateAddress,
            "Employer cannot work on his own Job.");

    // Create the forwarded contract, and place in the work listing.
    Forwarder fwd = new Forwarder(); // Proxy Contract with
    // target(EthlanceWorkContract)
    EthlanceWorkContract workContract = EthlanceWorkContract(address(fwd));

    // Permit Work Contract to fire registry events
    registry.permitDispatch(address(fwd));

    uint workIndex = workContractListing.length;
    workContractListing.push(address(workContract));
    workContractMapping[candidateAddress] = true;

    // Determine if it's an employer or a candidate request
    bool isEmployerRequest = false;
    if (msg.sender == employerAddress) {
      isEmployerRequest = true;
    }

    // Construct the work contract.
    workContract.construct(this, workIndex, candidateAddress, isEmployerRequest);
  }

    
  /// @dev Add an arbiter to the arbiter request listing.
  /// @param arbiterAddress The user address of the arbiter.
  /*

    Functionality changes based on who is requesting the arbiter,
    and the status of the requested arbiter.

    Case 1: Employer requests a Arbiter. (msg.sender == employerAddress)

    Case 2: Arbiter requests himself. (msg.sender == arbiterAddress)

    acceptedArbiter is set if:

    - employer had already requested the arbiter, and the arbiter requests the job contract
    - arbiter requests himself, and the employer requests the same arbiter.

  */
  function requestArbiter(address payable arbiterAddress)
    public {
    require(registry.isRegisteredUser(arbiterAddress),
            "Given address is not a registered user.");
    require(registry.isRegisteredArbiter(arbiterAddress),
            "Given address is not a registered arbiter.");
    require(acceptedArbiter == address(0), "Arbiter already accepted.");
    require(arbiterAddress != employerAddress,
            "Employer cannot be the arbiter of his own job contract.");
    require(msg.sender == employerAddress || msg.sender == arbiterAddress,
            "Only an employer can request an arbiter, only an arbiter can request themselves.");

    // Locals
    uint requestIndex;
    bool isEmployerRequest;

    //
    // Handle case where an arbiter is requesting the job contract.
    //

    if (msg.sender == arbiterAddress) {
      // No previous request, so create a new Arbiter Request
      if (arbiterRequestMapping[arbiterAddress] == 0) {
        arbiterRequestListing.push(ArbiterRequest(false, now, arbiterAddress));
        arbiterRequestMapping[arbiterAddress] = arbiterRequestListing.length;

        // Fire off event
        uint[] memory eventData = new uint[](2);
        eventData[0] = jobIndex;
        eventData[1] = EthlanceUser(registry.getUserByAddress(arbiterAddress)).userId();
        fireEvent("JobArbiterRequested", eventData);

        return;
      }

      // Was a previous request, check if an employer requested this arbiter
      requestIndex = arbiterRequestMapping[arbiterAddress] - 1;
      isEmployerRequest = arbiterRequestListing[requestIndex].isEmployerRequest;
      
      // If this arbiter was already requested by the employer, we have our accepted arbiter
      if (isEmployerRequest) {
        setAcceptedArbiter(arbiterAddress);
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
    if (arbiterRequestMapping[arbiterAddress] == 0) {
      arbiterRequestListing.push(ArbiterRequest(true, now, arbiterAddress));
      arbiterRequestMapping[arbiterAddress] = arbiterRequestListing.length;
      
      // Fire off event
      uint[] memory eventData = new uint[](2);
      eventData[0] = jobIndex;
      eventData[1] = EthlanceUser(registry.getUserByAddress(arbiterAddress)).userId();
      fireEvent("JobArbiterRequested", eventData);

      return;
    }

    // Was a previous request, check if a arbiter already requested this job.
    requestIndex = arbiterRequestMapping[arbiterAddress] - 1;
    isEmployerRequest = arbiterRequestListing[requestIndex].isEmployerRequest;

    // If this arbiter already requested this job, we have our accepted arbiter
    if (!isEmployerRequest) {
      setAcceptedArbiter(arbiterAddress);
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
    return arbiterRequestListing.length;
  }

    
  /// @dev Get the arbiter request data in the arbiter request listing.
  /// @param index The index of the ArbiterRequest within the listing.
  /// @return 2-element tuple containing the arbiter data.
  function getRequestedArbiterByIndex(uint index)
    public view returns (bool isEmployerRequest,
                         uint dateRequested,
                         address arbiterAddress) {
    require(index < arbiterRequestListing.length,
            "Given index out of bounds.");
    ArbiterRequest memory arbiterRequest = arbiterRequestListing[index];
    isEmployerRequest = arbiterRequest.isEmployerRequest;
    dateRequested = arbiterRequest.dateRequested;
    arbiterAddress = arbiterRequest.arbiterAddress;
  }


  /// @dev Get the current number of work contracts
  /// @return The number of work contracts in the job store.
  function getWorkContractCount()
    public view returns(uint) {
    return workContractListing.length;
  }


  /// @dev Returns the WorkContract address at the given index.
  /// @param index The index of the Work Contract to be retrieved.
  /// @return The address of the EthlanceWorkContract.
  function getWorkContractByIndex(uint index)
    public view returns(address) {
    require(index < workContractListing.length, "Given index is out of bounds.");
    return workContractListing[index];
  }

    
  /// @dev Returns true if the given _workContract instance resides
  /// in the current job_store.
  function isWorkContract(address _workContract) private returns(bool) {
    for (uint i = 0; i < workContractListing.length; i++) {
      if (address(workContractListing[i]) == _workContract) {
        return true;
      }
    }
    return false;
  }

    
  /// @dev Main function for resolving a dispute between an employer and a candidate.
  /// @param employerAmount Amount to give the employer for dispute resolution.
  function resolveDispute(uint employerAmount,
                          address employerToken,
                          uint candidateAmount,
                          address candidateToken,
                          uint arbiterAmount,
                          address arbiterToken,
                          address payable candidateAddress) external {
    require(employerToken == address(0) &&
            candidateToken == address(0) &&
            arbiterToken == address(0),
            "ERC20 Tokens are not implemented.");
  
    //FIXME: safemath, ERC20 compatible
    uint totalPayout = employerAmount + candidateAmount + arbiterAmount;
    if (address(this).balance < totalPayout) {
      revert("Job Store balance does not satisfy resolution payout.");
    }
  
    employerAddress.transfer(employerAmount);
    candidateAddress.transfer(candidateAmount);
    acceptedArbiter.transfer(arbiterAmount);
  }


  /// @dev Main function for paying an invoice, propagated up from EthlanceInvoice.
  /// @param candidateAddress The address of the person acquiring the payout.
  /// @param amountPaid The amount paid to the given candidateAddress in Wei.
  /*
    Notes:

    - This function is a propagation from EthlanceWorkContract -->
    EthlanceInvoice. Access rights should be reflected in each
    step.
  */
  function payInvoice(address payable candidateAddress, uint amountPaid) external {
    require(isWorkContract(msg.sender), "Only a work contract has permission to transfer from the job store.");
    require(address(this).balance >= amountPaid, "Job Store balance does not satisfy amount to pay");
    candidateAddress.transfer(amountPaid);
  }

    
  /// @dev Main function for paying an invoice with an ERC20 token, propagated from EthlanceInvoice.
  /// @param candidateAddress The address of the person acquiring the payout.
  /// @param amountPaid The amount paid to the given candidateAddress based on the ERC20 token contract amount.
  /// @param tokenAddress Address of the ERC20 token contract.
  function payInvoice(address candidateAddress, uint amountPaid, address tokenAddress) external {
    revert("Not Implemented");
  }


  /// @dev Main method for funding ethereum to the given JobStore.
  /*
    Notes:

    - Anyone can fund a JobStore with ether as long as the employer
    has included the ether token (includeEtherToken)

  */
  function fund() external payable {
    if (!includeEtherToken) {
      revert("Given JobStore is not ethereum fundable.");
    }
  }


  /// @dev Overload for funding the JobStore with an ERC20 token
  /// @param tokenAddress Address of the ERC20 token contract.
  /// @param amount The amount of tokens to transfer to the current JobStore contract.
  /*
    Notes:

    - The tokenAddress ERC20 token contract requires pre-approval
    from the person funding the contract. This will require a
    strictly client-side interaction for this pre-approval.
  */
  function fundToken(address tokenAddress, uint amount) external {
    revert("Not Implemented");
  }
}
