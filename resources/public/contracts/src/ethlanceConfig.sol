pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "ethlanceSetter.sol";
import "skillLibrary.sol";
import "sharedLibrary.sol";

contract EthlanceConfig is EthlanceSetter {

    event onSkillsAdded(uint[] skillIds);
    event onSkillsBlocked(uint[] skillIds);
    event onSkillNameSet(uint indexed skillId, bytes32 name);
    event onConfigsChanged(bytes32[] keys);

    function EthlanceConfig(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function setConfigs(bytes32[] keys, uint[] vals)
    onlyOwner {
        for (uint i = 0; i < keys.length; i++) {
            EthlanceDB(ethlanceDB).setUIntValue(sha3("config/", keys[i]), vals[i]);
        }
        onConfigsChanged(keys);
    }

    function getConfigs(bytes32[] keys) constant returns (uint[] values) {
        values = new uint[](keys.length);
        for (uint i = 0; i < keys.length; i++) {
            values[i] = EthlanceDB(ethlanceDB).getUIntValue(sha3("config/", keys[i]));
        }
        return values;
    }

    function addSkills(
        bytes32[] names
    )
        onlyActiveSmartContract
    {
        if (msg.sender != owner) {
            if (0 == getConfig("adding-skills-enabled?")) throw;
            if (names.length > getConfig("max-skills-create-at-once")) throw;
        }
        var skillIds = SkillLibrary.addSkillNames(ethlanceDB, names, msg.sender);
        onSkillsAdded(skillIds);
    }

    function blockSkills(
        uint[] skillIds
    )
        onlyOwner
    {
        SkillLibrary.blockSkills(ethlanceDB, skillIds);
        onSkillsBlocked(skillIds);
    }

    function setSkillName(
        uint skillId,
        bytes32 name
    )
        onlyOwner
    {
        SkillLibrary.setSkillName(ethlanceDB, skillId, name);
        onSkillNameSet(skillId, name);
    }
}
