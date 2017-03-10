pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "sharedLibrary.sol";
import "contractLibrary.sol";
import "userLibrary.sol";

library MessageLibrary {

    function addMessage(
        address db,
        uint senderId,
        uint receiverId,
        string text
    )
        internal returns (uint messageId)
    {
        if (senderId == receiverId) throw;
        if (receiverId == 0) throw;
        if (senderId == 0) throw;

        messageId = SharedLibrary.createNext(db, "message/count");

        EthlanceDB(db).setStringValue(sha3("message/text", messageId), text);
        EthlanceDB(db).setUIntValue(sha3("message/created-on", messageId), now);
        EthlanceDB(db).setUIntValue(sha3("message/receiver", messageId), receiverId);
        EthlanceDB(db).setUIntValue(sha3("message/sender", messageId), senderId);
        UserLibrary.addReceivedMessage(db, receiverId, messageId);
        UserLibrary.addSentMessage(db, senderId, messageId);

        return messageId;
    }

    function addJobContractMessage(
        address db,
        uint senderId,
        uint receiverId,
        string text,
        uint contractId
    )
        internal returns (uint messageId)
    {
        var status = ContractLibrary.getStatus(db, contractId);
        if (status == 4 || status == 5) throw;
        messageId = addMessage(db, senderId, receiverId, text);
        EthlanceDB(db).setUIntValue(sha3("message/contract", messageId), contractId);
        EthlanceDB(db).setUInt8Value(sha3("message/contract-status", messageId), status);
        ContractLibrary.addMessage(db, contractId, messageId);
        return messageId;
    }
}