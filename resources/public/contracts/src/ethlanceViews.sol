pragma solidity ^0.4.8;

import "userLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "invoiceLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";
import "sharedLibrary.sol";
import "sponsorLibrary.sol";

contract EthlanceViews {
    address public ethlanceDB;

    function EthlanceViews(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function getFreelancerContracts(address userId, uint[] contractStatuses, uint[] jobStatuses) public constant returns (uint[]) {
        return UserLibrary.getFreelancerContractsByStatus(ethlanceDB, userId, contractStatuses, jobStatuses);
    }

    function getEmployerContracts(address userId, uint[] contractStatuses, uint[] jobStatuses) public constant returns (uint[]) {
        return UserLibrary.getEmployerContractsByStatus(ethlanceDB, userId, contractStatuses, jobStatuses);
    }

    function getFreelancerInvoices(address userId, uint8 invoiceStatus) public constant returns (uint[]) {
        return UserLibrary.getFreelancerInvoicesByStatus(ethlanceDB, userId, invoiceStatus);
    }
    
    function getEmployerInvoices(address userId, uint8 invoiceStatus) public constant returns (uint[]) {
        return UserLibrary.getEmployerInvoicesByStatus(ethlanceDB, userId, invoiceStatus);
    }
    
    function getJobContracts(uint jobId, uint8 contractStatus) public constant returns (uint[]) {
        return JobLibrary.getContractsByStatus(ethlanceDB, jobId, contractStatus);
    }
    
    function getJobInvoices(uint jobId, uint8 invoiceStatus) public constant returns (uint[]) {
        return JobLibrary.getJobInvoicesByStatus(ethlanceDB, jobId, invoiceStatus);
    }

    function getFreelancersJobContracts(address[] userIds, uint jobId) public constant returns (uint[]) {
        return ContractLibrary.getContracts(ethlanceDB, userIds, jobId);
    }

    function getContractInvoices(uint contractId, uint8 invoiceStatus) public constant returns (uint[]) {
        return ContractLibrary.getInvoicesByStatus(ethlanceDB, contractId, invoiceStatus);
    }

    function getContractMessages(uint contractId) public constant returns (uint[]) {
        return ContractLibrary.getMessages(ethlanceDB, contractId);
    }

    function getEmployerJobs(address userId, uint8 jobStatus) public constant returns (uint[]) {
        return JobLibrary.getEmployerJobsByStatus(ethlanceDB, userId, jobStatus);
    }

    function getSkillNames(uint offset, uint limit) constant returns (uint[] skillIds, bytes32[] names) {
        return SkillLibrary.getNames(ethlanceDB, offset, limit);
    }

    function getSkillCount() constant returns (uint) {
        return SkillLibrary.getSkillCount(ethlanceDB);
    }

    function getEmployerJobsForFreelancerInvite(address employerId, address freelancerId) constant returns(uint[]) {
        return JobLibrary.getEmployerJobsForFreelancerInvite(ethlanceDB, employerId, freelancerId);
    }

    function getJobSponsorships(uint jobId) constant returns(uint[]) {
        var sponsorshipIds = JobLibrary.getSponsorships(ethlanceDB, jobId);
        var amounts = SponsorLibrary.getSponsorshipsAmounts(ethlanceDB, sponsorshipIds);
        return SharedLibrary.sortDescBy(sponsorshipIds, amounts);
    }

    function getJobApprovals(uint jobId) constant returns(address[], bool[]) {
        return JobLibrary.getApprovals(ethlanceDB, jobId);
    }

    function getUserSponsorships(address userId) constant returns(uint[]) {
        return UserLibrary.getSponsorships(ethlanceDB, userId);
    }

    function getSponsorableJobs() constant returns(uint[]) {
        return JobLibrary.getSponsorableJobs(ethlanceDB);
    }
}