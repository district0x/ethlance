pragma solidity ^0.4.24;

import "./EthlanceRegistry.sol";
import "./EthlanceUser.sol";
import "proxy/MutableForwarder.sol";
import "proxy/Forwarder.sol";

/// @title Ethlance User Factory
/// @dev Used for the creation of users, along with the relation to
/// Candidates, Employers and Arbiters.
contract EthlanceUserFactory {
    uint public constant version = 1;
    EthlanceRegistry public constant registry = EthlanceRegistry(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    //
    // Methods
    //

    /// @dev Fire events specific to the UserFactory
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string event_name, uint[] event_data) private {
	registry.fireEvent(event_name, version, event_data);
    }


    /// @dev Register user for the current address.
    /// @param _metahash IPFS metahash.
    function registerUser(string _metahash)
	public {
	require(registry.getUserByAddress(msg.sender) == 0x0,
		"Given user is already registered.");

	address user_fwd = new Forwarder(); // Proxy Contract with
					    // target(EthlanceUser)
	EthlanceUser user = EthlanceUser(address(user_fwd));

	// Note: contract address needs to be registered before it can
	// be constructed due to permission checks.
	uint user_id = registry.pushUser(msg.sender, address(user));
	user.construct(user_id, msg.sender, _metahash);

	// Create and Fire off event data
	uint[] memory edata = new uint[](1);
	edata[0] = user_id;
	fireEvent("UserRegistered", edata);
    }


    //
    // Views
    //


    /// @dev Returns IPFS metahash for the given `user_id`
    /// @param user_id User Id for the given user
    /// @return The EthlanceUser address.
    function getUserByID(uint user_id)
	public view returns(EthlanceUser) {
	require(user_id <= registry.getUserCount(),
		"Given user_id is out of bounds.");
	
	// Note: user_id is +1 of the index.
	EthlanceUser user = EthlanceUser(registry.getUserByIndex(user_id-1));

	return user;
    }


    /// @dev Returns IPFS metahash for the given address
    /// @param _address The address of the user.
    /// @return The EthlanceUser address.
    function getUserByAddress(address _address)
	public view
	registeredUser(_address)
	returns(EthlanceUser)
    {
	EthlanceUser user = EthlanceUser(registry.getUserByAddress(_address));

	return user;
    }


    /// @dev Returns the current User Contract Address
    /// @return The current user contract address.
    function getCurrentUser()
	public view
	registeredUser(msg.sender)
	returns (EthlanceUser)
    {
	EthlanceUser user = EthlanceUser(registry.getUserByAddress(msg.sender));
	
	return user;
    }


    /// @dev Returns the number of users.
    /// @return The number of users.
    function getUserCount()
	public view returns (uint) {

	return registry.getUserCount();
    }


    /// @dev Returns true, if the given user address is registered.
    /// @param _address The address of the user.
    /// @return True, if the address is registered.
    function isRegisteredUser(address _address)
	public view returns(bool) {
	if (getUserCount() == 0) {
	    return false;
	}
	return registry.getUserByAddress(_address) != 0x0;
    }


    //
    // Modifiers
    //


    /// @dev Checks if the given address is a registered User.
    modifier registeredUser(address _address) {
	require(isRegisteredUser(_address),
		"Given address identity is not a registered User.");
	_;
    }
}
