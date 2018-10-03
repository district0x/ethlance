pragma solidity ^0.4.24;

import "./EthlanceEventDispatcher.sol";
import "./EthlanceJob.sol";
import "./proxy/MutableForwarder.sol";
import "./proxy/Forwarder.sol";

/// @title For creation of Job Contracts.
contract EthlanceJobFactory {
    uint public constant version = 1;
    MutableForwarder public event_dispatcher = new MutableForwarder();

    address[] public job_listing;

    /// @dev Constructor
    /// @param _event_dispatcher The main dynamic event dispatcher
    constructor(address _event_dispatcher) public {
	event_dispatcher.setTarget(_event_dispatcher);
    }

    //
    // Methods
    //

    /// @dev Create Job Contract for given user defined by
    /// 'employer_user_id'. Note that parameters are described in
    /// EthlanceJob contract.
    function createJobContract(bool bid_hourly_rate,
			       bool bid_fixed_price,
			       bool bid_annual_salary,
			       address employer_address,
			       uint estimated_length_seconds,
			       bool include_ether_token,
			       bool is_bounty,
			       bool is_invitation_only,
			       string metahash_ipfs,
			       uint reward_value)
	public
    {
	address job = new Forwarder(); // Proxy Contract with
				       // target(EthlanceJob)
	job.construct(event_dispatcher,
		      bid_hourly_rate,
		      bid_fixed_price,
		      bid_annual_salary,
		      employer_address,
		      estimated_length_seconds,
		      include_ether_token,
		      is_bounty,
		      is_invitation_only,
		      metahash_ipfs,
		      reward_value);
	job_listing.push(job);
    }

    //
    // Views
    //
    
    function getJobListingLength()
	public view returns(uint) {
	return job_listing.length;
    }

    /// @dev Get the job address at `idx` within the job listing
    /// @param idx The index of the job address within the job listing.
    /// @return The address of the given index
    function getJobByIndex(uint idx)
	public view returns (address)
    {
	return job_listing[idx];
    }
}
