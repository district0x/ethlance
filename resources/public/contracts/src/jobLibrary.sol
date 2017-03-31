pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "safeMath.sol";
import "skillLibrary.sol";
import "userLibrary.sol";
import "categoryLibrary.sol";
import "invoiceLibrary.sol";
import "sharedLibrary.sol";

library JobLibrary {

    //    status:
    //    1: hiring, 2: hiringDone, 3: blocked, 4: draft, 5: refunding, 6: refunded

    function getJobCount(address db) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/count"));
    }

    function setJob(
        address db,
        uint existingJobId,
        uint senderId,
        address senderAddress,
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items,
        bool isSponsorable,
        address[] allowedUsers
    )
        internal returns (uint jobId)
    {
        if (existingJobId > 0) {
            require(getStatus(db, existingJobId) == 4);
            require(getEmployer(db, existingJobId) == senderId);
            jobId = existingJobId;
        } else {
            jobId = SharedLibrary.createNext(db, "job/count");
        }
        EthlanceDB(db).setUIntValue(sha3("job/employer", jobId), senderId);
        EthlanceDB(db).setStringValue(sha3("job/title", jobId), title);
        EthlanceDB(db).setStringValue(sha3("job/description", jobId), description);

        require(language > 0);
        EthlanceDB(db).setUIntValue(sha3("job/language", jobId), language);
        EthlanceDB(db).setUIntValue(sha3("job/budget", jobId), budget);

        require(uint8Items[0] > 0);
        EthlanceDB(db).setUInt8Value(sha3("job/category", jobId), uint8Items[0]);

        require(uint8Items[1] > 0);
        EthlanceDB(db).setUInt8Value(sha3("job/payment-type", jobId), uint8Items[1]);

        require(uint8Items[2] > 0);
        EthlanceDB(db).setUInt8Value(sha3("job/experience-level", jobId), uint8Items[2]);

        require(uint8Items[3] > 0);
        EthlanceDB(db).setUInt8Value(sha3("job/estimated-duration", jobId), uint8Items[3]);

        require(uint8Items[4] > 0);
        EthlanceDB(db).setUInt8Value(sha3("job/hours-per-week", jobId), uint8Items[4]);

        require(uint8Items[5] > 0);
        EthlanceDB(db).setUInt8Value(sha3("job/freelancers-needed", jobId), uint8Items[5]);

        EthlanceDB(db).setUInt8Value(sha3("job/reference-currency", jobId), uint8Items[6]);

        EthlanceDB(db).setBooleanValue(sha3("job/sponsorable?", jobId), isSponsorable);
        SharedLibrary.setIdArray(db, jobId, "job/allowed-users", "job/allowed-users-count",
                        allowedUsers);

        if (existingJobId > 0) {
            clearSponsorableJobApprovals(db, jobId, allowedUsers);
            EthlanceDB(db).setUIntValue(sha3("job/updated-on", jobId), now);
        } else {
            EthlanceDB(db).setUIntValue(sha3("job/created-on", jobId), now);
            UserLibrary.addEmployerJob(db, senderId, jobId);
            CategoryLibrary.addJob(db, uint8Items[0], jobId);
        }

        if (isSponsorable) {
            setStatus(db, jobId, 4);
            approveSponsorableJob(db, jobId, senderAddress, allowedUsers);
        } else {
            setStatus(db, jobId, 1);
        }

        setSkills(db, jobId, skills);

        return jobId;
    }

    function approveSponsorableJob(address db, uint jobId, address allowedUser, address[] allowedUsers)
    internal {
        if (SharedLibrary.contains(allowedUsers, allowedUser)) {
            EthlanceDB(db).setBooleanValue(sha3("job.allowed-user/approved?", jobId, allowedUser), true);
            if (isSponsorableJobApproved(db, jobId, allowedUsers)) {
                setStatus(db, jobId, 1);
            }
        }
    }

    function approveSponsorableJob(address db, uint jobId, address allowedUser)
    internal {
        var allowedUsers = getAllowedUsers(db, jobId);
        require(SharedLibrary.contains(allowedUsers, allowedUser));
        approveSponsorableJob(db, jobId, allowedUser, allowedUsers);
    }

    function isSponsorableJobApproved(address db, uint jobId, address[] allowedUsers)
    internal returns(bool isApproved)
    {
        isApproved = true;
        for (uint i = 0; i < allowedUsers.length ; i++) {
            if (!EthlanceDB(db).getBooleanValue(sha3("job.allowed-user/approved?", jobId, allowedUsers[i]))) {
                isApproved = false;
            }
        }
        return isApproved;
    }

    function getAllowedUsers(address db, uint jobId) internal returns(address[]) {
        return SharedLibrary.getAddressIdArray(db, jobId, "job/allowed-users", "job/allowed-users-count");
    }

    function isAllowedUser(address db, uint jobId, address addr) internal returns(bool) {
        var allowedUsers = getAllowedUsers(db, jobId);
        return SharedLibrary.contains(allowedUsers, addr);
    }

    function isSponsorable(address db, uint jobId) internal returns(bool) {
        return EthlanceDB(db).getBooleanValue(sha3("job/sponsorable?", jobId));
    }

    function clearSponsorableJobApprovals(address db, uint jobId, address[] allowedUsers) internal {
        for (uint i = 0; i < allowedUsers.length ; i++) {
            EthlanceDB(db).setBooleanValue(sha3("job.allowed-user/approved?", jobId, allowedUsers[i]), false);
        }
    }

    function getApprovals(address db, uint jobId) internal returns(address[] allowedUsers, bool[] approvals) {
        allowedUsers = getAllowedUsers(db, jobId);
        approvals = new bool[](allowedUsers.length);
        for (uint i = 0; i < allowedUsers.length ; i++) {
            approvals[i] = EthlanceDB(db).getBooleanValue(sha3("job.allowed-user/approved?", jobId, allowedUsers[i]));
        }
        return (allowedUsers, approvals);
    }

    function setSkills(address db, uint jobId, uint[] skills) internal {
        SharedLibrary.setIdArray(db, jobId, "job/skills", "job/skills-count", skills);
        for (uint i = 0; i < skills.length ; i++) {
            SkillLibrary.addJob(db, skills[i], jobId);
        }
    }

    function getSkills(address db, uint jobId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, jobId, "job/skills", "job/skills-count");
    }

    function addSponsorship(address db, uint jobId, uint sponsorId) internal {
        SharedLibrary.addIdArrayItem(db, jobId, "job/sponsorships", "job/sponsorships-count", sponsorId);
    }

    function addSponsorshipAmount(address db, uint jobId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("job/sponsorships-total", jobId), amount);
        EthlanceDB(db).addUIntValue(sha3("job/sponsorships-balance", jobId), amount);
    }

    function addSponsorshipsTotalRefunded(address db, uint jobId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("job/sponsorships-total-refunded", jobId), amount);
    }

    function getSponsorshipsTotal(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/sponsorships-total", jobId));
    }

    function getSponsorshipsBalance(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/sponsorships-balance", jobId));
    }

    function getSponsorshipsTotalRefunded(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/sponsorships-total-refunded", jobId));
    }

    function subJobSponsorshipsBalance(address db, uint jobId, uint amount) internal {
        EthlanceDB(db).subUIntValue(sha3("job/sponsorships-balance", jobId), amount);
    }

    function refundSponsorship(address db, uint jobId, uint amount) internal {
        addSponsorshipsTotalRefunded(db, jobId, amount);
        subJobSponsorshipsBalance(db, jobId, amount);
    }

    function getSponsorships(address db, uint jobId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, jobId, "job/sponsorships", "job/sponsorships-count");
    }

    function getSponsorshipsSortedByAmount(address db, uint jobId) internal returns(uint[]) {
        var sponsorships = getSponsorships(db, jobId);
    }
    
    function addContract(address db, uint jobId, uint contractId) internal {
        SharedLibrary.addIdArrayItem(db, jobId, "job/contracts", "job/contracts-count", contractId);
    }

    function getContracts(address db, uint jobId) internal returns(uint[]) {
        return SharedLibrary.getIdArray(db, jobId, "job/contracts", "job/contracts-count");
    }

    function getEmployer(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/employer", jobId));
    }
    
    function getCreatedOn(address db, uint jobId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job/created-on", jobId));
    }

    function getPaymentType(address db, uint jobId) internal returns(uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("job/payment-type", jobId));
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

    function getReferenceCurrency(address db, uint jobId) internal returns(uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("job/reference-currency", jobId));
    }

    function hasMinBudget(address db, uint jobId, uint[] minBudgets) internal returns(bool) {
        var referenceCurrency = getReferenceCurrency(db, jobId);
        if (minBudgets[referenceCurrency] == 0) {
            return true;
        }
        return minBudgets[referenceCurrency] <= EthlanceDB(db).getUIntValue(sha3("job/budget", jobId));
    }

   function hasLanguage(address db, uint jobId, uint languageId) internal returns (bool) {
        if (languageId == 0) {
            return true;
        }
        return languageId == EthlanceDB(db).getUIntValue(sha3("job/language", jobId));
   }

   function hasMinCreatedOn(address db, uint jobId, uint minCreatedOn) internal returns (bool) {
       if (minCreatedOn == 0) {
           return true;
       }
       return minCreatedOn <= EthlanceDB(db).getUIntValue(sha3("job/created-on", jobId));
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
        uint[] skillsAnd,
        uint[] skillsOr,
        uint8[][4] uint8Filters,
        uint[] minBudgets,
        uint[] uintArgs
    )
        internal returns (uint[] jobIds)
    {
        uint j = 0;
        uint jobId;
        uint employerId;
        var allJobIds = SharedLibrary.intersectCategoriesAndSkills(db, categoryId, skillsAnd, skillsOr,
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
                hasMinBudget(db, jobId, minBudgets) &&
                hasEmployerMinRating(db, employerId, uintArgs[0]) &&
                hasEmployerMinRatingsCount(db, employerId, uintArgs[1]) &&
                UserLibrary.isFromCountry(db, employerId, uintArgs[2]) &&
                UserLibrary.isFromState(db, employerId, uintArgs[3]) &&
                hasLanguage(db, jobId, uintArgs[4]) &&
                hasMinCreatedOn(db, jobId, uintArgs[5]) &&
                UserLibrary.hasStatus(db, employerId, 1))
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
