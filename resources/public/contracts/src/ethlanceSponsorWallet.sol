pragma solidity ^0.4.8;

import "mortal.sol";
import "ethlanceInvoice.sol";
import "ethlanceDB.sol";
import "ethlanceSponsor.sol";

contract EthlanceSponsorWallet is Mortal {

    address ethlanceInvoice;
    address ethlanceSponsor;

    function EthlanceSponsorWallet() {
    }

    function setEthlanceInvoiceContract(address _ethlanceInvoice)
    onlyOwner {
        require(_ethlanceInvoice != 0x0);
        ethlanceInvoice = _ethlanceInvoice;
    }

    function setEthlanceSponsorContract(address _ethlanceSponsor)
    onlyOwner {
        require(_ethlanceSponsor != 0x0);
        ethlanceSponsor = _ethlanceSponsor;
    }

    function receiveFunds() payable {
        require(msg.sender == ethlanceSponsor);
    }

    function sendFunds(address receiver, uint amount) {
        require(msg.sender == ethlanceSponsor || msg.sender == ethlanceInvoice);
        receiver.transfer(amount);
    }
}
