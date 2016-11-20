pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "safeMath.sol";
import "skillLibrary.sol";
import "userLibrary.sol";
import "categoryLibrary.sol";

library JobLibrary {

    //    status:
    //    1: hiring, 2: hiringDone, 3: blocked

    function addJob(
        address _storage,
        uint employerId,
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items)
    {
        if (!EternalStorage(_storage).getBooleanValue(sha3("user/employer?", employerId))) throw;
        var idx = SharedLibrary.createNext(_storage, "job/count");
        EternalStorage(_storage).setUIntValue(sha3("job/employer", idx), employerId);
        EternalStorage(_storage).setStringValue(sha3("job/title", idx), title);
        EternalStorage(_storage).setStringValue(sha3("job/description", idx), description);
        EternalStorage(_storage).setUIntValue(sha3("job/language", idx), language);
        EternalStorage(_storage).setUIntValue(sha3("job/budget", idx), budget);
        EternalStorage(_storage).setUIntValue(sha3("job/created-on", idx), now);
        EternalStorage(_storage).setUInt8Value(sha3("job/category", idx), uint8Items[0]);
        EternalStorage(_storage).setUInt8Value(sha3("job/payment-type", idx), uint8Items[1]);
        EternalStorage(_storage).setUInt8Value(sha3("job/experience-level", idx), uint8Items[2]);
        EternalStorage(_storage).setUInt8Value(sha3("job/estimated-duration", idx), uint8Items[3]);
        EternalStorage(_storage).setUInt8Value(sha3("job/hours-per-week", idx), uint8Items[4]);
        EternalStorage(_storage).setUInt8Value(sha3("job/freelancers-needed", idx), uint8Items[5]);
        EternalStorage(_storage).setUInt8Value(sha3("job/status", idx), 1);
        setSkills(_storage, idx, skills);
        UserLibrary.addEmployerJob(_storage, employerId, idx);
        CategoryLibrary.addJob(_storage, uint8Items[0], idx);
    }

    function setSkills(address _storage, uint jobId, uint[] skills) {
        SharedLibrary.setUIntArray(_storage, jobId, "job/skills", "job/skills-count", skills);
    }

    function getSkills(address _storage, uint jobId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(_storage, jobId, "job/skills", "job/skills-count");
    }
    
    function getJobProposalsCount(address _storage, uint jobId) constant returns(uint) {
        return SharedLibrary.getArrayItemsCount(_storage, jobId, "job/proposals-count");
    }

    function addJobProposal(address _storage, uint jobId, uint proposalId) {
        SharedLibrary.addArrayItem(_storage, jobId, "job/proposals", "job/proposals-count", proposalId);
    }

    function getJobContractsCount(address _storage, uint jobId) constant returns(uint) {
        return SharedLibrary.getArrayItemsCount(_storage, jobId, "job/contracts-count");
    }

    function addJobContract(address _storage, uint jobId, uint contractId) {
        SharedLibrary.addArrayItem(_storage, jobId, "job/contracts", "job/contracts-count", contractId);
    }
    
    function getJobInvitationsCount(address _storage, uint jobId) constant returns(uint) {
        return SharedLibrary.getArrayItemsCount(_storage, jobId, "job/invitations-count");
    }

    function addJobInvitation(address _storage, uint jobId, uint invitationId) {
        SharedLibrary.addArrayItem(_storage, jobId, "job/invitations", "job/invitations-count", invitationId);
    }

    function getEmployer(address _storage, uint jobId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("job/employer", jobId));
    }
    
    function getCreatedOn(address _storage, uint jobId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("job/created-on", jobId));
    }

    function setJobStatus(address _storage, uint jobId, uint8 status) {
        EternalStorage(_storage).setUInt8Value(sha3("job/status", jobId), status);
    }

    function hasStatus(address _storage, uint jobId, uint8 status) constant returns(bool) {
        return status == EternalStorage(_storage).getUInt8Value(sha3("job/status", jobId));
    }

    function hasMinBudget(address _storage, uint jobId, uint minBudget) constant returns(bool) {
        if (minBudget == 0) {
            return true;
        }
        return minBudget <= EternalStorage(_storage).getUIntValue(sha3("job/budget", jobId));
    }

   function hasLanguage(address _storage, uint jobId, uint languageId) constant returns (bool) {
        if (languageId == 0) {
            return true;
        }
        return languageId == EternalStorage(_storage).getUIntValue(sha3("job/language", jobId));
   }

    function hasEmployerMinRating(address _storage, uint employerId, uint8 minAvgRating) constant returns(bool) {
        if (minAvgRating == 0) {
            return true;
        }
        return minAvgRating <= UserLibrary.getEmployerAvgRating(_storage, employerId);
    }

    function intersectCategoriesAndSkills(address _storage, uint categoryId, uint[] skills) internal returns (uint[]) {
        uint[][] memory jobIdArrays;
        for (uint i = 0; i < skills.length ; i++) {
            jobIdArrays[i] = SkillLibrary.getJobs(_storage, skills[i]);
        }
        if (categoryId > 0) {
            jobIdArrays[jobIdArrays.length - 1] = CategoryLibrary.getJobs(_storage, categoryId);
        }
        return SharedLibrary.intersect(jobIdArrays);
    }

    function searchJobs(address _storage,
        uint categoryId,
        uint[] skills,
        uint8[][] uint8Filters,
        uint minBudget,
        uint8 minEmployerAvgRating,
        uint16 countryId,
        uint languageId
    )
        internal returns (uint[] result)
    {
        uint[] memory jobIds;
        uint j = 0;
        uint jobId;
        uint employerId;
        jobIds = intersectCategoriesAndSkills(_storage, categoryId, skills);

         for (uint i = 0; i < jobIds.length ; i++) {
            jobId = jobIds[i];
            employerId = getEmployer(_storage, jobId);
            if (hasStatus(_storage, jobId, 1) &&
                SharedLibrary.containsValue(_storage, jobId, "job/payment-type", uint8Filters[0]) &&
                SharedLibrary.containsValue(_storage, jobId, "job/experience-level", uint8Filters[1]) &&
                SharedLibrary.containsValue(_storage, jobId, "job/estimated-duration", uint8Filters[2]) &&
                SharedLibrary.containsValue(_storage, jobId, "job/hours-per-week", uint8Filters[3]) &&
                hasMinBudget(_storage, jobId, minBudget) &&
                hasEmployerMinRating(_storage, jobId, minEmployerAvgRating) &&
                UserLibrary.isFromCountry(_storage, jobId, countryId) &&
                hasLanguage(_storage, jobId, languageId) &&
                UserLibrary.hasStatus(_storage, employerId, 1)
                )
            {
                result[j] = jobId;
                j++;
            }
        }
        return result;
    }

    function getJobDetail(address _storage, uint jobId)
        internal returns (bytes32[] bytes32Items, uint8[] uint8Items, uint[] uintItems, uint[]) {
        uint employerId = getEmployer(_storage, jobId);
        bytes32Items[0] = sha3("job/title", jobId);
        bytes32Items[1] = sha3("job/description", jobId);
        bytes32Items[3] = EternalStorage(_storage).getBytes32Value(sha3("user/name", employerId));
        bytes32Items[4] = EternalStorage(_storage).getBytes32Value(sha3("user/gravatar", employerId));

        uint8Items[0] = EternalStorage(_storage).getUInt8Value(sha3("job/status", jobId));
        uint8Items[1] = EternalStorage(_storage).getUInt8Value(sha3("job/payment-type", jobId));
        uint8Items[2] = EternalStorage(_storage).getUInt8Value(sha3("job/experience-level", jobId));
        uint8Items[3] = EternalStorage(_storage).getUInt8Value(sha3("job/hours-per-week", jobId));
        uint8Items[4] = EternalStorage(_storage).getUInt8Value(sha3("job/freelancers-needed", jobId));
        uint8Items[5] = UserLibrary.getEmployerAvgRating(_storage, employerId);

        uintItems[0] = employerId;
        uintItems[1] = EternalStorage(_storage).getUIntValue(sha3("job/budget", jobId));
        uintItems[2] = EternalStorage(_storage).getUIntValue(sha3("job/category", jobId));
        uintItems[3] = EternalStorage(_storage).getUIntValue(sha3("job/created-on", jobId));
        uintItems[4] = SharedLibrary.getArrayItemsCount(_storage, jobId, "job/proposals-count");
        uintItems[5] = SharedLibrary.getArrayItemsCount(_storage, jobId, "job/invitations-count");

        return (bytes32Items, uint8Items, uintItems, getSkills(_storage, jobId));
    }

//    function getJobListValues(address _storage, uint[] jobIds) internal returns (uint[], bytes32[]) {
//        bytes32[] memory titles;
//         for (uint i = 0; i < jobIds.length ; i++) {
//            titles[i] =
//         }
//        return (jobIds, titles);
//    }
}