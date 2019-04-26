pragma solidity ^0.5.0;

import "proxy/MutableForwarder.sol";
import "./EthlanceRegistry.sol";
import "./EthlanceFeedback.sol";


/// @title User Contract which represents a User's information
/// describing their price points and skills for employment, for being
/// a candidate, and for being an Arbiter for jobs.
contract EthlanceUser {
  uint public constant version = 1;
  EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

  struct Candidate {
    bool isRegistered;
    uint64 hourlyRate; // In units of currency
    uint16 currencyType; // 0: Ethereum, 1: USD, ...
    // Additional Data in IPFS Metahash
  }

  struct Employer {
    bool isRegistered;
    // Additional Data in IPFS Metahash
  }

  struct Arbiter {
    bool isRegistered;
    uint paymentValue; // Based on paymentType:
    // [0] In units of currency
    // [1] 1-100 for percentage
    uint16 currencyType; // 0: Ethereum, 1: USD, ...
    uint8 paymentType; // 0: Flat Rate, 1: Percentage
    // Additonal Data in IPFS Metahash
  }

  address public userAddress;
  uint public userId;
  uint public dateCreated;
  uint public dateUpdated;
  string public metahash;
    
  // The entity that constructed contract
  address public owner;

  Candidate public candidateData;
  Employer public employerData;
  Arbiter public arbiterData;
  

  function construct(uint _userId, address _address, string calldata _metahash)
    external {
    require(owner == address(0), "EthlanceUser contract already constructed.");
    owner = msg.sender;

    userId = _userId;
    userAddress = _address;
    dateCreated = now;
    dateUpdated = now;
    metahash = _metahash;
  
  }


  /// @dev Fire events specific to the User
  /// @param eventName Unique to give the fired event
  /// @param eventData Additional event data to include in the
  /// fired event.
  function fireEvent(string memory eventName, uint[] memory eventData) private {
    registry.fireEvent(eventName, version, eventData);
  }


  function updateMetahash(string memory _metahash)
    public
    isUser {
    metahash = _metahash;
    updateDateUpdated();
  }


  function updateDateUpdated() internal {
    dateUpdated = now;
  }


  /// @dev Register Candidate for the User.
  /// @dev Note: Requires that the address is a registered user.
  /// @param hourlyRate Based on currency, the hourly suggested
  /// amount for payment.
  /// @param currencyType The type of currency to be paid in.
  function registerCandidate(uint64 hourlyRate, uint16 currencyType)
    public 
    isUser {
    require(!candidateData.isRegistered,
            "Given user is already registered as a Candidate");
    require(currencyType <= 1, "Currency Type out of range");

    candidateData.isRegistered = true;
    candidateData.hourlyRate = hourlyRate;
    candidateData.currencyType = currencyType;
    updateDateUpdated();

    // Fire "UserRegisteredCandidate" Event
    uint[] memory edata = new uint[](1);
    edata[0] = userId;
    fireEvent("UserRegisteredCandidate", edata);
  }


  /// @dev Update Candidate's rate of hourly pay and currency type.
  /// @param hourlyRate The rate of hourly pay for a particular currency.
  ///                    For USD, a unit of pay is a cent. For
  ///                    Ethereum, the unit of pay is a wei.
  /// @param currencyType Type of hourly pay. 0 - Eth, 1 - USD.
  function updateCandidateRate(uint64 hourlyRate,
                               uint16 currencyType)
    public
    isUser {
    candidateData.hourlyRate = hourlyRate;
    candidateData.currencyType = currencyType;
    updateDateUpdated();

    // Fire "UserRegisteredCandidate" Event
    uint[] memory edata = new uint[](1);
    edata[0] = userId;
    fireEvent("UserCandidateUpdate", edata);
  }

    
  /// @dev Return the user's candidate data
  /// @return Tuple of candidate data.
  function getCandidateData()
    public view returns(bool isRegistered,
                        uint64 hourlyRate,
                        uint16 currencyType) {
    isRegistered = candidateData.isRegistered;
    hourlyRate = candidateData.hourlyRate;
    currencyType = candidateData.currencyType;
  }


  /// @dev Registers an Arbiter for the User.
  /// @param paymentValue Unit of payment based on currencyType
  /// and paymentType
  /// @param currencyType Type of currency for the payment value
  ///        0 - ETH, 1 - USD
  /// @param paymentType Type of payment that the arbiter takes.
  ///        0 - Flat Rate, 1 - Percentage
  function registerArbiter(uint paymentValue,
                           uint16 currencyType,
                           uint8 paymentType)
    public
    isUser {
    require(!arbiterData.isRegistered,
            "Given user is already registered as an Arbiter.");
    require(currencyType <= 1, "Currency Type out of range");

    arbiterData.isRegistered = true;
    arbiterData.paymentValue = paymentValue;
    arbiterData.currencyType = currencyType;
    arbiterData.paymentType = paymentType;
    updateDateUpdated();

    // Fire Event
    uint[] memory edata = new uint[](1);
    edata[0] = userId;
    fireEvent("UserRegisteredArbiter", edata);
  }


  /// @dev Updates the given arbiter's rate of payment.
  /// @param paymentValue unit of payment based on currencyType
  /// and paymentType
  /// @param currencyType Type of currency for the payment value
  ///        0 - ETH, 1 - USD
  /// @param paymentType Type of payment that the arbiter takes.
  ///        0 - Flat Rate, 1 - Percentage
  function updateArbiterRate(uint paymentValue,
                             uint16 currencyType,
                             uint8 paymentType)
    public
    isUser {
    arbiterData.paymentValue = paymentValue;
    arbiterData.currencyType = currencyType;
    arbiterData.paymentType = paymentType;
    updateDateUpdated();

    // Fire Event
    uint[] memory edata = new uint[](1);
    edata[0] = userId;
    fireEvent("UserArbiterUpdated", edata);
  }


  /// @dev Gets the user's arbiter data.
  /// @return Tuple containing the arbiter data
  function getArbiterData()
    public view
    returns(bool isRegistered,
            uint paymentValue,
            uint16 currencyType,
            uint8 paymentType) {
    isRegistered = arbiterData.isRegistered;
    paymentValue = arbiterData.paymentValue;
    currencyType = arbiterData.currencyType;
    paymentType = arbiterData.paymentType;
  }


  /// @dev Registers an Employee for the User.
  function registerEmployer()
    public
    isUser {
    require(!employerData.isRegistered,
            "Given user is already registered as an Employer.");

    employerData.isRegistered = true;
    updateDateUpdated();

    // Fire Event
    uint[] memory edata = new uint[](1);
    edata[0] = userId;
    fireEvent("UserRegisteredEmployer", edata);
  }
    

  /// @dev Gets the user's employer data.
  /// @return Tuple containing the employer data
  function getEmployerData()
    public view
    returns(bool isRegistered) {
    isRegistered = employerData.isRegistered;
  }
    

  //
  // Modifiers
  //
    
  /// @dev Checks if the msg.sender is the user assigned to the user
  /// contract.
  modifier isUser {
    require(userAddress == msg.sender,
            "Unauthorized: Given User does not own this user contract.");
    _;
  }
    

}
