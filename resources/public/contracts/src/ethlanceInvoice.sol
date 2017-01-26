pragma solidity ^0.4.4;

import "ethlanceSetter.sol";
import "invoiceLibrary.sol";

contract EthlanceInvoice is EthlanceSetter {

    event onInvoiceAdded(uint invoiceId, uint indexed employerId);
    event onInvoicePaid(uint invoiceId, uint indexed freelancerId);
    event onInvoiceCancelled(uint invoiceId, uint indexed employerId);

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
        if (bytes(description).length > getConfig("max-invoice-description")) throw;
        var invoiceId = InvoiceLibrary.addInvoice(ethlanceDB, getSenderUserId(), contractId, description,
            amount, workedHours, workedFrom, workedTo);
        onInvoiceAdded(invoiceId, ContractLibrary.getEmployer(ethlanceDB, contractId));
    }

    function payInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveEmployer
        payable
    {
        address freelancerAddress = InvoiceLibrary.getFreelancerAddress(ethlanceDB, invoiceId);
        InvoiceLibrary.setInvoicePaid(ethlanceDB, getSenderUserId(), msg.value, invoiceId);
        if (!freelancerAddress.send(msg.value)) throw;
        var contractId = InvoiceLibrary.getContract(ethlanceDB, invoiceId);
        onInvoicePaid(invoiceId, ContractLibrary.getFreelancer(ethlanceDB, contractId));
    }

    function cancelInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        InvoiceLibrary.setInvoiceCancelled(ethlanceDB, getSenderUserId(), invoiceId);
        var contractId = InvoiceLibrary.getContract(ethlanceDB, invoiceId);
        var employerId = ContractLibrary.getEmployer(ethlanceDB, contractId);
        onInvoiceCancelled(invoiceId, employerId);
    }
}