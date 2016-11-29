pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "safeMath.sol";
import "userLibrary.sol";
import "jobLibrary.sol";

library JobActionLibrary {

//    status:
//    1: invited, 2: pending, 3: accepted

    function addProposal(
        address db,
        uint jobId,
        uint freelancerId,
        string description,
        uint rate
    )
        internal
    {
        if (freelancerId == JobLibrary.getEmployer(db, jobId)) throw;
        var jobActionId = getJobAction(db, freelancerId, jobId);
        if (jobActionId == 0) {
            jobActionId = SharedLibrary.createNext(db, "job-action/count");
            UserLibrary.addFreelancerJobAction(db, freelancerId, jobActionId);
        } else if (getProposalCreatedOn(db, jobActionId) != 0) throw;
        
        setFreelancerJobIndex(db, jobActionId, freelancerId, jobId);
        EthlanceDB(db).setUIntValue(sha3("proposal/rate", jobActionId), rate);
        EthlanceDB(db).setUIntValue(sha3("proposal/created-on", jobActionId), now);
        EthlanceDB(db).setStringValue(sha3("proposal/description", jobActionId), description);
        setStatus(db, jobActionId, 2);
        JobLibrary.addJobProposal(db, jobId, jobActionId);
    }
    
    function addInvitation(
        address db,
        uint senderId,
        uint jobId,
        uint freelancerId,
        string description
    )
        internal
    {
        var employerId = JobLibrary.getEmployer(db, jobId);
        if (senderId != employerId) throw;
        if (employerId == freelancerId) throw;
        if (getJobAction(db, freelancerId, jobId) != 0) throw;
        var jobActionId = SharedLibrary.createNext(db, "job-action/count");
        setFreelancerJobIndex(db, jobActionId, freelancerId, jobId);
        
        EthlanceDB(db).setStringValue(sha3("invitation/description", jobActionId), description);
        EthlanceDB(db).setUIntValue(sha3("invitation/created-on", jobActionId), now);
        setStatus(db, jobActionId, 1);
        UserLibrary.addFreelancerJobAction(db, freelancerId, jobActionId);
    }

    function setStatus(address db, uint jobActionId, uint8 status) internal {
        EthlanceDB(db).setUInt8Value(sha3("job-action/status", jobActionId), status);
    }

    function getStatus(address db, uint jobActionId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("job-action/status", jobActionId));
    }

    function getJob(address db, uint jobActionId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job-action/job", jobActionId));
    }
    
    function getProposalCreatedOn(address db, uint jobActionId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("proposal/created-on", jobActionId));
    }

    function getInvitationCreatedOn(address db, uint jobActionId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("invitation/created-on", jobActionId));
    }
    
    function getJobAction(address db, uint freelancerId, uint jobId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("job-action/freelancer-job", freelancerId, jobId));
    }

    function getFreelancer(address db, uint jobActionId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("job-action/freelancer", jobActionId));
    }

    function getRate(address db, uint jobActionId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("proposal/rate", jobActionId));
    }
    
    function setFreelancerJobIndex(address db, uint jobActionId, uint freelancerId, uint jobId) internal {
        EthlanceDB(db).setUIntValue(sha3("job-action/freelancer", jobActionId), freelancerId);
        EthlanceDB(db).setUIntValue(sha3("job-action/job", jobActionId), jobId);
        EthlanceDB(db).setUIntValue(sha3("job-action/freelancer-job", freelancerId, jobId), jobActionId);
    }
}