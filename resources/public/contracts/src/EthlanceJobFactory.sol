pragma solidity ^0.4.24;

/// @title For creation and management of Job Contracts.
contract EthlanceJobFactory {
    /// Represents Token Addresses for including new ERC20 tokens as
    /// forms of payment.
    struct JobToken {
	address token_address;
    }
    
    /// Represents a particular arbiter requesting, or being requested
    /// by an employer for a job contract.
    struct ArbiterRequest {
	uint arbiter_id;
	bool is_employer_request;
    }

    /// Represents a particular candidate requesting, or being
    /// requested by an employer for a job contract.
    struct CandidateRequest {
	uint candidate_id;
	bool is_employer_request;
    }

    /// Represents Different Bid Options for Candidate Requests
    struct BidOptions {
	bool hourly_rate;
	bool fixed_price;
	bool annual_salary;
    }

    /// Main Job Contract structure.
    struct JobContract {
	address accepted_arbiter;
	address accepted_candidate;

	uint32[] accepted_token_listing;
	mapping(uint32 => JobToken) accepted_token_mapping;

	uint32[] arbiter_request_listing;
	mapping(uint32 => ArbiterRequest) arbiter_request_mapping;

	uint32[] candidate_request_listing;
	mapping(uint32 => CandidateRequest) candidate_request_mapping;

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
			       uint employer_id,
			       uint estimated_length_seconds,
			       bool include_ether_token,
			       bool is_bounty,
			       bool is_invitation_only,
			       uint reward_value)
	public {
	
    }
}
