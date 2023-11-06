// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.21;
pragma experimental ABIEncoderV2;

import "./EthlanceStructs.sol";
import "./Job.sol";
import "./external/MutableForwarder.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155Receiver.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";

/**
 * @dev Factory contract for creating {Job} smart-contracts
 * It also emits all events related to Ethlance
 * This contract is used through a proxy, therefore its address will never change.
 * No breaking changes will be introduced for events, so they all stay accessible from a single contract.
 */
contract Ethlance is IERC721Receiver, IERC1155Receiver, DSAuth {
  address public target = 0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd; // Stores address of a contract that Job proxies will be delegating to
  address public jobProxyTarget = 0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd; // Stores address of a contract that Job proxies will be delegating to
  mapping(address => bool) public isJobMap; // Stores if given address is a Job proxy contract address

  event JobCreated(
    address indexed job,
    address indexed creator,
    EthlanceStructs.TokenValue[] offeredValues,
    address[] invitedArbiters,
    bytes ipfsData,
    uint timestamp,
    uint jobVersion
  );


  event QuoteForArbitrationSet(
    address indexed job,
    address indexed arbiter,
    EthlanceStructs.TokenValue[] quote,
    uint timestamp
  );


  event QuoteForArbitrationAccepted(
    address indexed job,
    address indexed arbiter,
    uint timestamp
  );


  event CandidateAdded(
    address indexed job,
    address indexed candidate,
    bytes ipfsData,
    uint timestamp
  );

  event ArbitersInvited(
    address indexed job,
    address[] arbiters,
    uint timestamp
  );

  event InvoiceCreated(
    address indexed job,
    address indexed invoicer,
    uint invoiceId,
    EthlanceStructs.TokenValue[] invoicedValue,
    bytes ipfsData,
    uint timestamp
  );


  event InvoicePaid(
    address indexed job,
    uint indexed invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event InvoiceCancelled(
    address indexed job,
    uint indexed invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event FundsWithdrawn(
    address indexed job,
    address indexed withdrawer,
    EthlanceStructs.TokenValue[] withdrawnValues,
    uint timestamp
  );


  event DisputeRaised(
    address indexed job,
    uint indexed invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event DisputeResolved(
    address indexed job,
    uint indexed invoiceId,
    EthlanceStructs.TokenValue[] _valueForInvoicer,
    bytes ipfsData,
    uint timestamp
  );

  event JobEnded(
    address indexed job,
    uint timestamp
  );

  event FundsIn(address indexed job, EthlanceStructs.TokenValue[] funds);
  function emitFundsIn(address job, EthlanceStructs.TokenValue[] memory funds) external {
    emit FundsIn(job, funds);
  }

  event FundsOut(address indexed job, EthlanceStructs.TokenValue[] funds);
  function emitFundsOut(address job, EthlanceStructs.TokenValue[] memory funds) external {
    emit FundsOut(job, funds);
  }

  event TestEvent(string info, uint answer);
  function emitTestEvent(string calldata info) external returns(uint) {
    emit TestEvent(info, 42);
    return 42;
  }
  function emitTestEvent(uint answer) external returns(uint) {
    emit TestEvent("Basic info", answer);
    return answer + 1;
  }

  modifier isJob {
    require(isJobMap[msg.sender], "Not a job contract address");
    _;
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
    // 'this.' needed because of https://github.com/tonlabs/TON-Solidity-Compiler/issues/36
    this.setJobProxyTarget(_jobProxyTarget);
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
   */
  function createJob(
    address _creator,
    EthlanceStructs.TokenValue[] memory _offeredValues,
    address[] memory _invitedArbiters,
    bytes memory _ipfsData
  ) public payable returns(address) {
    require(jobProxyTarget != address(0), "jobProxyTarget must be set from Ethlance#initialize first");

    address newJob = address(new MutableForwarder()); // This becomes the new proxy
    address payable newJobPayableAddress = payable(address(uint160(newJob)));
    MutableForwarder(newJobPayableAddress).setTarget(jobProxyTarget);

    EthlanceStructs.transferToJob(_creator, address(this), newJobPayableAddress, _offeredValues);
    isJobMap[newJobPayableAddress] = true;
    Job(newJobPayableAddress).initialize(this, _creator, _offeredValues, _invitedArbiters);

    emit JobCreated(newJobPayableAddress, _creator, _offeredValues, _invitedArbiters, _ipfsData, timestamp(), Job(newJobPayableAddress).version());

    return newJob;
  }

  function timestamp() internal view returns(uint) {
    return block.number;
  }

  /**
   * @dev Emits {QuoteForArbitrationSet} event
   * Can only be called by {Job} contract address
   */
  function emitQuoteForArbitrationSet(
    address _job,
    address _arbiter,
    EthlanceStructs.TokenValue[] memory _quote
  ) external isJob {
    emit QuoteForArbitrationSet(_job, _arbiter, _quote, timestamp());
  }


  /**
   * @dev Emits {QuoteForArbitrationAccepted} event
   * Can only be called by {Job} contract address
   */
  function emitQuoteForArbitrationAccepted(
    address _job,
    address _arbiter
  ) external isJob {
    emit QuoteForArbitrationAccepted(_job, _arbiter, timestamp());
  }


  /**
   * @dev Emits {CandidateAdded} event
   * Can only be called by {Job} contract address
   */
  function emitCandidateAdded(
    address _job,
    address _candidate,
    bytes memory _ipfsData
  ) external isJob {
    emit CandidateAdded(_job, _candidate, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {InvoiceCreated} event
   * Can only be called by {Job} contract address
   */
  function emitInvoiceCreated(
    address _job,
    address _invoicer,
    uint _invoiceId,
    EthlanceStructs.TokenValue[] memory _invoicedValue,
    bytes memory _ipfsData
  ) external isJob {
    emit InvoiceCreated(_job, _invoicer, _invoiceId, _invoicedValue, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {InvoicePaid} event
   * Can only be called by {Job} contract address
   */
  function emitInvoicePaid(
    address _job,
    uint _invoiceId,
    bytes memory _ipfsData
  ) external isJob {
    emit Ethlance.InvoicePaid(_job, _invoiceId, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {InvoiceCanceled} event
   * Can only be called by {Job} contract address
   */
  function emitInvoiceCanceled(
    address _job,
    uint _invoiceId,
    bytes memory _ipfsData
  ) external isJob {
    emit Ethlance.InvoiceCancelled(_job, _invoiceId, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {FundsWithdrawn} event
   * Can only be called by {Job} contract address
   */
  function emitFundsWithdrawn(
    address _job,
    address _withdrawer,
    EthlanceStructs.TokenValue[] memory _withdrawnValues
  ) external isJob {
    emit FundsWithdrawn(_job, _withdrawer, _withdrawnValues, timestamp());
  }


  /**
   * @dev Emits {DisputeRaised} event
   * Can only be called by {Job} contract address
   */
  function emitDisputeRaised(
    address _job,
    uint _invoiceId,
    bytes calldata _ipfsData
  ) external isJob {
    emit DisputeRaised(_job, _invoiceId, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {DisputeResolved} event
   * Can only be called by {Job} contract address
   */
  function emitDisputeResolved(
    address _job,
    uint _invoiceId,
    EthlanceStructs.TokenValue[] memory _valueForInvoicer,
    bytes memory _ipfsData
  ) external isJob {
    emit DisputeResolved(_job, _invoiceId, _valueForInvoicer, _ipfsData, timestamp());
  }

  function emitArbitersInvited(
    address job,
    address[] calldata arbiters
  ) external isJob {
    emit ArbitersInvited(job, arbiters, timestamp());
  }


  function emitJobEnded(address job) external isJob {
    emit JobEnded(job, timestamp());
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
    address,
    address,
    uint256,
    bytes calldata _data
  ) public override returns (bytes4) {
    if(isCalledForOneStepJobCreation(_data)) { _createJobWithPassedData(_data); }
    return bytes4(keccak256("onERC721Received(address,address,uint256,bytes)"));
  }

  /**
   * @dev This function is called automatically when this contract receives ERC1155 token
   * It calls {_createJob} where:
   * - passed `_creator` is `_from`
   * - passed `_offeredValues` are constructed from the transferred token
   * - rest of arguments is obtained by decoding `_data`
   */
  function onERC1155Received(
    address,
    address,
    uint256,
    uint256,
    bytes calldata _data
  ) external override returns (bytes4) {
    if(isCalledForOneStepJobCreation(_data)) { _createJobWithPassedData(_data); }
    return bytes4(keccak256("onERC1155Received(address,address,uint256,uint256,bytes)"));
  }

  enum OperationType {
    ONE_STEP_JOB_CREATION, // Create job via ERC721/1155 callback onERC<...>Received (1 transaction)
    TWO_STEP_JOB_CREATION, // First approve tokens, then create job (2 transactions)
    ADD_FUNDS
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
    address,
    address,
    uint256[] calldata,
    uint256[] calldata,
    bytes calldata _data
  ) external override returns (bytes4) {
    // TODO: iterate over _ids & _values
    _createJobWithPassedData(_data);
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

  function supportsInterface(bytes4 interfaceId) external override pure returns (bool) {
    return interfaceId == type(IERC20).interfaceId ||
      interfaceId == type(IERC721).interfaceId ||
      interfaceId == type(IERC1155).interfaceId ||
      interfaceId == type(IERC721Receiver).interfaceId ||
      interfaceId == type(IERC1155Receiver).interfaceId;
  }

  function transferCallbackDelegate(
    OperationType operationType,
    address _creator,
    EthlanceStructs.TokenValue[] memory _offeredValues,
    address[] memory _invitedArbiters,
    bytes memory _ipfsData
  ) public payable returns(address) {
    // This method is currently used just for its signature to allow encoding arguments
    // for the ERC721 and ERC1155 data parameter using Web3 encode-abi
    // It has the same signature as createJob + operationType
    //   operationType allows to distinguish the callbacks made to the contract
    //   E.g. different job creation methods (1tx, 2tx) or adding funds (no new job creation)
  }

  function _createJobWithPassedData(bytes calldata _data) internal {
    address creator;
    EthlanceStructs.TokenValue[] memory offeredValues;
    address[] memory invitedArbiters;
    OperationType operationType;
    bytes memory ipfsData;

    (operationType, creator, offeredValues, invitedArbiters, ipfsData) = _decodeJobCreationData(_data);
    createJob(creator, offeredValues, invitedArbiters, ipfsData);
  }

  // TODO: how to optimize so that multiple calls to this method wouldn't
  //       redo the work multiple times (and thus spend gas) during one
  //       contract execution
  function _decodeJobCreationData(bytes calldata _data) internal pure returns(OperationType, address, EthlanceStructs.TokenValue[] memory, address[] memory, bytes memory) {
    return abi.decode(_data[4:], (OperationType, address, EthlanceStructs.TokenValue[], address[], bytes));
  }

  function isCalledForOneStepJobCreation(bytes calldata _data) internal pure returns(bool) {
    if (_data.length > 0) {
      address creator;
      EthlanceStructs.TokenValue[] memory offeredValues;
      address[] memory invitedArbiters;
      OperationType operationType;
      bytes memory ipfsData;

      (operationType, creator, offeredValues, invitedArbiters, ipfsData) = _decodeJobCreationData(_data);

      if(operationType == OperationType.ONE_STEP_JOB_CREATION) {
        return true;
      }
    }
    return false;
  }
}
