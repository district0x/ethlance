pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "sharedLibrary.sol";

library SkillLibrary {

    function addSkill(address _storage, bytes32 name) {
        var skillId = SharedLibrary.createNext(_storage, "skill/count");
        EternalStorage(_storage).setBytes32Value(sha3("skill/name", skillId), name);
    }

    function addJob(address _storage, uint skillId, uint jobId) {
        SharedLibrary.addArrayItem(_storage, skillId, "skill/jobs", "skill/jobs-count", jobId);
    }

    function getJobs(address _storage, uint skillId) internal returns (uint[]) {
        return SharedLibrary.getUIntArray(_storage, skillId, "skill/jobs", "skill/jobs-count");
    }

    function addFreelancer(address _storage, uint[] skills, uint userId) {
        SharedLibrary.addRemovableArrayItem(_storage, skills, "skill/freelancers", "skill/freelancers-count",
            "skill/freelancers-keys", userId);
    }
    
    function getFreelancers(address _storage, uint skillId) internal returns (uint[]){
        return SharedLibrary.getRemovableArrayItems(_storage, skillId, "skill/freelancers", "skill/freelancers-count",
            "skill/freelancers-keys");
    }

    function removeFreelancer(address _storage, uint[] skills, uint userId) {
        SharedLibrary.removeArrayItem(_storage, skills, "skill/freelancers", userId);
    }
    
    function getNames(address _storage) internal returns (uint[] skillIds, bytes32[] names){
        var count = EternalStorage(_storage).getUIntValue(sha3("skill/count"));
        for (uint i = 1; i <= count ; i++) {
            skillIds[i - 1] = i;
            names[i - 1] = EternalStorage(_storage).getBytes32Value(sha3("skill/name", i));
        }
        return (skillIds, names);
    }
}