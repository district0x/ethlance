// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "./EthlanceStructs.sol";
import "./Ethlance.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";
import "@openzeppelin/contracts/token/ERC1155/IERC1155Receiver.sol";
import "@openzeppelin/contracts/utils/structs/EnumerableSet.sol";


/**
 * @dev Job contract on Ethlance
 * Job facilitates value transfers between job creator, one or multiple candidates
 * and optionally an arbiter.
 * Every new Job contract is created as a proxy contract.
 */

contract Job is IERC721Receiver, IERC1155Receiver {
  uint public constant version = 1; // current version of {Job} smart-contract
  Ethlance public ethlance; // Stores address of {Ethlance} smart-contract so it can emit events there

  address public creator;
  EthlanceStructs.JobType public jobType;

  // The bytes32 being keccak256(abi.encodePacked(depositorAddress, TokenType, contractAddress, tokenId))
  mapping(bytes32 => Deposit) deposits;
  struct Deposit {
    address depositor;
    // This (tokenValue) reflects the amount depositor has added - withdrawn
    // It excludes (doesn't get updated for) the amounts paid out as invoices
    EthlanceStructs.TokenValue tokenValue;
  }
  bytes32[] depositIds; // To allow looking up and listing all deposits

  mapping(address => EthlanceStructs.TokenValue) public arbiterQuotes;
  using EnumerableSet for EnumerableSet.AddressSet;
  EnumerableSet.AddressSet internal invitedArbiters;
  EnumerableSet.AddressSet internal invitedCandidates;
  address acceptedArbiter;

  struct Dispute {
    uint invoiceId;
    address creator;
    EthlanceStructs.TokenValue resolution;
    bool resolved;
  }
  mapping (uint => Dispute) public disputes;
  uint[] disputeIds;

  struct Invoice {
    EthlanceStructs.TokenValue item;
    address payable issuer;
    uint invoiceId;
    bool paid;
    bool cancelled;
  }
  mapping (uint => Invoice) public invoices;
  mapping (address => uint[]) public candidateInvoiceIds;
  uint lastInvoiceIndex;

  /**
   * @dev Contract initialization
   * It is manually called instead of native contructor,
   * because this contract is used through a proxy.
   * This function cannot be called twice.
   *
   * It stores passed arguments for later usage
   * It validates if `_offeredValues` have been actually transferred into this contract
   *
   * Requirements:
   *
   * - `_ethlance` cannot be empty
   * - `_creator` cannot be empty
   * - `_offeredValues` cannot be empty
   */
  function initialize(
    Ethlance _ethlance,
    address _creator,
    EthlanceStructs.JobType _jobType,
    EthlanceStructs.TokenValue[] calldata _offeredValues,
    address[] calldata _invitedArbiters
  ) external {
    require(address(ethlance) == address(0), "Contract already initialized. Can only be done once");
    require(address(_ethlance) != address(0), "Ethlance can't be null");
    require(_creator != address(0), "Creator can't be null");
    require(_offeredValues.length > 0, "You must offer some tokens as pay");

    ethlance = _ethlance;
    creator = _creator;
    jobType = _jobType;
    for(uint i = 0; i < _invitedArbiters.length; i++) { invitedArbiters.add(_invitedArbiters[i]); }

    _recordAddedFunds(creator, _offeredValues);
  }

  /**
   * @dev Sets quote for arbitration requested by arbiter for his services
   *
   * It stores passed arguments for later usage
   *
   * Requirements:
   * - `msg.sender` must be among invited arbiters
   * - `_quote` cannot be empty
   *
   * Emits {QuoteForArbitrationSet} event
   */
  function setQuoteForArbitration(
    EthlanceStructs.TokenValue[] memory _quote
  ) external {
    // Currently allowing & requiring single TokenValue, leaving the interface
    // backwards-compatible in case me support more in the future.
    require(isAmongstInvitedArbiters(msg.sender), "Quotes can only be set by invited arbiters");
    require(_quote.length == 1, "Exactly 1 quote is required");
    arbiterQuotes[msg.sender] = _quote[0];
    ethlance.emitQuoteForArbitrationSet(address(this), msg.sender, _quote);
  }

  /**
   * @dev It is called by job creator when he decides to accept an quote from an arbiter
   * It checks if `_transferredValue` matches the quote requested by an arbiter
   * It transfers the value to the arbiter's address
   *
   * This function is not meant to be called directly, but via token received callbacks
   *
   * Requirements:
   * - Can only be called by job creator
   * - `_arbiter` must be among arbiters who already set their quotes
   * - `_transferredValue` must match quote requested by an arbiter
   * - Only 1 arbiter can be accepted. Further accepts should revert.
   *
   * Emits {QuoteForArbitrationAccepted} event
   */
   // Employer sends Tx to Job contract with the necessary tokens included
   //  - or sends Tx to his ERC721/1155 contract, which then calls Job via onERC721Received/onERC1155Received
   // This function gets called via the ERC20/721/1155 callbacks
   // If the amounts are correct, the tokens get immediately forwarded to the Arbiter
  function acceptQuoteForArbitration(
    address _arbiter,
    EthlanceStructs.TokenValue[] memory _transferredValue
  ) public {
    require(acceptedArbiter == address(0) || acceptedArbiter == _arbiter, "Another arbiter had been accepted before. Only 1 can be accepted.");
    require(isAmongstInvitedArbiters(_arbiter));
    require(isCallerJobCreator(msg.sender));
    require(_transferredValue.length == 1, "Currently only 1 _transferredValue is supported at a time");
    require(EthlanceStructs.tokenValuesEqual(_transferredValue[0], arbiterQuotes[_arbiter]), "Accepted TokenValue must match exactly the value quoted by arbiter");

    acceptedArbiter = _arbiter;
    EthlanceStructs.transferTokenValue(_transferredValue[0], address(this), _arbiter);
    ethlance.emitQuoteForArbitrationAccepted(address(this), _arbiter);
  }


  /**
   * @dev It is called by job creator when he allows a new candidate to start invoicing for this job
   *
   * Requirements:
   * - Can only be called by job creator
   * - Can be called only when {EthlanceStructs.JobType} is GIG
   * - `_candidate` cannot be empty
   * - same `_candidate` cannot be added twice
   *
   * Emits {CandidateAdded} event
   */
  function addCandidate(
    address _candidate,
    bytes memory _ipfsData
  ) external {
    require(msg.sender == creator, "Only job creator can add candidates");
    require(invitedCandidates.contains(_candidate) == false, "Candidate already added. Can't add duplicates");
    require(jobType == EthlanceStructs.JobType.GIG, "Can only add candidates to GIG job type");
    invitedCandidates.add(_candidate);
    ethlance.emitCandidateAdded(address(this), address(_candidate), _ipfsData);
  }


  /**
   * @dev Function called by candidate to create an invoice to be paid
   *
   * Requirements:
   * - If {EthlanceStructs.JobType} is GIG, `msg.sender` must be among added candidates
   * - If {EthlanceStructs.JobType} is BOUNTY, anybody can call this function
   * - `_invoicedValue` cannot be empty
   * - `_ipfsData` cannot be empty
   *
   * Emits {InvoiceCreated} event
   * See spec :ethlance/invoice-created for the format of _ipfsData file
   */
  // mapping (address => (mapping (uint => EthlanceStructs.TokenValue[]))) public requestedInvoices;
  // invoiceId can be incremental id
  // (candidate => (invoiceId => invoicedValues[]))
  // FIXME: because I couldn't figure out how to store array of TokenValue-s in the mapping
  //        single token-value will be saved and separate invoice for each token value be created
  function createInvoice(
    EthlanceStructs.TokenValue[] memory _invoicedValue,
    bytes memory _ipfsData
  ) external {
    if (jobType == EthlanceStructs.JobType.GIG) {
      require(invitedCandidates.contains(msg.sender), "Sender must be amongst invitedCandidates to raise an invoice for GIG job type");
    }

    for(uint i = 0; i < _invoicedValue.length; i++) {
      Invoice memory newInvoice = Invoice(_invoicedValue[i], payable(msg.sender), lastInvoiceIndex, false, false);
      invoices[lastInvoiceIndex] = newInvoice;
      candidateInvoiceIds[msg.sender].push(lastInvoiceIndex);

      // FIXME: Is there a better way to emit array of TokenValue-s?
      EthlanceStructs.TokenValue[] memory single = new EthlanceStructs.TokenValue[](1);
      single[0] = _invoicedValue[0];
      ethlance.emitInvoiceCreated(address(this), address(msg.sender), lastInvoiceIndex, single, _ipfsData);
      lastInvoiceIndex += 1;
    }
  }

  function getInvoice(uint _invoiceId) external view returns(Invoice memory) {
    return invoices[_invoiceId];
  }

  /**
   * @dev Transfers invoiced value from this contract to the invoicer's address
   *
   * Requirements:
   * - Can be called only by job creator
   * - `_invoiceId` must be valid invoiceId
   * - `_ipfsData` can be empty
   *
   * Emits {InvoicePaid} event
   * See spec :ethlance/invoice-paid for the format of _ipfsData file
   */
  function payInvoice(
    uint _invoiceId,
    bytes memory _ipfsData
  ) public {
    require(msg.sender == creator);
    Invoice memory invoice = invoices[_invoiceId];
    require(invoice.paid == false);

    EthlanceStructs.transferTokenValue(invoice.item, address(this), invoice.issuer);

    invoice.paid = true;
    invoices[_invoiceId] = invoice;
    ethlance.emitInvoicePaid(_invoiceId, _ipfsData);
  }


  /**
   * @dev Cancels existing invoice
   *
   * Requirements:
   * - Can be called only by invoicer
   * - `_invoiceId` must be valid invoiceId and still not paid
   * - `_ipfsData` can be empty
   *
   * Emits {InvoiceCanceled} event
   * See spec :ethlance/invoice-canceled for the format of _ipfsData file
   */
  function cancelInvoice(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external {
    Invoice storage invoice = invoices[_invoiceId];
    require(invoice.issuer == msg.sender, "Invoice can only be cancelled by its issuer");
    require(invoice.cancelled != true, "The invoice was already cancelled");
    require(invoice.paid != true, "The invoice was already paid and so can't be cancelled");
    invoice.cancelled = true;
    ethlance.emitInvoiceCanceled(_invoiceId, _ipfsData);
  }


  /**
   * @dev Adds funds to the job smart-contract
   * Funds can be added by anyone and smart-contract keeps track of which address funded how much, so later
   * funds can be withdrawn by their original owner if desired
   *
   * This function is not meant to be called directly, but via token received callbacks
   *
   * Requirements:
   * - `_funder` cannot be empty
   * - `_fundedValue` cannot be empty
   *
   * Emits {FundsAdded} event
   * See spec :ethlance/funds-added for the format of _ipfsData file
   */
  function _recordAddedFunds(
    address _funder,
    EthlanceStructs.TokenValue[] memory _offeredValues
  ) internal {
    for(uint i = 0; i < _offeredValues.length; i++) {
      EthlanceStructs.TokenValue memory tv = _offeredValues[i];
      bytes32 depositId = _generateDepositId(_funder, tv);
      Deposit storage earlierDeposit = deposits[depositId];
      if (earlierDeposit.depositor == address(0)) {
        // No earlier deposit of that token from the depositor
        Deposit memory deposit = Deposit(_funder, tv);
        deposits[depositId] = deposit;
        depositIds.push(depositId);
      } else {
        // There was a deposit before of that token from the depositor
        // Record added funds
        earlierDeposit.tokenValue.value += tv.value;
      }
    }
    ethlance.emitFundsAdded(address(this), _funder, _offeredValues);
  }

  // For adding ETH and ERC20 funds. ERC721 and 1155 get added via callbacks (onERC<...>Received)
  function addFunds(EthlanceStructs.TokenValue[] memory _tokenValues) public payable {
    require(_tokenValues.length == 1, "Currently only single TokenValue can be added");
    EthlanceStructs.TokenValue memory tokenValue = _tokenValues[0];
    EthlanceStructs.transferTokenValue(tokenValue, msg.sender, address(this));
    _recordAddedFunds(msg.sender, _tokenValues);
  }

  function _generateDepositId(address depositor, EthlanceStructs.TokenValue memory tokenValue) internal returns(bytes32) {
    return keccak256(abi.encodePacked(
      depositor,
      tokenValue.token.tokenContract.tokenType,
      tokenValue.token.tokenContract.tokenAddress,
      tokenValue.token.tokenId));
  }

  function getDepositsCount() public view returns(uint) {
    return depositIds.length;
  }

  function getDepositIds() public view returns(bytes32[] memory) {
    return depositIds;
  }

  function getDeposits(address depositor) public view returns (EthlanceStructs.TokenValue[] memory) {
    EthlanceStructs.TokenValue[] memory selectedValues = new EthlanceStructs.TokenValue[](depositIds.length);
    uint lastFilled = 0;
    for(uint i = 0; i < depositIds.length; i++) {
      bytes32 depositId = depositIds[i];
      Deposit memory currentDeposit = deposits[depositId];
      if(currentDeposit.depositor == depositor) {
        selectedValues[lastFilled] = currentDeposit.tokenValue;
        lastFilled += 1;
      }
    }

    EthlanceStructs.TokenValue[] memory compactedValues = new EthlanceStructs.TokenValue[](lastFilled);
    for(uint i = 0; i < lastFilled; i++) {
      if(selectedValues[i].value != 0) {
        compactedValues[i] = selectedValues[i];
      }
    }
    return compactedValues;
  }

  /**
   * @dev It joins together `{addFunds}` and `{payInvoice}` calls
   *
   * The primary use is for ERC20 token transfers (as the 721 and 1155 will work though callbacks)
   */
  function addFundsAndPayInvoice(
    EthlanceStructs.TokenValue[] memory _tokenValues,
    uint _invoiceId,
    bytes memory _ipfsData
  ) public {
    addFunds(_tokenValues);
    payInvoice(_invoiceId, _ipfsData);
  }

  /**
   * @dev Withdraws funds back to the original funder
   *
   * Requirements:
   * - Funds cannot be withdrawn if there's raised dispute or unpaid invoice
   * - `msg.sender` can only withdraw exact amount he's previously funded if it's still available in contract
   *
   * Emits {FundsWithdrawn} event
   * See spec :ethlance/funds-withdrawn for the format of _ipfsData file
   */
  function withdrawFunds(
    EthlanceStructs.TokenValue[] memory _toBeWithdrawn
  ) external {
    require(_toBeWithdrawn.length == 1, "Currently only possible to withdraw single TokenValue at a time");
    require(_noUnresolvedDisputes(), "Can't withdraw funds when there is unresolved dispute");
    require(!_hasUnpaidInvoices(), "Can't withdraw whilst there are unpaid invoices");
    EthlanceStructs.TokenValue memory withdrawnValue = _toBeWithdrawn[0];

    _executeWithdraw(msg.sender, withdrawnValue);
    ethlance.emitFundsWithdrawn(address(this), msg.sender, _toBeWithdrawn);
  }

  function withdrawAll() external {
    EthlanceStructs.TokenValue[] memory withdrawAmounts = maxWithdrawableAmounts(msg.sender);
    for(uint i = 0; i < withdrawAmounts.length; i++) {
      _executeWithdraw(msg.sender, withdrawAmounts[i]);
    }
    ethlance.emitFundsWithdrawn(address(this), msg.sender, withdrawAmounts);
  }

  function _hasUnpaidInvoices() internal returns(bool) {
    for(uint i = 0; i < lastInvoiceIndex; i++) {
      Invoice memory invoice = invoices[i];
      if (invoice.paid == false && invoice.cancelled == false) {
        return true;
      }
    }
    return false;
  }

  function _noUnresolvedDisputes() internal returns(bool) {
    bool allResolved = true;
    for(uint i = 0; i < disputeIds.length; i++) {
      allResolved = allResolved && disputes[disputeIds[i]].resolved ;
    }
    return allResolved;
  }

  function _executeWithdraw(address receiver, EthlanceStructs.TokenValue memory tokenValue) internal {
    bytes32 valueDepositId = _generateDepositId(receiver, tokenValue);
    Deposit memory deposit = deposits[valueDepositId];
    require(tokenValue.value <= deposit.tokenValue.value, "Can't withdraw more than the withdrawer has deposited");
    EthlanceStructs.transferTokenValue(tokenValue, address(this), receiver);
    deposit.tokenValue.value -= tokenValue.value;
    deposits[valueDepositId] = deposit;
  }

  // Normally the contributors (job creator and those who have added funds) can withdraw all their funds
  // at any point. This is not the case when there have already been payouts and thus the funds kept in
  // this Job contract are less.
  // In such case these users will be eligible up to what they've contributed limited to what's left in Job
  //
  // This method can be used to receive array of TokenValue-s with max amounts to be used
  // for subsequent withdrawFunds call
  function maxWithdrawableAmounts(address contributor) public view returns(EthlanceStructs.TokenValue[] memory) {
    EthlanceStructs.TokenValue[] memory withdrawables = new EthlanceStructs.TokenValue[](depositIds.length);
    uint withdrawablesCount = 0;
    for(uint i = 0; i < depositIds.length; i++) {
      Deposit memory deposit = deposits[depositIds[i]];
      if(deposit.depositor == contributor) {
        EthlanceStructs.TokenValue memory tv = deposit.tokenValue;
        uint jobTokenBalance = EthlanceStructs.tokenValueBalance(address(this), tv);
        if (jobTokenBalance == 0) { break; } // Nothing to do if 0 tokens left of the kind
        uint valueToWithdraw = min(jobTokenBalance, tv.value);
        if (valueToWithdraw == 0) { break; } // Nothing to do if could withdraw 0
        tv.value = valueToWithdraw;
        withdrawables[withdrawablesCount] = tv;
        withdrawablesCount += 1;
      }
    }

    // Return only the ones that matched contributor (can't dynamically allocate in-memory array, need to reconstruct)
    EthlanceStructs.TokenValue[] memory compactWithdrawables = new EthlanceStructs.TokenValue[](withdrawablesCount);
    for(uint i = 0; i < withdrawablesCount; i++) {
      compactWithdrawables[i] = withdrawables[i];
    }

    return compactWithdrawables;
  }

  function min(uint a, uint b) pure internal returns(uint) {
    return a <= b ? a : b;
  }

  /**
   * @dev Raises a dispute between job creator and candidate
   *
   * Requirements:
   * - Only creator of the invoice with `invoiceId` can call this function
   * - Dispute can't be raised twice for the same `invoiceId`
   *
   * Emits {DisputeRaised} event
   * See spec :ethlance/dispute-raised for the format of _ipfsData file
   */
  function raiseDispute(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external {
    Invoice memory invoice = invoices[_invoiceId];
    require(invoice.issuer != address(0), "Can only raise dispute for invoices that exist");
    require(invoice.issuer == msg.sender, "Only issuer of an invoice can raise dispute about it");
    bool previousDisputeFound = false;
    for(uint i = 0; i < disputeIds.length; i++) {
      if(disputes[disputeIds[i]].invoiceId == _invoiceId) {
        previousDisputeFound = true;
        break;
      }
    }
    require(previousDisputeFound == false, "Can't raise dispute for same invoice more than once.");
    Dispute memory dispute = Dispute(_invoiceId, msg.sender, invoice.item, false);
    disputes[_invoiceId] = dispute;
    disputeIds.push(_invoiceId);
    ethlance.emitDisputeRaised(address(this), _invoiceId, _ipfsData);
  }


  /**
   * @dev Resolves a dispute between job creator and candidate
   * It tramsfers `_valueForInvoicer` from this contract into invoicer's address
   *
   * Requirements:
   * - Can be called only by arbiter accepted for this job
   * - Dispute can't be raised twice for the same `invoiceId`
   * - `_ipfsData` cannot be empty
   *
   * Emits {DisputeResolved} event
   * See spec :ethlance/dispute-resolved for the format of _ipfsData file
   */
  function resolveDispute(
    uint _invoiceId,
    EthlanceStructs.TokenValue[] memory _valueForInvoicer,
    bytes memory _ipfsData
  ) external {
    Dispute memory dispute = disputes[_invoiceId];
    require(dispute.creator != address(0), "The dispute to resolve didn't exist");
    require(dispute.resolved == false, "Can only resolve dispute once");
    require(invitedArbiters.contains(msg.sender), "Only invited arbitor can resolve disputes");
    dispute.resolved = true;

    Invoice memory invoice = invoices[_invoiceId];
    invoice.cancelled = true; // We pay it via dispute resolution. Should it be another state?

    for(uint i = 0; i < _valueForInvoicer.length; i++) {
      EthlanceStructs.TokenValue memory tv = _valueForInvoicer[i];
      EthlanceStructs.transferTokenValue(tv, address(this), invoice.issuer);
    }

    disputes[_invoiceId] = dispute;
    invoices[_invoiceId] = invoice;
    ethlance.emitDisputeResolved(_invoiceId, _valueForInvoicer, _ipfsData);
  }


  /**
   * @dev This function is called automatically when this contract receives approval for ERC20 MiniMe token
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   * TODO: Needs implementation
   */
  function receiveApproval(
    address _from,
    uint256 _amount,
    address _token,
    bytes memory _data
  ) external {
  }

  enum TargetMethod { ACCEPT_QUOTE_FOR_ARBITRATION, ADD_FUNDS, ADD_FUNDS_AND_PAY_INVOICE }

  // This function is not meant to have implementation, rather only to serve for ABI encoder
  // to use for encoding call data for token (ERC721/1155) callbacks
  function exampleFunctionSignatureForTokenCallbackDataEncoding(TargetMethod targetMethod,
                                                                address target,
                                                                EthlanceStructs.TokenValue[] memory tokenValues,
                                                                uint invoiceId) public payable {}


  function _decodeTokenCallbackData(bytes calldata _data) internal returns(TargetMethod, address, EthlanceStructs.TokenValue[] memory, uint) {
    return abi.decode(_data[4:], (TargetMethod, address, EthlanceStructs.TokenValue[], uint));
  }

  /**
   * @dev This function is called automatically when this contract receives ERC721 token
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   */
  function onERC721Received(
    address _operator,
    address _from,
    uint256 _tokenId,
    bytes calldata _data
  ) public override returns (bytes4) {
    if (_data.length > 0) { _delegateBasedOnData(_data); }
    return bytes4(keccak256("onERC721Received(address,address,uint256,bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives ERC1155 token
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   */
  function onERC1155Received(
    address _operator,
    address _from,
    uint256 _id,
    uint256 _value,
    bytes calldata _data
  ) public override returns (bytes4) {
    if (_data.length > 0) { _delegateBasedOnData(_data); }
    return bytes4(keccak256("onERC1155Received(address,address,uint256,uint256,bytes)"));
  }

  function _delegateBasedOnData(bytes calldata _data) internal {
    TargetMethod targetMethod;
    address target;
    EthlanceStructs.TokenValue[] memory tokenValues;
    uint invoiceId;
    (targetMethod, target, tokenValues, invoiceId) = _decodeTokenCallbackData(_data);

    if(targetMethod == TargetMethod.ACCEPT_QUOTE_FOR_ARBITRATION) {
      acceptQuoteForArbitration(target, tokenValues);
    } else if(targetMethod == TargetMethod.ADD_FUNDS) {
      _recordAddedFunds(target, tokenValues);
    } else if (targetMethod == TargetMethod.ADD_FUNDS_AND_PAY_INVOICE) {
      // 1. Take ownership of the tokens (by this time the tokens should be approved for this Job contract)
      // 2. Send them to the worker
      revert("Not yet implemented");
    } else {
      revert("Unknown TargetMethod on ERC721 receival callback");
    }
  }

  /**
   * @dev This function is called automatically when this contract receives multiple ERC1155 tokens
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   * TODO: Needs implementation
   */
  function onERC1155BatchReceived(
    address _operator,
    address _from,
    uint256[] calldata _ids,
    uint256[] calldata _values,
    bytes calldata _data
  ) public override returns (bytes4) {
    return bytes4(keccak256("onERC1155BatchReceived(address,address,uint256[],uint256[],bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives ETH
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `msg.data`
   */
  receive(
  ) external payable {
  }

  function supportsInterface(bytes4 interfaceId) external override view returns (bool) {
    return interfaceId == type(IERC20).interfaceId ||
      interfaceId == type(IERC721).interfaceId ||
      interfaceId == type(IERC1155).interfaceId ||
      interfaceId == type(IERC721Receiver).interfaceId ||
      interfaceId == type(IERC1155Receiver).interfaceId;
  }

  function isAmongstInvitedArbiters(address account) internal returns (bool) {
    return invitedArbiters.contains(account);
  }

  function isCallerJobCreator(address account) internal returns (bool) {
    return account == creator;
  }
}
