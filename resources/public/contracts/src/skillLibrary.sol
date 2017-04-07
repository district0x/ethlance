pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "sharedLibrary.sol";

library SkillLibrary {

    function getSkillCount(address db) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("skill/count"));
    }

    function addSkillName(address db, bytes32 name, address userId) internal returns(uint) {
        var skillId = SharedLibrary.createNext(db, "skill/count");
        EthlanceDB(db).setBytes32Value(sha3("skill/name", skillId), name);
        EthlanceDB(db).setAddressValue(sha3("skill/creator", skillId), userId);
        EthlanceDB(db).setUIntValue(sha3("skill/created-on", skillId), now);
        EthlanceDB(db).setUIntValue(sha3("skill/name->id", name), skillId);
        return skillId;
    }

    function addSkillNames(address db, bytes32[] names, address userId) internal returns(uint[] skillIds) {
        skillIds = new uint[](names.length);
        uint j;
        for (uint i = 0; i < names.length ; i++) {
            if (EthlanceDB(db).getUIntValue(sha3("skill/name->id", names[i])) == 0) {
                skillIds[j] = addSkillName(db, names[i], userId);
                j++;
            }
        }
        return SharedLibrary.take(j, skillIds);
    }

    function addJob(address db, uint skillId, uint jobId) internal {
        SharedLibrary.addIdArrayItem(db, skillId, "skill/jobs", "skill/jobs-count", jobId);
    }

    function getJobs(address db, uint skillId) internal returns (uint[]) {
        return SharedLibrary.getIdArray(db, skillId, "skill/jobs", "skill/jobs-count");
    }

    function addFreelancer(address db, uint[] skills, address userId) internal {
        SharedLibrary.addRemovableIdArrayItem(db, skills, "skill/freelancers", "skill/freelancers-count",
            "skill/freelancers-keys", userId);
    }
    
    function getFreelancers(address db, uint skillId) internal returns (address[]){
        return SharedLibrary.getRemovableIdArrayAddressItems(db, skillId, "skill/freelancers", "skill/freelancers-count",
            "skill/freelancers-keys");
    }

    function removeFreelancer(address db, uint[] skills, address userId) internal {
        SharedLibrary.removeIdArrayItem(db, skills, "skill/freelancers", userId);
    }

    function blockSkills(address db, uint[] skillIds) internal {
        for (uint i = 0; i < skillIds.length ; i++) {
            EthlanceDB(db).setBooleanValue(sha3("skill/blocked?", skillIds[i]), true);
        }
    }
    
    function getNames(address db, uint offset, uint limit) internal returns (uint[] skillIds, bytes32[] names){
        var count = getSkillCount(db);
        skillIds = new uint[](limit);
        names = new bytes32[](limit);
        uint j;
        var last = offset + limit;
        if (last > count) {
            last = count;
        }
        for (uint i = offset + 1; i <= last; i++) {
            if (!EthlanceDB(db).getBooleanValue(sha3("skill/blocked?", i))) {
                skillIds[j] = i;
                names[j] = EthlanceDB(db).getBytes32Value(sha3("skill/name", i));
                j++;
            }
        }
        return (SharedLibrary.take(j, skillIds), SharedLibrary.take(j, names));
    }

    function setSkillName(address db, uint skillId, bytes32 name) internal {
        EthlanceDB(db).setBytes32Value(sha3("skill/name", skillId), name);
        EthlanceDB(db).setUIntValue(sha3("skill/updated-on", skillId), now);
    }
}