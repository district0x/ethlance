pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "contractLibrary.sol";
import "strings.sol";

contract EthlanceContract is EthlanceSetter {
    using strings for *;

    event onJobProposalAdded(uint contractId, uint indexed employerId, uint freelancerId);
    event onJobContractAdded(uint contractId, uint employerId, uint indexed freelancerId);
    event onJobContractCancelled(uint contractId, uint indexed employerId, uint freelancerId);
    event onJobContractFeedbackAdded(uint contractId, uint indexed receiverId, uint senderId, bool isSenderFreelancer);
    event onJobInvitationAdded(uint jobId, uint contractId, uint indexed freelancerId);

    function EthlanceContract(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addJobContract(
        uint contractId,
        string description,
        bool isHiringDone
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        if (description.toSlice().len() > getConfig("max-contract-desc")) throw;
        var employerId = getSenderUserId();
        ContractLibrary.addContract(ethlanceDB, employerId, contractId, description, isHiringDone);
        var freelancerId = ContractLibrary.getFreelancer(ethlanceDB, contractId);
        onJobContractAdded(contractId, employerId, freelancerId);
    }

    function cancelJobContract(
        uint contractId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        if (description.toSlice().len() > getConfig("max-contract-desc")) throw;
        var freelancerId = getSenderUserId();
        ContractLibrary.cancelContract(ethlanceDB, freelancerId, contractId, description);
        var employerId = ContractLibrary.getEmployer(ethlanceDB, contractId);
        onJobContractCancelled(contractId, employerId, freelancerId);
    }

    function addJobContractFeedback(
        uint contractId,
        string feedback,
        uint8 rating
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        var feedbackLen = feedback.toSlice().len();
        if (feedbackLen > getConfig("max-feedback")) throw;
        if (feedbackLen < getConfig("min-feedback")) throw;
        if (rating > 100) throw;
        var senderId = getSenderUserId();
        ContractLibrary.addFeedback(ethlanceDB, contractId, senderId, feedback, rating);

        bool isSenderFreelancer;
        uint receiverId;
        (receiverId, isSenderFreelancer) = ContractLibrary.getOtherContractParticipant(ethlanceDB, contractId, senderId);
        onJobContractFeedbackAdded(contractId, receiverId, senderId, isSenderFreelancer);
    }

    function addJobProposal(
        uint jobId,
        string description,
        uint rate
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        if (description.toSlice().len() > getConfig("max-proposal-desc")) throw;
        var freelancerId = getSenderUserId();
        var contractId = ContractLibrary.addProposal(ethlanceDB, jobId, freelancerId, description, rate);
        var employerId = JobLibrary.getEmployer(ethlanceDB, jobId);
        onJobProposalAdded(contractId, employerId, freelancerId);
    }

    function addJobInvitation(
        uint jobId,
        uint freelancerId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        if (description.toSlice().len() > getConfig("max-invitation-desc")) throw;
        var contractId = ContractLibrary.addInvitation(ethlanceDB, getSenderUserId(), jobId, freelancerId, description);
        onJobInvitationAdded(jobId, contractId, freelancerId);
    }
}