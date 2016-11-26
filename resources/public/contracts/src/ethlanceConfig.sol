pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "ethlanceSetter.sol";
import "skillLibrary.sol";
import "sharedLibrary.sol";

contract EthlanceConfig is EthlanceSetter {

    function EthlanceConfig(address _ethlanceDB) {
        ethlanceDB = _ethlanceDB;
    }

    function setConfig(string key, uint val)
    onlyOwner {
        EthlanceDB(ethlanceDB).setUIntValue(sha3("config/", key), val);
    }

    function setConfigs(bytes32[] keys, uint[] vals)
    onlyOwner {
        for (uint i = 0; i < keys.length; i++) {
            EthlanceDB(ethlanceDB).setUIntValue(sha3("config/", SharedLibrary.bytes32ToString(keys[i])), vals[i]);
        }
    }

    function addSkillNames(
        bytes32[] names
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        SkillLibrary.addSkillNames(ethlanceDB, names);
    }

    function blockSkill(
        uint skillId
    )
        onlyOwner
    {
        SkillLibrary.blockSkill(ethlanceDB, skillId);
    }
}