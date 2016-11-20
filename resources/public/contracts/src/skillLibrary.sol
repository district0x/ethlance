pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "sharedLibrary.sol";

library SkillLibrary {

    function addSkill(address _storage, bytes32 name) {
        var idx = SharedLibrary.createNext(_storage, "skill/count");
        EternalStorage(_storage).setBytes32Value(sha3("skill/name", idx), name);
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
}