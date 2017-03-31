pragma solidity ^0.4.8;

import "ethlanceDB.sol";
import "safeMath.sol";
import "sharedLibrary.sol";
import "jobLibrary.sol";

library SponsorLibrary {

    function getSponsorshipCount(address db) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("sponsorship/count"));
    }

    function getJobSponsorshipId(address db, address sponsorAddress, uint jobId) internal returns(uint){
        return EthlanceDB(db).getUIntValue(sha3("sponsorship/ids", sponsorAddress, jobId));
    }

    function addJobSponsorship(
        address db,
        address sponsorAddress,
        uint jobId,
        string name,
        string link,
        uint amount
    )
        internal returns (uint sponsorshipId)
    {
        var jobStatus = JobLibrary.getStatus(db, jobId);
        require(jobStatus == 1 || jobStatus == 2);
        require(JobLibrary.isSponsorable(db, jobId));
        sponsorshipId = getJobSponsorshipId(db, sponsorAddress, jobId);
        if (sponsorshipId == 0) {
            sponsorshipId = SharedLibrary.createNext(db, "sponsorship/count");
            EthlanceDB(db).setUIntValue(sha3("sponsorship/ids", sponsorAddress, jobId), sponsorshipId);
            EthlanceDB(db).setAddressValue(sha3("sponsorship/user", sponsorshipId), sponsorAddress);
            JobLibrary.addSponsorship(db, jobId, sponsorshipId);
            UserLibrary.addSponsorship(db, sponsorAddress, sponsorshipId);
            EthlanceDB(db).setUIntValue(sha3("sponsorship/created-on", sponsorshipId), now);
        }
        EthlanceDB(db).addUIntValue(sha3("sponsorship/amount", sponsorshipId), amount);
        EthlanceDB(db).setUIntValue(sha3("sponsorship/job", sponsorshipId), jobId);
        EthlanceDB(db).setStringValue(sha3("sponsorship/name", sponsorshipId), name);
        EthlanceDB(db).setStringValue(sha3("sponsorship/link", sponsorshipId), link);
        EthlanceDB(db).setUIntValue(sha3("sponsorship/updated-on", sponsorshipId), now);
        JobLibrary.addSponsorshipAmount(db, jobId, amount);
        UserLibrary.addTotalSponsored(db, sponsorAddress, amount);

        return sponsorshipId;
    }

    function getSponsorshipAmount(address db, uint sponsorshipId) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3("sponsorship/amount", sponsorshipId));
    }

    function getSponsorshipsAmounts(address db, uint[] sponsorshipIds) internal returns(uint[] amounts) {
        amounts = new uint[](sponsorshipIds.length);
        for (uint i = 0; i < sponsorshipIds.length ; i++) {
            amounts[i] = getSponsorshipAmount(db, sponsorshipIds[i]);
        }
        return amounts;
    }

    function getSponsorshipProportionalAmount(address db, uint sponsorshipId, uint ratioSpent) internal returns(uint) {
        var amount = getSponsorshipAmount(db, sponsorshipId);
        return SafeMath.safeMul(amount, ratioSpent) / 1000000000000000000;
    }

    function getSponsorshipUser(address db, uint sponsorshipId) internal returns(address) {
        return EthlanceDB(db).getAddressValue(sha3("sponsorship/user", sponsorshipId));
    }

    function isSponsorshipRefunded(address db, uint sponsorshipId) internal returns(bool) {
        return EthlanceDB(db).getBooleanValue(sha3("sponsorship/refunded?", sponsorshipId));
    }

    function setAsRefunded(address db, uint sponsorshipId, uint amount) internal {
        EthlanceDB(db).setBooleanValue(sha3("sponsorship/refunded?", sponsorshipId), true);
        EthlanceDB(db).setUIntValue(sha3("sponsorship/refunded-amount", sponsorshipId), amount);
    }
}