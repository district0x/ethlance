pragma solidity ^0.4.24;

import "./proxy/MutableForwarder.sol";
import "./EthlanceRegistry.sol";

/// @title EthlanceJob Invoice members and methods
contract EthlanceJobInvoice {
    uint public constant version = 1;
    EthlanceRegistry public registry;

    /// Represents a job invoice sent by the candidate to the employer.
    struct JobInvoice {
	uint date_created;
	uint date_approved;
	uint duration_seconds;
    }

    // Job Invoices
    JobInvoice[] public invoice_listing;

    //
    // Methods
    //
    
    function setInvoiceEventDispatcher(EthlanceRegistry _registry) internal {
	registry = _registry;
    }

}
