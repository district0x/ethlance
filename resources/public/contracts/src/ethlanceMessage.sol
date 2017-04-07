pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "contractLibrary.sol";
import "messageLibrary.sol";
import "strings.sol";

contract EthlanceMessage is EthlanceSetter {
    using strings for *;

    event onJobContractMessageAdded(uint messageId, uint contractId, address indexed receiverId, address senderId);

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
        bool isSenderFreelancer;
        address receiverId;
        (receiverId, isSenderFreelancer) = ContractLibrary.getOtherContractParticipant(ethlanceDB, contractId, msg.sender);
        var messageId = MessageLibrary.addJobContractMessage(ethlanceDB, msg.sender, receiverId, message, contractId);
        onJobContractMessageAdded(messageId, contractId, receiverId, msg.sender);
    }
}