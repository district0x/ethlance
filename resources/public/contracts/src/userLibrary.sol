pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "safeMath.sol";
import "sharedLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";
import "jobActionLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "invoiceLibrary.sol";

library UserLibrary {

    //    status:
    //    1: active, 2: blocked

    function getUserCount(address db) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("user/count"));
    }

    function getUserId(address db, address userAddress) internal returns(uint){
        return EthlanceDB(db).getUIntValue(sha3("user/ids", userAddress));
    }

    function getUserAddress(address db, uint userId) internal returns(address){
        return EthlanceDB(db).getAddressValue(sha3("user/address", userId));
    }

    function setUser(address db,
        address userAddress,
        bytes32 name,
        bytes32 gravatar,
        uint country,
        uint[] languages
    )
        internal returns (uint)
    {
        uint userId;
        uint existingUserId = getUserId(db, userAddress);
        if (existingUserId > 0) {
            userId = existingUserId;
        } else {
            userId = SharedLibrary.createNext(db, "user/count");
        }
        EthlanceDB(db).setAddressValue(sha3("user/address", userId), userAddress);
        EthlanceDB(db).setBytes32Value(sha3("user/name", userId), name);
        EthlanceDB(db).setBytes32Value(sha3("user/gravatar", userId), gravatar);
        EthlanceDB(db).setUIntValue(sha3("user/country", userId), country);
        EthlanceDB(db).setUInt8Value(sha3("user/status", userId), 1);
        EthlanceDB(db).setUIntValue(sha3("user/created-on", userId), now);
        EthlanceDB(db).setUIntValue(sha3("user/ids", userAddress), userId);
        setUserLanguages(db, userId, languages);
        return userId;
    }

    function setFreelancer(address db,
        uint userId,
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        internal
    {
        if (userId == 0) throw;
        EthlanceDB(db).setBooleanValue(sha3("user/freelancer?", userId), true);
        EthlanceDB(db).setBooleanValue(sha3("freelancer/available?", userId), isAvailable);
        EthlanceDB(db).setBytes32Value(sha3("freelancer/job-title", userId), jobTitle);
        EthlanceDB(db).setUIntValue(sha3("freelancer/hourly-rate", userId), hourlyRate);
        EthlanceDB(db).setStringValue(sha3("freelancer/description", userId), description);
        setFreelancerSkills(db, userId, skills);
        setFreelancerCategories(db, userId, categories);
    }

    function setEmployer(address db,
        uint userId,
        string description
    )
        internal
    {
        if (userId == 0) throw;
        EthlanceDB(db).setBooleanValue(sha3("user/employer?", userId), true);
        EthlanceDB(db).setStringValue(sha3("employer/description", userId), description);
    }

    function setStatus(address db, uint userId, uint8 status) internal {
        EthlanceDB(db).setUInt8Value(sha3("user/status", userId), status);
    }

    function setFreelancerSkills(address db, uint userId, uint[] skills) internal {
        uint[] memory added;
        uint[] memory removed;
        uint[] memory currentSkills = getFreelancerSkills(db, userId);
        (added, removed) = SharedLibrary.diff(currentSkills, skills);
        SkillLibrary.addFreelancer(db, added, userId);
        SkillLibrary.removeFreelancer(db, removed, userId);
        if (added.length > 0 || removed.length > 0) {
            SharedLibrary.setUIntArray(db, userId, "freelancer/skills", "freelancer/skills-count", skills);
        }
    }
    
    function getFreelancerSkills(address db, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, userId, "freelancer/skills", "freelancer/skills-count");
    }

    function setFreelancerCategories(address db, uint userId, uint[] categories) internal {
        uint[] memory added;
        uint[] memory removed;
        uint[] memory currentCategories = getFreelancerCategories(db, userId);
        (added, removed) = SharedLibrary.diff(currentCategories, categories);
        CategoryLibrary.addFreelancer(db, added, userId);
        CategoryLibrary.removeFreelancer(db, removed, userId);
        if (added.length > 0 || removed.length > 0) {
            SharedLibrary.setUIntArray(db, userId, "freelancer/categories", "freelancer/categories-count", categories);
        }
    }

    function getFreelancerCategories(address db, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, userId, "freelancer/categories", "freelancer/categories-count");
    }

    function isActiveEmployer(address db, address userAddress) internal returns(bool) {
        var userId = getUserId(db, userAddress);
        return EthlanceDB(db).getBooleanValue(sha3("user/employer?", userId)) &&
               hasStatus(db, userId, 1);
    }
    
    function isActiveFreelancer(address db, address userAddress) internal returns(bool) {
        var userId = getUserId(db, userAddress);
        return EthlanceDB(db).getBooleanValue(sha3("user/freelancer?", userId)) &&
               hasStatus(db, userId, 1);
    }
    
    function hasStatus(address db, uint userId, uint8 status) internal returns(bool) {
        return status == EthlanceDB(db).getUInt8Value(sha3("user/status", userId));
    }

    function setUserLanguages(address db, uint userId, uint[] languages) internal {
        SharedLibrary.setUIntArray(db, userId, "user/languages", "user/languages-count", languages);
    }

    function addEmployerJob(address db, uint userId, uint jobId) internal {
        SharedLibrary.addArrayItem(db, userId, "employer/jobs", "employer/jobs-count", jobId);
    }

    function getEmployerJobs(address db, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, userId, "employer/jobs", "employer/jobs-count");
    }

    function addFreelancerJobAction(address db, uint userId, uint jobActionId) internal {
        SharedLibrary.addArrayItem(db, userId, "freelancer/job-actions", "freelancer/job-actions-count", jobActionId);
    }

    function getFreelancerJobActions(address db, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, userId, "freelancer/job-actions", "freelancer/job-actions-count");
    }
    
    function getFreelancerContractsCount(address db, uint userId) internal returns(uint) {
        return SharedLibrary.getArrayItemsCount(db, userId, "freelancer/contracts-count");
    }

    function addFreelancerContract(address db, uint userId, uint contractId) internal {
        SharedLibrary.addArrayItem(db, userId, "freelancer/contracts", "freelancer/contracts-count", contractId);
    }
    
    function getFreelancerContracts(address db, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, userId, "freelancer/contracts", "freelancer/contracts-count");
    }
    
    function addToAvgRating(address db, uint userId, string countKey, string key, uint8 rating) internal {
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
    
    function addToFreelancerAvgRating(address db, uint userId, uint8 rating) internal {
        addToAvgRating(db, userId, "freelancer/avg-rating-count", "freelancer/avg-rating", rating);
    }

    function getFreelancerAvgRating(address db, uint userId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("freelancer/avg-rating", userId));
    }

    function addToEmployerAvgRating(address db, uint userId, uint8 rating) {
        addToAvgRating(db, userId, "employer/avg-rating-count", "employer/avg-rating", rating);
    }

    function getEmployerAvgRating(address db, uint userId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("employer/avg-rating", userId));
    }

    function addToFreelancerTotalEarned(address db, uint userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("freelancer/total-earned", userId), amount);
    }

    function addToEmployerTotalPaid(address db, uint userId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("employer/total-paid", userId), amount);
    }

    function isFromCountry(address db, uint userId, uint countryId) internal returns(bool) {
        if (countryId == 0) {
            return true;
        }
        return countryId == EthlanceDB(db).getUIntValue(sha3("user/country", userId));
    }

    function hasMinRating(address db, uint userId, uint8 minAvgRating) internal returns(bool) {
        if (minAvgRating == 0) {
            return true;
        }
        return minAvgRating <= EthlanceDB(db).getUInt8Value(sha3("freelancer/avg-rating", userId));
    }

    function hasMinContractsCount(address db, uint userId, uint minContractsCount) internal returns(bool) {
        if (minContractsCount == 0) {
            return true;
        }
        return minContractsCount <= getFreelancerContractsCount(db, userId);
    }

    function getFreelancerHourlyRate(address db, uint userId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("freelancer/hourly-rate", userId));
    }

    function hasHourlyRateWithinRange(address db, uint userId, uint minHourlyRate, uint maxHourlyRate) internal returns(bool) {
        if (minHourlyRate == 0 && maxHourlyRate == 0) {
            return true;
        }
        var hourlyRate = getFreelancerHourlyRate(db, userId);

        if (minHourlyRate <= hourlyRate && maxHourlyRate == 0) {
            return true;
        }

        return minHourlyRate <= hourlyRate && hourlyRate <= maxHourlyRate;
    }

    function hasLanguage(address db, uint userId, uint languageId) internal returns (bool) {
        if (languageId == 0) {
            return true;
        }
        var count = SharedLibrary.getArrayItemsCount(db, userId, "user/languages-count");
        for (uint i = 0; i < count ; i++) {
            if (languageId == EthlanceDB(db).getUIntValue(sha3("user/languages", userId, i))) {
                return true;
            }
        }
        return false;
    }

    function isFreelancerAvailable(address db, uint userId) internal returns (bool) {
        return EthlanceDB(db).getBooleanValue(sha3("freelancer/available?", userId));
    }

    function searchFreelancers(
        address db,
        uint categoryId,
        uint[] skills,
        uint8 minAvgRating,
        uint minContractsCount,
        uint minHourlyRate,
        uint maxHourlyRate,
        uint countryId,
        uint languageId
    )
        internal returns (uint[] userIds)
    {
        uint j = 0;
        var allUserIds = SharedLibrary.intersectCategoriesAndSkills(db, categoryId, skills,
                         SkillLibrary.getFreelancers, CategoryLibrary.getFreelancers, getUserCount);
        userIds = new uint[](allUserIds.length);
        for (uint i = 0; i < allUserIds.length ; i++) {
            var userId = allUserIds[i];
            if (isFreelancerAvailable(db, userId) &&
                hasMinRating(db, userId, minAvgRating) &&
                hasMinContractsCount(db, userId, minContractsCount) &&
                hasHourlyRateWithinRange(db, userId, minHourlyRate, maxHourlyRate) &&
                isFromCountry(db, userId, countryId) &&
                hasLanguage(db, userId, languageId) &&
                hasStatus(db, userId, 1)
            ) {
                userIds[j] = userId;
                j++;
            }
        }
        return SharedLibrary.take(j, userIds);
    }

    function freelancerJobActionsPred(address db, uint[] args, uint jobActionId) internal returns(bool) {
        var jobId = JobActionLibrary.getJob(db, jobActionId);
        var jobActionStatus = args[0];
        var jobStatus = args[1];
        return (JobActionLibrary.getStatus(db, jobActionId) == jobActionStatus &&
                JobLibrary.getStatus(db, jobId) == jobStatus);
    }

    function getFreelancerJobActionsByStatus(address db, uint userId, uint8 jobActionStatus, uint8 jobStatus)
        internal returns (uint[] result)
    {
        var args = new uint[](2);
        args[0] = jobActionStatus;
        args[1] = jobStatus;
        return SharedLibrary.filter(db, freelancerJobActionsPred, getFreelancerJobActions(db, userId), args);
    }

    function getFreelancerInvoicesByStatus(address db, uint userId, uint8 invoiceStatus)
        internal returns (uint[] result)
    {
        var args = new uint[](1);
        args[0] = invoiceStatus;
        return SharedLibrary.filter(db, InvoiceLibrary.statusPred,
            ContractLibrary.getInvoices(db, getFreelancerContracts(db, userId)), args);
    }

    function getFreelancerContracts(address db, uint userId, bool isDone)
        internal returns (uint[] contractIds) {

        var maxCount = getFreelancerContractsCount(db, userId);
        var allContracts = getFreelancerContracts(db, userId);
        contractIds = new uint[](maxCount);
        uint j = 0;
        for (uint i = 0; i < allContracts.length ; i++) {
            var contractId = allContracts[i];
            var jobId = ContractLibrary.getJob(db, contractId);
            var jobStatus = JobLibrary.getStatus(db, jobId);
            if (isDone) {
                if (ContractLibrary.getStatus(db, contractId) != 2 ||
                    jobStatus != 1)
                    {
                        contractIds[j] = contractId;
                        j++;
                    }
            } else
                if (ContractLibrary.getStatus(db, contractId) == 1 &&
                    jobStatus == 1)
                {
                    contractIds[j] = contractId;
                    j++;
                }
        }
        return SharedLibrary.take(j, contractIds);
    }

    function testDb(address db, uint a) internal {
        EthlanceDB(db).setUIntValue(sha3("abc", a), a);
        if (EthlanceDB(db).getUIntValue(sha3("abc", a)) != a) {
            throw;
        }
    }
}

