pragma solidity ^0.4.24;

import "./EthlanceJobToken.sol";

/// @title Job Contracts to tie candidates, employers, and arbiters to
/// an agreement.
contract EthlanceJob is EthlanceJobToken {

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

    //
    // Members
    //

    address public accepted_arbiter;
    address public accepted_candidate;

    // Mask of allowed bid options for the contract
    BidOptions public bid_options;

    // Datetime of job contract creation
    uint public date_created;

    // Datetime of job contract obtaining an accepted_candidate and an
    // accepted_arbiter.
    uint public date_started;

    // Datetime of job contract finishing
    uint public date_finished;
    
    // Employer who created the job contract
    uint public employer_user_id;

    // Estimated amount of time to finish the contract (in seconds)
    uint public estimated_length_seconds;

    // If true, additionally include ether as a token to pay out.
    bool public include_ether_token;

    // If true, job contract is a bounty contract
    bool public is_bounty;

    // If true, only employers can request candidates and arbiters
    bool public is_invitation_only;

    // IPFS Hash for additional job contract data.
    string public metahash_ipfs;

    // If it is a bounty, the reward value to gain from completing
    // the contract.
    uint public reward_value; // wei units (?)

    //
    // Collections
    //

    // Job Disputes
    JobDispute[] public dispute_listing;

    // Job Invoices
    JobInvoice[] public invoice_listing;

    // Arbiter Requests
    ArbiterRequest[] public arbiter_request_listing;

    // Candidate Requests
    CandidateRequest[] public candidate_request_listing;

    //
    // Constructor
    //
    
    constructor(bool _bid_hourly_rate,
		bool _bid_fixed_price,
		bool _bid_annual_salary,
		uint _employer_user_id,
		uint _estimated_length_seconds,
		bool _include_ether_token,
		bool _is_bounty,
		bool _is_invitation_only,
		string _metahash_ipfs,
		uint _reward_value) {
	bid_options.hourly_rate = _bid_hourly_rate;
	bid_options.fixed_price = _bid_fixed_price;
	bid_options.annual_salary = _bid_annual_salary;
	date_created = now;
	employer_user_id = _employer_user_id;
	estimated_length_seconds = _estimated_length_seconds;
	include_ether_token = _include_ether_token;
	is_bounty = _is_bounty;
	is_invitation_only = _is_invitation_only;
	metahash_ipfs = _metahash_ipfs;
	reward_value = _reward_value;
    }

    //
    // Methods
    //

    
}
