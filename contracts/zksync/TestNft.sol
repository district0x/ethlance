// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import "@openzeppelin/contracts/utils/Counters.sol";

contract TestNft is ERC721("TestNft", "TNT") {
    using Counters for Counters.Counter;
    Counters.Counter private _tokenIds;

    function getCurrent() public view returns (uint256) {
        return _tokenIds.current();
    }

    function awardItem(address receiver) public returns (uint256) {
        _tokenIds.increment();
        uint256 newItemId = _tokenIds.current();
        _mint(receiver, newItemId);
        return newItemId;
    }
}
