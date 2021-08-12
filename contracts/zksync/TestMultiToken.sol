// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC1155/ERC1155.sol";
import "@openzeppelin/contracts/utils/Counters.sol";

contract TestMultiToken is ERC1155("http://example.com/something.json") {
    using Counters for Counters.Counter;
    Counters.Counter private _tokenIds;

    function awardItem(address receiver, uint amount) public returns (uint256) {
        _tokenIds.increment();
        uint256 newItemId = _tokenIds.current();
        _mint(receiver, newItemId, amount, "");
        return newItemId;
    }
}
