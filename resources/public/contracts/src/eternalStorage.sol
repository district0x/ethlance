pragma solidity ^0.4.4;

import "Ownable.sol";
import "safeMath.sol";

contract EternalStorage is Ownable {

    function EternalStorage(){
    }
    
    mapping(bytes32 => uint8) UInt8Storage;
    
    function getUInt8Value(bytes32 record) constant returns (uint8){
        return UInt8Storage[record];
    }

    function setUInt8Value(bytes32 record, uint8 value)
    onlyOwner
    {
        UInt8Storage[record] = value;
    }

    function deleteUInt8Value(bytes32 record)
    onlyOwner
    {
      delete UInt8Storage[record];
    }

    mapping(bytes32 => uint16) UInt16Storage;

    function getUInt16Value(bytes32 record) constant returns (uint16){
        return UInt16Storage[record];
    }

    function setUInt16Value(bytes32 record, uint16 value)
    onlyOwner
    {
        UInt16Storage[record] = value;
    }

    function deleteUInt16Value(bytes32 record)
    onlyOwner
    {
      delete UInt16Storage[record];
    }

    mapping(bytes32 => uint) UIntStorage;

    function getUIntValue(bytes32 record) constant returns (uint){
        return UIntStorage[record];
    }

    function setUIntValue(bytes32 record, uint value)
    onlyOwner
    {
        UIntStorage[record] = value;
    }

    function addUIntValue(bytes32 record, uint value)
    onlyOwner
    {
        UIntStorage[record] = SafeMath.safeAdd(UIntStorage[record], value);
    }

    function subUIntValue(bytes32 record, uint value)
    onlyOwner
    {
        UIntStorage[record] = SafeMath.safeSub(UIntStorage[record], value);
    }

    function deleteUIntValue(bytes32 record)
    onlyOwner
    {
      delete UIntStorage[record];
    }

    mapping(bytes32 => string) StringStorage;

    function getStringValue(bytes32 record) constant returns (string){
        return StringStorage[record];
    }

    function setStringValue(bytes32 record, string value)
    onlyOwner
    {
        StringStorage[record] = value;
    }

    function deleteStringValue(bytes32 record)
    onlyOwner
    {
      delete StringStorage[record];
    }

    mapping(bytes32 => address) AddressStorage;

    function getAddressValue(bytes32 record) constant returns (address){
        return AddressStorage[record];
    }

    function setAddressValue(bytes32 record, address value)
    onlyOwner
    {
        AddressStorage[record] = value;
    }

    function deleteAddressValue(bytes32 record)
    onlyOwner
    {
      delete AddressStorage[record];
    }

    mapping(bytes32 => bytes) BytesStorage;

    function getBytesValue(bytes32 record) constant returns (bytes){
        return BytesStorage[record];
    }

    function setBytesValue(bytes32 record, bytes value)
    onlyOwner
    {
        BytesStorage[record] = value;
    }

    function deleteBytesValue(bytes32 record)
    onlyOwner
    {
      delete BytesStorage[record];
    }

    mapping(bytes32 => bytes32) Bytes32Storage;

    function getBytes32Value(bytes32 record) constant returns (bytes32){
        return Bytes32Storage[record];
    }

    function setBytes32Value(bytes32 record, bytes32 value)
    onlyOwner
    {
        Bytes32Storage[record] = value;
    }

    function deleteBytes32Value(bytes32 record)
    onlyOwner
    {
      delete Bytes32Storage[record];
    }

    mapping(bytes32 => bool) BooleanStorage;

    function getBooleanValue(bytes32 record) constant returns (bool){
        return BooleanStorage[record];
    }

    function setBooleanValue(bytes32 record, bool value)
    onlyOwner
    {
        BooleanStorage[record] = value;
    }

    function deleteBooleanValue(bytes32 record)
    onlyOwner
    {
      delete BooleanStorage[record];
    }

    mapping(bytes32 => int) IntStorage;

    function getIntValue(bytes32 record) constant returns (int){
        return IntStorage[record];
    }

    function setIntValue(bytes32 record, int value)
    onlyOwner
    {
        IntStorage[record] = value;
    }

    function deleteIntValue(bytes32 record)
    onlyOwner
    {
      delete IntStorage[record];
    }
}