pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "sharedLibrary.sol";
import "contractLibrary.sol";
import "userLibrary.sol";

library MessageLibrary {

    function addMessage(
        address db,
        address senderId,
        address receiverId,
        string text
    )
        internal returns (uint messageId)
    {
        require(senderId != receiverId);
        require(receiverId != 0x0);
        require(senderId != 0x0);

        messageId = SharedLibrary.createNext(db, "message/count");

        EthlanceDB(db).setStringValue(sha3("message/text", messageId), text);
        EthlanceDB(db).setUIntValue(sha3("message/created-on", messageId), now);
        EthlanceDB(db).setAddressValue(sha3("message/receiver", messageId), receiverId);
        EthlanceDB(db).setAddressValue(sha3("message/sender", messageId), senderId);
        UserLibrary.addReceivedMessage(db, receiverId, messageId);
        UserLibrary.addSentMessage(db, senderId, messageId);

        return messageId;
    }

    function addJobContractMessage(
        address db,
        address senderId,
        address receiverId,
        string text,
        uint contractId
    )
        internal returns (uint messageId)
    {
        var status = ContractLibrary.getStatus(db, contractId);
        require(status != 4 && status != 5);
        messageId = addMessage(db, senderId, receiverId, text);
        EthlanceDB(db).setUIntValue(sha3("message/contract", messageId), contractId);
        EthlanceDB(db).setUInt8Value(sha3("message/contract-status", messageId), status);
        ContractLibrary.addMessage(db, contractId, messageId);
        return messageId;
    }
}