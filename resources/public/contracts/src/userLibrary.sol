pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "safeMath.sol";
import "sharedLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "invoiceLibrary.sol";
import "sponsorLibrary.sol";

library UserLibrary {

    //    status:
    //    1: active, 2: blocked

    function getUserCount(address db) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("user/count"));
    }

    function userExists(address db, address userId) internal returns(bool) {
        return getStatus(db, userId) > 0;
    }

    function getAllUsers(address db) internal returns(address[]) {
        return SharedLibrary.getAddressArray(db, "user/ids", "user/count");
    }

    function setUser(
        address db,
        address userId,
        string name,
        string email,
        bytes32 gravatar,
        uint country,
        uint state,
        uint[] languages,
        string github,
        string linkedin
    )
        internal
    {
        require(country != 0);

        if (!userExists(db, userId)) {
            EthlanceDB(db).setUIntValue(sha3("user/created-on", userId), now);
            EthlanceDB(db).setUInt8Value(sha3("user/status", userId), 1);
            SharedLibrary.addArrayItem(db, "user/ids", "user/count", userId);
        }

        EthlanceDB(db).setStringValue(sha3("user/name", userId), name);
        EthlanceDB(db).setStringValue(sha3("user/email", userId), email);
        EthlanceDB(db).setBytes32Value(sha3("user/gravatar", userId), gravatar);
        EthlanceDB(db).setUIntValue(sha3("user/country", userId), country);
        EthlanceDB(db).setUIntValue(sha3("user/state", userId), state);
        EthlanceDB(db).setStringValue(sha3("user/github", userId), github);
        EthlanceDB(db).setStringValue(sha3("user/linkedin", userId), linkedin);
        setUserLanguages(db, userId, languages);
    }

    function setFreelancer(
        address db,
        address userId,
        bool isAvailable,
        string jobTitle,
        uint hourlyRate,
        uint8 hourlyRateCurrency,
        uint[] categories,
        uint[] skills,
        string description
    )
        internal
    {
        require(userExists(db, userId));
        EthlanceDB(db).setBooleanValue(sha3("user/freelancer?", userId), true);
        EthlanceDB(db).setBooleanValue(sha3("freelancer/available?", userId), isAvailable);
        EthlanceDB(db).setStringValue(sha3("freelancer/job-title", userId), jobTitle);
        EthlanceDB(db).setUIntValue(sha3("freelancer/hourly-rate", userId), hourlyRate);
        EthlanceDB(db).setUInt8Value(sha3("freelancer/hourly-rate-currency", userId), hourlyRateCurrency);
        EthlanceDB(db).setStringValue(sha3("freelancer/description", userId), description);
        setFreelancerSkills(db, userId, skills);
        setFreelancerCategories(db, userId, categories);
    }


    function setEmployer(
        address db,
        address userId,
        string description
    )
        internal
    {
        require(userExists(db, userId));
        EthlanceDB(db).setBooleanValue(sha3("user/employer?", userId), true);
        EthlanceDB(db).setStringValue(sha3("employer/description", userId), description);
    }

    function setStatus(address db, address userId, uint8 status) internal {
        EthlanceDB(db).setUInt8Value(sha3("user/status", userId), status);
    }

    function setFreelancerSkills(address db, address userId, uint[] skills) internal {
        uint[] memory added;
        uint[] memory removed;
        uint[] memory currentSkills = getFreelancerSkills(db, userId);
        (added, removed) = SharedLibrary.diff(currentSkills, skills);
        SkillLibrary.addFreelancer(db, added, userId);
        SkillLibrary.removeFreelancer(db, removed, userId);

        if (added.length > 0 || removed.length > 0) {
            SharedLibrary.setIdArray(db, userId, "freelancer/skills", "freelancer/skills-count", skills);
        }
    }

    function getFreelancerSkills(address db, address userId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, userId, "freelancer/skills", "freelancer/skills-count");
    }

    function setFreelancerCategories(address db, address userId, uint[] categories) internal {
        uint[] memory added;
        uint[] memory removed;
        uint[] memory currentCategories = getFreelancerCategories(db, userId);
        (added, removed) = SharedLibrary.diff(currentCategories, categories);
        CategoryLibrary.addFreelancer(db, added, userId);
        CategoryLibrary.removeFreelancer(db, removed, userId);
        if (added.length > 0 || removed.length > 0) {
            SharedLibrary.setIdArray(db, userId, "freelancer/categories", "freelancer/categories-count", categories);
        }
    }

    function getFreelancerCategories(address db, address userId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, userId, "freelancer/categories", "freelancer/categories-count");
    }

    function setUserNotifications(address db, address userId, bool[] boolNotifSettings, uint8[] uint8NotifSettings) internal {
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-all?", userId), boolNotifSettings[0]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-newsletter?", userId), boolNotifSettings[1]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-job-invitation-added?", userId), boolNotifSettings[2]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-job-contract-added?", userId), boolNotifSettings[3]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-invoice-paid?", userId), boolNotifSettings[4]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-job-proposal-added?", userId), boolNotifSettings[5]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-invoice-added?", userId), boolNotifSettings[6]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-job-contract-feedback-added?", userId), boolNotifSettings[7]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-message-added?", userId), boolNotifSettings[8]);
        EthlanceDB(db).setBooleanValue(sha3("user.notif/disabled-on-job-sponsorship-added?", userId), boolNotifSettings[9]);
        EthlanceDB(db).setUInt8Value(sha3("user.notif/job-recommendations", userId), uint8NotifSettings[0]);
    }

    function addReceivedMessage(address db, address userId, uint messageId) internal {
        SharedLibrary.addIdArrayItem(db, userId, "user/received-messages", "user/received-messages-count", messageId);
    }

    function addSentMessage(address db, address userId, uint messageId) internal {
        SharedLibrary.addIdArrayItem(db, userId, "user/sent-messages", "user/sent-messages-count", messageId);
    }

    function isActiveEmployer(address db, address userId) internal returns(bool) {
        return EthlanceDB(db).getBooleanValue(sha3("user/employer?", userId)) &&
               hasStatus(db, userId, 1);
    }
    
    function isActiveFreelancer(address db, address userId) internal returns(bool) {
        return EthlanceDB(db).getBooleanValue(sha3("user/freelancer?", userId)) &&
               hasStatus(db, userId, 1);
    }

    function hasStatus(address db, address userId, uint8 status) internal returns(bool) {
        return status == EthlanceDB(db).getUInt8Value(sha3("user/status", userId));
    }

    function getStatus(address db, address userId) internal returns(uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("user/status", userId));
    }

    function setUserLanguages(address db, address userId, uint[] languages) internal {
        SharedLibrary.setIdArray(db, userId, "user/languages", "user/languages-count", languages);
    }

    function addEmployerJob(address db, address userId, uint jobId) internal {
        SharedLibrary.addIdArrayItem(db, userId, "employer/jobs", "employer/jobs-count", jobId);
    }

    function getEmployerJobs(address db, address userId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, userId, "employer/jobs", "employer/jobs-count");
    }

    function getFreelancerContractsCount(address db, address userId) internal returns(uint) {
        return SharedLibrary.getIdArrayItemsCount(db, userId, "freelancer/contracts-count");
    }

    function addFreelancerContract(address db, address userId, uint contractId) internal {
        SharedLibrary.addIdArrayItem(db, userId, "freelancer/contracts", "freelancer/contracts-count", contractId);
    }
    
    function getFreelancerContracts(address db, address userId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, userId, "freelancer/contracts", "freelancer/contracts-count");
    }
    
    function addEmployerContract(address db, address userId, uint contractId) internal {
        SharedLibrary.addIdArrayItem(db, userId, "employer/contracts", "employer/contracts-count", contractId);
    }
    
    function getEmployerContracts(address db, address userId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, userId, "employer/contracts", "employer/contracts-count");
    }
    
    function addFreelancerTotalInvoiced(address db, address userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("freelancer/total-invoiced", userId), amount);
    }
    
    function subFreelancerTotalInvoiced(address db, address userId, uint amount) internal {
        EthlanceDB(db).subUIntValue(sha3("freelancer/total-invoiced", userId), amount);
    }
    
    function addEmployerTotalInvoiced(address db, address userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("employer/total-invoiced", userId), amount);
    }
    
    function subEmployerTotalInvoiced(address db, address userId, uint amount) internal {
        EthlanceDB(db).subUIntValue(sha3("employer/total-invoiced", userId), amount);
    }

    function addSponsorship(address db, address userId, uint sponsorshipId) internal {
        SharedLibrary.addIdArrayItem(db, userId, "user/sponsorships", "user/sponsorships-count", sponsorshipId);
    }

    function addTotalSponsored(address db, address userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("user/total-sponsored", userId), amount);
    }

    function subTotalSponsored(address db, address userId, uint amount) internal {
        EthlanceDB(db).subUIntValue(sha3("user/total-sponsored", userId), amount);
    }

    function getSponsorships(address db, address userId) internal returns (uint[]) {
        return SharedLibrary.getIdArray(db, userId, "user/sponsorships", "user/sponsorships-count");
    }

    function getSponsorships(address db, address userId, bool isRefunded) internal returns (uint[]) {
        var sponsorshipIds = getSponsorships(db, userId);
        var args = new uint[](1);
        if (isRefunded) {
            args[0] = 1;
        } else {
            args[0] = 0;
        }
        return SharedLibrary.filter(db, userSponsorshipsPred, sponsorshipIds, args);
    }

    function userSponsorshipsPred(address db, uint[] args, uint sponsorshipId)
    internal returns(bool)
    {
        var isRefunded = SponsorLibrary.isSponsorshipRefunded(db, sponsorshipId);
        if (args[0] == 1) {
            return isRefunded;
        } else {
            return !isRefunded;
        }
    }
    
    function addToAvgRating(address db, address userId, string countKey, string key, uint8 rating) internal {
        var ratingsCount = EthlanceDB(db).getUIntValue(sha3(countKey, userId));
        var currentAvgRating = EthlanceDB(db).getUInt8Value(sha3(key, userId));
        var newRatingsCount = SafeMath.safeAdd(ratingsCount, 1);
        uint newAvgRating;
        if (ratingsCount == 0) {
            newAvgRating = rating;
        } else {
            var newTotalRating = SafeMath.safeAdd(SafeMath.safeMul(currentAvgRating, ratingsCount), rating);
            newAvgRating = newTotalRating / newRatingsCount;
        }
        EthlanceDB(db).setUIntValue(sha3(countKey, userId), newRatingsCount);
        EthlanceDB(db).setUInt8Value(sha3(key, userId), uint8(newAvgRating));
    }
    
    function addToFreelancerAvgRating(address db, address userId, uint8 rating) internal {
        addToAvgRating(db, userId, "freelancer/avg-rating-count", "freelancer/avg-rating", rating);
    }

    function getFreelancerAvgRating(address db, address userId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("freelancer/avg-rating", userId));
    }

    function addToEmployerAvgRating(address db, address userId, uint8 rating) {
        addToAvgRating(db, userId, "employer/avg-rating-count", "employer/avg-rating", rating);
    }

    function getEmployerAvgRating(address db, address userId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("employer/avg-rating", userId));
    }

    function addToFreelancerTotalEarned(address db, address userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("freelancer/total-earned", userId), amount);
    }

    function addToEmployerTotalPaid(address db, address userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("employer/total-paid", userId), amount);
    }

    function isFromCountry(address db, address userId, uint countryId) internal returns(bool) {
        if (countryId == 0) {
            return true;
        }
        return countryId == EthlanceDB(db).getUIntValue(sha3("user/country", userId));
    }
    
    function isFromState(address db, address userId, uint stateId) internal returns(bool) {
        if (stateId == 0) {
            return true;
        }
        return stateId == EthlanceDB(db).getUIntValue(sha3("user/state", userId));
    }

    function hasMinRating(address db, address userId, uint8 minAvgRating) internal returns(bool) {
        if (minAvgRating == 0) {
            return true;
        }
        return minAvgRating <= EthlanceDB(db).getUInt8Value(sha3("freelancer/avg-rating", userId));
    }

    function hasFreelancerMinRatingsCount(address db, address userId, uint minRatingsCount) internal returns(bool) {
        if (minRatingsCount == 0) {
            return true;
        }
        return minRatingsCount <= EthlanceDB(db).getUIntValue(sha3("freelancer/ratings-count", userId));
    }

    function getFreelancerHourlyRate(address db, address userId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("freelancer/hourly-rate", userId));
    }

    function getFreelancerHourlyRateCurrency(address db, address userId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("freelancer/hourly-rate-currency", userId));
    }

    function hasHourlyRateWithinRange(address db, address userId, uint[] minHourlyRates, uint[] maxHourlyRates) internal returns(bool) {
        var hourlyRateCurrency = getFreelancerHourlyRateCurrency(db, userId);
        if (minHourlyRates[hourlyRateCurrency] == 0 && maxHourlyRates[hourlyRateCurrency] == 0) {
            return true;
        }
        var hourlyRate = getFreelancerHourlyRate(db, userId);

        if (minHourlyRates[hourlyRateCurrency] <= hourlyRate && maxHourlyRates[hourlyRateCurrency] == 0) {
            return true;
        }

        return minHourlyRates[hourlyRateCurrency] <= hourlyRate && hourlyRate <= maxHourlyRates[hourlyRateCurrency];
    }

    function hasLanguage(address db, address userId, uint languageId) internal returns (bool) {
        if (languageId == 0) {
            return true;
        }
        var count = SharedLibrary.getIdArrayItemsCount(db, userId, "user/languages-count");
        for (uint i = 0; i < count ; i++) {
            if (languageId == EthlanceDB(db).getUIntValue(sha3("user/languages", userId, i))) {
                return true;
            }
        }
        return false;
    }

    function hasJobRecommendations(address db, address userId, uint jobRecommendations) internal returns (bool) {
        if (jobRecommendations == 0) {
            return true;
        }
        if (EthlanceDB(db).getBooleanValue(sha3("user.notif/disabled-all?", userId))) {
            return false;
        }

        uint userJobRecommendations = EthlanceDB(db).getUInt8Value(sha3("user.notif/job-recommendations", userId));
        if (userJobRecommendations == 0 && jobRecommendations == 1) { // default value
            return true;
        }

        return userJobRecommendations == jobRecommendations;
    }

    function isFreelancerAvailable(address db, address userId) internal returns (bool) {
        return EthlanceDB(db).getBooleanValue(sha3("freelancer/available?", userId));
    }

    function searchFreelancers(
        address db,
        uint categoryId,
        uint[] skillsAnd,
        uint[] skillsOr,
        uint8 minAvgRating,
        uint minRatingsCount,
        uint[] minHourlyRates,
        uint[] maxHourlyRates,
        uint[] uintArgs
    )
        internal returns (address[] userIds)
    {
        uint j = 0;
        var allUserIds = SharedLibrary.intersectCategoriesAndSkills(db, categoryId, skillsAnd, skillsOr,
            SkillLibrary.getFreelancers, CategoryLibrary.getFreelancers, getUserCount, getAllUsers);
        userIds = new address[](allUserIds.length);
        for (uint i = 0; i < allUserIds.length ; i++) {
            var userId = allUserIds[i];
            if (isFreelancerAvailable(db, userId) &&
                hasMinRating(db, userId, minAvgRating) &&
                hasFreelancerMinRatingsCount(db, userId, minRatingsCount) &&
                hasHourlyRateWithinRange(db, userId, minHourlyRates, maxHourlyRates) &&
                isFromCountry(db, userId, uintArgs[0]) &&
                isFromState(db, userId, uintArgs[1]) &&
                hasLanguage(db, userId, uintArgs[2]) &&
                hasJobRecommendations(db, userId, uintArgs[3]) &&
                hasStatus(db, userId, 1)
            ) {
                userIds[j] = userId;
                j++;
            }
        }
        return SharedLibrary.take(j, userIds);
    }


    function userContractsPred(address db, uint[] contractStatuses, uint[] jobStatuses, uint contractId)
    internal returns(bool)
    {
        var jobId = ContractLibrary.getJob(db, contractId);
        return ((contractStatuses.length == 0 ||
                SharedLibrary.contains(contractStatuses, ContractLibrary.getStatus(db, contractId)))
                &&
                (jobStatuses.length == 0 ||
                SharedLibrary.contains(jobStatuses, JobLibrary.getStatus(db, jobId))));
    }

    function getUserContractsByStatus
    (
        address db,
        address userId,
        uint[] contractStatuses,
        uint[] jobStatuses,
        function(address, address) returns (uint[] memory) getContracts
    )
        internal returns (uint[] result)
    {
        return SharedLibrary.filter(db, userContractsPred, getContracts(db, userId), contractStatuses, jobStatuses);
    }

    function getFreelancerContractsByStatus(address db, address userId, uint[] contractStatuses, uint[] jobStatuses)
        internal returns (uint[] result)
    {
        return getUserContractsByStatus(db, userId, contractStatuses, jobStatuses, getFreelancerContracts);
    }

    function getEmployerContractsByStatus(address db, address userId, uint[] contractStatuses, uint[] jobStatuses)
        internal returns (uint[] result)
    {
        return getUserContractsByStatus(db, userId, contractStatuses, jobStatuses, getEmployerContracts);
    }

    function getUserInvoicesByStatus
    (
        address db, 
        address userId,
        uint8 invoiceStatus,
        function(address, address) returns (uint[] memory) getContracts
    )
        internal returns (uint[] result)
    {
        var args = new uint[](1);
        args[0] = invoiceStatus;
        return SharedLibrary.filter(db, InvoiceLibrary.statusPred,
            ContractLibrary.getInvoices(db, getContracts(db, userId)), args);
    }

    function getFreelancerInvoicesByStatus(address db, address userId, uint8 invoiceStatus)
        internal returns (uint[] result)
    {
        return getUserInvoicesByStatus(db, userId, invoiceStatus, getFreelancerContracts);
    }
    
    function getEmployerInvoicesByStatus(address db, address userId, uint8 invoiceStatus)
        internal returns (uint[] result)
    {
        return getUserInvoicesByStatus(db, userId, invoiceStatus, getEmployerContracts);
    }
}

