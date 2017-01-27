pragma solidity ^0.4.8;

import "Ownable.sol";
import "userLibrary.sol";

contract EthlanceSetter is Ownable {
    address public ethlanceDB;
    uint8 public smartContractStatus;
    event onSmartContractStatusSet(uint8 status);

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
        onSmartContractStatusSet(_status);
    }

    function getSenderUserId() returns(uint) {
        return UserLibrary.getUserId(ethlanceDB, msg.sender);
    }

    function getConfig(bytes32 key) constant returns(uint) {
        return EthlanceDB(ethlanceDB).getUIntValue(sha3("config/", key));
    }
}
