pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "contractLibrary.sol";
import "messageLibrary.sol";
import "strings.sol";

contract EthlanceMessage is EthlanceSetter {
    using strings for *;

    event onJobContractMessageAdded(uint messageId, uint contractId, uint indexed receiverId, uint senderId);

    function EthlanceMessage(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addJobContractMessage(
        uint contractId,
        string message
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (message.toSlice().len() > getConfig("max-message-length")) throw;
        var senderId = getSenderUserId();
        bool isSenderFreelancer;
        uint receiverId;
        (receiverId, isSenderFreelancer) = ContractLibrary.getOtherContractParticipant(ethlanceDB, contractId, senderId);
        var messageId = MessageLibrary.addJobContractMessage(ethlanceDB, senderId, receiverId, message, contractId);
        onJobContractMessageAdded(messageId, contractId, receiverId, senderId);
    }
}