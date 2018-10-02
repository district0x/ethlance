pragma solidity ^0.4.24;

/// @title Ethlance User Factory
/// @dev Used for the creation of users, along with the relation to
/// Candidates, Employers and Arbiters.
contract EthlanceUserFactory {
    struct User {
	address user_address;
	uint date_created;
	uint date_updated;
	string metahash_ipfs;
    }

    struct Candidate {
	uint user_id;
	uint64 hourly_rate; // In units of currency
	uint16 currency_type; // 0: Ethereum, 1: USD, ...
    }

    struct Employer {
	uint user_id;
    }

    struct Arbiter {
	uint user_id;
	uint payment_value; // Based on type_of_payment:
	                    // [0] In units of currency
                            // [1] 1-100 for percentage
	uint16 currency_type; // 0: Ethereum, 1: USD, ...
	uint8 type_of_payment; // 0: Flat Rate, 1: Percentage
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
    /// @param _metahash IPFS metahash.
    function createUser(address _address, string _metahash)
	// FIXME: isAuthorized
	public returns (uint) {
	require(user_address_mapping[_address] == 0,
		"Given address already has a registered user.");

	User memory user = User(_address, now, now, _metahash);

	user_listing.push(user);
	user_address_mapping[_address] = user_listing.length;

	return user_listing.length;
    }


    /// @dev Updates the IPFS metahash endpoint.
    /// @param user_id User Id for the given user.
    /// @param _metahash Updated IPFS metahash.
    function updateUserMetahash(uint user_id, string _metahash)
	// FIXME: isAuthorized
	public {
	require(user_id <= user_listing.length,
		"Given user id is out of the user_listing range.");
	user_listing[user_id].date_updated = now;
	user_listing[user_id].metahash_ipfs = _metahash;
    }


    /// @dev Create Candidate for the given address
    /// @dev Note: Requires that the address is a registered user.
    /// @param _address The address to create a candidate for.
    /// @param hourly_rate Based on currency, the hourly suggested
    /// amount for payment.
    /// @param currency_type The type of currency to be paid in.
    /// @return The candidate_id of the entry within the candidate_listing.
    function createCandidate(address _address,
			     uint64 hourly_rate,
			     uint16 currency_type)
	public
        // FIXME: isAuthorized
	isRegisteredUser(_address)
	returns (uint) {
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


    /// @dev Update Candidate's rate of hourly pay and currency type.
    /// @param candidate_id Candidate ID
    /// @param hourly_rate The rate of hourly pay for a particular currency.
    ///                    For USD, a unit of pay is a cent. For
    ///                    Ethereum, the unit of pay is a wei.
    /// @param currency_type Type of hourly pay. 0 - Eth, 1 - USD.
    function updateCandidateRate(uint candidate_id,
				 uint64 hourly_rate,
				 uint16 currency_type)
	// FIXME: isAuthorized
	public {
	require(candidate_id != 0 && candidate_id <= candidate_listing.length,
		"Given id is out of the candidate_listing bounds.");
	Candidate memory candidate = candidate_listing[candidate_id];
	candidate.hourly_rate = hourly_rate;
	candidate.currency_type = currency_type;
	candidate_listing[candidate_id] = candidate;
    }


    /// @dev Creates an Arbiter for the given address
    /// @param _address The address to create an Arbiter for.
    /// @param payment_value unit of payment based on currency_type
    /// and type_of_payment
    /// @param currency_type Type of currency for the payment value
    ///        0 - ETH, 1 - USD
    /// @param type_of_payment Type of payment that the arbiter takes.
    ///        0 - Flat Rate, 1 - Percentage
    /// @return The arbiter ID of the Arbiter created.
    function createArbiter(address _address,
			   uint payment_value,
			   uint16 currency_type,
			   uint8 type_of_payment)
	//FIXME: isAuthorized
	isRegisteredUser(_address)
	public returns(uint) {
	require(arbiter_address_mapping[_address] == 0,
		"Given address is already an Arbiter.");
	uint user_id = user_address_mapping[_address];
	Arbiter memory arbiter = Arbiter({
	    user_id: user_id,
	    payment_value: payment_value,
	    currency_type: currency_type,
	    type_of_payment: type_of_payment
	});
	arbiter_listing.push(arbiter);
	arbiter_address_mapping[_address] = arbiter_listing.length;
	
	return arbiter_listing.length;
    }


    /// @dev Updates the given arbiter's rate of payment.
    /// @param arbiter_id Arbiter ID
    /// @param payment_value unit of payment based on currency_type
    /// and type_of_payment
    /// @param currency_type Type of currency for the payment value
    ///        0 - ETH, 1 - USD
    /// @param type_of_payment Type of payment that the arbiter takes.
    ///        0 - Flat Rate, 1 - Percentage
    function updateArbiterRate(uint arbiter_id,
			       uint payment_value,
			       uint16 currency_type,
			       uint8 type_of_payment)
	// FIXME: isAuthorized
	public {
	require(arbiter_id != 0 && arbiter_id <= arbiter_listing.length,
		"arbiter_id index out of bounds.");
	Arbiter memory arbiter = arbiter_listing[arbiter_id];
	arbiter.payment_value = payment_value;
	arbiter.currency_type = currency_type;
	arbiter.type_of_payment = type_of_payment;
	arbiter_listing[arbiter_id] = arbiter;
    }


    //
    // Views
    //


    /// @dev Returns IPFS metahash for the given `user_id`
    /// @param user_id User Id for the given user
    /// @return The IPFS metahash for the given user
    function getUserByID(uint user_id)
	public view returns(string _metahash) {
	require(user_id <= user_listing.length,
		"Given user id index is out of bounds.");
	
	User memory user = user_listing[user_id];

	_metahash =  user.metahash_ipfs;
    }


    /// @dev Returns the address of the given User ID
    /// @param user_id User Id for the given user
    function getUserAddressByID(uint user_id)
	public view returns(address _address) {
	require(user_id <= user_listing.length,
		"Given user id is out of the user_listing range.");
	User memory user = user_listing[user_id];
	
	_address = user.user_address;
    }


    /// @dev Returns IPFS metahash for the given address
    /// @param _address The address of the user.
    /// @return The IPFS metahash for the given user.
    function getUserByAddress(address _address)
	public view
	returns(string _metahash) {
	require(user_address_mapping[_address] != 0,
		"Given user address is not registered.");

	uint user_id = user_address_mapping[_address];
	User memory user = user_listing[user_id];

	_metahash = user.metahash_ipfs;
    }


    /// @dev Returns the user IPFS metahash for the current address
    /// @return The IPFS metahash for current user's data.
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


    /// @dev Get the Candidate by the designated address
    /// @param _address The designated address of the Candidate.
    /// @return The Candidate structure in the form of a tuple.
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


    /// @dev Get the Candidate by the candidate_id
    /// @param candidate_id The candidate ID within the candidate_listing.
    /// @return The Candidate structure in the form of a tuple.
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


    /// @dev Checks if the given address is a registered User.
    modifier isRegisteredUser(address _address) {
	require(user_address_mapping[_address] != 0,
		"Given address identity is not a registered User.");
	_;
    }
}
