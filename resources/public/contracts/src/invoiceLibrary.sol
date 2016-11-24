pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "safeMath.sol";
import "contractLibrary.sol";
import "userLibrary.sol";
import "jobLibrary.sol";
import "sharedLibrary.sol";

library InvoiceLibrary {

    //    status:
    //    1: pending, 2: paid, 3: cancelled

    function addInvoice(
        address _storage,
        uint senderId,
        uint contractId,
        string description,
        uint amount,
        uint workedHours,
        uint workedFrom,
        uint workedTo
        )
    {
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        if (freelancerId != senderId) throw;
        var invoiceId = SharedLibrary.createNext(_storage, "invoice/count");
        EternalStorage(_storage).setUIntValue(sha3("invoice/contract", invoiceId), contractId);
        EternalStorage(_storage).setStringValue(sha3("invoice/description", invoiceId), description);
        EternalStorage(_storage).setUIntValue(sha3("invoice/amount", invoiceId), amount);
        EternalStorage(_storage).setUIntValue(sha3("invoice/worked-hours", invoiceId), workedHours);
        EternalStorage(_storage).setUIntValue(sha3("invoice/worked-from", invoiceId), workedFrom);
        EternalStorage(_storage).setUIntValue(sha3("invoice/worked-to", invoiceId), workedTo);
        EternalStorage(_storage).setUIntValue(sha3("invoice/created-on", invoiceId), now);
        EternalStorage(_storage).setUIntValue(sha3("invoice/status", invoiceId), 1);
        ContractLibrary.addInvoice(_storage, contractId, invoiceId, amount);
    }

    function getContract(address _storage, uint invoiceId) constant returns (uint) {
        return EternalStorage(_storage).getUIntValue(sha3("invoice/contract", invoiceId));
    }

    function getAmount(address _storage, uint invoiceId) constant returns (uint) {
        return EternalStorage(_storage).getUIntValue(sha3("invoice/amount", invoiceId));
    }

    function getFreelancerAddress(address _storage, uint invoiceId) constant returns (address) {
        var contractId = getContract(_storage, invoiceId);
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        return UserLibrary.getUserAddress(_storage, freelancerId);
    }

    function setInvoicePaid(address _storage, uint senderId, uint sentAmount, uint invoiceId) {
        var amount = EternalStorage(_storage).getUIntValue(sha3("invoice/amount", invoiceId));
        var contractId = getContract(_storage, invoiceId);
        var employerId = ContractLibrary.getEmployer(_storage, contractId);
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        var jobId = ContractLibrary.getJob(_storage, contractId);

        if (employerId != senderId) throw;
        if (amount != sentAmount) throw;

        EternalStorage(_storage).setUInt8Value(sha3("invoice/status", invoiceId), 2);
        EternalStorage(_storage).setUIntValue(sha3("invoice/paid-on", invoiceId), now);
        ContractLibrary.addTotalPaid(_storage, contractId, amount);
        UserLibrary.addToFreelancerTotalEarned(_storage, freelancerId, amount);
        UserLibrary.addToEmployerTotalPaid(_storage, employerId, amount);
        JobLibrary.addTotalPaid(_storage, jobId, amount);
    }

    function setInvoiceCancelled(address _storage, uint senderId, uint invoiceId) {
        var contractId = getContract(_storage, invoiceId);
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        var amount = EternalStorage(_storage).getUIntValue(sha3("invoice/amount", invoiceId));
        if (freelancerId != senderId) throw;
        EternalStorage(_storage).setUInt8Value(sha3("invoice/status", invoiceId), 3);
        EternalStorage(_storage).setUIntValue(sha3("invoice/cancelled-on", invoiceId), now);
        ContractLibrary.subTotalInvoiced(_storage, contractId, amount);
    }
    
    function getStatus(address _storage, uint invoiceId) constant returns(uint8) {
        return EternalStorage(_storage).getUInt8Value(sha3("invoice/status", invoiceId));
    }
    
    function statusPred(address _storage, uint[] args, uint jobId) internal returns(bool) {
        var status = getStatus(_storage, jobId);
        return status == 0 || status == args[0];
    }


}