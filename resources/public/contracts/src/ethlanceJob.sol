pragma solidity ^0.4.4;

import "ethlanceSetter.sol";
import "jobLibrary.sol";

contract EthlanceJob is EthlanceSetter {

    function EthlanceJob(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
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
        if (bytes(title).length > getConfig("max-job-title")) throw;
        if (skills.length > getConfig("max-job-skills")) throw;
        JobLibrary.addJob(ethlanceDB, getSenderUserId(), title, description, skills, language, budget, uint8Items);
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