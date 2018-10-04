pragma solidity ^0.4.24;

import "./EthlanceEventDispatcher.sol";
import "./EthlanceUser.sol";
import "proxy/MutableForwarder.sol";
import "proxy/Forwarder.sol";

/// @title Ethlance User Factory
/// @dev Used for the creation of users, along with the relation to
/// Candidates, Employers and Arbiters.
contract EthlanceUserFactory {
    uint public constant version = 1;
    EthlanceEventDispatcher public constant event_dispatcher = EthlanceEventDispatcher(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);

    EthlanceUser[] user_listing;
    mapping(address => uint) user_address_mapping;

    //
    // Methods
    //

    /// @dev Fire events specific to the UserFactory
    /// @param event_name Unique to give the fired event
    /// @param event_data Additional event data to include in the
    /// fired event.
    function fireEvent(string event_name, uint[] event_data) private {
	event_dispatcher.fireEvent(event_name, version, event_data);
    }


    /// @dev Create User for the given address
    /// @param _address Address to the create the user for.
    /// @param _metahash IPFS metahash.
    function createUser(address _address, string _metahash)
	// FIXME: isAuthorized
	public returns (uint) {
	require(user_address_mapping[_address] == 0,
		"Given address already has a registered user.");

	address user_fwd = new Forwarder(); // Proxy Contract with
					    // target(EthlanceUser)
	EthlanceUser user = EthlanceUser(address(user_fwd));
	user.construct(event_dispatcher, _address, _metahash);

	user_listing.push(user);
	user_address_mapping[_address] = user_listing.length;

	uint[] memory edata = new uint[](1);
	edata[0] = user_listing.length;
	fireEvent("UserFactoryCreateUser", edata);

	return user_listing.length;
    }


    //
    // Views
    //


    /// @dev Returns IPFS metahash for the given `user_id`
    /// @param user_id User Id for the given user
    /// @return The IPFS metahash for the given user
    function getUserByID(uint user_id)
	public view returns(EthlanceUser) {
	require(user_id <= user_listing.length,
		"Given user id index is out of bounds.");
	
	EthlanceUser user = user_listing[user_id];

	return user;
    }


    /// @dev Returns the address of the given User ID
    /// @param user_id User Id for the given user
    function getUserAddressByID(uint user_id)
	public view returns(address _address) {
	require(user_id <= user_listing.length,
		"Given user id is out of the user_listing range.");
	EthlanceUser user = user_listing[user_id];
	
	_address = user.user_address();
    }


    /// @dev Returns IPFS metahash for the given address
    /// @param _address The address of the user.
    /// @return The IPFS metahash for the given user.
    function getUserByAddress(address _address)
	public view returns(EthlanceUser) {
	require(user_address_mapping[_address] != 0,
		"Given user address is not registered.");

	uint user_id = user_address_mapping[_address];
	EthlanceUser user = user_listing[user_id];

	return user;
    }


    /// @dev Returns the user IPFS metahash for the current address
    /// @return The IPFS metahash for current user's data.
    function getCurrentUser() public view returns (EthlanceUser) {
	require(user_address_mapping[msg.sender] != 0,
		"Current user is not registered.");
	
	uint user_id = user_address_mapping[msg.sender];
	EthlanceUser user = user_listing[user_id];
	
	return user;
    }


    /// @dev Returns the number of users.
    /// @return The number of users.
    function getUserCount()
	public view returns (uint) {

	return user_listing.length;
    }


    //
    // Modifiers
    //


    /// @dev Checks if the given address is a registered User.
    modifier isRegisteredUser(address _address) {
	require(user_address_mapping[_address] != 0,
		"Given address identity is not a registered User.");
	_;
    }
}
