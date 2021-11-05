// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "./EthlanceStructs.sol";
import "./Job.sol";
import "./external/ApproveAndCallFallback.sol";
import "./external/MutableForwarder.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155Receiver.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "./external/ds-auth/auth.sol";


/**
 * @dev Factory contract for creating {Job} smart-contracts
 * It also emits all events related to Ethlance
 * This contract is used through a proxy, therefore its address will never change.
 * No breaking changes will be introduced for events, so they all stay accessible from a single contract.
 */

contract Ethlance is ApproveAndCallFallBack, IERC721Receiver, IERC1155Receiver, DSAuth {

  address public jobProxyTarget; // Stores address of a contract that Job proxies will be delegating to
  mapping(address => bool) public isJobMap; // Stores if given address is a Job proxy contract address

  event JobCreated(
    address job,
    uint jobVersion,
    EthlanceStructs.JobType jobType,
    address creator,
    EthlanceStructs.TokenValue[] offeredValues,
    address[] invitedArbiters,
    bytes ipfsData,
    uint timestamp
  );


  event QuoteForArbitrationSet(
    address job,
    address arbiter,
    EthlanceStructs.TokenValue[] quote,
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
    EthlanceStructs.TokenValue[] invoicedValue,
    bytes ipfsData,
    uint timestamp
  );


  event InvoicePaid(
    uint invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event InvoiceCancelled(
    uint invoiceId,
    bytes ipfsData,
    uint timestamp
  );


  event FundsAdded(
    address job,
    address funder,
    EthlanceStructs.TokenValue[] fundedValue,
    uint timestamp
  );


  event FundsWithdrawn(
    address job,
    address withdrawer,
    EthlanceStructs.TokenValue[] withdrawnValues,
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
    EthlanceStructs.TokenValue[] _valueForInvoicer,
    bytes ipfsData,
    uint timestamp
  );


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
    EthlanceStructs.JobType _jobType,
    address[] memory _invitedArbiters,
    bytes memory _ipfsData
  ) public payable returns(address) {
    require(jobProxyTarget != address(0), "jobProxyTarget must be set from Ethlance#initialize first");

    address newJob = address(new MutableForwarder()); // This becomes the new proxy
    address payable newJobPayableAddress = payable(address(uint160(newJob)));
    MutableForwarder(newJobPayableAddress).setTarget(jobProxyTarget);

    EthlanceStructs.transferToJob(_creator, address(this), newJobPayableAddress, _offeredValues);
    isJobMap[newJobPayableAddress] = true;
    Job(newJobPayableAddress).initialize(this, _creator, _jobType, _offeredValues, _invitedArbiters);

    emit JobCreated(newJobPayableAddress, Job(newJobPayableAddress).version(), _jobType, _creator, _offeredValues, _invitedArbiters, _ipfsData, timestamp());

    return newJob;
  }

  function timestamp() internal returns(uint) {
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


  // FIXME:
  //   Exact copy of CandidateAdded event. For some reason
  //   in Clojure contract-event-in-tx doesn't find the event signature for :CandidateAdded
  //   Works perfectly fine with the changed name.
  // TODO: Remove this (CandidateAgregado) and all references to it
  event CandidatoAgregado(
    address job,
    address candidate,
    bytes ipfsData,
    uint timestamp
  );
  /**
   * @dev Emits {CandidateAdded} event
   * Can only be called by {Job} contract address
   */
  function emitCandidateAdded(
    address _job,
    address _candidate,
    bytes memory _ipfsData
  ) external isJob {
    emit CandidatoAgregado(_job, _candidate, _ipfsData, timestamp());
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
    uint _invoiceId,
    bytes memory _ipfsData
  ) external isJob {
    emit Ethlance.InvoicePaid(_invoiceId, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {InvoiceCanceled} event
   * Can only be called by {Job} contract address
   */
  function emitInvoiceCanceled(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external isJob {
    emit Ethlance.InvoiceCancelled(_invoiceId, _ipfsData, timestamp());
  }


  /**
   * @dev Emits {FundsAdded} event
   * Can only be called by {Job} contract address
   */
  function emitFundsAdded(
    address _job,
    address _funder,
    EthlanceStructs.TokenValue[] memory _fundedValue
  ) external isJob {
    emit FundsAdded(_job, _funder, _fundedValue, timestamp());
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
    uint _invoiceId,
    EthlanceStructs.TokenValue[] memory _valueForInvoicer,
    bytes memory _ipfsData
  ) external isJob {
    emit DisputeResolved(_invoiceId, _valueForInvoicer, _ipfsData, timestamp());
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
    address _operator,
    address _from,
    uint256 _id,
    uint256 _value,
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
    address _operator,
    address _from,
    uint256[] calldata _ids,
    uint256[] calldata _values,
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

  function supportsInterface(bytes4 interfaceId) external override view returns (bool) {
    // return interfaceId == type(IERC20).interfaceId ||
    //   interfaceId == type(IERC721).interfaceId ||
    //   interfaceId == type(IERC1155).interfaceId ||
    //   interfaceId == type(IERC721Receiver).interfaceId ||
    //   interfaceId == type(IERC1155Receiver).interfaceId;
    return true;
  }

  function transferCallbackDelegate(
    OperationType operationType,
    address _creator,
    EthlanceStructs.TokenValue[] memory _offeredValues,
    EthlanceStructs.JobType _jobType,
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
    EthlanceStructs.JobType jobType;
    address[] memory invitedArbiters;
    OperationType operationType;
    bytes memory ipfsData;

    (operationType, creator, offeredValues, jobType, invitedArbiters, ipfsData) = _decodeJobCreationData(_data);
    createJob(creator, offeredValues, jobType, invitedArbiters, ipfsData);
  }

  // TODO: how to optimize so that multiple calls to this method wouldn't
  //       redo the work multiple times (and thus spend gas) during one
  //       contract execution
  function _decodeJobCreationData(bytes calldata _data) internal returns(OperationType, address, EthlanceStructs.TokenValue[] memory, EthlanceStructs.JobType, address[] memory, bytes memory) {
    return abi.decode(_data[4:], (OperationType, address, EthlanceStructs.TokenValue[], EthlanceStructs.JobType, address[], bytes));
  }

  function isCalledForOneStepJobCreation(bytes calldata _data) internal returns(bool) {
    if (_data.length > 0) {
      address creator;
      EthlanceStructs.TokenValue[] memory offeredValues;
      EthlanceStructs.JobType jobType;
      address[] memory invitedArbiters;
      OperationType operationType;
      bytes memory ipfsData;

      (operationType, creator, offeredValues, jobType, invitedArbiters, ipfsData) = _decodeJobCreationData(_data);

      if(operationType == OperationType.ONE_STEP_JOB_CREATION) {
        return true;
      }
    }
    return false;
  }
}
