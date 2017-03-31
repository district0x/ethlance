pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "jobLibrary.sol";
import "strings.sol";

contract EthlanceJob is EthlanceSetter {
    using strings for *;

    event onJobAdded(uint jobId);
    event onSponsorableJobApproved(uint jobId, uint indexed employerId, address approver);

    function EthlanceJob(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setJob(
        uint jobId,
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items,
        bool isSponsorable,
        address[] allowedUsers
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        var descriptionLen = description.toSlice().len();
        var titleLen = title.toSlice().len();
        require(descriptionLen <= getConfig("max-job-description"));
        require(descriptionLen >= getConfig("min-job-description"));
        require(titleLen <= getConfig("max-job-title"));
        require(titleLen >= getConfig("min-job-title"));
        require(skills.length <= getConfig("max-job-skills"));
        require(skills.length >= getConfig("min-job-skills"));
        require(allowedUsers.length <= getConfig("max-job-allowed-users"));
        if (isSponsorable) {
            require(allowedUsers.length >= getConfig("min-job-allowed-users"));
        }

        var newJobId = JobLibrary.setJob(ethlanceDB, jobId, getSenderUserId(), msg.sender, title, description, skills,
            language, budget, uint8Items, isSponsorable, allowedUsers);
        if (jobId == 0) {
            onJobAdded(newJobId);
        }
    }

    function approveSponsorableJob
    (
        uint jobId
    )
        onlyActiveSmartContract
    {
        JobLibrary.approveSponsorableJob(ethlanceDB, jobId, msg.sender);
        onSponsorableJobApproved(jobId, JobLibrary.getEmployer(ethlanceDB, jobId), msg.sender);
    }

    function setJobHiringDone
    (
        uint jobId
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        JobLibrary.setHiringDone(ethlanceDB, jobId, getSenderUserId());
    }

    function setJobStatus(
        uint jobId,
        uint8 status
    )
        onlyOwner
    {
        JobLibrary.setStatus(ethlanceDB, jobId, status);
    }
}