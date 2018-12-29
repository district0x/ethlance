pragma solidity ^0.5.0;
/*
  Title:
  Multiple Linked List Storage

  Description:
  Dynamic Storage Contract for storing multiple linked lists each
  uniquely identified by a key. Implementation allows for
  inexpensive insertions and deletions. Implementation can be
  expanded upon for additional guarantees.

  Author:
  Benjamin Zaporzan

  Email:
  ben@district0x.io

  Advantages:
  - inexpensive insertion and deletion
  - dynamic storage, similar to eternalDB
  - can be adapted to work with other types besides address
    
  - implementation is rather simple, and could be expanded on for
  additional performance gains (DoublyLinked Nodes for fast
  reverse iteration, storing count in mapping, etc)
  
  Disadvantages:
  - not indexed, but iterable alleviates performance costs.
  - with iterable, it requires you to keep track of the iterable to
  retrieve values in a performant manner.

  Example:
  // Initializing, please see ./TestMultiLinkedList.sol
  TestMultiLinkedList mlist = new TestMultiLinkedList();
  
  // Create keys for your linked lists
  bytes ukey = keccak256("Users");
  bytes jkey = keccak256("Jobs");
  // Fill our linked list defined by the key
  // Mock Data
  mlist.push(ukey, 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef);
  mlist.push(ukey, 0xABABABABABABABABABABABABABABABABABABABBA);
  mlist.push(ukey, 0xACABACABACABABACABABACABABACABABABABACCA);
  mlist.push(jkey, 0xDABDABDABDDABDABDABDABDABDABDABDABDABDAD);
  mlist.push(jkey, 0xABBAABBAABBAABBAABBAABBAABBAABAABABABABA);
  mlist.push(ukey, 0xCABCABCABCABCABCABCABCABCABCABCABCABCABC);
  // Get the length of each list
  mlist.count(ukey) // 4
  mlist.count(jkey) // 2
  // Grab elements from the list
  mlist.nth(ukey, 0) // 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef
  mlist.nth(ukey, 2) // 0xACABACABACABABACABABACABABACABABABABACCA
  mlist.nth(ukey, 3) // 0xCABCABCABCABCABCABCABCABCABCABCABCABCABC
  
  // Iterate over list elements
  uint iter = mlist.iterStart(ukey);
  while(iter != 0) {
  user = mlist.value(iter);
  //
  // do stuff with user
  //
  // go to the next iteration
  iter = mlist.next(iter);
  }
  // For Loop iteration
  for(uint iter = mlist.iterStart(ukey); iter != 0; iter = mlist.next(iter)) {
  user = mlist.value(iter);
  //do stuff
  }
  // Remove List Elements
  mlist.count(ukey); // 4
  mlist.remove(ukey, 1);
  mlist.count(ukey); // 3
  mlist.nth(ukey, 1); // 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef
  mlist.nth(jkey, 2); // ERROR: Index out of bounds.
  // Insert List Elements
  mlist.insert(ukey, 2, 0xFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA);
  mlist.count(ukey); // 4
  mlist.nth(ukey, 2); // 0xFAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
*/

/// @title Multiple Linked List Storage
/// @dev Useful for storing multiple collections of addresses defined
/// by a unique key.
contract MultiLinkedList {
    
    // Main Node structure
    struct Node {
        address data; // The address data
        uint next;    // Next node pointer index
    }
    
    // Node Element representing null value
    Node NULL = Node(address(0), 0);
    
    // Main collection
    Node[] private linked_list;

    // Mappings to the beginning and end of our collection chains,
    // where bytes is our 'key' pointing at the head and tail elements
    // of each unique linked list.
    mapping(bytes32 => uint) private head_node_mapping;
    mapping(bytes32 => uint) private tail_node_mapping;
    
    /// @dev MultiLinkedList constructor
    constructor() internal {
        // We place a dummy value at the beginning of our linked_list
        // This lets us treat index 0 as null.
        if (linked_list.length <= 0) {
            linked_list.push(NULL);
        }
    }


    /// @dev Push a value onto the end of the the linked list defined by `bkey`
    /// @param bkey The key representing a unique linked list.
    /// @param _data The address we are storing in the linked list.
    /// @return The array index where the data is stored.
    function _push(bytes32 bkey, address _data) internal returns(uint) {
        // Create the start of our linked_list chain
        if (head_node_mapping[bkey] == 0) {
            uint start_index = linked_list.length;
            head_node_mapping[bkey] = start_index;
            tail_node_mapping[bkey] = start_index;
            linked_list.push(Node(_data, 0));
            return start_index;
        }
        // Extend our linked_list, make sure the previous
        // chain points at our inserted link.
        else {
            uint prev_index = tail_node_mapping[bkey];
            uint next_index = linked_list.length;
            linked_list[prev_index].next = next_index;
            linked_list.push(Node(_data, 0));
            tail_node_mapping[bkey] = next_index;
            return next_index;
        }
    }


    /// @dev Insert element at given location
    /// @param bkey The key representing the unique linked list.
    /// @param index The index to perform the insertion (start of slice)
    /// @param _data The data to insert at the given location
    /// @return The array index of the insertion
    function _insert(bytes32 bkey, uint index, address _data) internal returns (uint) {
        uint ncount = count(bkey);
        uint new_index;
        require(index >= ncount, "Index out of bounds.");

        // We push if the insert is at the end of the index
        if (ncount == index) {
            return _push(bkey, _data);
        }

        // Simpler insertion if it's at the beginning
        else if (index == 0) {
            uint current_head_index = head_node_mapping[bkey];
            new_index = linked_list.length;
            Node memory node = Node(_data, current_head_index);
            linked_list.push(node);
            head_node_mapping[bkey] = new_index;
            return new_index;
        }

        // Standard Insertion, iterate till we get
        // to element before the insertion
        uint current_index = head_node_mapping[bkey];
        uint icount = 0;
        Node storage prev_node = linked_list[current_index];
        while(icount != index - 1) {
            prev_node = linked_list[prev_node.next];
            icount++;
        }

        // Create a new node, and point it at the next node
        Node memory new_node = Node(_data, prev_node.next);
        new_index = linked_list.length;
        linked_list.push(new_node);
        
        // Point our previous index at our new next index
        prev_node.next = new_index;

        return new_index;
    }

    
    /// @dev Get the number of elements in a unique linked list.
    /// @param bkey The key representing a unique linked list.
    /// @return The number of elements in the linked list defined by `bkey`.
    function count(bytes32 bkey) public view returns (uint) {
        uint current_index = head_node_mapping[bkey];
        if (current_index == 0) {
            return 0;
        }
        
        uint len = 1;
        Node memory node = linked_list[current_index];
        while(node.next != 0) {
            node = linked_list[node.next];
            len++;
        }
        
        return len;
    }

    
    /// @dev Get the 'nth' element of the unique linked list.
    /// @param bkey The key representing a unique linked list.
    /// @param index The 'nth' value to get.
    /// @return The data at the 'nth' location.
    function nth(bytes32 bkey, uint index) public view returns (address) {
        require(index < count(bkey), "Index out of bounds.");
        uint current_index = head_node_mapping[bkey];
        uint icount = 0;
        Node memory node = linked_list[current_index];
        while(icount != index) {
            node = linked_list[node.next];
            icount++;
        }
        
        return node.data;
    }

    
    /// @dev Remove the element at `index`
    /// @param bkey The key representing a unique linked list
    /// @param index The element at the given index
    function _remove(bytes32 bkey, uint index) internal {
        uint ncount = count(bkey);
        require(index < ncount, "Index out of bounds.");
        uint current_index = head_node_mapping[bkey];

        // Handle specific situation where we're removing from the
        // beginning of a list.
        if (index == 0) {
            head_node_mapping[bkey] = linked_list[current_index].next;
            return;
        }
        
        uint icount = 0;
        Node storage pnode = linked_list[current_index];
        while(icount != index - 1) {
            current_index = pnode.next;
            pnode = linked_list[current_index];
            icount++;
        }
            
        if (ncount == index + 1) {
            tail_node_mapping[bkey] = current_index;
            pnode.next = 0;
        }
        else {
            Node memory inode = linked_list[pnode.next];
            Node memory nnode = linked_list[inode.next];
            pnode.next = nnode.next;
        }
    }


    /// @dev Start of iteration loop.
    /// @param bkey The key representing a unique linked list.
    /// @return The first element of the linked_list, or the NULL element.
    function iterStart(bytes32 bkey) public view returns (uint) {
        return head_node_mapping[bkey];
    }

    /// @dev End of iteration loop
    function iterEnd(bytes32 bkey) public view returns (uint) {
        return tail_node_mapping[bkey];
    }

    /// @dev Get the value from the current iteration
    /// @param index The iterator index value supplied by either iterStart(), or
    ///              by iterEnd().
    /// @return The value stored at the given iterator index.
    function value(uint index) public view returns (address) {
	return linked_list[index].data;
    }


    /// @dev Retrieves the next element in the linked list defined by
    /// the next element pointer `index`.
    /// @param index The index of the next element in the linked list.
    /// @return The next iterator index of the linked_list, or the
    /// NULL value (0) if it is the end of the list.
    function next(uint index) public view returns(uint) {
	return linked_list[index].next;
    }


    /// @dev Retrieves the first element in the linked list defined by
    /// the next element pointer `index`.
    /// @param bkey The key representing a unique linked list.
    /// @return The iterator index of the linked_list, or the
    /// NULL value (0) if it is the end of the list.
    function first(bytes32 bkey) public view returns(uint) {
	return iterStart(bkey);
    }
    
    
    /// @dev Retrieves the second element in the linked list defined by
    /// the next element pointer `index`.
    /// @param bkey The key representing a unique linked list.
    /// @return The iterator index of the linked_list, or the
    /// NULL value (0) if it is the end of the list.
    function second(bytes32 bkey) public view returns(uint) {
	return next(iterStart(bkey));
    }


    /// @dev Retrieves the last element in the linked list defined by
    /// the next element pointer `index`.
    /// @param bkey The key representing a unique linked list.
    /// @return The iterator index of the linked_list, or the
    /// NULL value (0) if the list is empty.
    function last(bytes32 bkey) public view returns(uint) {
	return iterEnd(bkey);
    }


    /// @dev Get the first value, same as value(iter(bkey))
    /// @param bkey The key representing a unique linked list.
    /// @return The first element of the linked_list, or the NULL element.
    function firstValue(bytes32 bkey) public view returns (address) {
        return value(iterStart(bkey));
    }

    
    /// @dev Get the second value, same as value(next(iter(bkey)))
    /// @param bkey The key representing a unique linked list.
    /// @return The second element of the linked_list, or the NULL element.
    function secondValue(bytes32 bkey) public view returns (address) {
        return value(next(iterStart(bkey)));
    }

    
    /// @dev Get the last value
    /// @param bkey The key representing a unique linked list.
    /// @return The last element of the linked_list, or the NULL element.
    function lastValue(bytes32 bkey) public view returns (address) {
        return value(tail_node_mapping[bkey]);
    }
    
}


/// @title MultiLinkedList Interface to implement a public function
/// with correct authorizations within the given ecosystem.
/*
  FIXME: issues when implementing interface. Workaround is to
  implement manually without inheriting the interface.
 */
interface IMultiLinkedList {
    /// @dev Should call and return MultiLinkedList._push(_bkey, _contract)
    function push(bytes32 _bkey, address _contract) external returns(uint);

    /// @dev Should call and return MultiLinkedList._insert(_bkey, _index, _contract)
    function insert(bytes32 _bkey, uint _index, address _contract) external returns(uint);

    /// @dev Should call MultiLinkedList._remove(_bkey, _index)
    function remove(bytes32 _bkey, uint _index) external;
}
