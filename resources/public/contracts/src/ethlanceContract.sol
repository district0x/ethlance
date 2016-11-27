pragma solidity ^0.4.4;

import "ethlanceSetter.sol";
import "contractLibrary.sol";

contract EthlanceContract is EthlanceSetter {

    function EthlanceContract(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addJobContract(
        uint jobActionId,
        uint rate,
        bool isHiringDone
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        ContractLibrary.addContract(ethlanceDB, getSenderUserId(), jobActionId, rate, isHiringDone);
    }

    function addJobContractFeedback(
        uint contractId,
        string feedback,
        uint8 rating
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (bytes(feedback).length > getConfig("max-feedback")) throw;
        if (rating > 100) throw;
        ContractLibrary.addFeedback(ethlanceDB, contractId, getSenderUserId(), feedback, rating);
    }
}