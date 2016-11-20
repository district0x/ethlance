pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "safeMath.sol";
import "sharedLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";
import "jobActionLibrary.sol";
import "jobLibrary.sol";

library UserLibrary {

    //    status:
    //    1: active, 2: blocked

    function getUserCount(address _storage) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("user/count"));
    }

    function getUserId(address _storage, address userAddress) constant returns(uint){
        return EternalStorage(_storage).getUIntValue(sha3("user/ids", userAddress));
    }

    function setUser(address _storage,
        address userAddress,
        bytes32 name,
        bytes32 gravatar,
        uint16 country,
        uint[] languages)
        returns (uint)
    {
        uint userId;
        uint existingUserId = getUserId(_storage, userAddress);
        if (existingUserId > 0) {
            userId = existingUserId;
        } else {
            userId = SharedLibrary.createNext(_storage, "user/count");
        }
        EternalStorage(_storage).setAddressValue(sha3("user/address", userId), userAddress);
        EternalStorage(_storage).setBytes32Value(sha3("user/name", userId), name);
        EternalStorage(_storage).setBytes32Value(sha3("user/gravatar", userId), gravatar);
        EternalStorage(_storage).setUInt16Value(sha3("user/country", userId), country);
        EternalStorage(_storage).setUInt8Value(sha3("user/status", userId), 1);
        EternalStorage(_storage).setUIntValue(sha3("user/ids", userAddress), userId);
        setUserLanguages(_storage, userId, languages);
        return userId;
    }

    function setFreelancer(address _storage,
        uint userId,
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description)
    {
        EternalStorage(_storage).setBooleanValue(sha3("user/freelancer?", userId), true);
        EternalStorage(_storage).setBooleanValue(sha3("freelancer/available?", userId), isAvailable);
        EternalStorage(_storage).setBytes32Value(sha3("freelancer/job-title", userId), jobTitle);
        EternalStorage(_storage).setUIntValue(sha3("freelancer/hourly-rate", userId), hourlyRate);
        EternalStorage(_storage).setStringValue(sha3("freelancer/description", userId), description);
        setFreelancerSkills(_storage, userId, skills);
        setFreelancerCategories(_storage, userId, categories);
    }

    function setEmployer(address _storage,
        uint userId,
        string description)
    {
        EternalStorage(_storage).setBooleanValue(sha3("user/employer?", userId), true);
        EternalStorage(_storage).setStringValue(sha3("employer/description", userId), description);
    }

    function setUserStatus(address _storage, uint userId, uint8 status) {
        EternalStorage(_storage).addUIntValue(sha3("user/count"), status);
    }

    function setFreelancerSkills(address _storage, uint userId, uint[] skills) {
        uint[] memory added;
        uint[] memory removed;
        uint[] memory currentSkills = getFreelancerSkills(_storage, userId);
        (added, removed) = SharedLibrary.diff(currentSkills, skills);
        SkillLibrary.addFreelancer(_storage, added, userId);
        SkillLibrary.removeFreelancer(_storage, removed, userId);
        SharedLibrary.setUIntArray(_storage, userId, "freelancer/skills", "freelancer/skills-count", skills);
    }
    
    function getFreelancerSkills(address _storage, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(_storage, userId, "freelancer/skills", "freelancer/skills-count");
    }

    function setFreelancerCategories(address _storage, uint userId, uint[] categories) {
        uint[] memory added;
        uint[] memory removed;
        uint[] memory currentCategories = getFreelancerCategories(_storage, userId);
        (added, removed) = SharedLibrary.diff(currentCategories, categories);
        CategoryLibrary.addFreelancer(_storage, added, userId);
        CategoryLibrary.removeFreelancer(_storage, removed, userId);
        SharedLibrary.setUIntArray(_storage, userId, "freelancer/categories", "freelancer/categories-count", categories);
    }

    function getFreelancerCategories(address _storage, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(_storage, userId, "freelancer/categories", "freelancer/categories-count");
    }

    function getDescriptionKeys(uint[] userIds) internal returns (bytes32[] result) {
         for (uint i = 0; i < userIds.length ; i++) {
            result[i] = sha3("freelancer/description", userIds[i]);
         }
         return result;
    }

    function hasStatus(address _storage, uint userId, uint8 status) constant returns(bool) {
        return status == EternalStorage(_storage).getUInt8Value(sha3("user/status", userId));
    }

    function setUserLanguages(address _storage, uint userId, uint[] languages) {
        SharedLibrary.setUIntArray(_storage, userId, "user/languages", "user/languages-count", languages);
    }

    function getEmployerJobsCount(address _storage, uint userId) constant returns(uint) {
        return SharedLibrary.getArrayItemsCount(_storage, userId, "employer/jobs-count");
    }

    function addEmployerJob(address _storage, uint userId, uint jobId) {
        SharedLibrary.addArrayItem(_storage, userId, "employer/jobs", "employer/jobs-count", jobId);
    }

    function addFreelancerJobAction(address _storage, uint userId, uint jobActionId) {
        SharedLibrary.addArrayItem(_storage, userId, "freelancer/job-actions", "freelancer/job-actions-count", jobActionId);
    }

    function getFreelancerJobActions(address _storage, uint userId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(_storage, userId, "freelancer/job-actions", "freelancer/job-actions-count");
    }
    
    function getFreelancerContractsCount(address _storage, uint userId) constant returns(uint) {
        return SharedLibrary.getArrayItemsCount(_storage, userId, "freelancer/contracts-count");
    }

    function addFreelancerContract(address _storage, uint userId, uint contractId) {
        SharedLibrary.addArrayItem(_storage, userId, "freelancer/contracts", "freelancer/contracts-count", contractId);
    }
    
    function addToAvgRating(address _storage, uint userId, string countKey, string key, uint8 rating) {
        var ratingsCount = EternalStorage(_storage).getUIntValue(sha3(countKey, userId));
        var currentAvgRating = EternalStorage(_storage).getUInt8Value(sha3(key, userId));
        var newRatingsCount = SafeMath.safeAdd(ratingsCount, 1);
        uint newAvgRating;
        if (ratingsCount == 0) {
            newAvgRating = rating;
        } else {
            var newTotalRating = SafeMath.safeAdd(SafeMath.safeMul(currentAvgRating, ratingsCount), rating);
            newAvgRating = newTotalRating / newRatingsCount;
        }
        EternalStorage(_storage).setUIntValue(sha3(countKey, userId), newRatingsCount);
        EternalStorage(_storage).setUInt8Value(sha3(key, userId), uint8(newAvgRating));
    }
    
    function addToFreelancerAvgRating(address _storage, uint userId, uint8 rating) {
        addToAvgRating(_storage, userId, "freelancer/avg-rating-count", "freelancer/avg-rating", rating);
    }

    function getFreelancerAvgRating(address _storage, uint userId) constant returns (uint8) {
        EternalStorage(_storage).getUInt8Value(sha3("freelancer/avg-rating", userId));
    }

    function addToEmployerAvgRating(address _storage, uint userId, uint8 rating) {
        addToAvgRating(_storage, userId, "employer/avg-rating-count", "employer/avg-rating", rating);
    }

    function getEmployerAvgRating(address _storage, uint userId) constant returns (uint8) {
        EternalStorage(_storage).getUInt8Value(sha3("employer/avg-rating", userId));
    }

    function addToFreelancerTotalEarned(address _storage, uint userId, uint amount) {
        EternalStorage(_storage).addUIntValue(sha3("freelancer/total-earned", userId), amount);
    }

    function addToEmployerTotalPaid(address _storage, uint userId, uint amount) {
        EternalStorage(_storage).addUIntValue(sha3("employer/total-paid", userId), amount);
    }

    function isFromCountry(address _storage, uint userId, uint16 countryId) constant returns(bool) {
        if (countryId == 0) {
            return true;
        }
        return countryId == EternalStorage(_storage).getUInt16Value(sha3("user/country", userId));
    }

    function hasMinRating(address _storage, uint userId, uint8 minAvgRating) constant returns(bool) {
        if (minAvgRating == 0) {
            return true;
        }
        return minAvgRating <= EternalStorage(_storage).getUInt8Value(sha3("freelancer/avg-rating", userId));
    }

    function hasMinContractsCount(address _storage, uint userId, uint minContractsCount) constant returns(bool) {
        if (minContractsCount == 0) {
            return true;
        }
        return minContractsCount <= getFreelancerContractsCount(_storage, userId);
    }

    function getFreelancerHourlyRate(address _storage, uint userId) constant returns (uint) {
        EternalStorage(_storage).getUIntValue(sha3("freelancer/hourly-rate", userId));
    }

    function hasHourlyRateWithinRange(address _storage, uint userId, uint minHourlyRate, uint maxHourlyRate) constant returns(bool) {
        if (minHourlyRate == 0 && maxHourlyRate == 0) {
            return true;
        }
        var hourlyRate = getFreelancerHourlyRate(_storage, userId);
        if (minHourlyRate <= hourlyRate && maxHourlyRate == 0) {
            return true;
        }

        return minHourlyRate <= hourlyRate && hourlyRate <= maxHourlyRate;
    }

    function hasLanguage(address _storage, uint userId, uint languageId) constant returns (bool) {
        if (languageId == 0) {
            return true;
        }
        var count = SharedLibrary.getArrayItemsCount(_storage, userId, "user/languages-count");
        for (uint i = 0; i < count ; i++) {
            if (languageId == EternalStorage(_storage).getUIntValue(sha3("user/languages", userId, i))) {
                return true;
            }
        }
        return false;
    }

    function isFreelancerAvailable(address _storage, uint userId) constant returns (bool) {
        return EternalStorage(_storage).getBooleanValue(sha3("freelancer/available?", userId));
    }

    function intersectCategoriesAndSkills(address _storage, uint categoryId, uint[] skills) internal returns (uint[]) {
        uint[][] memory freelancerIdArrays;
        for (uint i = 0; i < skills.length ; i++) {
            freelancerIdArrays[i] = SkillLibrary.getFreelancers(_storage, skills[i]);
        }
        if (categoryId > 0) {
            freelancerIdArrays[freelancerIdArrays.length - 1] = CategoryLibrary.getFreelancers(_storage, categoryId);
        }
        return SharedLibrary.intersect(freelancerIdArrays);
    }

    function searchFreelancers(
        address _storage,
        uint categoryId,
        uint[] skills,
        uint8 minAvgRating,
        uint minContractsCount,
        uint minHourlyRate,
        uint maxHourlyRate,
        uint16 countryId,
        uint languageId
    )
        internal returns (uint[] result)
    {
        uint[] memory userIds;
        uint j = 0;
        userIds = intersectCategoriesAndSkills(_storage, categoryId, skills);
        for (uint i = 0; i < userIds.length ; i++) {
            var userId = userIds[i];
            if (isFreelancerAvailable(_storage, userId) &&
                hasMinRating(_storage, userId, minAvgRating) &&
                hasMinContractsCount(_storage, userId, minContractsCount) &&
                hasHourlyRateWithinRange(_storage, userId, minHourlyRate, maxHourlyRate) &&
                isFromCountry(_storage, userId, countryId) &&
                hasLanguage(_storage, userId, languageId) &&
                hasStatus(_storage, userId, 1)
            ) {
                result[j] = userId;
                j++;
            }
        }
        return result;
    }

    function getUserDetail(address _storage, uint userId)
        internal returns
    (
        bytes32[] bytesItems,
        uint[] uintItems,
        bool[] boolItems,
        uint[] categories,
        uint[] skills,
        uint[] languages)
    {
        bytesItems[0] = EternalStorage(_storage).getBytes32Value(sha3("user/name", userId));
        bytesItems[1] = EternalStorage(_storage).getBytes32Value(sha3("user/gravatar", userId));
        bytesItems[2] = EternalStorage(_storage).getBytes32Value(sha3("freelancer/job-title", userId));
        bytesItems[3] = sha3("freelancer/description", userId);
        bytesItems[4] = sha3("employer/description", userId);

        uintItems[0] = userId;
        uintItems[1] = uint(EternalStorage(_storage).getUInt8Value(sha3("user/status", userId)));
        uintItems[3] = uint(EternalStorage(_storage).getUInt16Value(sha3("user/country", userId)));
        uintItems[4] = uint(EternalStorage(_storage).getUInt16Value(sha3("user/country", userId)));
        uintItems[5] = getFreelancerHourlyRate(_storage, userId);
        uintItems[6] = uint(getFreelancerAvgRating(_storage, userId));
        uintItems[7] = EternalStorage(_storage).getUIntValue(sha3("freelancer/total-earned", userId));
        uintItems[8] = getFreelancerContractsCount(_storage, userId);

        uintItems[0] = uint(getEmployerAvgRating(_storage, userId));
        uintItems[1] = EternalStorage(_storage).getUIntValue(sha3("employer/total-paid", userId));

        boolItems[0] = EternalStorage(_storage).getBooleanValue(sha3("user/freelancer?", userId));
        boolItems[1] = EternalStorage(_storage).getBooleanValue(sha3("user/employer?", userId));
        boolItems[2] = isFreelancerAvailable(_storage, userId);

        categories = getFreelancerCategories(_storage, userId);
        skills = getFreelancerSkills(_storage, userId);

        return(bytesItems, uintItems, boolItems, categories, skills, languages);
    }

    function filterJobActions(address _storage, uint userId, uint8 jobActionStatus, uint8 jobStatus)
        internal returns (uint[] jobActionIds)
    {
        uint j = 0;
        uint[] memory allJobActions = getFreelancerJobActions(_storage, userId);
         for (uint i = 0; i < allJobActions.length ; i++) {
            var jobActionId = allJobActions[i];
            var jobId = JobActionLibrary.getJob(_storage, jobActionId);
            if (JobActionLibrary.getStatus(_storage, jobActionId) == jobActionStatus &&
                JobLibrary.hasStatus(_storage, jobId, jobStatus))
            {
                jobActionIds[j] = jobActionId;
                j++;
            }
         }
         return jobActionIds;
    }

    function filterFreelancerJobActions(address _storage, uint userId, uint8 jobActionType)
        internal returns
    (
        uint[] jobIds,
        uint[] proposalsCounts,
        uint[] invitationsCounts,
        uint[] proposedOns,
        uint[] invitedOns,
        uint[] jobCreatedOns)
    {
        uint[] memory jobActionIds;
        if (jobActionType == 1 || jobActionType == 2) {
            jobActionIds = filterJobActions(_storage, userId, 2, jobActionType);
        } else {
            jobActionIds = filterJobActions(_storage, userId, 1, 1);
        }
        for (uint i = 0; i < jobActionIds.length ; i++) {
            var jobActionId = jobActionIds[i];
            var jobId = JobActionLibrary.getJob(_storage, jobActionId);
            jobIds[i] = jobId;
            proposalsCounts[i] = JobLibrary.getJobProposalsCount(_storage, jobId);
            invitationsCounts[i] = JobLibrary.getJobInvitationsCount(_storage, jobId);
            proposedOns[i] = JobActionLibrary.getProposalCreatedOn(_storage, jobActionId);
            invitedOns[i] = JobActionLibrary.getInvitationCreatedOn(_storage, jobActionId);
            jobCreatedOns[i] = JobLibrary.getCreatedOn(_storage, jobId);
        }
        return (jobIds, proposalsCounts, invitationsCounts, proposedOns, invitedOns, jobCreatedOns);
    }

}

