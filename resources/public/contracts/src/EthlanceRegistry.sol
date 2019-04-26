pragma solidity ^0.5.0;

import "DSAuth.sol";
import "proxy/MutableForwarder.sol";
import "./EthlanceUser.sol";

/*
  EthlanceRegistry deployment should make use of the mutable forwarder.
*/

/// @title Dynamic Event Dispatch
contract EthlanceEventDispatcher {
  event EthlanceEvent(address indexed eventSender,
                      string eventName,
                      uint eventVersion,
                      uint timestamp,
                      uint[] eventData);
}


contract EthlanceRegistry is DSAuth, EthlanceEventDispatcher {

  // Only one contract user address per an ethereum user address
  address[] userAddressListing;
  mapping(address => uint) userAddressMapping;

  // Comment Listings
  mapping(address => address[]) commentListing;

  // Feedback Mapping
  mapping(address => address) feedbackMapping;

  // Ethereum users can have multiple jobs.
  address[] jobStoreAddressListing;

  // Mapping of privileged factories to carry out contract construction
  mapping(address => bool) public privilegedFactoryContracts;

  // Mapping of contracts that can send an EthlanceEvent,
  // append Comments, and append Feedback.
  mapping(address => bool) public dispatchWhitelist;

  /// @dev Push user address into the user listing.
  /// @param _ethAddress The address of the ethereum user.
  /// @param _userAddress The address of the user contract.
  /// @return The user_id of the pushed user address.
  function pushUser(address _ethAddress,
                    address _userAddress)
    auth
    public returns(uint) {
    userAddressListing.push(_userAddress);
    userAddressMapping[_ethAddress] = userAddressListing.length;
    permitDispatch(_userAddress);
    return userAddressListing.length;
  }

    
  /// @dev Get the number of users currently registered.
  /// @return The number of users.
  function getUserCount()
    public view
    returns(uint) {
    return userAddressListing.length;
  }


  /// @dev Get the current user address based on the assigned
  /// address, or return 0x0 if the given user does not exist.
  /// @param _ethAddress The ethereum address of the user
  /// @return The user contract address.
  function getUserByAddress(address _ethAddress) 
    public view
    returns(address) {
    uint user_id = userAddressMapping[_ethAddress];
    if (user_id == 0) {
      return address(0);
    }
    return userAddressListing[user_id - 1];
  }

    
  /// @dev Get the User ID linked to the given Ethereum
  /// Address. Returns 0 if there is no linked ethereum address.
  function getUserId(address _ethAddress) public view returns(uint) {
    return userAddressMapping[_ethAddress];
  }


  /// @dev Get the user contract address by the provided index.
  /// @param index The user contract index.
  /// @return The address of the user contract.
  function getUserByIndex(uint index)
    public view
    returns(address) {
    require(index < userAddressListing.length,
            "Given index is out of bounds.");
    return userAddressListing[index];
  }


  /// @dev Push job address into the job listing
  /// @param _jobAddress The address to place in the job listing.
  /// @return The job index of the pushed contract address.
  function pushJobStore(address _jobAddress)
    auth
    public returns(uint) {
    jobStoreAddressListing.push(_jobAddress);
    permitDispatch(_jobAddress);
    return jobStoreAddressListing.length - 1;
  }


  /// @dev Get the total number of jobs
  /// @return The number of job contract addresses.
  function getJobStoreCount()
    public view
    returns(uint) {
    return jobStoreAddressListing.length;
  }

    
  /// @dev Gets the job contract address at the given index.
  /// @param index The index of the job contract.
  /// @return The address at the given job contract.
  function getJobStoreByIndex(uint index)
    public view
    returns(address) {
    require(index < jobStoreAddressListing.length,
            "Given index is out of bounds.");
    return jobStoreAddressListing[index];
  }


  /// @dev Allow a factory contract to be privileged for contract
  /// construction.
  /// @param factoryAddress The address of the privileged factory.
  function permitFactoryPrivilege(address factoryAddress)
    auth
    public {
    privilegedFactoryContracts[factoryAddress] = true;
  }

    
  /// @dev Checks if the given factoryAddress is privileged to
  /// carry out contract construction.
  /// @param factoryAddress The address of the factory contract
  /// @return True, if the factory is privileged
  function checkFactoryPrivilege(address factoryAddress)
    public view returns(bool) {
    return privilegedFactoryContracts[factoryAddress];
  }


  /// @dev Emit the dynamic Ethlance Event.
  /// @param eventName - Name of the event.
  /// @param eventVersion - Version of the event.
  /// @param eventData - Array of data within the event.
  function fireEvent(string memory eventName,
                     uint eventVersion,
                     uint[] memory eventData)
    public {
    require(dispatchWhitelist[msg.sender] == true ||
            isAuthorized(msg.sender, msg.sig),
            "Not Permitted to fire EthlanceEvent.");
  
    emit EthlanceEvent(msg.sender,
                       eventName,
                       eventVersion,
                       now,
                       eventData);
  }

    
  /// @dev Permits the given address to call fireEvent and push
  /// values to the Registry listings. (comment listing, feedback
  /// listing)
  /// @param _address The address to permit the use of fireEvent.
  /*
    Note that permitting dispatch allows the provided address to
    propagate the same permissions.
  */
  function permitDispatch(address _address)
    public {
    require(dispatchWhitelist[msg.sender] ||
            isAuthorized(msg.sender, msg.sig),
            "Not Authorized to permit dispatch.");
    dispatchWhitelist[_address] = true;
  }


  /// @dev Returns true, if the given address is a registered user
  /// within the registry.
  /// @param _address Address of the user.
  /// @return Returns true if it is a registered user.
  function isRegisteredUser(address _address)
    public view returns(bool) {
    EthlanceUser user = EthlanceUser(getUserByAddress(_address));
    if (address(user) == address(0)) {
      return false;
    }
    return true;
  }


  /// @dev Returns true, if the given user address is a registered employer
  /// @param _address Address of the user
  /// @return Returns true if it is an employer address.
  function isRegisteredEmployer(address _address)
    public view returns(bool) {
    EthlanceUser user = EthlanceUser(getUserByAddress(_address));
    if (address(user) == address(0)) {
      return false;
    }

    bool isRegistered = user.getEmployerData();
    return isRegistered;
  }


  /// @dev Returns true, if the given user address is a registered candidate
  /// @param _address Address of the user
  /// @return Returns true if it is a registered candidate.
  function isRegisteredCandidate(address _address)
    public view returns(bool) {
    EthlanceUser user = EthlanceUser(getUserByAddress(_address));
    if (address(user) == address(0)) {
      return false;
    }

    (bool isRegistered,,) = user.getCandidateData();
    return isRegistered;
  }

    
  /// @dev Returns true, if the given user address is a registered arbiter
  /// @param _address Address of the user
  /// @return Returns true if it is a registered arbiter.
  function isRegisteredArbiter(address _address)
    public view returns(bool) {
    EthlanceUser user = EthlanceUser(getUserByAddress(_address));
    if (address(user) == address(0)) {
      return false;
    }

    (bool isRegistered,,,) = user.getArbiterData();
    return isRegistered;
  }


  /// @dev Push Comment into comment listing
  /// @param contractAddress Address of the contract which contains comments
  /// @param comment Comment Contract being appended
  function pushComment(address contractAddress, address comment)
    public {
    require(dispatchWhitelist[msg.sender] == true ||
            isAuthorized(msg.sender, msg.sig),
            "Not Permitted to fire EthlanceEvent.");

    commentListing[contractAddress].push(comment);
  }

    
  /// @dev Get the number of comments linked to the given contract
  function getCommentCount(address contractAddress)
    public view returns(uint) {
    return commentListing[contractAddress].length;
  }
    
    
  /// @dev Get the comment contract at the given address, with the given index
  function getCommentByIndex(address contractAddress, uint index)
    public view returns(address) {
    require(index < getCommentCount(contractAddress), "Index out of bounds");
    return commentListing[contractAddress][index];
  }


  /// @dev Push Feedback into feedback listing
  /// @param contractAddress Address of the contract which contains feedbacks
  /// @param feedback Feedback Contract being appended
  function pushFeedback(address contractAddress, address feedback)
    public {
    require(dispatchWhitelist[msg.sender] == true ||
            isAuthorized(msg.sender, msg.sig),
            "Not Permitted to fire EthlanceEvent.");
    require(!hasFeedback(contractAddress), "Given contract address already has a feedback instance.");

    feedbackMapping[contractAddress] = feedback;
  }
    

  /// @dev Returns true if a feedback instance is already linked to the given contract address.
  function hasFeedback(address contractAddress) public view returns(bool) {
    return feedbackMapping[contractAddress] != address(0);
  }
    

  /// @dev Get the feedback contract at the given address, with the given index
  function getFeedbackByAddress(address contractAddress)
    public view returns(address) {
    return feedbackMapping[contractAddress];
  }
}
