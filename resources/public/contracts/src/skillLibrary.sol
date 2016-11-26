pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "sharedLibrary.sol";

library SkillLibrary {

    function addSkillName(address db, bytes32 name) internal {
        var skillId = SharedLibrary.createNext(db, "skill/count");
        EthlanceDB(db).setBytes32Value(sha3("skill/name", skillId), name);
    }

    function addSkillNames(address db, bytes32[] names) internal {
        for (uint i = 0; i < names.length ; i++) {
            addSkillName(db, names[i]);
        }
    }

    function addJob(address db, uint skillId, uint jobId) internal {
        SharedLibrary.addArrayItem(db, skillId, "skill/jobs", "skill/jobs-count", jobId);
    }

    function getJobs(address db, uint skillId) internal returns (uint[]) {
        return SharedLibrary.getUIntArray(db, skillId, "skill/jobs", "skill/jobs-count");
    }

    function addFreelancer(address db, uint[] skills, uint userId) internal {
        SharedLibrary.addRemovableArrayItem(db, skills, "skill/freelancers", "skill/freelancers-count",
            "skill/freelancers-keys", userId);
    }
    
    function getFreelancers(address db, uint skillId) internal returns (uint[]){
        return SharedLibrary.getRemovableArrayItems(db, skillId, "skill/freelancers", "skill/freelancers-count",
            "skill/freelancers-keys");
    }

    function removeFreelancer(address db, uint[] skills, uint userId) internal {
        SharedLibrary.removeArrayItem(db, skills, "skill/freelancers", userId);
    }

    function blockSkill(address db, uint skillId) internal {
        EthlanceDB(db).setBooleanValue(sha3("skill/blocked?", skillId), true);
    }
    
    function getNames(address db) internal returns (uint[] skillIds, bytes32[] names){
        var count = EthlanceDB(db).getUIntValue(sha3("skill/count"));
        for (uint i = 1; i <= count ; i++) {
            if (!EthlanceDB(db).getBooleanValue(sha3("skill/blocked", i))) {
                skillIds[i - 1] = i;
                names[i - 1] = EthlanceDB(db).getBytes32Value(sha3("skill/name", i));
            }
        }
        return (skillIds, names);
    }
}