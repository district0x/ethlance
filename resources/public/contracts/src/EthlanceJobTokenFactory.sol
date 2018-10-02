pragma solidity ^0.4.24;

/// @title Used to store token listings for JobContracts.
contract EthlanceJobTokenFactory {
    
    /// Represents Token Addresses for including new ERC20 tokens as
    /// forms of payment.
    struct JobToken {
        bool active;
	uint job_id;
	address token_address;
    }

    JobToken[] job_token_listing;

    //
    // Methods
    //


    /// @dev Add a job token for a job contract
    /// @param job_id The job_id for the jobcontract
    /// @param token_address The address of the ERC20 Token
    function addJobToken(uint job_id, address token_address)
	public {
	//TODO: checks

	JobToken memory jobToken = JobToken(true, job_id, token_address);
	job_token_listing.push(jobToken);
    }


    //
    // Views
    //


    /// @dev Gets all active job tokens for a particular job contract
    /// @param job_token_id The job token id
    /// @returns the given job_token_id
    function getJobTokenById(uint job_token_id)
	public view returns (bool active,
			     uint job_id,
			     address token_address) {
	JobToken memory jobToken = job_token_listing[job_token_id];

	active = jobToken.active;
	job_id = jobToken.job_id;
	token_address = jobToken.token_address;
    }


    /// @dev Returns the total number of job tokens
    function getJobTokenCount()
	public view returns(uint) {
	return job_token_listing.length;
    }
}
