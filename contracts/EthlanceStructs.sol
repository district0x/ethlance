// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155.sol";
// import "@ganache/console.log/console.sol";

library EthlanceStructs {
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

  struct Deposit {
    address depositor;
    // This (tokenValue) reflects the amount depositor has added - withdrawn
    // It excludes (doesn't get updated for) the amounts paid out as invoices
    TokenValue tokenValue;
  }

  function makeTokenValue(uint value, TokenType tokenType) public pure returns(TokenValue[] memory) {
      EthlanceStructs.TokenValue[] memory single = new EthlanceStructs.TokenValue[](1);
      single[0].value = value;
      single[0].token.tokenContract.tokenAddress = address(0);
      single[0].token.tokenContract.tokenType = tokenType;

      return single;
  }

  function min(uint a, uint b) pure internal returns(uint) {
    return a <= b ? a : b;
  }

  // Normally the contributors (job creator and those who have added funds) can withdraw all their funds
  // at any point. This is not the case when there have already been payouts and thus the funds kept in
  // this Job contract are less.
  // In such case these users will be eligible up to what they've contributed limited to what's left in Job
  //
  // This method can be used to receive array of TokenValue-s with max amounts to be used
  // for subsequent withdrawFunds call
  function maxWithdrawableAmounts(address contributor,
                                  bytes32[] memory depositIds,
                                  mapping(bytes32 => Deposit) storage deposits
                                 ) public view returns(EthlanceStructs.TokenValue[] memory) {
    EthlanceStructs.TokenValue[] memory withdrawables = new EthlanceStructs.TokenValue[](depositIds.length);
    uint withdrawablesCount = 0;
    for(uint i = 0; i < depositIds.length; i++) {
      Deposit memory deposit = deposits[depositIds[i]];
      if(deposit.depositor == contributor) {
        TokenValue memory tv = deposit.tokenValue;
        uint jobTokenBalance = tokenValueBalance(address(this), tv);
        if (jobTokenBalance == 0) { break; } // Nothing to do if 0 tokens left of the kind
        uint valueToWithdraw = min(jobTokenBalance, tv.value);
        if (valueToWithdraw == 0) { break; } // Nothing to do if could withdraw 0
        tv.value = valueToWithdraw;
        withdrawables[withdrawablesCount] = tv;
        withdrawablesCount += 1;
      }
    }

    // Return only the ones that matched contributor (can't dynamically allocate in-memory array, need to reconstruct)
    TokenValue[] memory compactWithdrawables = new TokenValue[](withdrawablesCount);
    for(uint i = 0; i < withdrawablesCount; i++) {
      compactWithdrawables[i] = withdrawables[i];
    }

    return compactWithdrawables;
  }

  function tokenValuesEqual(EthlanceStructs.TokenValue memory first, EthlanceStructs.TokenValue memory second) public pure returns (bool) {
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
    to.call{value: tokenValue.value}("");
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

  function toString(address account) public pure returns(string memory) {
      return toString(abi.encodePacked(account));
  }

  function toString(uint256 value) public pure returns(string memory) {
      return toString(abi.encodePacked(value));
  }

  function toString(bytes32 value) public pure returns(string memory) {
      return toString(abi.encodePacked(value));
  }

  function toString(bytes memory data) public pure returns(string memory) {
      bytes memory alphabet = "0123456789abcdef";

      bytes memory str = new bytes(2 + data.length * 2);
      str[0] = "0";
      str[1] = "x";
      for (uint i = 0; i < data.length; i++) {
          str[2+i*2] = alphabet[uint(uint8(data[i] >> 4))];
          str[3+i*2] = alphabet[uint(uint8(data[i] & 0x0f))];
      }
      return string(str);
  }
}
