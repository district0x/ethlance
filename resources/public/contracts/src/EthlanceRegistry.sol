pragma solidity ^0.5.0;

import "DSAuth.sol";
import "proxy/MutableForwarder.sol";
import "./EthlanceUser.sol";

/*
  EthlanceRegistry deployment should make use of the mutable forwarder.
 */

/// @title Dynamic Event Dispatch
contract EthlanceEventDispatcher {
    event EthlanceEvent(address indexed event_sender,
			string event_name,
			uint event_version,
			uint timestamp,
			uint[] event_data);
}


contract EthlanceRegistry is DSAuth, EthlanceEventDispatcher {

    // Only one contract user address per an ethereum user address
    address[] user_address_listing;
    mapping(address => uint) user_address_mapping;

    // Comment Listings
    mapping(address => address[]) comment_listing;

    // Feedback Mapping
    mapping(address => address) feedback_mapping;

    // Ethereum users can have multiple jobs.
    address[] job_store_address_listing;

    // Mapping of privileged factories to carry out contract construction
    mapping(address => bool) public privileged_factory_contracts;

    // Mapping of contracts that can send an EthlanceEvent,
    // append Comments, and append Feedback.
    mapping(address => bool) public dispatch_whitelist;

    /// @dev Push user address into the user listing.
    /// @param _eth_address The address of the ethereum user.
    /// @param _user_address The address of the user contract.
    /// @return The user_id of the pushed user address.
    function pushUser(address _eth_address,
		      address _user_address)
	auth
	public returns(uint) {
	user_address_listing.push(_user_address);
	user_address_mapping[_eth_address] = user_address_listing.length;
	permitDispatch(_user_address);
	return user_address_listing.length;
    }

    
    /// @dev Get the number of users currently registered.
    /// @return The number of users.
    function getUserCount()
	public view
        returns(uint) {
	return user_address_listing.length;
    }


    /// @dev Get the current user address based on the assigned
    /// address, or return 0x0 if the given user does not exist.
    /// @param _eth_address The ethereum address of the user
    /// @return The user contract address.
    function getUserByAddress(address _eth_address) 
	public view
	returns(address) {
	uint user_id = user_address_mapping[_eth_address];
	if (user_id == 0) {
	    return address(0);
	}
	return user_address_listing[user_id - 1];
    }

    
    /// @dev Get the User ID linked to the given Ethereum
    /// Address. Returns 0 if there is no linked ethereum address.
    function getUserId(address _eth_address) public view returns(uint) {
	return user_address_mapping[_eth_address];
    }


    /// @dev Get the user contract address by the provided index.
    /// @param index The user contract index.
    /// @return The address of the user contract.
    function getUserByIndex(uint index)
	public view
	returns(address) {
	require(index < user_address_listing.length,
		"Given index is out of bounds.");
	return user_address_listing[index];
    }


    /// @dev Push job address into the job listing
    /// @param _job_address The address to place in the job listing.
    /// @return The job index of the pushed contract address.
    function pushJobStore(address _job_address)
	auth
	public returns(uint) {
	job_store_address_listing.push(_job_address);
	permitDispatch(_job_address);
	return job_store_address_listing.length - 1;
    }


    /// @dev Get the total number of jobs
    /// @return The number of job contract addresses.
    function getJobStoreCount()
	public view
	returns(uint) {
	return job_store_address_listing.length;
    }

    
    /// @dev Gets the job contract address at the given index.
    /// @param index The index of the job contract.
    /// @return The address at the given job contract.
    function getJobStoreByIndex(uint index)
	public view
	returns(address) {
	require(index < job_store_address_listing.length,
		"Given index is out of bounds.");
	return job_store_address_listing[index];
    }


    /// @dev Allow a factory contract to be privileged for contract
    /// construction.
    /// @param factory_address The address of the privileged factory.
    function permitFactoryPrivilege(address factory_address)
	auth
	public {
	privileged_factory_contracts[factory_address] = true;
    }

    
    /// @dev Checks if the given factory_address is privileged to
    /// carry out contract construction.
    /// @param factory_address The address of the factory contract
    /// @return True, if the factory is privileged
    function checkFactoryPrivilege(address factory_address)
	public view returns(bool) {
	return privileged_factory_contracts[factory_address];
    }


    /// @dev Emit the dynamic Ethlance Event.
    /// @param event_name - Name of the event.
    /// @param event_version - Version of the event.
    /// @param event_data - Array of data within the event.
    function fireEvent(string memory event_name,
                       uint event_version,
                       uint[] memory event_data)
        public {
	require(dispatch_whitelist[msg.sender] == true ||
		isAuthorized(msg.sender, msg.sig),
		"Not Permitted to fire EthlanceEvent.");
	
        emit EthlanceEvent(msg.sender,
			   event_name,
			   event_version,
			   now,
			   event_data);
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
	require(dispatch_whitelist[msg.sender] ||
		isAuthorized(msg.sender, msg.sig),
		"Not Authorized to permit dispatch.");
	dispatch_whitelist[_address] = true;
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

	bool is_registered = user.getEmployerData();
	return is_registered;
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

	(bool is_registered,,) = user.getCandidateData();
	return is_registered;
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

	(bool is_registered,,,) = user.getArbiterData();
	return is_registered;
    }


    /// @dev Push Comment into comment listing
    /// @param contract_address Address of the contract which contains comments
    /// @param comment Comment Contract being appended
    function pushComment(address contract_address, address comment)
	public {
	require(dispatch_whitelist[msg.sender] == true ||
		isAuthorized(msg.sender, msg.sig),
		"Not Permitted to fire EthlanceEvent.");

	comment_listing[contract_address].push(comment);
    }

    
    /// @dev Get the number of comments linked to the given contract
    function getCommentCount(address contract_address)
	public view returns(uint) {
	return comment_listing[contract_address].length;
    }
    
    
    /// @dev Get the comment contract at the given address, with the given index
    function getCommentByIndex(address contract_address, uint index)
	public view returns(address) {
	require(index < getCommentCount(contract_address), "Index out of bounds");
	return comment_listing[contract_address][index];
    }


    /// @dev Push Feedback into feedback listing
    /// @param contract_address Address of the contract which contains feedbacks
    /// @param feedback Feedback Contract being appended
    function pushFeedback(address contract_address, address feedback)
	public {
	require(dispatch_whitelist[msg.sender] == true ||
		isAuthorized(msg.sender, msg.sig),
		"Not Permitted to fire EthlanceEvent.");
	require(!hasFeedback(contract_address), "Given contract address already has a feedback instance.");

	feedback_mapping[contract_address] = feedback;
    }
    

    /// @dev Returns true if a feedback instance is already linked to the given contract address.
    function hasFeedback(address contract_address) public view returns(bool) {
	return feedback_mapping[contract_address] != address(0);
    }
    

    /// @dev Get the feedback contract at the given address, with the given index
    function getFeedbackByAddress(address contract_address)
	public view returns(address) {
	return feedback_mapping[contract_address];
    }
}
