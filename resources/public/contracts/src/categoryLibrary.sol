pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "sharedLibrary.sol";


library CategoryLibrary {

    function addJob(address db, uint categoryId, uint jobId) internal {
        SharedLibrary.addIdArrayItem(db, categoryId, "category/jobs", "category/jobs-count", jobId);
    }

    function getJobs(address db, uint categoryId) internal returns (uint[]) {
        return SharedLibrary.getIdArray(db, categoryId, "category/jobs", "category/jobs-count");
    }

    function addFreelancer(address db, uint[] categories, address userId) internal {
        SharedLibrary.addRemovableIdArrayItem(db, categories, "category/freelancers", "category/freelancers-count",
            "category/freelancers-keys", userId);
    }

    function getFreelancers(address db, uint categoryId) internal returns (address[]) {
        return SharedLibrary.getRemovableIdArrayAddressItems(db, categoryId, "category/freelancers",
            "category/freelancers-count", "category/freelancers-keys");
    }

    function removeFreelancer(address db, uint[] categories, address userId) internal {
        SharedLibrary.removeIdArrayItem(db, categories, "category/freelancers", userId);
    }
}