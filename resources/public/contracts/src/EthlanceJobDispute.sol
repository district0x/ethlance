pragma solidity ^0.4.24;

import "./proxy/MutableForwarder.sol";
import "./EthlanceRegistry.sol";

/// @title Job Dispute
contract EthlanceJobDispute {
    uint public constant version = 1;
    EthlanceRegistry public registry;

    // Represents a job dispute between the candidate and the employee
    struct JobDispute {
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
    
    function setDisputeEventDispatcher(EthlanceRegistry _registry) internal {
	registry = _registry;
    }
}
