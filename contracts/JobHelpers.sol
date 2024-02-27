// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "./EthlanceStructs.sol";

library JobHelpers {
  struct Dispute {
    uint invoiceId;
    address creator;
    EthlanceStructs.TokenValue resolution;
    uint raisedAt;
    bool resolved;
  }

  function isAcceptedArbiterIdle(uint[] calldata disputeIds,
                                 mapping (uint => Dispute) storage disputes,
                                 uint idleTimeout,
                                 uint timeNow)
                                 public view returns (bool) {
    for(uint i = 0; i < disputeIds.length; i++) {
      if(disputes[disputeIds[i]].resolved == false &&
         (timeNow - disputes[disputeIds[i]].raisedAt) > idleTimeout) {
        return true;
      }
    }
    return false;
  }

  function getDeposits(bytes32[] calldata depositIds,
                       mapping(bytes32 => EthlanceStructs.Deposit) storage deposits,
                       address depositor)
                       public view returns (EthlanceStructs.TokenValue[] memory) {
    EthlanceStructs.TokenValue[] memory selectedValues = new EthlanceStructs.TokenValue[](depositIds.length);
    uint lastFilled = 0;
    for(uint i = 0; i < depositIds.length; i++) {
      bytes32 depositId = depositIds[i];
      EthlanceStructs.Deposit memory currentDeposit = deposits[depositId];
      if(currentDeposit.depositor == depositor) {
        selectedValues[lastFilled] = currentDeposit.tokenValue;
        lastFilled += 1;
      }
    }

    EthlanceStructs.TokenValue[] memory compactedValues = new EthlanceStructs.TokenValue[](lastFilled);
    for(uint i = 0; i < lastFilled; i++) {
      if(selectedValues[i].value != 0) {
        compactedValues[i] = selectedValues[i];
      }
    }
    return compactedValues;
  }
}
