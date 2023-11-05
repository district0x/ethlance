// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;

import "./EthlanceStructs.sol";
import "./JobHelpers.sol";
import "./Ethlance.sol";
import "./JobStorage.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155Receiver.sol";
import "@openzeppelin/contracts/utils/structs/EnumerableSet.sol";

contract JobStorage {
  address public target; // Stores address of a contract that Job proxies will be delegating to
  uint public constant version = 1; // current version of {Job} smart-contract
  uint public constant ARBITER_IDLE_TIMEOUT = 30 days;
  uint public constant FIRST_INVOICE_INDEX = 1;
  Ethlance public ethlance; // Stores address of {Ethlance} smart-contract so it can emit events there

  address public creator;

  // The bytes32 being keccak256(abi.encodePacked(depositorAddress, TokenType, contractAddress, tokenId))
  mapping(bytes32 => EthlanceStructs.Deposit) deposits;
  bytes32[] public depositIds; // To allow looking up and listing all deposits
  EnumerableSet.AddressSet depositors;

  mapping(address => EthlanceStructs.TokenValue) public arbiterQuotes;
  using EnumerableSet for EnumerableSet.AddressSet;
  EnumerableSet.AddressSet invitedArbiters;
  EnumerableSet.AddressSet invitedCandidates;
  address public acceptedArbiter;

  mapping (uint => JobHelpers.Dispute) public disputes; // invoiceId => dispute
  uint[] disputeIds;

  struct Invoice {
    EthlanceStructs.TokenValue item;
    address payable issuer;
    uint invoiceId;
    bool paid;
    bool cancelled;
  }
  mapping (uint => Invoice) public invoices;
  uint[] public invoiceIds;
  uint public lastInvoiceIndex;
  bool jobEnded;

  function addArbiter(address arbiter) public {
    invitedArbiters.add(arbiter);
  }

  function addCandidate(address candidate) public {
    invitedCandidates.add(candidate);
  }

  function addDepositor(address depositor) public {
    depositors.add(depositor);
  }

  function containsCandidate(address candidate) public view returns(bool) {
    return invitedCandidates.contains(candidate);
  }

  function getDepositor(uint index) public view returns(address) {
    return depositors.at(index);
  }

  function depositorsLength() public view returns(uint) {
    return depositors.length();
  }

  function isAmongstInvitedArbiters(address account) public view returns (bool) {
    return invitedArbiters.contains(account);
  }
}
