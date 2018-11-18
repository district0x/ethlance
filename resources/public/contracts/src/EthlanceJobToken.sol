pragma solidity ^0.5.0;

/// @title Used to store token listings for JobContracts.
contract EthlanceJobToken {
    
    /// Represents Token Addresses for including new ERC20 tokens as
    /// forms of payment.
    struct JobToken {
        bool active;
	address token_address;
    }

    JobToken[] internal job_token_listing;

    //
    // Methods
    //

    /// @dev Add a job token for a job contract
    /// @param token_address The address of the ERC20 Token
    function addJobToken(address token_address)
	public {
	//TODO: checks

	JobToken memory jobToken = JobToken(true, token_address);
	job_token_listing.push(jobToken);
    }


    //
    // Views
    //


    /// @dev Gets all active job tokens for a particular job contract
    /// @param job_token_id The job token id
    /// @return the given job_token_id
    function getJobTokenById(uint job_token_id)
	public view returns (bool active,
			     address token_address) {
	JobToken memory jobToken = job_token_listing[job_token_id];

	active = jobToken.active;
	token_address = jobToken.token_address;
    }


    /// @dev Returns the total number of job tokens
    function getJobTokenCount()
	public view returns(uint) {
	return job_token_listing.length;
    }
}
