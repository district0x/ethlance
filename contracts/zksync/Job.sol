// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "./EthlanceStructs.sol";
import "./Ethlance.sol";
// import "../token/ApproveAndCallFallback.sol";
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
  address public creator;
  EthlanceStructs.JobType public jobType;
  mapping(uint => EthlanceStructs.TokenValue) public offeredValues;
  mapping(address => EthlanceStructs.TokenValue) public arbiterQuotes;
  using EnumerableSet for EnumerableSet.AddressSet;
  EnumerableSet.AddressSet internal invitedArbiters;
  EnumerableSet.AddressSet internal invitedCandidates;

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
    for(uint i = 0; i < _offeredValues.length; i++) { offeredValues[i] = _offeredValues[i]; }
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
   // Arbiter can set any quote that they want
   // Just check that they're amongst invited ones <<<---- ADD THIS (and test)
  function setQuoteForArbitration(
    EthlanceStructs.TokenValue[] memory _quote
  ) external {
    // Currently allowing & requiring single TokenValue, leaving the interface
    // backwards-compatible in case me support more in the future.
    require(invitedArbiters.contains(msg.sender), "Quotes can only be set by invited arbiters");
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
   *
   * TODO: Needs implementation
   */
   // Employer sends Tx to Job contract with the necessary tokens included
   // This function gets called via the ERC20/721/1155 callbacks
   // If the amounts are correct, the tokens get immediately forwarded to the Arbiter
  function _acceptQuoteForArbitration(
    address _arbiter,
    EthlanceStructs.TokenValue[] memory _transferredValue
  ) internal {
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
    // TODO: Needs test
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
  struct Invoice {
    EthlanceStructs.TokenValue item;
    address payable issuer;
    uint invoiceId;
    bool paid;
  }
  mapping (uint => Invoice) public invoices;
  mapping (address => uint[]) public candidateInvoiceIds;
  uint lastInvoiceIndex;

  function createInvoice(
    EthlanceStructs.TokenValue[] memory _invoicedValue,
    bytes memory _ipfsData
  ) external {
    if (jobType == EthlanceStructs.JobType.GIG) { require(invitedCandidates.contains(msg.sender)); }
    // TODO: Check that job isn't paid
    // TODO: Check that issuer has been set

    for(uint i = 0; i < _invoicedValue.length; i++) {
      Invoice memory newInvoice = Invoice(_invoicedValue[i], payable(msg.sender), lastInvoiceIndex, false);
      invoices[lastInvoiceIndex] = newInvoice;
      candidateInvoiceIds[msg.sender].push(lastInvoiceIndex);

      // TODO: Is there a better way to emit array of TokenValue-s?
      EthlanceStructs.TokenValue[] memory single = new EthlanceStructs.TokenValue[](1);
      single[0] = _invoicedValue[0];
      ethlance.emitInvoiceCreated(address(this), address(msg.sender), lastInvoiceIndex, single, _ipfsData);
      lastInvoiceIndex += 1;
    }
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
  ) external {
    require(msg.sender == creator);
    Invoice memory invoice = invoices[_invoiceId];
    require(invoice.paid == false);
    EthlanceStructs.TokenType tokenType = invoice.item.token.tokenContract.tokenType;
    if (tokenType == EthlanceStructs.TokenType.ETH) {
      invoice.issuer.transfer(invoice.item.value);
    } else if (tokenType == EthlanceStructs.TokenType.ERC20) {
      IERC20 offeredToken = IERC20(invoice.item.token.tokenContract.tokenAddress);
      require(offeredToken.balanceOf(address(this)) > 0, "Job must own the token in order to pay it out");
      offeredToken.transfer(invoice.issuer, invoice.item.value);
    } else if (tokenType == EthlanceStructs.TokenType.ERC721) {
      IERC721 offeredToken = IERC721(invoice.item.token.tokenContract.tokenAddress);
      require(offeredToken.ownerOf(invoice.item.token.tokenId) == address(this), "Job must own the token in order to pay it out");
      offeredToken.safeTransferFrom(address(this), invoice.issuer, invoice.item.token.tokenId);
    } else if (tokenType == EthlanceStructs.TokenType.ERC1155) {
      IERC1155 offeredToken = IERC1155(invoice.item.token.tokenContract.tokenAddress);
      uint payableAmount = invoice.item.value;
      require(offeredToken.balanceOf(address(this), invoice.item.token.tokenId) >= payableAmount, "Job must enough of the ERC1155 token in order to pay it out");
      offeredToken.safeTransferFrom(address(this), invoice.issuer, invoice.item.token.tokenId, payableAmount, "");
    } else {
      revert("Unsupported token type");
    }

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
   * TODO: Needs implementation
   */
  function cancelInvoice(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external {
    // TODO: do I need additional boolean to determine whether it's cancelled? Probably yes
    //       we don't want to delete from invoices (useful to show in the UI)
    // We mark invoice as cancelled (use this info to determine whether an invoice can be paid)
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
   * TODO: Needs implementation
   */
  function _addFunds(
    address _funder,
    EthlanceStructs.TokenValue[] memory _fundedValue
  ) internal {
    // check that the _fundedValue is within _offeredValue (used during initialization)
  }


  /**
   * @dev It joins together `{_addFunds}` and `{payInvoice}` calls
   *
   * This function is not meant to be called directly, but via token received callbacks
   * TODO: Needs implementation
   */
  function _addFundsAndPayInvoice(
    EthlanceStructs.TokenValue[] memory _fundedValue,
    uint _invoiceId
  ) internal {
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
   * TODO: Needs implementation
   */
  function withdrawFunds(
    EthlanceStructs.TokenValue[] memory _toBeWithdrawn
  ) external {
    // Check if sender has funded this value or more before
    // If this contract has that amount balance
    // At least for now no proportional paying - if there's available amount to be withdrawn
    //   and the user has contributed it, they can withdraw it.
    // If contract has 0.8 ETH but caller calls with 1, transaction will fail.
    // The amount has to match (input data preparetion and validation will happen in the front-end)
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
   * TODO: Needs implementation
   */
  function raiseDispute(
    uint _invoiceId,
    bytes memory _ipfsData
  ) external {
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
   * TODO: Needs implementation
   */
  function resolveDispute(
    uint _invoiceId,
    EthlanceStructs.TokenValue[] memory _valueForInvoicer,
    bytes memory _ipfsData
  ) external {
  }


  /**
   * @dev This function is called automatically when this contract receives approval for ERC20 MiniMe token
   * It calls either {_acceptQuoteForArbitration} or {_addFunds} or {_addFundsAndPayInvoice} based on decoding `_data`
   * TODO: Needs implementation
   */
  function receiveApproval(
    address _from,
    uint256 _amount,
    address _token,
    bytes memory _data
  ) external {
  }


  /**
   * @dev This function is called automatically when this contract receives ERC721 token
   * It calls either {_acceptQuoteForArbitration} or {_addFunds} or {_addFundsAndPayInvoice} based on decoding `_data`
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
   * It calls either {_acceptQuoteForArbitration} or {_addFunds} or {_addFundsAndPayInvoice} based on decoding `_data`
   * TODO: Needs implementation
   */
  function onERC1155Received(
    address _operator,
    address _from,
    uint256 _id,
    uint256 _value,
    bytes calldata _data
  ) public override returns (bytes4) {
    return bytes4(keccak256("onERC1155Received(address,address,uint256,uint256,bytes)"));
  }


  /**
   * @dev This function is called automatically when this contract receives multiple ERC1155 tokens
   * It calls either {_acceptQuoteForArbitration} or {_addFunds} or {_addFundsAndPayInvoice} based on decoding `_data`
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
   * It calls either {_acceptQuoteForArbitration} or {_addFunds} or {_addFundsAndPayInvoice} based on decoding `msg.data`
   * TODO: Needs implementation
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
}
