pragma solidity ^0.4.4;

import "safeMath.sol";
import "userLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "jobActionLibrary.sol";
import "invoiceLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";

contract Ethlance is Ownable {
    address public eternalStorage;

    function Ethlance(address _eternalStorage) {
        eternalStorage = _eternalStorage;
        setConfig("max-user-languages", 10);
        setConfig("max-freelancer-categories", 20);
        setConfig("max-freelancer-skills", 15);
        setConfig("max-user-description", 1000);
        setConfig("max-job-description", 1000);
        setConfig("max-job-title", 100);
    }

    function setConfig(string key, uint val)
    onlyOwner {
        EternalStorage(eternalStorage).setUIntValue(sha3("config/", key), val);
    }

    function getConfig(string key) constant returns(uint) {
        return EternalStorage(eternalStorage).getUIntValue(sha3("config/", key));
    }

    function setUser(bytes32 name, bytes32 gravatar, uint16 country, uint[] languages) {
        if (languages.length > getConfig("max-user-languages")) throw;
        UserLibrary.setUser(eternalStorage, msg.sender, name, gravatar, country, languages);
    }

    function setFreelancer(bytes32 name, bytes32 gravatar, uint16 country, uint[] languages,
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        public
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
        public
    {
        if (categories.length > getConfig("max-freelancer-categories")) throw;
        if (skills.length > getConfig("max-freelancer-skills")) throw;
        if (bytes(description).length > getConfig("max-user-description")) throw;
        var userId = UserLibrary.getUserId(eternalStorage, msg.sender);
        UserLibrary.setFreelancer(eternalStorage, userId, isAvailable, jobTitle, hourlyRate, categories, skills, description);
    }

    function setEmployer(bytes32 name, bytes32 gravatar, uint16 country, uint[] languages, string description) public {
        setUser(name, gravatar, country, languages);
        setEmployer(description);
    }

    function setEmployer(string description) public {
        if (bytes(description).length > getConfig("max-user-description")) throw;
        var userId = UserLibrary.getUserId(eternalStorage, msg.sender);
        UserLibrary.setEmployer(eternalStorage, userId, description);
    }

    function addJob(
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items
    )
    {
        if (bytes(description).length > getConfig("max-job-description")) throw;
        if (bytes(title).length > getConfig("max-title-description")) throw;
        uint employerId = UserLibrary.getUserId(eternalStorage, msg.sender);
        JobLibrary.addJob(eternalStorage, employerId, title, description, skills, language, budget, uint8Items);
    }

    function searchJobs(
        uint categoryId,
        uint[] skills,
        uint8[] paymentTypes,
        uint8[] experienceLevels,
        uint8[] estimatedDurations,
        uint8[] hoursPerWeeks,
        uint minBudget,
        uint8 minEmployerAvgRating,
        uint16 countryId,
        uint languageId,
        uint offset,
        uint limit
    )
        constant public returns (uint[] jobIds)
    {
        uint8[][] memory uint8Filters; // To avoid compiler stack too deep error
        uint8Filters[0] = paymentTypes;
        uint8Filters[1] = experienceLevels;
        uint8Filters[2] = estimatedDurations;
        uint8Filters[3] = hoursPerWeeks;
        jobIds = JobLibrary.searchJobs(eternalStorage, categoryId, skills, uint8Filters, minBudget, minEmployerAvgRating, countryId, languageId);
        return SharedLibrary.getPage(jobIds, offset, limit);
    }

    function getJobDetail(uint jobId) public constant returns (bytes32[], uint8[], uint[], uint[]) {
        return JobLibrary.getJobDetail(eternalStorage, jobId);
    }

    function searchFreelancers(
        uint categoryId,
        uint[] skills,
        uint8 minAvgRating,
        uint minContractsCount,
        uint minHourlyRate,
        uint maxHourlyRate,
        uint16 countryId,
        uint languageId,
        uint offset,
        uint limit
    )
        constant returns
        (uint[] userIds,
        bytes32[] descriptionKeys)
    {
        userIds = UserLibrary.searchFreelancers(eternalStorage, categoryId, skills, minAvgRating, minContractsCount,
            minHourlyRate, maxHourlyRate, countryId, languageId);
        userIds = SharedLibrary.getPage(userIds, offset, limit);

        return(userIds, UserLibrary.getDescriptionKeys(userIds));
    }

    function getUserDetail(address userAddress)
        public constant returns
    (
        bytes32[] bytesItems,
        uint[] uintItems,
        bool[] boolItems,
        uint[] categories,
        uint[] skills,
        uint[] languages)
    {
        var userId = UserLibrary.getUserId(eternalStorage, userAddress);
        return UserLibrary.getUserDetail(eternalStorage, userId);
    }

    function getFreelancerProposals(uint userId, uint8 jobActionsType)
        public constant returns
    (
        uint[] jobIds,
        uint[] proposalsCounts,
        uint[] invitationsCounts,
        uint[] proposedOns,
        uint[] invitedOns,
        uint[] jobCreatedOns
        )
    {
        return UserLibrary.filterFreelancerJobActions(eternalStorage, userId, jobActionsType);
    }


}