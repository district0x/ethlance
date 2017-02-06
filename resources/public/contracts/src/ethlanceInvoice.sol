pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "invoiceLibrary.sol";
import "strings.sol";

contract EthlanceInvoice is EthlanceSetter {
    using strings for *;

    event onInvoiceAdded(uint invoiceId, uint indexed employerId, uint freelancerId);
    event onInvoicePaid(uint invoiceId, uint employerId, uint indexed freelancerId);
    event onInvoiceCancelled(uint invoiceId, uint indexed employerId, uint freelancerId);

    function EthlanceInvoice(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addInvoice(
        uint contractId,
        string description,
        uint amount,
        uint workedHours,
        uint workedFrom,
        uint workedTo
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        if (description.toSlice().len() > getConfig("max-invoice-description")) throw;
        var freelancerId = getSenderUserId();
        var invoiceId = InvoiceLibrary.addInvoice(ethlanceDB, freelancerId, contractId, description,
            amount, workedHours, workedFrom, workedTo);
        onInvoiceAdded(invoiceId, ContractLibrary.getEmployer(ethlanceDB, contractId), freelancerId);
    }

    function payInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveEmployer
        payable
    {
        address freelancerAddress = InvoiceLibrary.getFreelancerAddress(ethlanceDB, invoiceId);
        var employerId = getSenderUserId();
        InvoiceLibrary.setInvoicePaid(ethlanceDB, employerId, msg.value, invoiceId);
        if (!freelancerAddress.send(msg.value)) throw;
        var contractId = InvoiceLibrary.getContract(ethlanceDB, invoiceId);
        onInvoicePaid(invoiceId, employerId, ContractLibrary.getFreelancer(ethlanceDB, contractId));
    }

    function cancelInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        var freelancerId = getSenderUserId();
        InvoiceLibrary.setInvoiceCancelled(ethlanceDB, freelancerId, invoiceId);
        var contractId = InvoiceLibrary.getContract(ethlanceDB, invoiceId);
        var employerId = ContractLibrary.getEmployer(ethlanceDB, contractId);
        onInvoiceCancelled(invoiceId, employerId, freelancerId);
    }
}