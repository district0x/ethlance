pragma solidity ^0.4.4;

import "Ownable.sol";
import "userLibrary.sol";

contract EthlanceSetter is Ownable {
    address public ethlanceDB;
    uint8 public smartContractStatus;

    modifier onlyActiveSmartContract {
      if (smartContractStatus != 0) throw;
      _;
    }

    modifier onlyActiveEmployer {
      if (!UserLibrary.isActiveEmployer(ethlanceDB, msg.sender)) throw;
      _;
    }

    modifier onlyActiveFreelancer {
      if (!UserLibrary.isActiveFreelancer(ethlanceDB, msg.sender)) throw;
      _;
    }

    modifier onlyActiveUser {
      if (!UserLibrary.hasStatus(ethlanceDB, getSenderUserId(), 1)) throw;
      _;
    }

    function setSmartContractStatus(
          uint8 _status
    )
      onlyOwner
    {
      smartContractStatus = _status;
    }

    function getSenderUserId() returns(uint) {
        return UserLibrary.getUserId(ethlanceDB, msg.sender);
    }

    function getConfig(string key) constant returns(uint) {
        return EthlanceDB(ethlanceDB).getUIntValue(sha3("config/", key));
    }
}