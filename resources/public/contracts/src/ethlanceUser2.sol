pragma solidity ^0.4.9;

import "ethlanceSetter.sol";

contract EthlanceUser2 is EthlanceSetter {

    function EthlanceUser2(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setUserNotifications
    (
        bool[] boolNotifSettings,
        uint8[] uint8NotifSettings
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        UserLibrary.setUserNotifications(ethlanceDB, getSenderUserId(), boolNotifSettings, uint8NotifSettings);
    }

    function setUserStatus
    (
        uint userId,
        uint8 status
    )
        onlyOwner
    {
        UserLibrary.setStatus(ethlanceDB, userId, status);
    }
}