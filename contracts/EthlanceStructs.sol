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

  function tokenValuesEqual(EthlanceStructs.TokenValue memory first, EthlanceStructs.TokenValue memory second) public returns (bool) {
    bytes32 firstHash = keccak256(abi.encodePacked(first.value, first.token.tokenId, first.token.tokenContract.tokenAddress));
    bytes32 secondHash = keccak256(abi.encodePacked(second.value, second.token.tokenId, second.token.tokenContract.tokenAddress));

    return firstHash == secondHash;
  }

  function tokenValueBalance(address owner, TokenValue memory tokenValue) view public returns(uint) {
    TokenType tokenType = tokenValue.token.tokenContract.tokenType;

    if (tokenType == TokenType.ETH) {
      return address(owner).balance;
    } else if (tokenType == TokenType.ERC20) {
      IERC20 token = IERC20(tokenValue.token.tokenContract.tokenAddress);
      return token.balanceOf(owner);
    } else if (tokenType == TokenType.ERC721) {
      IERC721 token = IERC721(tokenValue.token.tokenContract.tokenAddress);
      bool isOwner = token.ownerOf(tokenValue.token.tokenId) == owner;
      return isOwner ? 1 : 0;
    } else if (tokenType == TokenType.ERC1155) {
      IERC1155 token = IERC1155(tokenValue.token.tokenContract.tokenAddress);
      return token.balanceOf(owner, tokenValue.token.tokenId);
    } else {
      revert UnsupportedTokenType(tokenType);
    }
  }

  error UnsupportedTokenType(TokenType);
  function transferTokenValue(TokenValue memory tokenValue, address from, address to) public {
    TokenType tokenType = tokenValue.token.tokenContract.tokenType;

    if (tokenType == TokenType.ETH) {
      transferETH(tokenValue, payable(to));
    } else if (tokenType == TokenType.ERC20) {
      transferERC20(tokenValue, from, to);
    } else if (tokenType == TokenType.ERC721) {
      transferERC721(tokenValue, from, to);
    } else if (tokenType == TokenType.ERC1155) {
      transferERC1155(tokenValue, from, to);
    } else {
      revert UnsupportedTokenType(tokenType);
    }
  }

  function transferETH(TokenValue memory tokenValue, address payable to) public {
    // Is the following restriction necessary? Wouldn't the tx fail anyway if there wasn't enough ETH in the contract
    // require(msg.value >= tokenValue.value, "Transaction must contain >= of ETH vs that defined in the offer");
    to.transfer(tokenValue.value); // If more was included in msg.value, the reminder stays in the calling contract
  }

  function transferERC20(TokenValue memory tokenValue, address from, address to) public {
    uint offeredAmount = tokenValue.value;
    IERC20 offeredToken = IERC20(tokenValue.token.tokenContract.tokenAddress);

    bool isAlreadyOwner = from == address(this);
    if (isAlreadyOwner) {
      require(offeredToken.balanceOf(address(this)) >= offeredAmount, "Insufficient ERC20 tokens");
      offeredToken.transfer(to, offeredAmount);
    } else {
      uint allowedToTransfer = offeredToken.allowance(from, to);
      require(allowedToTransfer >= offeredAmount, "Insufficient amount of ERC20 allowed. Approve more.");
      offeredToken.transferFrom(from, to, offeredAmount);
    }
  }

  function transferERC721(TokenValue memory tokenValue, address from, address to) public {
    uint tokenId = tokenValue.token.tokenId;
    IERC721 offeredToken = IERC721(tokenValue.token.tokenContract.tokenAddress);
    offeredToken.safeTransferFrom(from, to, tokenId);
  }

  function transferERC1155(TokenValue memory tokenValue, address from, address to) public {
    uint tokenId = tokenValue.token.tokenId;
    uint tokenAmount = tokenValue.value;
    IERC1155 offeredToken = IERC1155(tokenValue.token.tokenContract.tokenAddress);
    offeredToken.safeTransferFrom(from, to, tokenId, tokenAmount, "");
  }

  function transferToJob(address initialOwner, address ethlance, address jobProxy, TokenValue[] memory _offeredValues) public {
    for(uint i = 0; i < _offeredValues.length; i++) {
      EthlanceStructs.TokenValue memory offer = _offeredValues[i];
      TokenType tokenType = offer.token.tokenContract.tokenType;

      if (tokenType == TokenType.ETH) {
        transferETH(offer, payable(jobProxy));
      } else if (tokenType == TokenType.ERC20) {
        transferERC20(offer, initialOwner, ethlance);
        transferERC20(offer, ethlance, jobProxy);
      } else if (tokenType == TokenType.ERC721) {
        transferERC721ToJob(initialOwner, ethlance, jobProxy, offer);
      } else if (tokenType == TokenType.ERC1155) {
        transferERC1155ToJob(initialOwner, ethlance, jobProxy, offer);
      } else {
        revert UnsupportedTokenType(tokenType);
      }
    }
  }

  // Attempts to transfer tokens from initialOwner end up in job address
  // Works both when job is created through `Ethlance#createJob` and `onERC721Received`
  //
  // 1. In first case the token owner would have to approve the transfer first and
  //   the process requires 2 transactions
  // 2. In the second case, user sends the transaction with extra data to Token contract
  //   which will call back onERC721Received on Ethlance which will end up creating job and
  //   completing the process (at the end of which the tokens belong to the Job created)
  function transferERC721ToJob(address initialOwner, address ethlance, address job, TokenValue memory offer) public {
    uint tokenId = offer.token.tokenId;
    IERC721 offeredToken = IERC721(offer.token.tokenContract.tokenAddress);
    bool needToTransferToEthlanceFirst = offeredToken.ownerOf(tokenId) == initialOwner && offeredToken.getApproved(tokenId) == ethlance;

    if (needToTransferToEthlanceFirst) { transferERC721(offer, initialOwner, ethlance); }
    transferERC721(offer, ethlance, job);
  }

  function transferERC1155ToJob(address initialOwner, address ethlance, address job, TokenValue memory offer) public {
    uint tokenId = offer.token.tokenId;
    uint tokenAmount = offer.value;
    IERC1155 offeredToken = IERC1155(offer.token.tokenContract.tokenAddress);
    bool needToTransferToEthlanceFirst = offeredToken.balanceOf(initialOwner, tokenId) >= tokenAmount && offeredToken.isApprovedForAll(initialOwner, ethlance);

    if (needToTransferToEthlanceFirst) { transferERC1155(offer, initialOwner, ethlance); }
    transferERC1155(offer, ethlance, job);
  }
}
