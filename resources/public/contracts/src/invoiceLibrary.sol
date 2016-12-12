pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "safeMath.sol";
import "contractLibrary.sol";
import "userLibrary.sol";
import "jobLibrary.sol";
import "sharedLibrary.sol";

library InvoiceLibrary {

    //    status:
    //    1: pending, 2: paid, 3: cancelled

    function addInvoice(
        address db,
        uint senderId,
        uint contractId,
        string description,
        uint amount,
        uint workedHours,
        uint workedFrom,
        uint workedTo
    )
        internal
    {
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        var employerId = ContractLibrary.getEmployer(db, contractId);
        if (freelancerId != senderId) throw;
        if (ContractLibrary.getStatus(db, contractId) != 3) throw;
        var invoiceId = SharedLibrary.createNext(db, "invoice/count");
        EthlanceDB(db).setUIntValue(sha3("invoice/contract", invoiceId), contractId);
        EthlanceDB(db).setStringValue(sha3("invoice/description", invoiceId), description);
        EthlanceDB(db).setUIntValue(sha3("invoice/amount", invoiceId), amount);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-hours", invoiceId), workedHours);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-from", invoiceId), workedFrom);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-to", invoiceId), workedTo);
        EthlanceDB(db).setUIntValue(sha3("invoice/created-on", invoiceId), now);
        EthlanceDB(db).setUInt8Value(sha3("invoice/status", invoiceId), 1);
        ContractLibrary.addInvoice(db, contractId, invoiceId, amount);
        UserLibrary.addFreelancerTotalInvoiced(db, freelancerId, amount);
        UserLibrary.addEmployerTotalInvoiced(db, employerId, amount);
    }

    function getContract(address db, uint invoiceId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("invoice/contract", invoiceId));
    }

    function getAmount(address db, uint invoiceId) internal returns (uint) {
        return EthlanceDB(db).getUIntValue(sha3("invoice/amount", invoiceId));
    }

    function getFreelancerAddress(address db, uint invoiceId) internal returns (address) {
        var contractId = getContract(db, invoiceId);
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        return UserLibrary.getUserAddress(db, freelancerId);
    }

    function setInvoicePaid(address db, uint senderId, uint sentAmount, uint invoiceId) internal {
        var amount = EthlanceDB(db).getUIntValue(sha3("invoice/amount", invoiceId));
        var contractId = getContract(db, invoiceId);
        var employerId = ContractLibrary.getEmployer(db, contractId);
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        var jobId = ContractLibrary.getJob(db, contractId);

        if (getStatus(db, invoiceId) != 1) throw;
        if (employerId != senderId) throw;
        if (amount != sentAmount) throw;

        EthlanceDB(db).setUInt8Value(sha3("invoice/status", invoiceId), 2);
        EthlanceDB(db).setUIntValue(sha3("invoice/paid-on", invoiceId), now);
        ContractLibrary.addTotalPaid(db, contractId, amount);
        UserLibrary.addToFreelancerTotalEarned(db, freelancerId, amount);
        UserLibrary.addToEmployerTotalPaid(db, employerId, amount);
        UserLibrary.subEmployerTotalInvoiced(db, employerId, amount);
        JobLibrary.addTotalPaid(db, jobId, amount);
    }

    function setInvoiceCancelled(address db, uint senderId, uint invoiceId) internal {
        var contractId = getContract(db, invoiceId);
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        var employerId = ContractLibrary.getEmployer(db, contractId);
        var amount = EthlanceDB(db).getUIntValue(sha3("invoice/amount", invoiceId));

        if (freelancerId != senderId) throw;
        if (getStatus(db, invoiceId) != 1) throw;

        EthlanceDB(db).setUInt8Value(sha3("invoice/status", invoiceId), 3);
        EthlanceDB(db).setUIntValue(sha3("invoice/cancelled-on", invoiceId), now);
        ContractLibrary.subTotalInvoiced(db, contractId, amount);
        UserLibrary.subFreelancerTotalInvoiced(db, freelancerId, amount);
        UserLibrary.subEmployerTotalInvoiced(db, employerId, amount);
    }
    
    function getStatus(address db, uint invoiceId) internal returns(uint8) {
        return EthlanceDB(db).getUInt8Value(sha3("invoice/status", invoiceId));
    }
    
    function statusPred(address db, uint[] args, uint jobId) internal returns(bool) {
        var status = getStatus(db, jobId);
        return args[0] == 0 || status == args[0];
    }
}