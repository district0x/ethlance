pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "safeMath.sol";
import "userLibrary.sol";
import "jobLibrary.sol";

library JobActionLibrary {

//    status:
//    1: invited, 2: pending, 3: accepted

    function addProposal(
        address _storage,
        uint jobId,
        uint freelancerId,
        string description,
        uint rate)
    {
        var jobActionId = getJobAction(_storage, freelancerId, jobId);
        if (jobActionId == 0) {
            jobActionId = SharedLibrary.createNext(_storage, "job-action/count");
            UserLibrary.addFreelancerJobAction(_storage, freelancerId, jobActionId);
        } else if (getProposalCreatedOn(_storage, jobActionId) != 0) throw;
        
        setFreelancerJobIndex(_storage, jobActionId, freelancerId, jobId);
        EternalStorage(_storage).setUIntValue(sha3("proposal/rate", jobActionId), rate);
        EternalStorage(_storage).setUIntValue(sha3("proposal/created-on", jobActionId), now);
        EternalStorage(_storage).setStringValue(sha3("proposal/description", jobActionId), description);
        setStatus(_storage, jobActionId, 2);
        JobLibrary.addJobProposal(_storage, jobId, jobActionId);
    }
    
    function addInvitation(
        address _storage,
        address senderAddress,
        uint jobId,
        uint freelancerId,
        string description
        )
    {
        var senderId = UserLibrary.getUserId(_storage, senderAddress);
        var employerId = JobLibrary.getEmployer(_storage, jobId);
        if (senderId != employerId) throw;
        if (getJobAction(_storage, freelancerId, jobId) != 0) throw;
        var jobActionId = SharedLibrary.createNext(_storage, "job-action/count");
        setFreelancerJobIndex(_storage, jobActionId, freelancerId, jobId);
        
        EternalStorage(_storage).setStringValue(sha3("invitation/description", jobActionId), description);
        EternalStorage(_storage).setUIntValue(sha3("invitation/created-on", jobActionId), now);
        setStatus(_storage, jobActionId, 1);
        UserLibrary.addFreelancerJobAction(_storage, freelancerId, jobActionId);
    }

    function setStatus(address _storage, uint jobActionId, uint8 status) {
        EternalStorage(_storage).setUInt8Value(sha3("job-action/status", jobActionId), status);
    }

    function getStatus(address _storage, uint jobActionId) constant returns (uint8) {
        return EternalStorage(_storage).getUInt8Value(sha3("job-action/status", jobActionId));
    }

    function getJob(address _storage, uint jobActionId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("job-action/job", jobActionId));
    }
    
    function getProposalCreatedOn(address _storage, uint jobActionId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("proposal/created-on", jobActionId));
    }

    function getInvitationCreatedOn(address _storage, uint jobActionId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("invitation/created-on", jobActionId));
    }
    
    function getJobAction(address _storage, uint freelancerId, uint jobId) constant returns (uint) {
        return EternalStorage(_storage).getUIntValue(sha3("invitation/freelancer-job", freelancerId, jobId));
    }

    function getFreelancer(address _storage, uint jobActionId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("job-action/freelancer", jobActionId));
    }

    function getRate(address _storage, uint jobActionId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("proposal/rate", jobActionId));
    }
    
    function setFreelancerJobIndex(address _storage, uint jobActionId, uint freelancerId, uint jobId) {
        EternalStorage(_storage).setUIntValue(sha3("job-action/freelancer", jobActionId), freelancerId);
        EternalStorage(_storage).setUIntValue(sha3("job-action/job", jobActionId), jobId);
        EternalStorage(_storage).setUIntValue(sha3("job-action/freelancer-job", freelancerId, jobId), jobActionId);
    }

    function getProposalList(address _storage, uint[] jobActionIds)
        internal returns
     (
        uint[],
        uint[] freelancerIds,
        uint[] createdOns,
        uint[] invitedOns,
        uint[] statuses,
        uint[] rates
     )
    {
        for (uint i = 0; i < jobActionIds.length ; i++) {
            var jobActionId = jobActionIds[i];
            freelancerIds[i] = getFreelancer(_storage, jobActionId);
            createdOns[i] = getProposalCreatedOn(_storage, jobActionId);
            invitedOns[i] = getInvitationCreatedOn(_storage, jobActionId);
            statuses[i] = getStatus(_storage, jobActionId);
            rates[i] = getRate(_storage, jobActionId);
        }
        return (jobActionIds, freelancerIds, createdOns, invitedOns, statuses, rates);
    }
    
}