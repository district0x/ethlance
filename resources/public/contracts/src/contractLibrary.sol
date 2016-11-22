pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "userLibrary.sol";
import "jobLibrary.sol";
import "sharedLibrary.sol";
import "jobActionLibrary.sol";

library ContractLibrary {

    //    status:
    //    1: running, 2: done

    function addContract(
        address _storage,
        address senderAddress,
        uint proposalId,
        uint freelancerId,
        uint rate)
    {
        var senderId = UserLibrary.getUserId(_storage, senderAddress);
        var jobId = JobActionLibrary.getJob(_storage, proposalId);
        var employerId = JobLibrary.getEmployer(_storage, jobId);
        if (senderId != employerId) throw;
        var idx = SharedLibrary.createNext(_storage, "contract/count");
        EternalStorage(_storage).setUIntValue(sha3("contract/job", idx), jobId);
        EternalStorage(_storage).setUIntValue(sha3("contract/freelancer", idx), freelancerId);
        EternalStorage(_storage).setUIntValue(sha3("contract/rate", idx), rate);
        EternalStorage(_storage).setUInt8Value(sha3("contract/status", idx), 1);
        EternalStorage(_storage).setUIntValue(sha3("contract/created-on", idx), now);
        JobActionLibrary.setStatus(_storage, proposalId, 3);
        UserLibrary.addFreelancerContract(_storage, freelancerId, idx);
        JobLibrary.addJobContract(_storage, jobId, idx);
    }

    function addTotalInvoiced(address _storage, uint contractId, uint amount) {
        EternalStorage(_storage).addUIntValue(sha3("contract/total-invoiced", contractId), amount);
    }

    function subTotalInvoiced(address _storage, uint contractId, uint amount) {
        EternalStorage(_storage).subUIntValue(sha3("contract/total-invoiced", contractId), amount);
    }

    function addTotalPaid(address _storage, uint contractId, uint amount) {
        EternalStorage(_storage).addUIntValue(sha3("contract/total-paid", contractId), amount);
    }

    function getTotalPaid(address _storage, uint contractId) constant returns (uint) {
        return EternalStorage(_storage).getUIntValue(sha3("contract/total-paid", contractId));
    }

    function getFreelancer(address _storage, uint contractId) constant returns (uint) {
        return EternalStorage(_storage).getUIntValue(sha3("contract/freelancer", contractId));
    }

    function getEmployer(address _storage, uint contractId) constant returns (uint) {
        var jobId = EternalStorage(_storage).getUIntValue(sha3("contract/job", contractId));
        return JobLibrary.getEmployer(_storage, jobId);
    }

    function getJob(address _storage, uint jobActionId) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3("contract/job", jobActionId));
    }

    function getStatus(address _storage, uint contractId) constant returns (uint8) {
        return EternalStorage(_storage).getUInt8Value(sha3("contract/status", contractId));
    }

    function addFreelancerFeedback(address _storage, uint contractId, string description, uint8 rating) {
        var userId = getFreelancer(_storage, contractId);
        addFeedback(_storage, contractId, userId, "contract/freelancer-feedback",
            "contract/freelancer-feedback-rating", "contract/freelancer-feedback-on", "freelancer/ratings-count",
            "freelancer/avg-rating", description, rating);
    }
    
    function addEmployerFeedback(address _storage, uint contractId, string description, uint8 rating) {
        var userId = getEmployer(_storage, contractId);
        addFeedback(_storage, contractId, userId, "contract/employer-feedback",
            "contract/employer-feedback-rating", "contract/employer-feedback-on", "employer/ratings-count",
            "employer/avg-rating", description, rating);
    }

    function addFeedback(address _storage, uint contractId, uint userId, string feedbackKey,
        string ratingKey, string dateKey, string avgRatingKey, string ratingsCountKey, string description,
        uint8 rating) {
        EternalStorage(_storage).setStringValue(sha3(feedbackKey, contractId), description);
        EternalStorage(_storage).setUInt8Value(sha3(ratingKey, contractId), rating);
        EternalStorage(_storage).setUIntValue(sha3(dateKey, contractId), now);
        UserLibrary.addToAvgRating(_storage, userId, ratingsCountKey, avgRatingKey, rating);
    }

    function setContractDone(address _storage, uint contractId) {
        EternalStorage(_storage).setUInt8Value(sha3("contract/status", contractId), 2);
        EternalStorage(_storage).setUIntValue(sha3("contract/done-on", contractId), now);
    }

    function getContractList(address _storage, uint[] contractIds)
        internal returns
    (
        uint[],
        uint[] jobIds,
        uint[] freelancerIds,
        uint[] totalPaids,
        uint[] createdOns,
        uint[] doneOns,
        uint[] rates)
    {
        for (uint i = 0; i < contractIds.length ; i++) {
            var contractId = contractIds[i];
            jobIds[i] = getJob(_storage, contractId);
            freelancerIds[i] = getFreelancer(_storage, contractId);
            totalPaids[i] = getTotalPaid(_storage, contractId);
            createdOns[i] = EternalStorage(_storage).getUIntValue(sha3("contract/created-on", contractId));
            doneOns[i] = EternalStorage(_storage).getUIntValue(sha3("contract/done-on", contractId));
            rates[i] = EternalStorage(_storage).getUIntValue(sha3("contract/rate", contractId));
        }
        return (contractIds, jobIds, freelancerIds, totalPaids, createdOns, doneOns, rates);
    }
}