pragma solidity ^0.4.24;

/// @title For creation and management of Job Contracts.
contract EthlanceJobFactory {
    struct JobToken {
	address token_address;
    }
    
    struct ArbiterRequest {
	uint arbiter_id;
	bool is_employer_request;
    }

    struct CandidateRequest {
	uint candidate_id;
	bool is_employer_request;
    }

    struct JobContract {
	address accepted_arbiter;
	address accepted_candidate;

	bytes32[] accepted_token_listing;
	mapping(bytes32 => JobToken) accepted_token_mapping;

	bytes32[] arbiter_request_listing;
	mapping(bytes32 => ArbiterRequest) arbiter_request_mapping;

	bytes32[] candidate_request_listing;
	mapping(bytes32 => CandidateRequest) candidate_request_mapping;

	uint8 bid_mask;
	uint date_created; // FIXME: use 32-bit?
	uint date_started; // FIXME: use 32-bit?
	uint date_finished; // FIXME: use 32-bit?
	uint employer_user_id;
	uint32 estimated_project_length; // enum
	bool is_bounty;
	bool is_eth_payment;
	bool is_invitation_only;
	string metahash_ipfs;
	uint reward_value; // wei units (?)
    }

    struct JobInvoice {
	uint job_id;
	uint date_created; //FIXME: use 32-bit?
	uint date_approved; //FIXME: use 32-bit?
	uint duration_seconds;
    }

    struct JobDispute {
	uint job_id;
	uint dispute_type; // enum
	uint date_created; // FIXME: use 32-bit?
	uint date_resolved; // FIXME: use 32-bit?
	uint employer_resolution_amount;
        uint candidate_resolution_amount;
    }

    JobContract[] job_contract_listing;
    JobInvoice[] job_invoice_listing;
    JobDispute[] job_dispute_listing;

    //
    // Methods
    //

    /// @dev Current User creates a job contract
    /// @return FIXME
    function createJobContract()
	public {
	
    }
}
