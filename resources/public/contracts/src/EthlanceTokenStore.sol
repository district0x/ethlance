pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "./EthlanceJobStore.sol";
import "token/IERC20.sol";


/// @title Used to store ERC20 token contracts for the Job.
contract EthlanceTokenStore {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Collections
    //

    // Determine what token addresses are linked to the job store
    mapping(address => bool) internal job_token_mapping;

    // Listing of all accepted tokens
    address[] internal job_token_listing;

    //
    // Members
    //

    EthlanceJobStore job_instance;
    

    // Forwarded Constructor
    function construct(address _owner) public {
	//TODO: auth

	job_instance = EthlanceJobStore(_owner);
	
    }


    //
    // Methods
    //

    /// @dev Add a job token for a job contract
    /// @param token_address The address of the ERC20 Token
    function addJobToken(address token_address)
	public {
	//TODO: checks

	job_token_listing.push(token_address);
	job_token_mapping[token_address] = true;
    }


    //
    // Views
    //

    /// @dev Returns true if the JobStore supports the given token.
    function hasJobToken(address token_address) public view returns (bool) {
	return job_token_mapping[token_address];
    }


    /// @dev Gets all active job tokens for a particular job contract
    /// @return the address of the token at the given index.
    function getJobTokenById(uint index)
	public view returns (address) {
	require(index < job_token_listing.length, "Index out of bounds");
	return job_token_listing[index];
    }


    /// @dev Returns the total number of job tokens
    function getJobTokenCount()
	public view returns(uint) {
	return job_token_listing.length;
    }


    /// @dev Fire events specific to the work contract
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string memory event_name, uint[] memory event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }

}
