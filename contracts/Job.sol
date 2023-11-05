// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.21;
pragma experimental ABIEncoderV2;

import "./EthlanceStructs.sol";
import "./JobHelpers.sol";
import "./Ethlance.sol";
import "./JobStorage.sol";
import "./external/ds-auth/auth.sol";

/**
 * @dev Job contract on Ethlance
 * Job facilitates value transfers between job creator, one or multiple candidates
 * and optionally an arbiter.
 * Every new Job contract is created as a proxy contract.
 */
contract Job is IERC721Receiver, IERC1155Receiver, DSAuth, JobStorage {
  // Storage layout is inherited from JobStorage

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
    EthlanceStructs.TokenValue[] calldata _offeredValues,
    address[] calldata _invitedArbiters
  ) external {
    require(address(ethlance) == address(0), "Contract already initialized. Can only be done once");
    require(address(_ethlance) != address(0), "Ethlance can't be null");
    require(_creator != address(0), "Creator can't be null");
    require(_offeredValues.length > 0, "You must offer some tokens as pay");

    ethlance = _ethlance;
    creator = _creator;
    lastInvoiceIndex = FIRST_INVOICE_INDEX;
    inviteArbiters(_creator, _invitedArbiters);
    _recordAddedFunds(_creator, _offeredValues);
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
    EthlanceStructs.TokenValue[] calldata _quote
  ) external {
    // Currently allowing & requiring single TokenValue, leaving the interface
    // backwards-compatible in case me support more in the future.
    require(isAmongstInvitedArbiters(msg.sender), "Quotes can only be set by invited arbiters");
    require(_quote.length == 1, "Exactly 1 quote is required");
    arbiterQuotes[msg.sender] = _quote[0];
    ethlance.emitQuoteForArbitrationSet(address(this), msg.sender, _quote);
  }


  function inviteArbiters(
    address _sender,
    address[] calldata _invitedArbiters
  ) public {
    string memory message = string.concat("Only job creator is allowed to add arbiters ", "sender: ", EthlanceStructs.toString(_sender), " <-> creator: ", EthlanceStructs.toString(creator));
    require(isCallerJobCreator(_sender), message);

    for(uint i = 0; i < _invitedArbiters.length; i++) {
      addArbiter(_invitedArbiters[i]);
    }
    ethlance.emitArbitersInvited(address(this), _invitedArbiters);
  }

  /**
   * @dev It is called by job creator when he decides to accept an quote from an arbiter
   * It checks if `_transferredValue` matches the quote requested by an arbiter
   * It transfers the value to the arbiter's address
   *
   * This function is not meant to be called directly, but via token received callbacks
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
  ) public payable {
    // Allow re-assigning accepted arbiter after 30 days since raising dispute (remains unresolved)
    // Generate feedback from part of employer stating "This arbiter got replaced due to inactivity > 30 days of open dispute"
    require(true
            || acceptedArbiter == address(0)
            || acceptedArbiter == _arbiter
            || JobHelpers.isAcceptedArbiterIdle(disputeIds, disputes, ARBITER_IDLE_TIMEOUT, block.timestamp),
            "Another arbiter (non-idle) had been accepted before. Only 1 can be accepted.");
    require(isAmongstInvitedArbiters(_arbiter), "Arbiter to be accepted must be amongst invited arbiters");
    require(isCallerJobCreator(msg.sender), "Only job creator (employer) can accept quote for arbitration");
    require(_transferredValue.length == 1, "Currently only 1 _transferredValue is supported at a time");
    require(EthlanceStructs.tokenValuesEqual(_transferredValue[0], arbiterQuotes[_arbiter]), "Accepted TokenValue must match exactly the value quoted by arbiter");

    acceptedArbiter = _arbiter;
    EthlanceStructs.transferTokenValue(_transferredValue[0], address(this), _arbiter);
    ethlance.emitQuoteForArbitrationAccepted(address(this), _arbiter);
  }

  function isAcceptedArbiterIdle() public view returns (bool) {
    return JobHelpers.isAcceptedArbiterIdle(disputeIds, disputes, ARBITER_IDLE_TIMEOUT, block.timestamp);
  }

  /**
   * @dev It is called by job creator when he allows a new candidate to start invoicing for this job
   *
   * Requirements:
   * - Can only be called by job creator
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
    require(_candidate != creator, "Candidate can't be the same address as creator (employer)");
    require(_candidate != acceptedArbiter, "Candidate can't be the same address as acceptedArbiter");
    require(containsCandidate(_candidate) == false, "Candidate already added. Can't add duplicates");
    addCandidate(_candidate);
    ethlance.emitCandidateAdded(address(this), address(_candidate), _ipfsData);
  }

  /**
   * @dev Function called by candidate to create an invoice to be paid
   *
   * Requirements:
   * - `msg.sender` must be among added candidates
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
    require(containsCandidate(msg.sender), "Sender must be amongst invitedCandidates to raise an invoice");

    for(uint i = 0; i < _invoicedValue.length; i++) {
      Invoice memory newInvoice = Invoice(_invoicedValue[i], payable(msg.sender), lastInvoiceIndex, false, false);
      invoices[lastInvoiceIndex] = newInvoice;
      invoiceIds.push(lastInvoiceIndex);

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
    require(msg.sender == creator, "Only job creator can pay invoice");
    Invoice storage invoice = invoices[_invoiceId];
    require(invoice.paid == false, "Invoice already paid");
    if (disputeExistsForInvoice(_invoiceId)) { // If employer wants to pay disputed invoice, mark the dispute as resolved
      disputes[_invoiceId].resolved = true;
    }

    EthlanceStructs.transferTokenValue(invoice.item, address(this), invoice.issuer);

    invoice.paid = true;
    invoices[_invoiceId] = invoice;

    EthlanceStructs.TokenValue[] memory outValues = new EthlanceStructs.TokenValue[](1);
    outValues[0] = invoice.item;

    ethlance.emitFundsOut(address(this), outValues);
    ethlance.emitInvoicePaid(address(this), _invoiceId, _ipfsData);
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
    ethlance.emitInvoiceCanceled(address(this), _invoiceId, _ipfsData);
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
   * Emits {FundsIn} event
   * See spec :ethlance/funds-added for the format of _ipfsData file
   */
  function _recordAddedFunds(
    address _funder,
    EthlanceStructs.TokenValue[] memory _offeredValues
  ) internal {
    for(uint i = 0; i < _offeredValues.length; i++) {
      EthlanceStructs.TokenValue memory tv = _offeredValues[i];
      bytes32 depositId = _generateDepositId(_funder, tv);
      EthlanceStructs.Deposit storage earlierDeposit = deposits[depositId];
      if (earlierDeposit.depositor == address(0)) {
        // No earlier deposit of that token from the depositor
        EthlanceStructs.Deposit memory deposit = EthlanceStructs.Deposit(_funder, tv);
        deposits[depositId] = deposit;
        depositIds.push(depositId);
        addDepositor(_funder);

      } else {
        // There was a deposit before of that token from the depositor
        // Record added funds
        earlierDeposit.tokenValue.value += tv.value;
      }
    }
    ethlance.emitFundsIn(address(this), _offeredValues);
  }

  // For adding ETH and ERC20 funds. ERC721 and 1155 get added via callbacks (onERC<...>Received)
  function addFunds(EthlanceStructs.TokenValue[] memory _tokenValues) public payable {
    require(_tokenValues.length == 1, "Currently only single TokenValue can be added");
    EthlanceStructs.TokenValue memory tokenValue = _tokenValues[0];
    EthlanceStructs.transferTokenValue(tokenValue, msg.sender, address(this));
    _recordAddedFunds(msg.sender, _tokenValues);
  }

  function _generateDepositId(address depositor, EthlanceStructs.TokenValue memory tokenValue)
  internal pure returns(bytes32) {
    return keccak256(abi.encodePacked(
      depositor,
      tokenValue.token.tokenContract.tokenType,
      tokenValue.token.tokenContract.tokenAddress,
      tokenValue.token.tokenId));
  }

  function getDeposits(address depositor) public view returns (EthlanceStructs.TokenValue[] memory) {
    return JobHelpers.getDeposits(depositIds, deposits, depositor);
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
  ) external hasNoOutstandingPayments {
    require(_toBeWithdrawn.length == 1, "Currently only possible to withdraw single TokenValue at a time");
    EthlanceStructs.TokenValue memory withdrawnValue = _toBeWithdrawn[0];

    _executeWithdraw(msg.sender, withdrawnValue);
    ethlance.emitFundsWithdrawn(address(this), msg.sender, _toBeWithdrawn);
  }

  function withdrawAll() external {
    EthlanceStructs.TokenValue[] memory withdrawAmounts = EthlanceStructs.maxWithdrawableAmounts(msg.sender,
                                                                                                depositIds,
                                                                                                deposits);
    for(uint i = 0; i < withdrawAmounts.length; i++) {
      _executeWithdraw(msg.sender, withdrawAmounts[i]);
    }
    ethlance.emitFundsWithdrawn(address(this), msg.sender, withdrawAmounts);
  }

  function endJob() external hasNoOutstandingPayments {
    for(uint i = 0; i < depositorsLength(); i++) {
      address depositor = getDepositor(i);
      EthlanceStructs.TokenValue[] memory depositAmounts = EthlanceStructs.maxWithdrawableAmounts(depositor,
                                                                                                 depositIds,
                                                                                                 deposits);
      for (uint j = 0; j < depositAmounts.length; j++) {
        _executeWithdraw(depositor, depositAmounts[j]);
      }
    }
    jobEnded = true;
    ethlance.emitJobEnded(address(this));
  }

  function _hasUnpaidInvoices() public view returns(bool) {
    for(uint i = 0; i < invoiceIds.length; i++) {
      Invoice memory invoice = invoices[invoiceIds[i]];
      if (invoice.paid == false && invoice.cancelled == false) {
        return true;
      }
    }
    return false;
  }

  function _noUnresolvedDisputes() public view returns(bool) {
    bool allResolved = true;
    for(uint i = 0; i < disputeIds.length; i++) {
      allResolved = allResolved && disputes[disputeIds[i]].resolved ;
    }
    return allResolved;
  }

  function _executeWithdraw(address receiver, EthlanceStructs.TokenValue memory tokenValue) internal {
    bytes32 valueDepositId = _generateDepositId(receiver, tokenValue);
    EthlanceStructs.Deposit memory deposit = deposits[valueDepositId];
    require(tokenValue.value <= deposit.tokenValue.value, "Can't withdraw more than the withdrawer has deposited");
    EthlanceStructs.transferTokenValue(tokenValue, address(this), receiver);
    deposit.tokenValue.value -= tokenValue.value;
    deposits[valueDepositId] = deposit;
    EthlanceStructs.TokenValue[] memory outValues = new EthlanceStructs.TokenValue[](1);
    outValues[0] = tokenValue;
    ethlance.emitFundsOut(address(this), outValues);
  }


  function disputeExistsForInvoice(uint invoiceId) public view returns(bool) {
    for(uint i = 0; i < disputeIds.length; i++) {
      if(disputes[disputeIds[i]].invoiceId == invoiceId) {
        return true;
      }
    }
    return false;
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
    require(disputeExistsForInvoice(_invoiceId) == false, "Can't raise dispute for same invoice more than once.");
    JobHelpers.Dispute memory dispute = JobHelpers.Dispute(_invoiceId, msg.sender, invoice.item, block.timestamp, false);
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
    JobHelpers.Dispute memory dispute = disputes[_invoiceId];
    require(dispute.creator != address(0), "The dispute to resolve didn't exist");
    require(dispute.resolved == false, "Can only resolve dispute once");
    require(acceptedArbiter == msg.sender, "Only accepted arbitor can resolve disputes");
    dispute.resolved = true;

    Invoice memory invoice = invoices[_invoiceId];
    invoice.cancelled = true; // We pay it via dispute resolution. Should it be another state?

    for(uint i = 0; i < _valueForInvoicer.length; i++) {
      EthlanceStructs.TokenValue memory tv = _valueForInvoicer[i];
      EthlanceStructs.transferTokenValue(tv, address(this), invoice.issuer);
    }

    disputes[_invoiceId] = dispute;
    invoices[_invoiceId] = invoice;
    ethlance.emitDisputeResolved(address(this), _invoiceId, _valueForInvoicer, _ipfsData);
  }

  enum TargetMethod { ACCEPT_QUOTE_FOR_ARBITRATION, ADD_FUNDS, ADD_FUNDS_AND_PAY_INVOICE }

  // This function is not meant to have implementation, rather only to serve for ABI encoder
  // to use for encoding call data for token (ERC721/1155) callbacks
  function transferCallbackDelegate(TargetMethod targetMethod,
                                    address target,
                                    EthlanceStructs.TokenValue[] memory tokenValues,
                                    uint invoiceId
                                   ) public payable {}

  function _decodeTokenCallbackData(bytes calldata _data) internal pure returns(TargetMethod, address, EthlanceStructs.TokenValue[] memory, uint) {
    return abi.decode(_data[4:], (TargetMethod, address, EthlanceStructs.TokenValue[], uint));
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

  function isCallerJobCreator(address account) internal view returns (bool) {
    return account == creator;
  }

  // Modifiers
  modifier hasNoOutstandingPayments {
    require(_noUnresolvedDisputes(), "Can't withdraw funds when there is unresolved dispute");
    require(!_hasUnpaidInvoices(), "Can't withdraw whilst there are unpaid invoices");
    _;
  }

  modifier ongoingJob {
    require(jobEnded == false, "This job was ended. Can't receive more funds");
    _;
  }

  // Implemented interfaces
  /**
   * @dev This function is called automatically when this contract receives ERC721 token
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   */
  function onERC721Received(
    address,
    address,
    uint256,
    bytes calldata _data
  ) external override ongoingJob returns (bytes4) {
    if (_data.length > 0) { _delegateBasedOnData(_data); }
    return bytes4(keccak256("onERC721Received(address,address,uint256,bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives ERC1155 token
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   */
  function onERC1155Received(
    address,
    address,
    uint256,
    uint256,
    bytes calldata _data
  ) external override ongoingJob returns (bytes4) {
    if (_data.length > 0) { _delegateBasedOnData(_data); }
    return bytes4(keccak256("onERC1155Received(address,address,uint256,uint256,bytes)"));
  }

  /**
   * @dev This function is called automatically when this contract receives multiple ERC1155 tokens
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `_data`
   */
  function onERC1155BatchReceived(
    address,
    address,
    uint256[] calldata,
    uint256[] calldata,
    bytes calldata _data
  ) external override ongoingJob returns (bytes4) {
    if (_data.length > 0) { _delegateBasedOnData(_data); }
    return bytes4(keccak256("onERC1155BatchReceived(address,address,uint256[],uint256[],bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives ETH
   * It calls either {_acceptQuoteForArbitration} or {_recordAddedFunds} or {addFundsAndPayInvoice} based on decoding `msg.data`
   */
  receive() external ongoingJob payable {
    ethlance.emitFundsIn(address(this), EthlanceStructs.makeTokenValue(msg.value, EthlanceStructs.TokenType.ETH));
  }

  fallback() external payable {
  }

  function supportsInterface(bytes4 interfaceId) external override pure returns (bool) {
    return interfaceId == type(IERC20).interfaceId ||
      interfaceId == type(IERC721).interfaceId ||
      interfaceId == type(IERC1155).interfaceId ||
      interfaceId == type(IERC721Receiver).interfaceId ||
      interfaceId == type(IERC1155Receiver).interfaceId;
  }
}
