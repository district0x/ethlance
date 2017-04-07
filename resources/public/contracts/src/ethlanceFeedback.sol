pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "contractLibrary.sol";
import "strings.sol";

contract EthlanceFeedback is EthlanceSetter {
    using strings for *;

    event onJobContractFeedbackAdded(uint contractId, address indexed receiverId, address senderId, bool isSenderFreelancer);

    function EthlanceFeedback(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function addJobContractFeedback(
        uint contractId,
        string feedback,
        uint8 rating
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        var feedbackLen = feedback.toSlice().len();
        if (feedbackLen > getConfig("max-feedback")) throw;
        if (feedbackLen < getConfig("min-feedback")) throw;
        if (rating > 100) throw;
        ContractLibrary.addFeedback(ethlanceDB, contractId, msg.sender, feedback, rating);
        bool isSenderFreelancer;
        address receiverId;
        (receiverId, isSenderFreelancer) = ContractLibrary.getOtherContractParticipant(ethlanceDB, contractId, msg.sender);
        onJobContractFeedbackAdded(contractId, receiverId, msg.sender, isSenderFreelancer);
    }
}