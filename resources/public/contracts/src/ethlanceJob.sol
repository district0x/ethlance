pragma solidity ^0.4.4;

import "ethlanceSetter.sol";
import "jobLibrary.sol";
import "jobActionLibrary.sol";

contract EthlanceJob is EthlanceSetter {

    function Ethlance(address _ethlanceDB) {
        ethlanceDB = _ethlanceDB;
    }

    function addJob(
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        if (bytes(description).length > getConfig("max-job-description")) throw;
        if (bytes(title).length > getConfig("max-title-description")) throw;
        if (skills.length > getConfig("max-job-skills")) throw;
        JobLibrary.addJob(ethlanceDB, getSenderUserId(), title, description, skills, language, budget, uint8Items);
    }

    function addJobProposal(
        uint jobId,
        string description,
        uint rate
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        JobActionLibrary.addProposal(ethlanceDB, jobId, getSenderUserId(), description, rate);
    }

    function addJobInvitation(
        uint jobId,
        uint freelancerId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        JobActionLibrary.addInvitation(ethlanceDB, getSenderUserId(), jobId, freelancerId, description);
    }

    function setJobStatus(
        uint jobId,
        uint8 status
    )
        onlyOwner
    {
        JobLibrary.setStatus(ethlanceDB, jobId, status);
    }
}