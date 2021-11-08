pragma solidity ^0.8.0;

import "./StandardBounties.sol";
import "./EthlanceJobs.sol";
import "./token/IERC20.sol";
import "./token/IERC721.sol";

contract EthlanceIssuer {

  StandardBounties internal constant standardBounties = StandardBounties(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  EthlanceJobs internal constant ethlanceJobs = EthlanceJobs(0xdeaDDeADDEaDdeaDdEAddEADDEAdDeadDEADDEaD);

  enum JobType {EthlanceJob, StandardBounty}

  struct TokenParams {
    address token;
    uint tokenVersion;
  }

  // Track invited arbiters and their fees
  mapping(address => mapping(uint => uint)) public jobsArbitersFees;
  mapping(address => mapping(uint => uint)) public bountiesArbitersFees;

  // Track accepted (payed) arbiters per job
  mapping(uint => address) public jobsAcceptedArbiters;
  mapping(uint => address) public bountiesAcceptedArbiters;

  mapping(uint => TokenParams) public bounties;
  mapping(uint => TokenParams) public jobs;

  function transfer(address from, address to, address token, uint tokenVersion, uint depositAmount) private {
    require(depositAmount > 0, "Insufficient amount"); // Contributions of 0 tokens or token ID 0 should fail

    if (tokenVersion == 0){
      if(from == address(this)){
        address payable toPayable = address(uint160(to));
        toPayable.send(depositAmount);
      }else{
        require(msg.value >= depositAmount,"Insuficien ETH");
      }

    } else if (tokenVersion == 20){

      require(msg.value == 0, "No ETH should be provided for ERC20"); // Ensures users don't accidentally send ETH alongside a token contribution, locking up funds

      require(IERC20(token).transferFrom(from,to,depositAmount), "Couldn't transfer ERC20");
    } else if (tokenVersion == 721){
      require(msg.value == 0,"No ETH should be provided for tokenVersion 721"); // Ensures users don't accidentally send ETH alongside a token contribution, locking up funds
      IERC721(token).transferFrom(from,to,depositAmount);
    } else {
      revert();
    }
  }

  /**
      This function is for inviting more arbiters, in case nobody
      accepted in the first round of invites.
  */
  function inviteArbiters(address[] memory arbiters, uint fee, uint feeCurrencyId, uint jobId, JobType jobType) public payable {
    address token = bounties[jobId].token;
    uint tokenVersion = bounties[jobId].tokenVersion;

    // If paying in eth make sure you send enough funds for paying all arbiters
    if(tokenVersion == 0) require(msg.value == fee*arbiters.length,"Insuficien funds");

    // Transfer the fee that is going to be payed to the first arbiter who accepts
    transfer(msg.sender,address(this), token, tokenVersion, fee);

    for(uint i = 0; i < arbiters.length; i ++){
      // transfer fee to this contract so we can transfer it to arbiter when
      // invitation gets accepted


      if(jobType == JobType.StandardBounty){
        bountiesArbitersFees[arbiters[i]][jobId] = fee;
      } else if (jobType == JobType.EthlanceJob){
        jobsArbitersFees[arbiters[i]][jobId] = fee;
      }
    }

    emit ArbitersInvited(arbiters, fee, feeCurrencyId, jobId, jobType);
  }

  /**
     This function creates a bounty in StandardBouties contract,
     passing as issuers addresses of this contract and sender's
     address. Also it stores addresses of invited arbiters (approvers)
     and arbiter's fee for created bounty.
  */
  function issueBounty(string memory bountyData, uint deadline, address token, uint tokenVersion, uint depositAmount) public payable{
    address[] memory arbiters=new address [](0);

    // EthlanceBountyIssuer is the issuer of all bounties
    address payable[] memory issuers = new address payable[](1);

    address payable thisPayable = address(uint160(address(this)));
    issuers[0] = thisPayable;

    transfer(msg.sender, address(this), token, tokenVersion, depositAmount);

    // Also pass whatever value was sent to us forward
    uint bountyId = standardBounties.issueAndContribute.value(msg.value)(thisPayable,
                                                                         issuers,
                                                                         arbiters,
                                                                         bountyData,
                                                                         deadline,
                                                                         token,
                                                                         tokenVersion,
                                                                         depositAmount);

    bounties[bountyId] = TokenParams(token, tokenVersion);

  }

  /**
     This function creates a job in EthlanceJobs contract,
     passing as issuers addresses of this contract and sender's
     address. Also it stores addresses of invited arbiters (approvers)
     and arbiter's fee for created Job.
  */
  function issueJob(string memory jobData, address token, uint tokenVersion, uint depositAmount) public payable{
    address[] memory arbiters = new address [](0);

    // EthlanceBountyIssuer is the issuer of all bounties
    address payable[] memory issuers = new address payable[](1);

    address payable thisPayable = address(uint160(address(this)));
    issuers[0] = thisPayable;

    transfer(msg.sender, address(this), token, tokenVersion, depositAmount);

    // Also pass whatever value was sent to us forward
    uint jobId = ethlanceJobs.issueAndContribute.value(msg.value)(thisPayable,
                                                                  issuers,
                                                                  arbiters,
                                                                  jobData,
                                                                  token,
                                                                  tokenVersion,
                                                                  depositAmount);

    jobs[jobId] = TokenParams(token, tokenVersion);

  }

  /**
     Arbiter runs this function to accept invitation. If he's first, it'll
     transfer fee to him and it'll add him as arbiter for the bounty.
  */
  function acceptArbiterInvitation(JobType jobType, uint jobId) public {
    // check that it was invited

    address[] memory arbiters = new address [](1);
    arbiters[0] = msg.sender;
    address token;
    uint tokenVersion;
    uint fee;


    if(jobType == JobType.StandardBounty){

      if(bountiesAcceptedArbiters[jobId] != address(0)){
        revert("This position is close.");
      }

      fee=bountiesArbitersFees[msg.sender][jobId];
      standardBounties.addApprovers(address(this),
                                    jobId,
                                    0, // since there is only one issuer, it is the first one
                                    arbiters);
      token = bounties[jobId].token;
      tokenVersion = bounties[jobId].tokenVersion;
    } else if (jobType == JobType.EthlanceJob){

      if(jobsAcceptedArbiters[jobId] != address(0)){
        revert("This position is close.");
      }

      fee=jobsArbitersFees[msg.sender][jobId];

      ethlanceJobs.addApprovers(address(this),
                                jobId,
                                0, // since there is only one issuer, it is the first one
                                arbiters);
      token = jobs[jobId].token;
      tokenVersion = jobs[jobId].tokenVersion;
    }

    require(fee > 0,"Arbiters fees should be greater than zero.");

    transfer(address(this), msg.sender, token, tokenVersion, fee);

    emit ArbiterAccepted(msg.sender, jobId);
  }

  event ArbitersInvited(address[] _arbiters, uint _fee, uint _feeCurrencyId, uint _jobId, JobType _jobType);
  event ArbiterAccepted(address _arbiter, uint _jobId);

}
