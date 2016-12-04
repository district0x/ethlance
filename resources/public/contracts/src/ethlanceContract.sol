pragma solidity ^0.4.4;

import "ethlanceSetter.sol";
import "contractLibrary.sol";

contract EthlanceContract is EthlanceSetter {

    function EthlanceContract(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addJobContract(
        uint contractId,
        uint rate,
        string description,
        bool isHiringDone
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        ContractLibrary.addContract(ethlanceDB, getSenderUserId(), contractId, rate, description, isHiringDone);
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

    function addJobProposal(
        uint jobId,
        string description,
        uint rate
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        ContractLibrary.addProposal(ethlanceDB, jobId, getSenderUserId(), description, rate);
    }

    function addJobInvitation(
        uint jobId,
        uint freelancerId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        ContractLibrary.addInvitation(ethlanceDB, getSenderUserId(), jobId, freelancerId, description);
    }
}