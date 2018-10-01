pragma solidity ^0.4.24;

/// @title Ethlance User with Candidate / Employer / Arbiter Creation
///        and retrieval
/// @dev  
contract EthlanceUserFactory {
    struct User {
	address user_address;
	string metahash_ipfs;
    }

    struct Candidate {
	uint user_id;
	uint64 hourly_rate; // wei or cents
	uint16 currency_type; // 0: Ethereum, 1: USD, ...
    }

    struct Employer {
	uint user_id;
    }

    struct Arbiter {
	uint user_id;
	uint payment_value; // wei or cents / 1-100 for percentage
	uint16 currency_type; // 0: Ethereum, 1: USD, ...
	uint16 type_of_payment; // 0: Flat Rate, 1: Percentage
    }

    User[] user_listing;
    mapping(address => uint) user_address_mapping;

    Candidate[] candidate_listing;
    mapping(address => uint) candidate_address_mapping;

    Employer[] employer_listing;
    mapping(address => uint) employer_address_mapping;
    
    Arbiter[] arbiter_listing;
    mapping(address => uint) arbiter_address_mapping;

    //
    // Methods
    //

    /// @dev Create User for the given address
    /// @param _address Address to the create the user for.
    /// @param _metahash IPNS metahash.
    function createUser(address _address, string _metahash)
	internal returns (uint) {
	require(user_address_mapping[_address] == 0,
		"Given address already has a registered user.");

	User memory user = User(_address, _metahash);

	user_listing.push(user);
	user_address_mapping[_address] = user_listing.length;

	return user_listing.length;
    }

    /// @dev Updates the IPNS metahash endpoint.
    /// @param _id User Id for the given user.
    /// @param _metahash Updated IPNS metahash.
    function updateUser_metahash(uint _id, string _metahash)
	public {
	require(_id <= user_listing.length,
		"Given user id is out of the user_listing range.");
	user_listing[_id].metahash_ipfs = _metahash;
    }

    /// @dev Create Candidate for the given address
    /// @dev Note: Requires that the address contain a registered user.
    /// @param _address The address to create a candidate for.
    /// @param hourly_rate Based on currency, the hourly suggested
    /// amount for payment.
    /// @param currency_type The type of currency to be paid in.
    /// @return The candidate_id of the entry within the candidate_listing.
    function createCandidate(address _address,
			     uint64 hourly_rate,
			     uint16 currency_type)
      internal returns (uint) {
	require(user_address_mapping[_address] != 0,
		"Given address needs to be a registered user.");
	require(candidate_address_mapping[_address] == 0,
		"Given address is already a registered Candidate.");

	uint user_id = user_address_mapping[_address];
	Candidate memory candidate = Candidate({
	    user_id: user_id,
	    hourly_rate: hourly_rate,
	    currency_type: currency_type
        });
	
	candidate_listing.push(candidate);
	candidate_address_mapping[_address] = candidate_listing.length;

	return candidate_listing.length;
    }

    //
    // Views
    //

    /// @dev Returns IPNS metahash for the given `_id`
    /// @param _id User Id for the given user
    /// @return The IPNS metahash for the given user
    function getUserByID(uint _id) public view returns(string _metahash) {
	require(_id <= user_listing.length,
		"Given user id is out of the user_listing range.");
	
	User memory user = user_listing[_id];

	_metahash =  user.metahash_ipfs;
    }
    
    /// @dev Returns the address of the given User ID
    /// @param _id User Id for the given user
    function getUserAddressByID(uint _id) public view returns(address _address) {
	require(_id <= user_listing.length,
		"Given user id is out of the user_listing range.");
	User memory user = user_listing[_id];
	
	_address = user.user_address;
    }

    /// @dev Returns IPNS metahash for the given address
    /// @param _address The address of the user.
    /// @return The IPNS metahash for the given user.
    function getUserByAddress(address _address)
	public view
	returns(string _metahash) {
	require(user_address_mapping[_address] != 0,
		"Given user address is not registered.");

	uint user_id = user_address_mapping[_address];
	User memory user = user_listing[user_id];

	_metahash = user.metahash_ipfs;
    }
    

    /// @dev Returns the user IPNS metahash for the current address
    /// @return The IPNS metahash for current user's data.
    function getCurrentUser() public view returns (string _metahash) {
	require(user_address_mapping[msg.sender] != 0,
		"Current user is not registered.");
	
	uint user_id = user_address_mapping[msg.sender];
	User memory user = user_listing[user_id];
	
	_metahash = user.metahash_ipfs;
    }


    /// @dev Returns the number of users.
    /// @return The number of users.
    function getUserCount()
	public view returns (uint) {

	return user_listing.length;
    }

    
    function getCandidateByAddress(address _address)
	public view returns (uint user_id,
			     uint64 hourly_rate,
			     uint16 currency_type) {
	require(candidate_address_mapping[_address] != 0,
		"Given address is not a registered candidate.");

        uint candidate_id = candidate_address_mapping[_address];
	Candidate memory candidate = candidate_listing[candidate_id];
	
	user_id = candidate.user_id;
	hourly_rate = candidate.hourly_rate;
	currency_type = candidate.currency_type;
    }

    /// @dev FIXME
    /// @param candidate_id The candidate ID within the candidate_listing.
    /// @return FIXME
    function getCandidateById(uint candidate_id)
	public view returns (uint user_id,
			     uint64 hourly_rate,
			     uint16 currency_type) {
	
    }
    
    /// @dev Returns the number of candidates in ethlance.
    /// @return Number of candidates
    function getCandidateCount()
	public view returns (uint) {
	return candidate_listing.length;
    }

    //
    // Modifiers
    //

    modifier isRegisteredUser {
	_;
    }
}
