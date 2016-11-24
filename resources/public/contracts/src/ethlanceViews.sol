pragma solidity ^0.4.4;

import "userLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "invoiceLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";

contract EthlanceViews {
    address public eternalStorage;

    function EthlanceViews(address _eternalStorage) {
        eternalStorage = _eternalStorage;
    }

    function getFreelancerJobActions(uint userId, uint8 jobActionStatus, uint8 jobStatus) public constant returns (uint[]) {
        return UserLibrary.getFreelancerJobActionsByStatus(eternalStorage, userId, jobActionStatus, jobStatus);
    }

    function getFreelancerInvoices(uint userId, uint8 invoiceStatus) public constant returns (uint[]) {
        return UserLibrary.getFreelancerInvoicesByStatus(eternalStorage, userId, invoiceStatus);
    }

    function getFreelancerContracts(uint userId, bool isDone) public constant returns (uint[]) {
        return UserLibrary.getFreelancerContracts(eternalStorage, userId, isDone);
    }

    function getJobContracts(uint userId) public constant returns (uint[]) {
        return JobLibrary.getContracts(eternalStorage, userId);
    }

    function getJobProposals(uint jobId) public constant returns (uint[]) {
        return JobLibrary.getProposals(eternalStorage, jobId);
    }

    function getJobInvoices(uint jobId, uint8 invoiceStatus) public constant returns (uint[]) {
        return JobLibrary.getJobInvoicesByStatus(eternalStorage, jobId, invoiceStatus);
    }

    function getEmployerJobs(uint userId, uint8 jobStatus) public constant returns (uint[]) {
        return JobLibrary.getEmployerJobsByStatus(eternalStorage, userId, jobStatus);
    }

    function getNames() constant returns (uint[] skillIds, bytes32[] names) {
        return SkillLibrary.getNames(eternalStorage);
    }
}