pragma solidity ^0.4.24;

import "./EthlanceJob.sol";

/// @title For creation of Job Contracts.
contract EthlanceJobFactory {
    
    //
    // Methods
    //

    /// @dev Create Job Contract for given user defined by
    /// 'employer_user_id'
    /// @return FIXME
    function createJobContract(bool bid_hourly_rate,
			       bool bid_fixed_price,
			       bool bid_annual_salary,
			       uint employer_user_id,
			       uint estimated_length_seconds,
			       bool include_ether_token,
			       bool is_bounty,
			       bool is_invitation_only,
			       string metahash_ipfs,
			       uint reward_value)
	public {
	
    }
}
