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
  mapping(address => bool) internal jobTokenMapping;

  // Listing of all accepted tokens
  address[] internal jobTokenListing;

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
  /// @param tokenAddress The address of the ERC20 Token
  function addToken(address tokenAddress)
    public {
    require(owner == msg.sender, "Only the owner can add a job token.");

    //TODO: check to see if it's an ERC20 token

    jobTokenListing.push(tokenAddress);
    jobTokenMapping[tokenAddress] = true;
  }


  //
  // Views
  //

  /// @dev Returns true if the store contains the given token.
  function hasToken(address tokenAddress) public view returns (bool) {
    return jobTokenMapping[tokenAddress];
  }


  /// @dev Gets all active tokens for a particular job contract
  /// @return the address of the token at the given index.
  function getTokenByIndex(uint index)
    public view returns (address) {
    require(index < jobTokenListing.length, "Index out of bounds");
    return jobTokenListing[index];
  }


  /// @dev Returns the total number of tokens
  function getTokenCount()
    public view returns(uint) {
    return jobTokenListing.length;
  }


  /// @dev Fire events specific to the work contract
  /// @param eventName Unique to give the fired event
  /// @param eventData Additional event data to include in the
  /// fired event.
  function fireEvent(string memory eventName, uint[] memory eventData) private {
    registry.fireEvent(eventName, version, eventData);
  }

}
