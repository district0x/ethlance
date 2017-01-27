pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "sharedLibrary.sol";


library CategoryLibrary {

    function addCategory(address db, bytes32 name) internal {
        var idx = SharedLibrary.createNext(db, "category/count");
        EthlanceDB(db).setBytes32Value(sha3("category/name", idx), name);
    }

    function addJob(address db, uint categoryId, uint jobId) internal {
        SharedLibrary.addArrayItem(db, categoryId, "category/jobs", "category/jobs-count", jobId);
    }

    function getJobs(address db, uint categoryId) internal returns (uint[]) {
        return SharedLibrary.getUIntArray(db, categoryId, "category/jobs", "category/jobs-count");
    }

    function addFreelancer(address db, uint[] categories, uint userId) internal {
        SharedLibrary.addRemovableArrayItem(db, categories, "category/freelancers", "category/freelancers-count",
            "category/freelancers-keys", userId);
    }

    function getFreelancers(address db, uint categoryId) internal returns (uint[]) {
        return SharedLibrary.getRemovableArrayItems(db, categoryId, "category/freelancers", "category/freelancers-count",
            "category/freelancers-keys");
    }

    function removeFreelancer(address db, uint[] categories, uint userId) internal {
        SharedLibrary.removeArrayItem(db, categories, "category/freelancers", userId);
    }
}