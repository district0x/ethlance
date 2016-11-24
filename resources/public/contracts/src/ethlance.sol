pragma solidity ^0.4.4;

import "safeMath.sol";
import "userLibrary.sol";
import "jobLibrary.sol";
import "contractLibrary.sol";
import "jobActionLibrary.sol";
import "invoiceLibrary.sol";
import "categoryLibrary.sol";
import "skillLibrary.sol";

contract Ethlance is Ownable {
    address public eternalStorage;
    uint8 public status;

    modifier onlyActiveEmployer {
        if (!UserLibrary.isActiveEmployer(eternalStorage, msg.sender)) throw;
        _;
    }

    modifier onlyActiveFreelancer {
        if (!UserLibrary.isActiveFreelancer(eternalStorage, msg.sender)) throw;
        _;
    }

    modifier onlyActiveUser {
        if (!UserLibrary.hasStatus(eternalStorage, getSenderUserId(), 1)) throw;
        _;
    }

    modifier onlyActiveSmartContract {
        if (status != 0) throw;
        _;
    }

    function Ethlance(address _eternalStorage) {
        eternalStorage = _eternalStorage;
        setConfig("max-user-languages", 10);
        setConfig("max-freelancer-categories", 20);
        setConfig("max-freelancer-skills", 15);
        setConfig("max-job-skills", 7);
        setConfig("max-user-description", 1000);
        setConfig("max-job-description", 1000);
        setConfig("max-invoice-description", 500);
        setConfig("max-feedback", 1000);
        setConfig("max-job-title", 100);
    }

    function setConfig(string key, uint val)
    onlyOwner {
        EternalStorage(eternalStorage).setUIntValue(sha3("config/", key), val);
    }

    function getConfig(string key) constant returns(uint) {
        return EternalStorage(eternalStorage).getUIntValue(sha3("config/", key));
    }

    function getSenderUserId() returns(uint) {
        return UserLibrary.getUserId(eternalStorage, msg.sender);
    }

    function setUser(bytes32 name, bytes32 gravatar, uint country, uint[] languages)
    onlyActiveSmartContract
    {
        if (languages.length > getConfig("max-user-languages")) throw;
        UserLibrary.setUser(eternalStorage, msg.sender, name, gravatar, country, languages);
    }

    function setFreelancer(bytes32 name, bytes32 gravatar, uint country, uint[] languages,
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
    {
        setUser(name, gravatar, country, languages);
        setFreelancer(isAvailable, jobTitle, hourlyRate, categories, skills, description);
    }

    function setFreelancer(
        bool isAvailable,
        bytes32 jobTitle,
        uint hourlyRate,
        uint[] categories,
        uint[] skills,
        string description
    )
        onlyActiveSmartContract
    {
        if (categories.length > getConfig("max-freelancer-categories")) throw;
        if (skills.length > getConfig("max-freelancer-skills")) throw;
        if (bytes(description).length > getConfig("max-user-description")) throw;
        UserLibrary.setFreelancer(eternalStorage, getSenderUserId(), isAvailable, jobTitle, hourlyRate, categories,
            skills, description);
    }

    function setEmployer(bytes32 name, bytes32 gravatar, uint country, uint[] languages, string description)
    onlyActiveSmartContract
    {
        setUser(name, gravatar, country, languages);
        setEmployer(description);
    }

    function setEmployer(string description)
    onlyActiveSmartContract
    {
        if (bytes(description).length > getConfig("max-user-description")) throw;
        UserLibrary.setEmployer(eternalStorage, getSenderUserId(), description);
    }

    function setJobHiringDone(uint jobId)
    onlyActiveSmartContract
    {
        JobLibrary.setHiringDone(eternalStorage, jobId, getSenderUserId());
    }

    function addJob(
        string title,
        string description,
        uint[] skills,
        uint language,
        uint budget,
        uint8[] uint8Items
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        if (bytes(description).length > getConfig("max-job-description")) throw;
        if (bytes(title).length > getConfig("max-title-description")) throw;
        if (skills.length > getConfig("max-job-skills")) throw;
        JobLibrary.addJob(eternalStorage, getSenderUserId(), title, description, skills, language, budget, uint8Items);
    }

    function addJobProposal(
        uint jobId,
        string description,
        uint rate
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        JobActionLibrary.addProposal(eternalStorage, jobId, getSenderUserId(), description, rate);
    }

    function addJobInvitation(
        uint jobId,
        uint freelancerId,
        string description
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        JobActionLibrary.addInvitation(eternalStorage, getSenderUserId(), jobId, freelancerId, description);
    }

    function addJobContract(
        uint jobActionId,
        uint rate,
        bool isHiringDone
    )
        onlyActiveSmartContract
        onlyActiveEmployer
    {
        ContractLibrary.addContract(eternalStorage, getSenderUserId(), jobActionId, rate, isHiringDone);
    }

    function addJobContractFeedback(
        uint contractId,
        string feedback,
        uint8 rating
    )
        onlyActiveSmartContract
        onlyActiveUser
    {
        if (bytes(feedback).length > getConfig("max-feedback")) throw;
        if (rating > 100) throw;
        ContractLibrary.addFeedback(eternalStorage, contractId, getSenderUserId(), feedback, rating);
    }

    function addInvoice(
        uint contractId,
        string description,
        uint amount,
        uint workedHours,
        uint workedFrom,
        uint workedTo
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        if (bytes(description).length > getConfig("max-invoice-description")) throw;
        InvoiceLibrary.addInvoice(eternalStorage, getSenderUserId(), contractId, description, amount, workedHours,
            workedFrom, workedTo);
    }

    function payInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveEmployer
        payable
    {
        address freelancerAddress = InvoiceLibrary.getFreelancerAddress(eternalStorage, invoiceId);
        InvoiceLibrary.setInvoicePaid(eternalStorage, getSenderUserId(), msg.value, invoiceId);
        if (!freelancerAddress.send(msg.value)) throw;
    }

    function cancelInvoice(
        uint invoiceId
    )
        onlyActiveSmartContract
        onlyActiveFreelancer
    {
        InvoiceLibrary.setInvoiceCancelled(eternalStorage, getSenderUserId(), invoiceId);
    }

    function setJobStatus(
        uint jobId,
        uint8 status
    )
        onlyOwner
    {
        JobLibrary.setStatus(eternalStorage, jobId, status);
    }

    function setUserStatus(
        address userAddress,
        uint8 status
    )
        onlyOwner
    {
        UserLibrary.setStatus(eternalStorage, UserLibrary.getUserId(eternalStorage, userAddress), status);
    }

    function setSmartContractStatus(
        uint8 _status
    )
        onlyOwner
    {
        status = _status;
    }
}