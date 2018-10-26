pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";

/// @title Create Job Contracts as an assigned Employer as a group of
/// identical contracts.
contract EthlanceJobWagon {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Structures
    //

    /// Represents a particular arbiter requesting, or being requested
    /// by an employer for a job contract.
    struct ArbiterRequest {
	bool is_employer_request;
	address arbiter_address;
    }

    struct JobDetails {
	uint estimated_length_seconds;
	bool include_ether_token;
	bool is_bounty;
	bool is_invitation_only;
	uint reward_value;
    }

    // Members

    uint public employer_address;
    uint public date_created;
    uint public date_updated;
    uint public max_job_listing;
    address public accepted_arbiter;

    //
    // Collections
    //

    // Arbiter Requests
    ArbiterRequest[] public arbiter_request_listing;
    mapping(address => uint) public arbiter_request_mapping;

    // Job Contract Listing
    address[] public job_listing;

    
    function construct(uint _max_job_listing
		       string _metahash)
      public {
	
    }

}
