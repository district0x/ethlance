pragma solidity ^0.5.0;

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
    function fireEvent(string memory event_name, uint[] memory event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }


    /// @dev Create Job Contract for given user defined by
    /// 'employer_user_id'. Note that parameters are described in
    /// EthlanceJob contract.
    function createJobStore(uint8 bid_option,
			    uint estimated_length_seconds,
			    bool include_ether_token,
			    bool is_invitation_only,
			    string memory metahash,
			    uint reward_value)
	public {
	require(registry.isRegisteredEmployer(msg.sender),
		"You are not a registered employer.");

	// TODO: bounds on parameters

	Forwarder fwd = new Forwarder(); // Proxy Contract with
	                               // target(EthlanceJobStore)
	EthlanceJobStore jobStore = EthlanceJobStore(address(fwd));

	// Permit JobStore to fire registry events
	registry.permitDispatch(address(fwd));

	uint job_index = registry.pushJobStore(address(jobStore));
	jobStore.construct(job_index,
			   msg.sender,
			   bid_option,
			   estimated_length_seconds,
			   include_ether_token,
			   is_invitation_only,
			   metahash,
			   reward_value);
	
	// Create and Fire off event data
	uint[] memory edata = new uint[](1);
	edata[0] = job_index;
	fireEvent("JobStoreCreated", edata);
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
