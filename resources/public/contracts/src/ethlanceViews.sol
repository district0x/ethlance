pragma solidity ^0.4.4;

import "userLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "invoiceLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";
import "sharedLibrary.sol";


contract EthlanceViews {
    address public ethlanceDB;

    function EthlanceViews(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function getFreelancerJobActions(uint userId, uint8 jobActionStatus, uint8 jobStatus) public constant returns (uint[]) {
        return UserLibrary.getFreelancerJobActionsByStatus(ethlanceDB, userId, jobActionStatus, jobStatus);
    }

    function getFreelancerInvoices(uint userId, uint8 invoiceStatus) public constant returns (uint[]) {
        return UserLibrary.getFreelancerInvoicesByStatus(ethlanceDB, userId, invoiceStatus);
    }
    
    function getFreelancerContracts(uint userId, bool isDone) public constant returns (uint[]) {
        return UserLibrary.getFreelancerContracts(ethlanceDB, userId, isDone);
    }
    
    function getJobContracts(uint userId) public constant returns (uint[]) {
        return JobLibrary.getContracts(ethlanceDB, userId);
    }
    
    function getJobContractsList(uint jobId, uint[] ids, bytes32[] fieldNames, uint8[] uintTypes)
    public constant returns (uint[], uint[], uint[], uint[], uint[], uint[], uint[]) {
        return SharedLibrary.getEntityList(ethlanceDB, getJobContracts(jobId), fieldNames, uintTypes);
    }

    function getJobProposals(uint jobId) public constant returns (uint[]) {
        return JobLibrary.getProposals(ethlanceDB, jobId);
    }
    
    function getJobProposalsList(uint jobId, uint[] ids, bytes32[] fieldNames, uint8[] uintTypes)
    public constant returns (uint[], uint[], uint[], uint[], uint[], uint[], uint[]) {
        return SharedLibrary.getEntityList(ethlanceDB, getJobProposals(jobId), fieldNames, uintTypes);
    }

    function getJobInvoices(uint jobId, uint8 invoiceStatus) public constant returns (uint[]) {
        return JobLibrary.getJobInvoicesByStatus(ethlanceDB, jobId, invoiceStatus);
    }
    
    function getJobInvoicesList(uint jobId, uint8 invoiceStatus, uint[] ids, bytes32[] fieldNames, uint8[] uintTypes)
    public constant returns (uint[], uint[], uint[], uint[], uint[], uint[], uint[]) {
        return SharedLibrary.getEntityList(ethlanceDB, getJobInvoices(jobId, invoiceStatus), fieldNames, uintTypes);
    }

    function getEmployerJobs(uint userId, uint8 jobStatus) public constant returns (uint[]) {
        return JobLibrary.getEmployerJobsByStatus(ethlanceDB, userId, jobStatus);
    }

    function getEmployerJobsList(uint userId, uint8 jobStatus, uint[] ids, bytes32[] fieldNames, uint8[] uintTypes)
    public constant returns (uint[], uint[], uint[], uint[], uint[], uint[], uint[]) {
        return SharedLibrary.getEntityList(ethlanceDB, getEmployerJobs(userId, jobStatus), fieldNames, uintTypes);
    }

    function getNames() constant returns (uint[] skillIds, bytes32[] names) {
        return SkillLibrary.getNames(ethlanceDB);
    }
}