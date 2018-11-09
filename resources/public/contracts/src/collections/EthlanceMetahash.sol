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

    uint constant EMPLOYER_TYPE = 1;
    uint constant CANDIDATE_TYPE = 2;
    uint constant ARBITER_TYPE = 3;


    function append(HashListing storage self, uint user_type, string hash_value) private {
	self.listing.push(HashEntry(user_type, hash_value));
    }

    
    function appendEmployer(HashListing storage self, string hash_value) internal {
	append(self, EMPLOYER_TYPE, hash_value);
    }

    
    function appendCandidate(HashListing storage self, string hash_value) internal {
	append(self, CANDIDATE_TYPE, hash_value);
    }

    
    function appendArbiter(HashListing storage self, string hash_value) internal {
	append(self, ARBITER_TYPE, hash_value);
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
