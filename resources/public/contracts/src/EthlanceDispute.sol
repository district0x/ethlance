pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceWorkContract.sol";
import "./collections/EthlanceMetahash.sol";

/// @title Represents a Employer / Candidate work dispute
contract EthlanceDispute is MetahashStore {
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
    
    /// @dev Forwarder Constructor
    /// @param _work_instance The EthlanceWorkContract parent instance for this Dispute.
    /// @param _reason Short string defining the reason for the dispute
    /// @param metahash A structure IPFS data structure defined by a
    /// hash string. The hash is stored as the employer or the
    /// candidate depending on is_employer_request.
    function construct(EthlanceWorkContract _work_instance,
		       string _reason,
		       string metahash,
		       bool is_employer_request) external {

	// TODO: authenticate
	work_instance = _work_instance;
	reason = _reason;
	if (is_employer_request) {
	    appendEmployer(metahash);
	}
	else {
	    appendCandidate(metahash);
	}
	date_created = now;
	date_updated = now;
    }


    function updateDateUpdated() private {
	date_updated = now;
    }


    /// @dev Append a metahash, which will identify the type of user
    /// and append to a MetahashStore
    /// @param metahash The metahash string you wish to append to hash listing.
    /*
      Notes:

      - Only the Candidate, Arbiter, and Employer can append a
        metahash string. The metahash structure is predefined.

      - Retrieving data from the metahash store (getHashByIndex)
        should contain a comparison between the user_type and the data
        present to guarantee valid data from each constituent within
        the listing.

     */
    function appendMetahash(string metahash) external {
	if (work_instance.store_instance().employer_address() == msg.sender) {
	    appendEmployer(metahash);
	    updateDateUpdated();
	}
	else if (work_instance.candidate_address() == msg.sender) {
	    appendCandidate(metahash);
	    updateDateUpdated();
	}
	else if (work_instance.store_instance().accepted_arbiter() == msg.sender) {
	    appendArbiter(metahash);
	    updateDateUpdated();
	}
	else {
	    revert("You are not privileged to append a comment.");
	}
    }

}
