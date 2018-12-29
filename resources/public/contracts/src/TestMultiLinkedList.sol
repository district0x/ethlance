pragma solidity ^0.5.0;


import "./collections/MultiLinkedList.sol";


/// @title Test the MultiLinkedList implementation
contract TestMultiLinkedList is MultiLinkedList {
    /// @dev public function for MultiLinkedList
    function push(bytes32 _bkey, address _contract) external {
	_push(_bkey, _contract);
    }


    /// @dev public function for MultiLinkedList
    function insert(bytes32 _bkey, uint _index, address _contract) external {
	_insert(_bkey, _index, _contract);
    }

    /// @dev public function for MultiLinkedList
    function remove(bytes32 _bkey, uint _index) external {
	_remove(_bkey, _index);
    }
}
