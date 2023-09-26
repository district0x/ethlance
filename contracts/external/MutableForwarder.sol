// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;

import "./DelegateProxy.sol";
import "./ds-auth/auth.sol";

/**
 * @title Forwarder proxy contract with editable target
 */
contract MutableForwarder is DelegateProxy, DSAuth {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

  /**
   * @dev Replaces targer forwarder contract is pointing to
   * Only authenticated user can replace target

   * @param _target New target to proxy into
   */
  function setTarget(address _target) public auth {
    target = _target;
  }

  event Received(address, uint);
  receive() external payable {
    emit Received(msg.sender, msg.value);
  }

  fallback() external payable {
    delegatedFwd(target, msg.data);
  }

}
