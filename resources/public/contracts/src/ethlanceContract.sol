pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "contractLibrary.sol";
import "strings.sol";

contract EthlanceContract is EthlanceSetter {
    using strings for *;

    event onJobProposalAdded(uint contractId, uint jobId, address indexed employerId, address freelancerId);
    event onJobContractAdded(uint contractId, address employerId, address indexed freelancerId);
    event onJobContractCancelled(uint contractId, address indexed employerId, address freelancerId);
    event onJobContractFeedbackAdded(uint contractId, address indexed receiverId, address senderId, bool isSenderFreelancer);
    event onJobInvitationAdded(uint jobId, uint contractId, address indexed freelancerId);

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
        ContractLibrary.addContract(ethlanceDB, msg.sender, contractId, description, isHiringDone);
        var freelancerId = ContractLibrary.getFreelancer(ethlanceDB, contractId);
        onJobContractAdded(contractId, msg.sender, freelancerId);
    }

    function cancelJobContract(
        uint contractId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        if (description.toSlice().len() > getConfig("max-contract-desc")) throw;
        ContractLibrary.cancelContract(ethlanceDB, msg.sender, contractId, description);
        var employerId = ContractLibrary.getEmployer(ethlanceDB, contractId);
        onJobContractCancelled(contractId, employerId, msg.sender);
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
        var contractId = ContractLibrary.addProposal(ethlanceDB, jobId, msg.sender, description, rate);
        var employerId = JobLibrary.getEmployer(ethlanceDB, jobId);
        onJobProposalAdded(contractId, jobId, employerId, msg.sender);
    }

    function addJobInvitation(
        uint jobId,
        address freelancerId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        if (description.toSlice().len() > getConfig("max-invitation-desc")) throw;
        var contractId = ContractLibrary.addInvitation(ethlanceDB, msg.sender, jobId, freelancerId, description);
        onJobInvitationAdded(jobId, contractId, freelancerId);
    }
}