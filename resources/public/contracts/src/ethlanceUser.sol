pragma solidity ^0.4.4;

import "ethlanceSetter.sol";
import "sharedLibrary.sol";

contract EthlanceUser is EthlanceSetter {

    function EthlanceUser(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setUser(string name, bytes32 gravatar, uint country, uint[] languages)
    onlyActiveSmartContract
    {
        if (languages.length > getConfig("max-user-languages")) throw;
        if (bytes(name).length > getConfig("max-user-name")) throw;
        if (bytes(name).length < getConfig("min-user-name")) throw;
        UserLibrary.setUser(ethlanceDB, msg.sender, name, gravatar, country, languages);
    }

    function registerFreelancer(string name, bytes32 gravatar, uint country, uint[] languages,
        bool isAvailable,
        string jobTitle,
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
        if (skills.length > getConfig("max-freelancer-skills")) throw;
        if (bytes(description).length > getConfig("max-user-description")) throw;
        if (bytes(jobTitle).length > getConfig("max-freelancer-job-title")) throw;
        UserLibrary.setFreelancer(ethlanceDB, getSenderUserId(), isAvailable, jobTitle, hourlyRate, categories,
            skills, description);
    }


    function registerEmployer(string name, bytes32 gravatar, uint country, uint[] languages, string description)
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

//    function diff(uint[] _old, uint[] _new) constant returns(uint[] added, uint[] removed) {
//        return SharedLibrary.diff(_old, _new);
//    }
//
//    function sort(uint[] array) constant returns(uint[]) {
//        return SharedLibrary.sort(array);
//    }
//
//    function intersect(uint[] a, uint[] b) constant returns(uint[] c) {
//        return SharedLibrary.intersect(a, b);
//    }

}