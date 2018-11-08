pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";

/// @title Represents a Candidate Invoice for work
contract EthlanceInvoice {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Members
    //
    
    uint public date_created;
    
    uint public date_updated;

    // When date_paid is set >0, this reflects completion
    uint public date_paid;
    
    //FIXME: needs to be more flexible for other currency types
    //defined in the contract.

    // In Wei, the amount that needs to be paid to the candidate
    uint public amount;

    // The EthlanceWorkContract reference.
    EthlanceWorkContract public work_instance;

    //
    // Collections
    //
    
    string[] public employer_metahash_listing;
    string[] public candidate_metahash_listing;
    
    /// @dev Forwarder Constructor
    function construct(EthlanceWorkContract _work_instance, string metahash, uint _amount) {
	// TODO: authenticate
	work_instance = _work_instance;
	candidate_metahash_listing.push(metahash);
	amount = _amount;
	date_created = now;
	date_updated = now;
    }

}
