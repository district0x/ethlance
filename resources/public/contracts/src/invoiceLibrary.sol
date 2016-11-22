pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "safeMath.sol";
import "contractLibrary.sol";
import "userLibrary.sol";
import "jobLibrary.sol";

library InvoiceLibrary {

    //    status:
    //    1: pending, 2: paid, 3: cancelled

    function addInvoice(
        address _storage,
        address senderAddress,
        uint contractId,
        string description,
        uint amount,
        uint16 workedHours,
        uint workedFrom,
        uint workedTo
        )
    {
        var senderId = UserLibrary.getUserId(_storage, senderAddress);
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        if (freelancerId != senderId) throw;
        var idx = SharedLibrary.createNext(_storage, "invoice/count");
        EternalStorage(_storage).setUIntValue(sha3("invoice/contract", idx), contractId);
        EternalStorage(_storage).setStringValue(sha3("invoice/description", idx), description);
        EternalStorage(_storage).setUIntValue(sha3("invoice/amount", idx), amount);
        EternalStorage(_storage).setUInt16Value(sha3("invoice/worked-hours", idx), workedHours);
        EternalStorage(_storage).setUIntValue(sha3("invoice/worked-from", idx), workedFrom);
        EternalStorage(_storage).setUIntValue(sha3("invoice/worked-to", idx), workedTo);
        EternalStorage(_storage).setUIntValue(sha3("invoice/created-on", idx), now);
        EternalStorage(_storage).setUIntValue(sha3("invoice/status", idx), 1);
        ContractLibrary.addTotalInvoiced(_storage, contractId, amount);
    }

    function getContract(address _storage, uint invoiceId) constant returns (uint) {
        return EternalStorage(_storage).getUIntValue(sha3("invoice/contract", invoiceId));
    }

    function setInvoicePaid(address _storage, address senderAddress, uint invoiceId) {
        var senderId = UserLibrary.getUserId(_storage, senderAddress);
        var amount = EternalStorage(_storage).getUIntValue(sha3("invoice/amount", invoiceId));
        var contractId = getContract(_storage, invoiceId);
        var employerId = ContractLibrary.getEmployer(_storage, contractId);
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        var jobId = ContractLibrary.getJob(_storage, contractId);
        if (employerId != senderId) throw;
        EternalStorage(_storage).setUInt8Value(sha3("invoice/status", invoiceId), 2);
        EternalStorage(_storage).setUIntValue(sha3("invoice/paid-on", invoiceId), now);
        ContractLibrary.addTotalPaid(_storage, contractId, amount);
        UserLibrary.addToFreelancerTotalEarned(_storage, freelancerId, amount);
        UserLibrary.addToEmployerTotalPaid(_storage, employerId, amount);
        JobLibrary.addTotalPaid(_storage, jobId, amount);
    }

    function setInvoiceCancelled(address _storage, address senderAddress, uint invoiceId) {
        var senderId = UserLibrary.getUserId(_storage, senderAddress);
        var contractId = getContract(_storage, invoiceId);
        var freelancerId = ContractLibrary.getFreelancer(_storage, contractId);
        var amount = EternalStorage(_storage).getUIntValue(sha3("invoice/amount", invoiceId));
        if (freelancerId != senderId) throw;
        EternalStorage(_storage).setUInt8Value(sha3("invoice/status", invoiceId), 3);
        EternalStorage(_storage).setUIntValue(sha3("invoice/cancelled-on", invoiceId), now);
        ContractLibrary.subTotalInvoiced(_storage, contractId, amount);
    }
}