pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "ethlanceSetter.sol";
import "skillLibrary.sol";
import "sharedLibrary.sol";

contract EthlanceConfig is EthlanceSetter {

    event onSkillsAdded(uint[] skillIds, bytes32[] names);
    event onSkillBlocked(uint skillId);

    function EthlanceConfig(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setConfigs(bytes32[] keys, uint[] vals)
    onlyOwner {
        for (uint i = 0; i < keys.length; i++) {
            EthlanceDB(ethlanceDB).setUIntValue(sha3("config/", keys[i]), vals[i]);
        }
    }

    function getConfigs(bytes32[] keys) constant returns (bytes32[], uint[] values) {
        for (uint i = 0; i < keys.length; i++) {
            values[i] = EthlanceDB(ethlanceDB).getUIntValue(sha3("config/", keys[i]));
        }
        return (keys, values);
    }

    function addSkills(
        bytes32[] names
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (msg.sender != owner) {
            if (0 == getConfig("adding-skills-enabled?")) throw;
            if (names.length > getConfig("max-skills-create-at-once")) throw;
        }
        var skillIds = SkillLibrary.addSkillNames(ethlanceDB, names, getSenderUserId());
        onSkillsAdded(skillIds, names);
    }

    function blockSkills(
        uint[] skillIds
    )
        onlyOwner
    {
        SkillLibrary.blockSkills(ethlanceDB, skillIds);
    }
}
