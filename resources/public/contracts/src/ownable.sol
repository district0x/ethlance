pragma solidity ^0.4.4;

contract Ownable {
  address public owner = msg.sender;

  /// @notice check if the caller is the owner of the contract
  modifier onlyOwner {
    if (msg.sender != owner) throw;
    _;
  }

  /// @notice change the owner of the contract
  /// @param _newOwner the address of the new owner of the contract.
  function changeOwner(address _newOwner)
  onlyOwner
  {
    if(_newOwner == 0x0) throw;
    owner = _newOwner;
  }
}