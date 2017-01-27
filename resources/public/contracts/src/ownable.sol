pragma solidity ^0.4.8;

contract Ownable {
  address public owner = msg.sender;

  modifier onlyOwner {
    if (msg.sender != owner) throw;
    _;
  }

  function changeOwner(address _newOwner)
  onlyOwner
  {
    if(_newOwner == 0x0) throw;
    owner = _newOwner;
  }
}