pragma solidity ^0.4.4;

import "ethlanceSetter.sol";

contract EthlanceUser is EthlanceSetter {

    function EthlanceUser(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setUser(bytes32 name, bytes32 gravatar, uint country, uint[] languages)
    onlyActiveSmartContract
    {
        if (languages.length > getConfig("max-user-languages")) throw;
        UserLibrary.setUser(ethlanceDB, msg.sender, name, gravatar, country, languages);
    }

    function registerFreelancer(bytes32 name, bytes32 gravatar, uint country, uint[] languages,
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
    {
        setUser(name, gravatar, country, languages);
        setFreelancer(isAvailable, jobTitle, hourlyRate, categories, skills, description);
    }

    function setFreelancer(
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (categories.length > getConfig("max-freelancer-categories")) throw;
        if (skills.length > getConfig("max-freelancer-skills")) throw;
        if (bytes(description).length > getConfig("max-user-description")) throw;
        UserLibrary.setFreelancer(ethlanceDB, getSenderUserId(), isAvailable, jobTitle, hourlyRate, categories,
            skills, description);
    }

    function registerEmployer(bytes32 name, bytes32 gravatar, uint country, uint[] languages, string description)
    onlyActiveSmartContract
    {
        setUser(name, gravatar, country, languages);
        setEmployer(description);
    }

    function setEmployer(string description)
    onlyActiveSmartContract
    onlyActiveUser
    {
        if (bytes(description).length > getConfig("max-user-description")) throw;
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