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

  error UnsupportedTokenType();

  event DescNumb(string desc, uint numb);

  function transferToJob(address initialOwner, address ethlance, address jobProxy, TokenValue[] memory _offeredValues) internal {
    for(uint i = 0; i < _offeredValues.length; i++) {
      EthlanceStructs.TokenValue memory offer = _offeredValues[i];

      if (offer.token.tokenContract.tokenType == TokenType.ETH) {
        transferETH(jobProxy, offer);
      } else if (offer.token.tokenContract.tokenType == TokenType.ERC20) {
        transferERC20(initialOwner, ethlance, jobProxy, offer);
      } else if (offer.token.tokenContract.tokenType == TokenType.ERC721) {
        transferERC721(initialOwner, ethlance, jobProxy, offer);
      } else if (offer.token.tokenContract.tokenType == TokenType.ERC1155) {
        transferERC1155(initialOwner, ethlance, jobProxy, offer);
      } else {
        revert UnsupportedTokenType();
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
    require(offeredToken.balanceOf(ethlance) > 0, "Ethlance must have received the token");
    offeredToken.transfer(job, offeredAmount);
  }

  function transferERC721(address initialOwner, address ethlance, address job, TokenValue memory offer) internal {
    uint tokenId = offer.token.tokenId;
    IERC721 offeredToken = IERC721(offer.token.tokenContract.tokenAddress);
    require(offeredToken.ownerOf(tokenId) == ethlance, "Ethlance must have received the token");
    offeredToken.safeTransferFrom(ethlance, job, tokenId);
  }

  function transferERC1155(address initialOwner, address ethlance, address job, TokenValue memory offer) internal {
    uint tokenId = offer.token.tokenId;
    uint offeredAmount = offer.value;
    IERC1155 offeredToken = IERC1155(offer.token.tokenContract.tokenAddress);
    offeredToken.safeTransferFrom(ethlance, job, tokenId, offeredAmount, "");
  }
}
