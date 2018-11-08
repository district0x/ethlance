pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";

/// @title Represents a Employer / Candidate work dispute
contract EthlanceDispute {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Members
    //
    
    uint public date_created;

    uint public date_updated;

    // When date_resolved is set >0, this reflects completion
    uint public date_resolved;

    // Title of dispute, describing the reason for the dispute
    string public reason;

    //FIXME: needs to be more flexible for other currency types.

    // In Wei, the amount that the employer should receive as a result
    // of resolution.
    uint public employer_resolution_amount;

    // In Wei, the amount that the candidate should receive as a
    // result of resolution.
    uint public candidate_resolution_amount;

    // The EthlanceWorkContract reference.
    EthlanceWorkContract public work_instance;

    //
    // Collections
    //
    
    // Listing consists of IPFS hashes with a pre-defined EDN structure.
    string[] public employer_metahash_listing;
    string[] public candidate_metahash_listing;
    string[] public arbiter_metahash_listing;

    
    /// @dev Forwarder Constructor
    function construct(EthlanceWorkContract _work_instance, string _reason, string comment_metahash) {
	// TODO: authenticate
	work_instance = _work_instance;
	reason = _reason;
	// TODO: distinguish for pushing comments
	//comment_listing.push(comment_metahash);
	date_created = now;
	date_updated = now;
    }

}
