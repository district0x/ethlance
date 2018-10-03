pragma solidity ^0.4.18;

import "../proxy/DelegateProxy.sol";
import "../auth/DSAuth.sol";

/**
 * @title Forwarder proxy contract with editable target
 *
 * @dev For TCR Registry contracts (Registry.sol, ParamChangeRegistry.sol) we use mutable forwarders instead of using
 * contracts directly. This is for better upgradeability. Since registry contracts fire all events related to registry
 * entries, we want to be able to access whole history of events always on the same address. Which would be address of
 * a MutableForwarder. When a registry contract is replaced with updated one, mutable forwarder just replaces target
 * and all events stay still accessible on the same address.
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

  function() payable {
    delegatedFwd(target, msg.data);
  }

}