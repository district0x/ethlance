pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "sharedLibrary.sol";

library CategoryLibrary {

    function addCategory(address _storage, bytes32 name) {
        var idx = SharedLibrary.createNext(_storage, "category/count");
        EternalStorage(_storage).setBytes32Value(sha3("category/name", idx), name);
    }

    function addJob(address _storage, uint categoryId, uint jobId) {
        SharedLibrary.addArrayItem(_storage, categoryId, "category/jobs", "category/jobs-count", jobId);
    }

    function getJobs(address _storage, uint categoryId) internal returns (uint[]) {
        return SharedLibrary.getUIntArray(_storage, categoryId, "category/jobs", "category/jobs-count");
    }

    function addFreelancer(address _storage, uint[] categories, uint userId) {
        SharedLibrary.addRemovableArrayItem(_storage, categories, "category/freelancers", "category/freelancers-count",
            "category/freelancers-keys", userId);
    }

    function getFreelancers(address _storage, uint categoryId) internal returns (uint[]) {
        return SharedLibrary.getRemovableArrayItems(_storage, categoryId, "category/freelancers", "category/freelancers-count",
            "category/freelancers-keys");
    }

    function removeFreelancer(address _storage, uint[] categories, uint userId) {
        SharedLibrary.removeArrayItem(_storage, categories, "category/freelancers", userId);
    }
}