pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "safeMath.sol";
import "skillLibrary.sol";
import "userLibrary.sol";
import "categoryLibrary.sol";
import "invoiceLibrary.sol";
import "sharedLibrary.sol";

library JobLibrary {

    //    status:
    //    1: hiring, 2: hiringDone, 3: blocked

    function getJobCount(address db) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/count"));
    }

    function addJob(
        address db,
        uint employerId,
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items
    )
        internal
    {
        if (!EthlanceDB(db).getBooleanValue(sha3("user/employer?", employerId))) throw;
        var jobId = SharedLibrary.createNext(db, "job/count");
        EthlanceDB(db).setUIntValue(sha3("job/employer", jobId), employerId);
        EthlanceDB(db).setStringValue(sha3("job/title", jobId), title);
        EthlanceDB(db).setStringValue(sha3("job/description", jobId), description);

        if (language == 0) throw;
        EthlanceDB(db).setUIntValue(sha3("job/language", jobId), language);
        EthlanceDB(db).setUIntValue(sha3("job/budget", jobId), budget);
        EthlanceDB(db).setUIntValue(sha3("job/created-on", jobId), now);

        if (uint8Items[0] == 0) throw;
        EthlanceDB(db).setUInt8Value(sha3("job/category", jobId), uint8Items[0]);

        if (uint8Items[1] == 0) throw;
        EthlanceDB(db).setUInt8Value(sha3("job/payment-type", jobId), uint8Items[1]);

        if (uint8Items[2] == 0) throw;
        EthlanceDB(db).setUInt8Value(sha3("job/experience-level", jobId), uint8Items[2]);

        if (uint8Items[3] == 0) throw;
        EthlanceDB(db).setUInt8Value(sha3("job/estimated-duration", jobId), uint8Items[3]);

        if (uint8Items[4] == 0) throw;
        EthlanceDB(db).setUInt8Value(sha3("job/hours-per-week", jobId), uint8Items[4]);

        if (uint8Items[5] == 0) throw;
        EthlanceDB(db).setUInt8Value(sha3("job/freelancers-needed", jobId), uint8Items[5]);
        EthlanceDB(db).setUInt8Value(sha3("job/status", jobId), 1);
        setSkills(db, jobId, skills);
        UserLibrary.addEmployerJob(db, employerId, jobId);
        CategoryLibrary.addJob(db, uint8Items[0], jobId);
    }

    function setSkills(address db, uint jobId, uint[] skills) internal {
        SharedLibrary.setUIntArray(db, jobId, "job/skills", "job/skills-count", skills);
        for (uint i = 0; i < skills.length ; i++) {
            SkillLibrary.addJob(db, skills[i], jobId);
        }
    }

    function getSkills(address db, uint jobId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, jobId, "job/skills", "job/skills-count");
    }
    
    function addContract(address db, uint jobId, uint contractId) internal {
        SharedLibrary.addArrayItem(db, jobId, "job/contracts", "job/contracts-count", contractId);
    }

    function getContracts(address db, uint jobId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, jobId, "job/contracts", "job/contracts-count");
    }

    function getEmployer(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/employer", jobId));
    }
    
    function getCreatedOn(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/created-on", jobId));
    }

    function setStatus(address db, uint jobId, uint8 status) internal {
        EthlanceDB(db).setUInt8Value(sha3("job/status", jobId), status);
    }

    function setHiringDone(address db, uint jobId, uint senderId) internal {
        if (getEmployer(db, jobId) != senderId) throw;
        if (getStatus(db, jobId) != 1) throw;
        setStatus(db, jobId, 2);
        EthlanceDB(db).setUIntValue(sha3("job/hiring-done-on", jobId), now);
    }

    function getStatus(address db, uint jobId) internal returns(uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("job/status", jobId));
    }

    function addTotalPaid(address db, uint jobId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("job/total-paid", jobId), amount);
    }

    function hasMinBudget(address db, uint jobId, uint minBudget) internal returns(bool) {
        if (minBudget == 0) {
            return true;
        }
        return minBudget <= EthlanceDB(db).getUIntValue(sha3("job/budget", jobId));
    }

   function hasLanguage(address db, uint jobId, uint languageId) internal returns (bool) {
        if (languageId == 0) {
            return true;
        }
        return languageId == EthlanceDB(db).getUIntValue(sha3("job/language", jobId));
   }

    function hasEmployerMinRating(address db, uint employerId, uint minAvgRating) internal returns(bool) {
        if (minAvgRating == 0) {
            return true;
        }
        return minAvgRating <= UserLibrary.getEmployerAvgRating(db, employerId);
    }

    function hasEmployerMinRatingsCount(address db, uint employerId, uint minRatingsCount) internal returns(bool) {
        if (minRatingsCount == 0) {
            return true;
        }
        return minRatingsCount <= EthlanceDB(db).getUIntValue(sha3("employer/ratings-count", employerId));
    }

    function statusPred(address db, uint[] args, uint jobId) internal returns(bool) {
        var status = getStatus(db, jobId);
        return args[0] == 0 || status == args[0];
    }

    function searchJobs(address db,
        uint categoryId,
        uint[] skills,
        uint8[][4] uint8Filters,
        uint[] uintArgs
    )
        internal returns (uint[] jobIds)
    {
        uint j = 0;
        uint jobId;
        uint employerId;
        var allJobIds = SharedLibrary.intersectCategoriesAndSkills(db, categoryId, skills,
            SkillLibrary.getJobs, CategoryLibrary.getJobs, getJobCount);
        jobIds = new uint[](allJobIds.length);
        for (uint i = 0; i < allJobIds.length ; i++) {
            jobId = allJobIds[i];
            employerId = getEmployer(db, jobId);
            if (getStatus(db, jobId) == 1 &&
                SharedLibrary.containsValue(db, jobId, "job/payment-type", uint8Filters[0]) &&
                SharedLibrary.containsValue(db, jobId, "job/experience-level", uint8Filters[1]) &&
                SharedLibrary.containsValue(db, jobId, "job/estimated-duration", uint8Filters[2]) &&
                SharedLibrary.containsValue(db, jobId, "job/hours-per-week", uint8Filters[3]) &&
                hasMinBudget(db, jobId, uintArgs[0]) &&
                hasEmployerMinRating(db, employerId, uintArgs[1]) &&
                hasEmployerMinRatingsCount(db, employerId, uintArgs[2]) &&
                UserLibrary.isFromCountry(db, employerId, uintArgs[3]) &&
                UserLibrary.isFromState(db, employerId, uintArgs[4]) &&
                hasLanguage(db, jobId, uintArgs[5]) &&
                UserLibrary.hasStatus(db, employerId, 1)
                )
            {
                jobIds[j] = jobId;
                j++;
            }
        }
        return SharedLibrary.take(j, jobIds);
    }

    function getEmployerJobsByStatus(address db, uint userId, uint8 jobStatus)
        internal returns (uint[] jobIds)
    {
        var args = new uint[](1);
        args[0] = jobStatus;
        return SharedLibrary.filter(db, statusPred, UserLibrary.getEmployerJobs(db, userId), args);
    }

    function getEmployerJobsForFreelancerInvite(address db, uint employerId, uint freelancerId)
        internal returns (uint[] jobIds)
    {
        var args = new uint[](1);
        args[0] = freelancerId;
        return SharedLibrary.filter(db, ContractLibrary.notContractPred, getEmployerJobsByStatus(db, employerId, 1), args);
    }

    function getJobInvoicesByStatus(address db, uint jobId, uint8 invoiceStatus)
        internal returns (uint[])
    {
        var args = new uint[](1);
        args[0] = invoiceStatus;
        return SharedLibrary.filter(db, InvoiceLibrary.statusPred,
                    ContractLibrary.getInvoices(db, getContracts(db, jobId)), args);
    }

    function getContractsByStatus
    (
        address db,
        uint jobId,
        uint8 contractStatus
    )
        internal returns (uint[] result)
    {
        var args = new uint[](1);
        args[0] = contractStatus;
        return SharedLibrary.filter(db, ContractLibrary.statusPred, getContracts(db, jobId), args);
    }
}
