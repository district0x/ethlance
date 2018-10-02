pragma solidity ^0.4.24;

/// @title For creation of Job Contracts.
contract EthlanceJobContractFactory {
    
    /// Represents a particular arbiter requesting, or being requested
    /// by an employer for a job contract.
    struct ArbiterRequest {
	bool is_employer_request;
    }

    /// Represents a particular candidate requesting, or being
    /// requested by an employer for a job contract.
    struct CandidateRequest {
	bool is_employer_request;
    }

    /// Represents Available Bid Options for Candidate Requests
    struct BidOptions {
	bool hourly_rate;
	bool fixed_price;
	bool annual_salary;
    }

    /// Main Job Contract structure.
    struct JobContract {
	address accepted_arbiter;
	address accepted_candidate;

	//uint[] accepted_token_listing;
	//mapping(uint => JobToken) accepted_token_mapping;

	//uint[] arbiter_request_listing;
	//mapping(uint => ArbiterRequest) arbiter_request_mapping;

	//uint[] candidate_request_listing;
	//mapping(uint => CandidateRequest) candidate_request_mapping;

	// Mask of allowed bid options for the contract
	BidOptions bid_options;

	// Datetime of job contract creation
	uint date_created;

	// Datetime of job contract obtaining accepted_candidate and
	// accepted_arbiter.
	uint date_started;

	// Datetime of job contract finishing
	uint date_finished;

	// Employer who created the job contract
	uint employer_user_id;

	// Estimated amount of time to finish the contract (in seconds)
	uint estimated_length_seconds;

	// If true, additionally include ether as a token to pay out.
	bool include_ether_token;

	// If true, job contract is a bounty contract
	bool is_bounty;

	// If true, only employers can request candidates and arbiters
	bool is_invitation_only;

	// IPFS Hash for additional job contract data.
	string metahash_ipfs;

	// If it is a bounty, the reward value to gain from completing
	// the contract.
	uint reward_value; // wei units (?)
    }

    /// Represents a job invoice sent by the candidate to the employer.
    struct JobInvoice {
	uint job_id;
	uint date_created;
	uint date_approved;
	uint duration_seconds;
    }

    // Represents a job dispute between the candidate and the employee
    struct JobDispute {
	uint job_id;
	uint dispute_type; // enum
	uint date_created;
	uint date_resolved;
	uint employer_resolution_amount;
        uint candidate_resolution_amount;
    }

    JobContract[] job_contract_listing;
    JobInvoice[] job_invoice_listing;
    JobDispute[] job_dispute_listing;

    //
    // Methods
    //

    /// @dev Create Job Contract for 'employer_id'
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
	internal {
        BidOptions memory bid_options = BidOptions(bid_hourly_rate,
						   bid_fixed_price,
						   bid_annual_salary);
	JobContract memory jobContract = JobContract({
          accepted_arbiter: 0,
          accepted_candidate: 0,
          bid_options: bid_options,
          date_created: now,
          date_started: 0,
          date_finished: 0,
          employer_user_id: employer_user_id,
          estimated_length_seconds: estimated_length_seconds,
          include_ether_token: include_ether_token,
          is_bounty: is_bounty,
          is_invitation_only: is_invitation_only,
          metahash_ipfs: metahash_ipfs,
          reward_value: reward_value
        });

	
    }
}
