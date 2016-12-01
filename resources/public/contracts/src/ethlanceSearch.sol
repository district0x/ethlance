pragma solidity ^0.4.4;

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
        uint[] skills,
        uint8[] paymentTypes,
        uint8[] experienceLevels,
        uint8[] estimatedDurations,
        uint8[] hoursPerWeeks,
        uint[] uintArgs
    )
        constant public returns (uint[] jobIds)
    {
        uint8[][4] memory uint8Filters; // To avoid compiler stack too deep error
        uint8Filters[0] = paymentTypes;
        uint8Filters[1] = experienceLevels;
        uint8Filters[2] = estimatedDurations;
        uint8Filters[3] = hoursPerWeeks;
        jobIds = JobLibrary.searchJobs(ethlanceDB, categoryId, skills, uint8Filters, uintArgs);
        return  SharedLibrary.getPage(jobIds, uintArgs[5], uintArgs[6]);
    }

    function searchFreelancers(
        uint categoryId,
        uint[] skills,
        uint8 minAvgRating,
        uint minContractsCount,
        uint minHourlyRate,
        uint maxHourlyRate,
        uint countryId,
        uint languageId,
        uint offset,
        uint limit
    )
        constant returns
    (
        uint[] userIds)
    {
        userIds = UserLibrary.searchFreelancers(ethlanceDB, categoryId, skills, minAvgRating, minContractsCount,
            minHourlyRate, maxHourlyRate, countryId, languageId);
        userIds = SharedLibrary.getPage(userIds, offset, limit);

        return userIds;
    }
}