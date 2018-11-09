pragma solidity ^0.4.24;

/// @title Used to separate candidate, arbiter and employer IPFS
/// metahash values
library MetahashStore {
    // A metahash
    struct HashEntry {
	uint user_type;
	string hash_value;
    }


    struct HashListing {
	HashEntry[] listing;
    }

    uint public constant EMPLOYER_TYPE = 1;
    uint public constant CANDIDATE_TYPE = 2;
    uint public constant ARBITER_TYPE = 3;

    
    function appendEmployer(HashListing storage self, string hash_value) internal {
	self.listing.push(HashEntry(EMPLOYER_TYPE, hash_value));
    }

    
    function appendCandidate(HashListing storage self, string hash_value) internal {
	self.listing.push(HashEntry(CANDIDATE_TYPE, hash_value));
    }

    
    function appendArbiter(HashListing storage self, string hash_value) internal {
	self.listing.push(HashEntry(ARBITER_TYPE, hash_value));
    }
    
    
    function getCount(HashListing storage self) external view returns(uint) {
	return self.listing.length;
    }

    
    function getByIndex(HashListing storage self, uint index)
	external view returns(uint user_type, string hash_value) {
	HashEntry memory entry = self.listing[index];
	user_type = entry.user_type;
	hash_value = entry.hash_value;
    }
}
