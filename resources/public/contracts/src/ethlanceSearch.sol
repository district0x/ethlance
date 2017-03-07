pragma solidity ^0.4.8;

import "userLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "invoiceLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";

contract EthlanceSearch {
    address public ethlanceDB;

    function EthlanceSearch(address _ethlanceDB) {
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

    function searchFreelancers(
        uint categoryId,
        uint[] skillsAnd,
        uint[] skillsOr,
        uint8 minAvgRating,
        uint minRatingsCount,
        uint[] minHourlyRates,
        uint[] maxHourlyRates,
        uint[] uintArgs // To avoid compiler stack too deep error
        /*
        uint countryId, 0
        uint stateId, 1
        uint languageId, 2
        uint jobRecommendations 3
        uint offset, 4
        uint limit, 5
        uint seed 6
        */
    )
        constant returns
    (
        uint[] userIds)
    {
        userIds = UserLibrary.searchFreelancers(ethlanceDB, categoryId, skillsAnd, skillsOr,
        minAvgRating, minRatingsCount, minHourlyRates, maxHourlyRates, uintArgs);
        if (userIds.length > 0) {
            if (uintArgs[4] > userIds.length) {
                return SharedLibrary.take(0, userIds);
            } else if (uintArgs[4] + uintArgs[5] > userIds.length) {
                uintArgs[5] = userIds.length - uintArgs[4];
            }
            userIds = SharedLibrary.getPage(userIds, (uintArgs[6] + uintArgs[4]) % userIds.length, uintArgs[5], true);
        }
        return userIds;
    }
}