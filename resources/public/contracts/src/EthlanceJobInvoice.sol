pragma solidity ^0.4.24;

import "./proxy/MutableForwarder.sol";
import "./EthlanceEventDispatcher.sol";

/// @title EthlanceJob Invoice members and methods
contract EthlanceJobInvoice {
    uint public constant version = 1;
    EthlanceEventDispatcher public event_dispatcher;

    /// Represents a job invoice sent by the candidate to the employer.
    struct JobInvoice {
	uint job_id;
	uint date_created;
	uint date_approved;
	uint duration_seconds;
    }

    // Job Invoices
    JobInvoice[] public invoice_listing;

    //
    // Methods
    //
    
    function setInvoiceEventDispatcher(EthlanceEventDispatcher _event_dispatcher) internal {
	event_dispatcher = _event_dispatcher;
    }

}
