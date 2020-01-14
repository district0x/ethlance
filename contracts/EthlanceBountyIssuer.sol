pragma solidity ^0.5.0;

import "./StandardBounties.sol";
import "./token/IERC20.sol";
import "./token/IERC721.sol";

contract EthlanceBountyIssuer {

  StandardBounties internal constant standardBounties = StandardBounties(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);

  struct BountyParams {
    address token;
    uint tokenVersion;
  }

  mapping(address => mapping(uint => uint)) public arbitersFees;
  mapping(uint => BountyParams) public bounties;

  function transfer(address from, address to, address token, uint tokenVersion, uint depositAmount) private{
    require(depositAmount > 0); // Contributions of 0 tokens or token ID 0 should fail

    if (tokenVersion == 0){
      if(from==address(this)){
        address payable toPayable=address(uint160(to));
        toPayable.send(depositAmount);
      }else{
        require(msg.value >= depositAmount);
      }

    } else if (tokenVersion == 20){

      require(msg.value == 0); // Ensures users don't accidentally send ETH alongside a token contribution, locking up funds

      require(IERC20(token).transferFrom(from,to,depositAmount));
    } else if (tokenVersion == 721){
      require(msg.value == 0); // Ensures users don't accidentally send ETH alongside a token contribution, locking up funds
      IERC721(token).transferFrom(from,to,depositAmount);
    } else {
      revert();
    }
  }

  /**
      This function is for inviting more arbiters, in case nobody
      accepted in the first initial of invites.
  */
  function inviteArbiters(address[] memory arbiters, uint fee, uint bountyId) public payable {
    address token=bounties[bountyId].token;
    uint tokenVersion=bounties[bountyId].tokenVersion;

    // If paying in eth make sure you send enough funds for paying all arbiters
    if(tokenVersion==0) require(msg.value==fee*arbiters.length,"Insuficien funds");

    // NOTE : shoulud we check that bountyId exists in StandardBounties ?
    for(uint i = 0; i < arbiters.length; i ++){
      // transfer fee to this contract so we can transfer it to arbiter when
      // invitation gets accepted
      transfer(msg.sender,address(this), token, tokenVersion, fee);

      arbitersFees[arbiters[i]][bountyId]=fee;
    }
  }

  /**
     This function creates a bounty in StandardBouties contract,
     passing as issuers addresses of this contract and sender's
     address. Also it stores addresses of invited arbiters (approvers)
     and arbiter's fee for created bounty.
  */
  function issueAndContribute(string memory bountyData, uint deadline, address token, uint tokenVersion, uint depositAmount) public payable{
    address[] memory arbiters=new address [](0);

    // EthlanceBountyIssuer is the issuer of all bounties
    address payable[] memory issuers=new address payable[](1);

    address payable thisPayable=address(uint160(address(this)));
    issuers[0]=thisPayable;

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

    bounties[bountyId]=BountyParams(token, tokenVersion);

  }

  /**
     Arbiter runs this function to accept invitation. If he's first, it'll
     transfer fee to him and it'll add him as arbiter for the bounty.
  */
  function acceptArbiterInvitation(uint bountyId) public {
    // check that it was invited
    require(arbitersFees[msg.sender][bountyId] > 0);

    address[] memory arbiters=new address [](1);
    arbiters[0]=msg.sender;

    standardBounties.addApprovers(address(this),
                                  bountyId,
                                  0, // since there is only one issuer, it is the first one
                                  arbiters);

    uint fee=arbitersFees[msg.sender][bountyId];
    address token=bounties[bountyId].token;
    uint tokenVersion=bounties[bountyId].tokenVersion;

    transfer(address(this), msg.sender, token, tokenVersion, fee);

  }

}
