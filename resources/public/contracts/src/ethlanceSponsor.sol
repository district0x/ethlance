pragma solidity ^0.4.8;

import "ethlanceSetter.sol";
import "ethlanceSponsorWallet.sol";
import "sponsorLibrary.sol";
import "strings.sol";
import "jobLibrary.sol";
import "userLibrary.sol";
import "sponsorRelated.sol";

contract EthlanceSponsor is EthlanceSetter, SponsorRelated {
    using strings for *;

    event onJobSponsorshipAdded(uint sponsorshipId, uint jobId, address indexed employerId, uint amount);
    event onJobSponsorshipRefunded(uint sponsorshipId, uint jobId, address indexed receiverId, uint amount);

    function EthlanceSponsor(address _ethlanceDB) {
        require(_ethlanceDB != 0x0);
        ethlanceDB = _ethlanceDB;
    }

    function addJobSponsorship(
        uint jobId,
        string name,
        string link
    )
        payable
        onlyActiveSmartContract
    {
        require(name.toSlice().len() <= getConfig("max-sponsor-name"));
        require(link.toSlice().len() <= getConfig("max-sponsor-link"));
        require(msg.value >= getConfig("min-sponsorship-amount"));
        var sponsorshipId = SponsorLibrary.addJobSponsorship(ethlanceDB, msg.sender, jobId, name, link, msg.value);
        EthlanceSponsorWallet(ethlanceSponsorWallet).receiveFunds.value(msg.value)();
        onJobSponsorshipAdded(sponsorshipId, jobId, JobLibrary.getEmployer(ethlanceDB, jobId), msg.value);
    }

    function refundJobSponsorships(uint jobId, uint limit) {
        var jobStatus = JobLibrary.getStatus(ethlanceDB, jobId);
        var sponsorshipsBalance = JobLibrary.getSponsorshipsBalance(ethlanceDB, jobId);
        var sponsorshipsTotal = JobLibrary.getSponsorshipsTotal(ethlanceDB, jobId);
        var sponsorshipsRefunded = JobLibrary.getSponsorshipsTotalRefunded(ethlanceDB, jobId);
        require(JobLibrary.getEmployer(ethlanceDB, jobId) == msg.sender);
        require(jobStatus == 1 || jobStatus == 2 || jobStatus == 5);
        require(sponsorshipsBalance > 0);
        require(sponsorshipsTotal > 0);
        var ratioSpent = SafeMath.safeMul(SafeMath.safeAdd(sponsorshipsBalance, sponsorshipsRefunded),
            1000000000000000000) / sponsorshipsTotal;
        uint i = 0;
        uint refundedCount = 0;
        var jobSponsorshipsIds = JobLibrary.getSponsorships(ethlanceDB, jobId);
        while (i < jobSponsorshipsIds.length && refundedCount < limit) {
            var sponsorshipId = jobSponsorshipsIds[i];
            if (!SponsorLibrary.isSponsorshipRefunded(ethlanceDB, sponsorshipId)) {
                var sponsor = SponsorLibrary.getSponsorshipUser(ethlanceDB, sponsorshipId);
                var proportionalAmount = SponsorLibrary.getSponsorshipProportionalAmount(ethlanceDB, sponsorshipId, ratioSpent);
                EthlanceSponsorWallet(ethlanceSponsorWallet).sendFunds(sponsor, proportionalAmount);
                SponsorLibrary.setAsRefunded(ethlanceDB, sponsorshipId, proportionalAmount);
                JobLibrary.refundSponsorship(ethlanceDB, jobId, proportionalAmount);
                UserLibrary.subTotalSponsored(ethlanceDB, sponsor, proportionalAmount);
                onJobSponsorshipRefunded(sponsorshipId, jobId, sponsor, proportionalAmount);
                refundedCount++;
            }
            i++;
        }
        if (i == jobSponsorshipsIds.length) {
            JobLibrary.setStatus(ethlanceDB, jobId, 6);
        } else if (jobStatus != 5) {
            JobLibrary.setStatus(ethlanceDB, jobId, 5);
        }
    }
}
