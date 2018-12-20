pragma solidity ^0.5.0;

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
    uint public dispute_index;
    
    uint public date_created;

    uint public date_updated;

    // When date_resolved is set >0, this reflects completion
    uint public date_resolved;

    // Title of dispute, describing the reason for the dispute
    string public reason;

    // The amount that the employer did receive as a result of
    // resolution, with a token address for the type of ERC20 token.
    uint public employer_resolution_amount;
    address public employer_resolution_token;

    // The amount that the candidate did receive as a result of
    // resolution, with a token address for the type of ERC20 token.
    uint public candidate_resolution_amount;
    address public candidate_resolution_token;

    // The amount that the arbiter did receive as a result of the
    // resolution, with a token address for the type of ERC20 token.
    uint public arbiter_resolution_amount;
    address public arbiter_resolution_token;

    // The EthlanceWorkContract reference.
    EthlanceWorkContract public work_instance;
    
    /// @dev Forwarder Constructor
    /// @param _work_instance The EthlanceWorkContract parent instance for this Dispute.
    /// @param _reason Short string defining the reason for the dispute
    /// @param metahash A structure IPFS data structure defined by a
    /// hash string. The hash is stored as the employer or the
    /// candidate depending on is_employer_request.
    function construct(EthlanceWorkContract _work_instance,
		       uint _dispute_index,
		       string calldata _reason,
		       string calldata metahash,
		       bool is_employer_request) external {

	// TODO: authenticate
	work_instance = _work_instance;
	dispute_index = _dispute_index;
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
    function appendMetahash(string calldata metahash) external {
	if (work_instance.store_instance().employer_address() == msg.sender) {
	    appendEmployer(metahash);
	}
	else if (work_instance.candidate_address() == msg.sender) {
	    appendCandidate(metahash);
	}
	else if (work_instance.store_instance().accepted_arbiter() == msg.sender) {
	    appendArbiter(metahash);
	}
	else {
	    revert("You are not privileged to append a comment.");
	}
	updateDateUpdated();
    }

    
    /// @dev Resolves the dispute between the employer and the
    /// candidate, and pays the employer and the candidate's the given
    /// amounts.
    /// @param _employer_amount The amount of tokens to pay the employer for resolution.
    /// @param _employer_token The token address of the type of token, set to 0x0 for ETH.
    /// @param _candidate_amount The amount of tokens to pay the candidate for resolution.
    /// @param _candidate_token The token address of the type of token, set to 0x0 for ETH.
    /// @param _arbiter_amount The amount of tokens to pay the arbiter for resolution.
    /// @param _arbiter_token The token address of the type of token, set to 0x0 for ETH.
    function resolve(uint _employer_amount,
		     address _employer_token,
		     uint _candidate_amount,
		     address _candidate_token,
		     uint _arbiter_amount,
		     address _arbiter_token) external {
	require(work_instance.store_instance().accepted_arbiter() == msg.sender,
		"Only the accepted arbiter can resolve a dispute.");
	require(date_resolved == 0, "This dispute has already been resolved.");
	work_instance.resolveDispute(_employer_amount, _employer_token,
				     _candidate_amount, _candidate_token,
				     _arbiter_amount, _arbiter_token);
	employer_resolution_amount = _employer_amount;
	employer_resolution_token = _employer_token;
	candidate_resolution_amount = _candidate_amount;
	candidate_resolution_token = _candidate_token;
	arbiter_resolution_amount = _arbiter_amount;
	arbiter_resolution_token = _arbiter_token;
	date_resolved = now;
	updateDateUpdated();
    }

    
    /// @dev Returns true if the current dispute is resolved.
    function isResolved() external view returns(bool) {
	return date_resolved != 0;
    }

}
