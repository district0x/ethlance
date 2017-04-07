pragma solidity ^0.4.8;

import "jobLibrary.sol";
import "sharedLibrary.sol";

contract EthlanceSearchJobs {
    address public ethlanceDB;

    function EthlanceSearchJobs(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
    }

    function searchJobs(
        uint categoryId,
        uint[] skillsAnd,
        uint[] skillsOr,
        uint8[] paymentTypes,
        uint8[] experienceLevels,
        uint8[] estimatedDurations,
        uint8[] hoursPerWeeks,
        uint[] minBudgets,
        uint[] uintArgs
        /*
        :search/min-employer-avg-rating 0
        :search/min-employer-ratings-count 1
        :search/country 2
        :search/state 3
        :search/language 4
        :search/min-created-on 5
        :search/offset 6
        :search/limit 7
        */
    )
        constant public returns (uint[] jobIds)
    {
        uint8[][4] memory uint8Filters; // To avoid compiler stack too deep error
        uint8Filters[0] = paymentTypes;
        uint8Filters[1] = experienceLevels;
        uint8Filters[2] = estimatedDurations;
        uint8Filters[3] = hoursPerWeeks;
        jobIds = JobLibrary.searchJobs(ethlanceDB, categoryId, skillsAnd, skillsOr, uint8Filters, minBudgets, uintArgs);
        jobIds = SharedLibrary.findTopNValues(jobIds, uintArgs[6] + uintArgs[7]);
        return SharedLibrary.getPage(jobIds, uintArgs[6], uintArgs[7], false);
    }
}