pragma solidity ^0.5.0;

/// @title Used to separate candidate, arbiter and employer IPFS
/// metahash values
contract MetahashStore {
    // A metahash with user identifier
    struct HashEntry {
	uint user_type;    // Enumeration
	uint timestamp;    // Timestamp on creation
	string hash_value; // IPFS Hash String
    }

    uint public constant GUEST_TYPE = 0;
    uint public constant EMPLOYER_TYPE = 1;
    uint public constant CANDIDATE_TYPE = 2;
    uint public constant ARBITER_TYPE = 3;

    
    // Holds the listing of metahash entries
    HashEntry[] internal hash_listing;


    /// @dev Append an guest hash to the hash listing.
    function appendGuest(string memory hash_value) internal {
	hash_listing.push(HashEntry(GUEST_TYPE, now, hash_value));
    }

    
    /// @dev Append an employer hash to the hash listing.
    function appendEmployer(string memory hash_value) internal {
	hash_listing.push(HashEntry(EMPLOYER_TYPE, now, hash_value));
    }

    
    /// @dev Append a candidate hash to the hash listing.
    function appendCandidate(string memory hash_value) internal {
	hash_listing.push(HashEntry(CANDIDATE_TYPE, now, hash_value));
    }

    
    /// @dev Append an arbiter hash to the hash listing.
    function appendArbiter(string memory hash_value) internal {
	hash_listing.push(HashEntry(ARBITER_TYPE, now, hash_value));
    }
    

    /// @dev Return the number of hashes within the hash listing.
    function getHashCount() external view returns(uint) {
	return hash_listing.length;
    }

    
    /// @dev Get the (user type, hash value) for the hash at the given index.
    function getHashByIndex(uint index)
	external view returns(uint user_type, uint timestamp, string memory hash_value) {
	HashEntry memory entry = hash_listing[index];
	user_type = entry.user_type;
	timestamp = entry.timestamp;
	hash_value = entry.hash_value;
    }

}
