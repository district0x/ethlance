pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceJobStore.sol";
import "./EthlanceUserFactory.sol";
import "./EthlanceUser.sol";
import "./proxy/MutableForwarder.sol";
import "./proxy/Forwarder.sol";

/// @title For creation of Job Contracts.
contract EthlanceJobFactory {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Methods
    //


    /// @dev Fire events specific to the JobFactory
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string event_name, uint[] event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }


    /// @dev Returns true, if the given user address is a registered employer
    /// @param _address Address of the user
    /// @return Returns true, if the it is an employer address.
    function isRegisteredEmployer(address _address)
	public view returns(bool) {
	EthlanceUser user = EthlanceUser(registry.getUserByAddress(_address));
	if (address(user) == 0x0) {
	    return false;
	}

	bool is_registered = user.getEmployerData();
	return is_registered;
    }


    /// @dev Create Job Contract for given user defined by
    /// 'employer_user_id'. Note that parameters are described in
    /// EthlanceJob contract.
    function createJob(address employer_address)
	public {
	require(isRegisteredEmployer(msg.sender),
		"You are not a registered employer.");

	// TODO: bounds on parameters

	address fwd = new Forwarder(); // Proxy Contract with
	                               // target(EthlanceJobStore)
	EthlanceJobStore jobStore = EthlanceJobStore(address(fwd));
	uint job_index = registry.pushJobStore(address(jobStore));
	jobStore.construct(msg.sender);
	
	// Create and Fire off event data
	uint[] memory edata = new uint[](1);
	edata[0] = job_index;
	fireEvent("JobCreated", edata);
    }

    //
    // Views
    //
    
    function getJobStoreCount()
	public view returns(uint) {
	return registry.getJobStoreCount();
    }

    /// @dev Get the job address at `index` within the job listing
    /// @param index The index of the job address within the job listing.
    /// @return The address of the given index
    function getJobStoreByIndex(uint index)
	public view returns (address)
    {
	return registry.getJobStoreByIndex(index);
    }
}
