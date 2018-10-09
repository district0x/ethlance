pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceJob.sol";
import "./proxy/MutableForwarder.sol";
import "./proxy/Forwarder.sol";

/// @title For creation of Job Contracts.
contract EthlanceJobFactory {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    EthlanceJob[] public job_listing;

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
	address job_fwd = new Forwarder(); // Proxy Contract with
					   // target(EthlanceJob)
	EthlanceJob job = EthlanceJob(address(job_fwd));
	job.construct(registry,
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
	registry.pushJob(address(job));
    }

    //
    // Views
    //
    
    function getJobCount()
	public view returns(uint) {
	return registry.getJobCount();
    }

    /// @dev Get the job address at `idx` within the job listing
    /// @param index The index of the job address within the job listing.
    /// @return The address of the given index
    function getJobByIndex(uint index)
	public view returns (address)
    {
	return registry.getJobByIndex(index);
    }
}
