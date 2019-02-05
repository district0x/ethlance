pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "token/IERC20.sol";


/// @title Used to store general ERC20 token contracts.
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

    address public owner;
    

    // Forwarded Constructor
    function construct() external {
	require(owner == address(0), "EthlanceTokenStore contract was already constructed.");
	owner = msg.sender;
    }


    //
    // Methods
    //

    /// @dev Add a job token for a job contract
    /// @param token_address The address of the ERC20 Token
    function addToken(address token_address)
	public {
	require(owner == msg.sender, "Only the owner can add a job token.");

	//TODO: check to see if it's an ERC20 token

	job_token_listing.push(token_address);
	job_token_mapping[token_address] = true;
    }


    //
    // Views
    //

    /// @dev Returns true if the store contains the given token.
    function hasToken(address token_address) public view returns (bool) {
	return job_token_mapping[token_address];
    }


    /// @dev Gets all active tokens for a particular job contract
    /// @return the address of the token at the given index.
    function getTokenByIndex(uint index)
	public view returns (address) {
	require(index < job_token_listing.length, "Index out of bounds");
	return job_token_listing[index];
    }


    /// @dev Returns the total number of tokens
    function getTokenCount()
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
