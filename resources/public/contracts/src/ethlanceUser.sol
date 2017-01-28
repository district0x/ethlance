pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "sharedLibrary.sol";
import "strings.sol";

contract EthlanceUser is EthlanceSetter {
    using strings for *;

    function EthlanceUser(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setUser(string name, bytes32 gravatar, uint country, uint state, uint[] languages)
    onlyActiveSmartContract
    {
        if (languages.length > getConfig("max-user-languages")) throw;
        if (languages.length < getConfig("min-user-languages")) throw;
        var nameLen = name.toSlice().len();
        if (nameLen > getConfig("max-user-name")) throw;
        if (nameLen < getConfig("min-user-name")) throw;
        UserLibrary.setUser(ethlanceDB, msg.sender, name, gravatar, country, state, languages);
    }

    function registerFreelancer(string name, bytes32 gravatar, uint country, uint state, uint[] languages,
        bool isAvailable,
        string jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
    {
        setUser(name, gravatar, country, state, languages);
        setFreelancer(isAvailable, jobTitle, hourlyRate, categories, skills, description);
    }

    function setFreelancer(
        bool isAvailable,
        string jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (categories.length > getConfig("max-freelancer-categories")) throw;
        if (categories.length < getConfig("min-freelancer-categories")) throw;
        if (skills.length > getConfig("max-freelancer-skills")) throw;
        if (skills.length < getConfig("min-freelancer-skills")) throw;
        if (description.toSlice().len() > getConfig("max-user-description")) throw;
        var jobTitleLen = jobTitle.toSlice().len();
        if (jobTitleLen > getConfig("max-freelancer-job-title")) throw;
        if (jobTitleLen < getConfig("min-freelancer-job-title")) throw;
        UserLibrary.setFreelancer(ethlanceDB, getSenderUserId(), isAvailable, jobTitle, hourlyRate, categories,
            skills, description);
    }

    function registerEmployer(string name, bytes32 gravatar, uint country, uint state, uint[] languages,
        string description
    )
    onlyActiveSmartContract
    {
        setUser(name, gravatar, country, state, languages);
        setEmployer(description);
    }

    function setEmployer(string description
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (description.toSlice().len() > getConfig("max-user-description")) throw;
        UserLibrary.setEmployer(ethlanceDB, getSenderUserId(), description);
    }

    function setUserStatus(
        uint userId,
        uint8 status
    )
        onlyOwner
    {
        UserLibrary.setStatus(ethlanceDB, userId, status);
    }
}