pragma solidity ^0.4.24;

import "DSAuth.sol";
import "proxy/MutableForwarder.sol";


/*
  EthlanceRegistry deployment should make use of the mutable forwarder.
 */

/// @title Dynamic Event Dispatch
contract EthlanceEventDispatcher {
    event EthlanceEvent(address indexed _address,
			string event_name,
			uint event_version,
			uint timestamp,
			uint[] event_data);
}


contract EthlanceRegistry is DSAuth, EthlanceEventDispatcher {

    // Only one contract user address per an ethereum user address
    address[] user_address_listing;
    mapping(address => uint) user_address_mapping;

    // Ethereum users can have multiple jobs.
    address[] job_address_listing;

    // Mapping of privileged factories to carry out contract construction
    mapping(address => bool) public privileged_factory_contracts;

    // Mapping of contracts that can send an EthlanceEvent
    mapping(address => bool) public event_dispatch_whitelist;

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
	permitEventDispatch(_user_address);
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
	    return 0x0;
	}
	return user_address_listing[user_id - 1];
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
    function pushJob(address _job_address)
	auth
	public returns(uint) {
	job_address_listing.push(_job_address);
	permitEventDispatch(_job_address);
	return job_address_listing.length - 1;
    }


    /// @dev Get the total number of jobs
    /// @return The number of job contract addresses.
    function getJobCount()
	public view
	returns(uint) {
	return job_address_listing.length;
    }

    
    /// @dev Gets the job contract address at the given index.
    /// @param index The index of the job contract.
    /// @return The address at the given job contract.
    function getJobByIndex(uint index)
	public view
	returns(address) {
	require(index < job_address_listing.length,
		"Given index is out of bounds.");
	return job_address_listing[index];
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
    function fireEvent(string event_name,
                       uint event_version,
                       uint[] event_data)
        public {
	require(event_dispatch_whitelist[msg.sender] ||
		isAuthorized(msg.sender, msg.sig),
		"Not Permitted to fire EthlanceEvent.");
	
        emit EthlanceEvent(msg.sender,
			   event_name,
			   event_version,
			   now,
			   event_data);
    }

    
    /// @dev Permits the given address to call fireEvent.
    /// @param _address The address to permit the use of fireEvent.
    function permitEventDispatch(address _address)
	auth
	public {
	event_dispatch_whitelist[_address] = true;
    }

}
