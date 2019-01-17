pragma solidity ^0.5.0;

import "../proxy/DelegateProxy.sol";

contract ThirdForwarder is DelegateProxy {
  // After compiling contract, the address placeholder is replaced in the bytecode by the real target address
  address public constant target = 0xdBDadBADbabDABdbadabdbaFffD1234fDfAdBcCc; // checksumed to silence warning

  /*
  * @dev Forwards all calls to target
  */
  function() external payable {
    delegatedFwd(target, msg.data);
  }
}
