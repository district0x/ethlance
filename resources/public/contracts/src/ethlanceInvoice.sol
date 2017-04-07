pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "invoiceLibrary.sol";
import "strings.sol";
import "sponsorRelated.sol";
import "ethlanceSponsorWallet.sol";

contract EthlanceInvoice is EthlanceSetter, SponsorRelated {
    using strings for *;

    event onInvoiceAdded(uint invoiceId, address indexed employerId, address freelancerId);
    event onInvoicePaid(uint invoiceId, address employerId, address indexed freelancerId);
    event onInvoiceCancelled(uint invoiceId, address indexed employerId, address freelancerId);

    function EthlanceInvoice(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addInvoice(
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
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        if (description.toSlice().len() > getConfig("max-invoice-description")) throw;
        if (uintArgs[3] > 59) throw;
        var invoiceId = InvoiceLibrary.addInvoice(ethlanceDB, msg.sender, contractId, description, uintArgs);
        onInvoiceAdded(invoiceId, ContractLibrary.getEmployer(ethlanceDB, contractId), msg.sender);
    }

    function payInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        payable
    {
        var freelancerId = InvoiceLibrary.getFreelancer(ethlanceDB, invoiceId);
        require(freelancerId != 0x0);
        uint amount;
        bool payFromSponsorship;
        (amount, payFromSponsorship) = InvoiceLibrary.setInvoicePaid(ethlanceDB, msg.sender, msg.value, invoiceId);
        if (payFromSponsorship) {
            EthlanceSponsorWallet(ethlanceSponsorWallet).sendFunds(freelancerId, amount);
        } else {
            freelancerId.transfer(msg.value);
        }

        onInvoicePaid(invoiceId, msg.sender, freelancerId);
    }

    function cancelInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        InvoiceLibrary.setInvoiceCancelled(ethlanceDB, msg.sender, invoiceId);
        var contractId = InvoiceLibrary.getContract(ethlanceDB, invoiceId);
        var employerId = ContractLibrary.getEmployer(ethlanceDB, contractId);
        onInvoiceCancelled(invoiceId, employerId, msg.sender);
    }
}