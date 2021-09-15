// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155.sol";

library EthlanceStructs {
  enum JobType {
    GIG,
    BOUNTY
  }

  enum TokenType {
    ETH,
    ERC20,
    ERC721,
    ERC1155
  }

  struct TokenContract {
    TokenType tokenType;
    address tokenAddress;
  }

  struct Token {
    TokenContract tokenContract;
    uint tokenId;
  }

  struct TokenValue {
    Token token;
    uint value;
  }

  error UnsupportedTokenType(TokenType);

  function transferToJob(address initialOwner, address ethlance, address jobProxy, TokenValue[] memory _offeredValues) internal {
    for(uint i = 0; i < _offeredValues.length; i++) {
      EthlanceStructs.TokenValue memory offer = _offeredValues[i];
      TokenType tokenType = offer.token.tokenContract.tokenType;

      if (tokenType == TokenType.ETH) {
        transferETH(jobProxy, offer);
      } else if (tokenType == TokenType.ERC20) {
        transferERC20(initialOwner, ethlance, jobProxy, offer);
      } else if (tokenType == TokenType.ERC721) {
        transferERC721(initialOwner, ethlance, jobProxy, offer);
      } else if (tokenType == TokenType.ERC1155) {
        transferERC1155(initialOwner, ethlance, jobProxy, offer);
      } else {
        revert UnsupportedTokenType(tokenType);
      }
    }
  }

  function transferETH(address job, TokenValue memory offer) internal {
    address payable jobPayable = payable(address(uint160(job)));
    require(msg.value >= offer.value, "Transaction must contain >= of ETH vs that defined in the offer");
    jobPayable.transfer(offer.value); // If more was included in msg.value, the reminder stays in Ethlance contract
  }

  function transferERC20(address initialOwner, address ethlance, address job, TokenValue memory offer) internal {
    uint offeredAmount = offer.value;
    IERC20 offeredToken = IERC20(offer.token.tokenContract.tokenAddress);
    uint allowedToTransfer = offeredToken.allowance(initialOwner, ethlance);
    require(allowedToTransfer >= offeredAmount, "Offer must equal to deposit");
    offeredToken.transferFrom(initialOwner, ethlance, offeredAmount);
    require(offeredToken.balanceOf(ethlance) > 0, "Ethlance must own the ERC20 token");
    offeredToken.transfer(job, offeredAmount);
  }

  // Attempts to transfer tokens from initialOwner end up in job address
  // Works both when job is created through `Ethlance#createJob` and `onERC721Received`
  //
  // 1. In first case the token owner would have to approve the transfer first and
  //   the process requires 2 transactions
  // 2. In the second case, user sends the transaction with extra data to Token contract
  //   which will call back onERC721Received on Ethlance which will end up creating job and
  //   completing the process (at the end of which the tokens belong to the Job created)
  event ZeDebug(string desc, uint num, address addr);
  function emitZeDebugDesc(string memory desc) internal { emit ZeDebug(desc, 0, address(this)); }
  function emitZeDebugDescNum(string memory desc, uint num) internal { emit ZeDebug(desc, num, address(this)); }
  function emitZeDebugDescNumAddr(string memory desc, uint num, address addr) internal { emit ZeDebug(desc, num, addr); }

  function transferERC721(address initialOwner, address ethlance, address job, TokenValue memory offer) internal {
    uint tokenId = offer.token.tokenId;
    IERC721 offeredToken = IERC721(offer.token.tokenContract.tokenAddress);

    if(offeredToken.ownerOf(tokenId) == initialOwner && offeredToken.getApproved(tokenId) == ethlance) {
      // Token still belongs to the original owner but they have approved it to be transferred to Ethlance
      offeredToken.safeTransferFrom(initialOwner, ethlance, tokenId);
    }
    require(offeredToken.ownerOf(tokenId) == ethlance, "Ethlance must own the ERC721 token");
    offeredToken.safeTransferFrom(ethlance, job, tokenId);
  }

  // TODO: just for debugging, remove!
  function toAsciiString(address x) internal view returns (string memory) {
      bytes memory s = new bytes(40);
      for (uint i = 0; i < 20; i++) {
          bytes1 b = bytes1(uint8(uint(uint160(x)) / (2**(8*(19 - i)))));
          bytes1 hi = bytes1(uint8(b) / 16);
          bytes1 lo = bytes1(uint8(b) - 16 * uint8(hi));
          s[2*i] = char(hi);
          s[2*i+1] = char(lo);
      }
      return string(s);
  }

  // TODO: just for debugging, remove!
  function char(bytes1 b) internal view returns (bytes1 c) {
      if (uint8(b) < 10) return bytes1(uint8(b) + 0x30);
      else return bytes1(uint8(b) + 0x57);
  }

  function transferERC1155(address initialOwner, address ethlance, address job, TokenValue memory offer) internal {
    uint tokenId = offer.token.tokenId;
    uint tokenAmount = offer.value;
    IERC1155 offeredToken = IERC1155(offer.token.tokenContract.tokenAddress);

    if(offeredToken.balanceOf(initialOwner, tokenId) >= tokenAmount && offeredToken.isApprovedForAll(initialOwner, ethlance)) {
      // Token still belongs to the original owner but they have approved it to be transferred to Ethlance
      offeredToken.safeTransferFrom(initialOwner, ethlance, tokenId, tokenAmount, "");
    }
    require(offeredToken.balanceOf(ethlance, tokenId) >= tokenAmount, "Ethlance must own offered amount of ERC1155 token");
    offeredToken.safeTransferFrom(ethlance, job, tokenId, tokenAmount, "");
  }
}
