pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "sharedLibrary.sol";
import "strings.sol";

contract EthlanceUser is EthlanceSetter {
    using strings for *;

    event onFreelancerAdded(uint userId);
    event onEmployerAdded(uint userId);

    function EthlanceUser(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setUser(string name, string email, bytes32 gravatar, uint country, uint state, uint[] languages,
        string github,
        string linkedin
    )
        onlyActiveSmartContract
    {
        if (languages.length > getConfig("max-user-languages")) throw;
        if (languages.length < getConfig("min-user-languages")) throw;
        var nameLen = name.toSlice().len();
        if (nameLen > getConfig("max-user-name")) throw;
        if (nameLen < getConfig("min-user-name")) throw;
        if (email.toSlice().len() > 254) throw;
        if (linkedin.toSlice().len() > 100) throw;
        if (github.toSlice().len() > 100) throw;
        UserLibrary.setUser(ethlanceDB, msg.sender, name, email, gravatar, country, state, languages, github, linkedin);
    }

    function registerFreelancer(string name,  string email, bytes32 gravatar, uint country, uint state, uint[] languages,
        string github,
        string linkedin,

        bool isAvailable,
        string jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
    {
        setUser(name, email, gravatar, country, state, languages, github, linkedin);
        setFreelancer(isAvailable, jobTitle, hourlyRate, categories, skills, description);
        onFreelancerAdded(UserLibrary.getUserId(ethlanceDB, msg.sender));
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

    function registerEmployer(string name, string email, bytes32 gravatar, uint country, uint state, uint[] languages,
        string github,
        string linkedin,

        string description
    )
    onlyActiveSmartContract
    {
        setUser(name, email, gravatar, country, state, languages, github, linkedin);
        setEmployer(description);
        onEmployerAdded(UserLibrary.getUserId(ethlanceDB, msg.sender));
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