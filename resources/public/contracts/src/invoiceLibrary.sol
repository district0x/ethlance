pragma solidity ^0.4.8;

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
        uint[] uintArgs
//        uint rate,
//        uint exchangeRate,
//        uint workedHours,
//        uint workedMinutes,
//        uint workedFrom,
//        uint workedTo
    )
        internal returns (uint invoiceId)
    {
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        var employerId = ContractLibrary.getEmployer(db, contractId);
        var jobId = ContractLibrary.getJob(db, contractId);
        var jobStatus = JobLibrary.getStatus(db, jobId);
        require(freelancerId == senderId);
        require(ContractLibrary.getStatus(db, contractId) == 3);
        require(jobStatus == 1 || jobStatus == 2);
        var paymentType = JobLibrary.getPaymentType(db, jobId);
        uint amount = SafeMath.safeMul(uintArgs[0], 1000000000000000000) / uintArgs[1];
        if (paymentType == 1) {
            var hoursWei = SafeMath.safeMul(uintArgs[2], 1000000000000000000);
            var minutesWei = SafeMath.safeMul(uintArgs[3], 1000000000000000000);
            amount = SafeMath.safeMul(amount, SafeMath.safeAdd(hoursWei, minutesWei / 60)) / 1000000000000000000;
        }

        invoiceId = SharedLibrary.createNext(db, "invoice/count");
        EthlanceDB(db).setUIntValue(sha3("invoice/contract", invoiceId), contractId);
        EthlanceDB(db).setStringValue(sha3("invoice/description", invoiceId), description);
        EthlanceDB(db).setUIntValue(sha3("invoice/rate", invoiceId), uintArgs[0]);
        EthlanceDB(db).setUIntValue(sha3("invoice/conversion-rate", invoiceId), uintArgs[1]);
        EthlanceDB(db).setUIntValue(sha3("invoice/amount", invoiceId), amount);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-hours", invoiceId), uintArgs[2]);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-minutes", invoiceId), uintArgs[3]);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-from", invoiceId), uintArgs[4]);
        EthlanceDB(db).setUIntValue(sha3("invoice/worked-to", invoiceId), uintArgs[5]);
        EthlanceDB(db).setUIntValue(sha3("invoice/created-on", invoiceId), now);
        EthlanceDB(db).setUInt8Value(sha3("invoice/status", invoiceId), 1);
        ContractLibrary.addInvoice(db, contractId, invoiceId, amount);
        UserLibrary.addFreelancerTotalInvoiced(db, freelancerId, amount);
        UserLibrary.addEmployerTotalInvoiced(db, employerId, amount);

        return invoiceId;
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

    function setInvoicePaid(address db, uint senderId, address senderAddress, uint sentAmount, uint invoiceId
    )
        internal returns(uint amount, bool payFromSponsorship)
    {
        amount = EthlanceDB(db).getUIntValue(sha3("invoice/amount", invoiceId));
        var contractId = getContract(db, invoiceId);
        var employerId = ContractLibrary.getEmployer(db, contractId);
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        var jobId = ContractLibrary.getJob(db, contractId);
        var jobStatus = JobLibrary.getStatus(db, jobId);
        var isSponsorable = JobLibrary.isSponsorable(db, jobId);
        payFromSponsorship = false;

        require(getStatus(db, invoiceId) == 1);
        require(UserLibrary.hasStatus(db, freelancerId, 1));
        require(jobStatus == 1 || jobStatus == 2);

        if (sentAmount != amount) {
            require(isSponsorable);
            require(JobLibrary.isAllowedUser(db, jobId, senderAddress));
            EthlanceDB(db).setAddressValue(sha3("invoice/paid-by", invoiceId), senderAddress);
            JobLibrary.subJobSponsorshipsBalance(db, jobId, amount);
            payFromSponsorship = true;
        } else {
            require(employerId == senderId);
        }

        EthlanceDB(db).setUInt8Value(sha3("invoice/status", invoiceId), 2);
        EthlanceDB(db).setUIntValue(sha3("invoice/paid-on", invoiceId), now);
        ContractLibrary.addTotalPaid(db, contractId, amount);
        UserLibrary.addToFreelancerTotalEarned(db, freelancerId, amount);
        UserLibrary.addToEmployerTotalPaid(db, employerId, amount);
        UserLibrary.subEmployerTotalInvoiced(db, employerId, amount);
        JobLibrary.addTotalPaid(db, jobId, amount);

        return (amount, payFromSponsorship);
    }

    function setInvoiceCancelled(address db, uint senderId, uint invoiceId) internal {
        var contractId = getContract(db, invoiceId);
        var freelancerId = ContractLibrary.getFreelancer(db, contractId);
        var employerId = ContractLibrary.getEmployer(db, contractId);
        var amount = EthlanceDB(db).getUIntValue(sha3("invoice/amount", invoiceId));

        require(freelancerId == senderId);
        require(getStatus(db, invoiceId) == 1);

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