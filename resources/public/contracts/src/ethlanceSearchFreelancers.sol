pragma solidity ^0.4.8;

import "userLibrary.sol";
import "sharedLibrary.sol";

contract EthlanceSearchFreelancers {
    address public ethlanceDB;

    function EthlanceSearchFreelancers(address _ethlanceDB) {
        if(_ethlanceDB == 0x0) throw;
        ethlanceDB = _ethlanceDB;
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
        address[] userIds)
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