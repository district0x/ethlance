pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "userLibrary.sol";
import "jobLibrary.sol";
import "sharedLibrary.sol";

library ContractLibrary {

    //    status:
    //    1: invited, 2: pending proposal, 3: accepted, 4: finished

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
        if (getContract(db, freelancerId, jobId) != 0) throw;
        var contractId = SharedLibrary.createNext(db, "contract/count");
        setFreelancerJobIndex(db, contractId, freelancerId, jobId);
        
        EthlanceDB(db).setStringValue(sha3("invitation/description", contractId), description);
        EthlanceDB(db).setUIntValue(sha3("invitation/created-on", contractId), now);
        setStatus(db, contractId, 1);
        UserLibrary.addFreelancerContract(db, freelancerId, contractId);
        JobLibrary.addContract(db, jobId, contractId);
    }
    
    function addProposal(
        address db,
        uint jobId,
        uint freelancerId,
        string description,
        uint rate
    )
        internal
    {
        var employerId = JobLibrary.getEmployer(db, jobId);
        if (employerId == 0) throw;
        if (freelancerId == employerId) throw;
        if (freelancerId == 0) throw;
        var contractId = getContract(db, freelancerId, jobId);
        if (contractId == 0) {
            contractId = SharedLibrary.createNext(db, "contract/count");
            UserLibrary.addFreelancerContract(db, freelancerId, contractId);
            JobLibrary.addContract(db, jobId, contractId);
        } else if (getProposalCreatedOn(db, contractId) != 0) throw;
        
        setFreelancerJobIndex(db, contractId, freelancerId, jobId);
        EthlanceDB(db).setUIntValue(sha3("proposal/rate", contractId), rate);
        EthlanceDB(db).setUIntValue(sha3("proposal/created-on", contractId), now);
        EthlanceDB(db).setStringValue(sha3("proposal/description", contractId), description);
        setStatus(db, contractId, 2);
    }

    function addContract(
        address db,
        uint senderId,
        uint contractId,
        string description,
        bool isHiringDone
    )
        internal
    {
        var jobId = getJob(db, contractId);
        var freelancerId = getFreelancer(db, contractId);
        var employerId = JobLibrary.getEmployer(db, jobId);
        if (employerId == 0) throw;
        if (senderId != employerId) throw;
        if (senderId == freelancerId) throw;
        EthlanceDB(db).setUIntValue(sha3("contract/created-on", contractId), now);
        EthlanceDB(db).setStringValue(sha3("contract/description", contractId), description);
        setStatus(db, contractId, 3);
        if (isHiringDone) {
            JobLibrary.setHiringDone(db, jobId, senderId);
        }
    }

    function addFeedback(address db, uint contractId, uint senderId, string feedback, uint8 rating) internal {
        var freelancerId = getFreelancer(db, contractId);
        var employerId = getEmployer(db, contractId);
        if (senderId != freelancerId && senderId != employerId) throw;

        if (getStatus(db, contractId) == 1) {
            setStatus(db, contractId, 4);
            EthlanceDB(db).setUIntValue(sha3("contract/done-on", contractId), now);
        }

        if (senderId == freelancerId) {
            if (getFreelancerFeedbackOn(db, contractId) > 0) throw;
            EthlanceDB(db).setBooleanValue(sha3("contract/done-by-freelancer?", contractId), true);
            addFreelancerFeedback(db, contractId, feedback, rating);
        } else {
            if (getEmployerFeedbackOn(db, contractId) > 0) throw;
            addEmployerFeedback(db, contractId, feedback, rating);
        }
    }

    function addUserFeedback(address db, uint contractId, uint userId, string feedbackKey,
        string ratingKey, string dateKey, string ratingsCountKey, string avgRatingKey, string description,
        uint8 rating) internal {
        EthlanceDB(db).setStringValue(sha3(feedbackKey, contractId), description);
        EthlanceDB(db).setUInt8Value(sha3(ratingKey, contractId), rating);
        EthlanceDB(db).setUIntValue(sha3(dateKey, contractId), now);
        UserLibrary.addToAvgRating(db, userId, ratingsCountKey, avgRatingKey, rating);
    }

    function addFreelancerFeedback(address db, uint contractId, string description, uint8 rating) internal {
        var userId = getFreelancer(db, contractId);
        addUserFeedback(db, contractId, userId, "contract/freelancer-feedback",
            "contract/freelancer-feedback-rating", "contract/freelancer-feedback-on", "freelancer/ratings-count",
            "freelancer/avg-rating", description, rating);
    }

    function addEmployerFeedback(address db, uint contractId, string description, uint8 rating) internal {
        var userId = getEmployer(db, contractId);
        addUserFeedback(db, contractId, userId, "contract/employer-feedback",
            "contract/employer-feedback-rating", "contract/employer-feedback-on", "employer/ratings-count",
            "employer/avg-rating", description, rating);
    }

    function addTotalInvoiced(address db, uint contractId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("contract/total-invoiced", contractId), amount);
    }

    function subTotalInvoiced(address db, uint contractId, uint amount) internal {
        EthlanceDB(db).subUIntValue(sha3("contract/total-invoiced", contractId), amount);
    }

    function addInvoice(address db, uint contractId, uint invoiceId, uint amount) internal {
        SharedLibrary.addArrayItem(db, contractId, "contract/invoices", "contract/invoices-count", invoiceId);
        addTotalInvoiced(db, contractId, amount);
    }

    function getInvoices(address db, uint contractId) internal returns(uint[]) {
        return SharedLibrary.getUIntArray(db, contractId, "contract/invoices", "contract/invoices-count");
    }

    function getInvoicesCount(address db, uint contractId) internal returns(uint) {
        return SharedLibrary.getArrayItemsCount(db, contractId, "contract/invoices-count");
    }

    function getInvoices(address db, uint[] contractIds) internal returns(uint[] invoiceIds) {
        uint k = 0;
        uint totalCount = getTotalInvoicesCount(db, contractIds);
        invoiceIds = new uint[](totalCount);
        for (uint i = 0; i < contractIds.length ; i++) {
            var contractInvoiceIds = getInvoices(db, contractIds[i]);
            for (uint j = 0; j < contractInvoiceIds.length ; j++) {
                invoiceIds[k] = contractInvoiceIds[j];
                k++;
            }
        }
    }

    function getTotalInvoicesCount(address db, uint[] contractIds) internal returns(uint) {
        uint total;
        for (uint i = 0; i < contractIds.length ; i++) {
            total += getInvoicesCount(db, contractIds[i]);
        }
        return total;
    }

    function addTotalPaid(address db, uint contractId, uint amount) internal {
        EthlanceDB(db).addUIntValue(sha3("contract/total-paid", contractId), amount);
    }

    function getTotalPaid(address db, uint contractId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("contract/total-paid", contractId));
    }

    function getFreelancer(address db, uint contractId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("contract/freelancer", contractId));
    }

    function getEmployer(address db, uint contractId) internal returns (uint) {
        var jobId = getJob(db, contractId);
        return JobLibrary.getEmployer(db, jobId);
    }

    function getJob(address db, uint contractId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("contract/job", contractId));
    }

    function setStatus(address db, uint contractId, uint8 status) internal {
        EthlanceDB(db).setUInt8Value(sha3("contract/status", contractId), status);
    }

    function getStatus(address db, uint contractId) internal returns (uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("contract/status", contractId));
    }

    function getFreelancerFeedbackOn(address db, uint contractId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("contract/freelancer-feedback-on", contractId));
    }

    function getEmployerFeedbackOn(address db, uint contractId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("contract/employer-feedback-on", contractId));
    }

    function getProposalCreatedOn(address db, uint contractId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("proposal/created-on", contractId));
    }

    function getInvitationCreatedOn(address db, uint contractId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("invitation/created-on", contractId));
    }
    
    function getContract(address db, uint freelancerId, uint jobId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("contract/freelancer+job", freelancerId, jobId));
    }

    function getRate(address db, uint contractId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("proposal/rate", contractId));
    }
    
    function setFreelancerJobIndex(address db, uint contractId, uint freelancerId, uint jobId) internal {
        EthlanceDB(db).setUIntValue(sha3("contract/freelancer", contractId), freelancerId);
        EthlanceDB(db).setUIntValue(sha3("contract/job", contractId), jobId);
        EthlanceDB(db).setUIntValue(sha3("contract/freelancer+job", freelancerId, jobId), contractId);
    }
}