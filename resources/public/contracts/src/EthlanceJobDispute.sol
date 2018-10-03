pragma solidity ^0.4.24;

import "./proxy/MutableForwarder.sol";
import "./EthlanceEventDispatcher.sol";

/// @title Job Dispute
contract EthlanceJobDispute {
    uint public constant version = 1;
    MutableForwarder public event_dispatcher;

    // Represents a job dispute between the candidate and the employee
    struct JobDispute {
	uint job_id;
	uint dispute_type; // enum
	uint date_created;
	uint date_resolved;
	uint employer_resolution_amount;
        uint candidate_resolution_amount;
    }

    // Job Disputes
    JobDispute[] public dispute_listing;

    //
    // Methods
    //
    
    constructor(MutableForwarder _event_dispatcher) {
	event_dispatcher = _event_dispatcher;
    }
}
