pragma solidity ^0.4.8;

import "Ownable.sol";

contract SponsorRelated is Ownable {
    address public ethlanceSponsorWallet;

    function setEthlanceSponsorWalletContract(address _ethlanceSponsorWallet)
    onlyOwner {
        require(_ethlanceSponsorWallet != 0x0);
        ethlanceSponsorWallet = _ethlanceSponsorWallet;
    }
}
