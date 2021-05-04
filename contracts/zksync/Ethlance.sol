// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "./Job.sol";
import "../token/ApproveAndCallFallback.sol";
import "../proxy/MutableForwarder.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155Receiver.sol";
import "../DSAuth.sol";


/**
 * @dev Factory contract for creating {Job} smart-contracts
 * It also emits all events related to Ethlance
 * This contract is used through a proxy, therefore its address will never change.
 * No breaking changes will be introduced for events, so they all stay accessible from a single contract.
 */

contract Ethlance is ApproveAndCallFallBack, IERC721Receiver, IERC1155Receiver, DSAuth {

  address public jobProxyTarget; // Stores address of a contract that Job proxies will be delegating to
  mapping(address => bool) public isJob; // Stores if given address is a Job proxy contract address

  event JobCreated(
    address job,
    uint jobVersion,
    JobType jobType,
    address creator,
    TokenValue[] offeredValues,
    address[] invitedArbiters,
    bytes ipfsData,
    uint timestamp
  );


  event QuoteForArbitrationSet(
    address job,
    address arbiter,
    TokenValue[] quote,
    uint timestamp
  );


  event QuoteForArbitrationAccepted(
    address job,
    address arbiter,
    uint timestamp
  );


  event CandidateAdded(
    address job,
    address candidate,
    bytes ipfsData,
    uint timestamp
  );


  event InvoiceCreated(
    address job,
    address invoicer,
    uint invoiceId,
    TokenValue[] invoicedValue,
    bytes ipfsData,
    uint timestamp
  );


  event InvoicePaid(
    uint invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event InvoiceCanceled(
    uint invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event FundsAdded(
    address job,
    address funder,
    TokenValue[] fundedValue,
    uint timestamp
  );


  event FundsWithdrawn(
    address job,
    address withdrawer,
    TokenValue[] withdrawnValues,
    uint timestamp
  );


  event DisputeRaised(
    address job,
    uint invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event DisputeResolved(
    uint invoiceId,
    TokenValue[] _valueForInvoicer,
    bytes ipfsData,
    uint timestamp
  );


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


  modifier isJob {
    require(isJob[msg.sender], "Not a job contract address");
    _;
  }


  /**
   * @dev Contract initialization
   * It is manually called instead of native contructor,
   * because this contract is used through a proxy.
   * This function cannot be called twice.
   *
   * It stores address of a contract that Job proxies will be delegating to
   */
  function initialize(
    address _jobProxyTarget
  ) external {
    require(_jobProxyTarget != address(0));
    setJobProxyTarget(_jobProxyTarget);
  }


  /**
   * @dev Creates a new {Job}
   *
   * It creates a new job in following steps:
   * 1. Creates new {MutableForwarder} forwarding to an offer contract based on `_offerType`.
   * 2. Transfers `_offeredValues` from this contract into newly created contract
   * 3. Calls `initialize` on the newly created contract
   *
   * Owner of the proxy is this contract. Created proxy is not meant to be updated.
   * This function is not meant to be called directly, but via token received callbacks
   *
   * Requirements:
   *
   * - `_creator` cannot be zero address
   * - `_offeredValues` cannot be empty
   * - `_ipfsData` cannot be empty
   *
   * Emits an {JobCreated} event
   *
   * See spec :ethlance/job-created for the format of _ipfsData file
   * TODO: Add validation and step 2
   */
  function _createJob(
    address _creator,
    TokenValue[] memory _offeredValues,
    JobType _jobType,
    address[] _invitedArbiters,
    bytes memory _ipfsData
  ) internal {
    address newJob = address(new MutableForwarder());
    MutableForwarder(newJob).setTarget(jobProxyTarget);
    Job(newJob).initialize(this, _creator, _jobType, _offeredValues, _invitedArbiters);
    emit JobCreated(newJob, Job(newJob).version(), _jobType, _creator, _offeredValues, _invitedArbiters, _ipfsData);
  }


  /**
   * @dev Sets a new address where job proxies will be delegating to
   *
   * Requirements:
   *
   * - Only authorized address can call this function
   * - `_newJobProxyTarget` cannot be empty
   */
  function setJobProxyTarget(
    address _newJobProxyTarget
  ) external auth {
    require(_newJobProxyTarget != address(0));
    jobProxyTarget = _newJobProxyTarget;
  }


  /**
   * @dev Emits {QuoteForArbitrationSet} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitQuoteForArbitrationSet(
    address _job,
    address _arbiter,
    TokenValue[] memory _quote
  ) external isJob {
  }


  /**
   * @dev Emits {QuoteForArbitrationAccepted} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitQuoteForArbitrationAccepted(
    address _job,
    address _arbiter
  ) external isJob {
  }


  /**
   * @dev Emits {CandidateAdded} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitCandidateAdded(
    address _job,
    address _candidate,
    bytes memory _ipfsData
  ) external isJob {
  }


  /**
   * @dev Emits {InvoiceCreated} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitInvoiceCreated(
    address _job,
    address _invoicer,
    uint _invoiceId,
    TokenValue[] memory _invoicedValue,
    bytes memory _ipfsData
  ) external isJob {
  }


  /**
   * @dev Emits {InvoicePaid} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitInvoicePaid(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external isJob {
  }


  /**
   * @dev Emits {InvoiceCanceled} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitInvoiceCanceled(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external isJob {
  }


  /**
   * @dev Emits {FundsAdded} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitFundsAdded(
    address _job,
    address _funder,
    TokenValue[] memory _fundedValue
  ) external isJob {
  }


  /**
   * @dev Emits {FundsWithdrawn} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitFundsWithdrawn(
    address _job,
    address _withdrawer,
    TokenValue[] memory _withdrawnValues
  ) external isJob {
  }


  /**
   * @dev Emits {DisputeRaised} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitDisputeRaised(
    address _job,
    uint _invoiceId,
    bytes _ipfsData
  ) external isJob {
  }


  /**
   * @dev Emits {DisputeResolved} event
   * Can only be called by {Job} contract address
   * TODO: Needs implementation
   */
  function emitDisputeResolved(
    uint _invoiceId,
    TokenValue[] memory _valueForInvoicer,
    bytes memory _ipfsData
  ) external isJob {
  }


  /**
   * @dev This function is called automatically when this contract receives approval for ERC20 MiniMe token
   * It calls {_createJob} where:
   * - passed `_creator` is `_from`
   * - passed `_offeredValues` are constructed from the transferred token
   * - rest of arguments is obtained by decoding `_data`
   * TODO: Needs implementation
   */
  function receiveApproval(
    address _from,
    uint256 _amount,
    address _token,
    bytes memory _data
  ) external override {
  }


  /**
   * @dev This function is called automatically when this contract receives ERC721 token
   * It calls {_createJob} where:
   * - passed `_creator` is `_from`
   * - passed `_offeredValues` are constructed from the transferred token
   * - rest of arguments is obtained by decoding `_data`
   * TODO: Needs implementation
   */
  function onERC721Received(
    address _operator,
    address _from,
    uint256 _tokenId,
    bytes memory _data
  ) public override returns (bytes4) {
    return bytes4(keccak256("onERC721Received(address,address,uint256,bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives ERC1155 token
   * It calls {_createJob} where:
   * - passed `_creator` is `_from`
   * - passed `_offeredValues` are constructed from the transferred token
   * - rest of arguments is obtained by decoding `_data`
   * TODO: Needs implementation
   */
  function onERC1155Received(
    address _operator,
    address _from,
    uint256 _id,
    uint256 _value,
    bytes calldata _data
  ) external override returns (bytes4) {
    return bytes4(keccak256("onERC1155Received(address,address,uint256,uint256,bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives multiple ERC1155 tokens
   * It calls {_createJob} where:
   * - passed `_creator` is `_from`
   * - passed `_offeredValues` are constructed from the transferred token
   * - rest of arguments is obtained by decoding `_data`
   * TODO: Needs implementation
   */
  function onERC1155BatchReceived(
    address _operator,
    address _from,
    uint256[] calldata _ids,
    uint256[] calldata _values,
    bytes calldata _data
  ) external override returns (bytes4) {
    return bytes4(keccak256("onERC1155BatchReceived(address,address,uint256[],uint256[],bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives ETH
   * It calls {_createJob} where:
   * - passed `_offerer` is `msg.sender`
   * - passed `_offeredValues` are constructed from `msg.value`
   * - rest of arguments is obtained by decoding `msg.data`
   * TODO: Needs implementation
   */
  receive(
  ) external payable {
  }


}