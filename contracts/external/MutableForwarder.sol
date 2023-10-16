// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;

import "./DelegateProxy.sol";
import "./ds-auth/auth.sol";
// import "../JobStorage.sol";

/**
 * @title Forwarder proxy contract with editable target
 */
contract MutableForwarder is DelegateProxy, DSAuth {
  // Storage layout is inherited from JobStorage
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
    payable(target).transfer(msg.value);
  }

  fallback() external payable {
    delegatedFwd(target, msg.data);
  }

}
