pragma solidity ^0.5.0;

import "./EthlanceRegistry.sol";
import "./EthlanceJobStore.sol";
import "./EthlanceUserFactory.sol";
import "./EthlanceUser.sol";
import "./proxy/MutableForwarder.sol";
import "./proxy/Forwarder.sol";

/// @title For creation of Job Contracts.
contract EthlanceJobFactory {
  uint public constant version = 1;
  EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

  //
  // Methods
  //

  /// @dev Fire events specific to the JobFactory
  /// @param eventName Unique to give the fired event
  /// @param eventData Additional event data to include in the
  /// fired event.
  function fireEvent(string memory eventName, uint[] memory eventData) private {
    registry.fireEvent(eventName, version, eventData);
  }


  /// @dev Create Job Contract for given user defined by
  /// 'employer_user_id'. Note that parameters are described in
  /// EthlanceJob contract.
  function createJobStore(uint8 bidOption,
                          uint estimatedLengthSeconds,
                          bool includeEtherToken,
                          bool isInvitationOnly,
                          string memory metahash,
                          uint rewardValue)
    public {
    require(registry.isRegisteredEmployer(msg.sender),
            "You are not a registered employer.");

    // TODO: bounds on parameters

    Forwarder fwd = new Forwarder(); // Proxy Contract with
    // target(EthlanceJobStore)
    EthlanceJobStore jobStore = EthlanceJobStore(address(fwd));

    // Permit JobStore to fire registry events
    registry.permitDispatch(address(fwd));

    uint jobIndex = registry.pushJobStore(address(jobStore));
    jobStore.construct(jobIndex,
                       msg.sender,
                       bidOption,
                       estimatedLengthSeconds,
                       includeEtherToken,
                       isInvitationOnly,
                       metahash,
                       rewardValue);
  
    // Create and Fire off event data
    uint[] memory edata = new uint[](1);
    edata[0] = jobIndex;
    fireEvent("JobStoreCreated", edata);
  }

  //
  // Views
  //
    
  function getJobStoreCount()
    public view returns(uint) {
    return registry.getJobStoreCount();
  }

  /// @dev Get the job address at `index` within the job listing
  /// @param index The index of the job address within the job listing.
  /// @return The address of the given index
  function getJobStoreByIndex(uint index)
    public view returns (address)
  {
    return registry.getJobStoreByIndex(index);
  }
}
