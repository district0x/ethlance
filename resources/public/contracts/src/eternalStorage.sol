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

    function getEntity(bytes32[] records, uint8[] types)
        public constant returns
    (
        bool[] bools,
        uint8[] uint8s,
        uint[] uints,
        address[] addresses,
        bytes32[] bytes32s,
        int[] ints,
        string str
    )
    {
        for (uint i = 0; i < records.length ; i++) {
            var recordType = types[i];
            var record = records[i];
            if (recordType == 1) {
                bools[bools.length - 1] = getBooleanValue(record);
            } else if (recordType == 2) {
                uint8s[uint8s.length - 1] = getUInt8Value(record);

            } else if (recordType == 3) {
                uints[uints.length - 1] = getUIntValue(record);

            } else if (recordType == 4) {
                addresses[addresses.length - 1] = getAddressValue(record);

            } else if (recordType == 5) {
                bytes32s[bytes32s.length - 1] = getBytes32Value(record);

            } else if (recordType == 6) {
                ints[ints.length - 1] = getIntValue(record);

            } else if (recordType == 7) {
                str = getStringValue(record);
            }
        }
        return (bools, uint8s, uints, addresses, bytes32s, ints, str);
    }

    function getEntityStrings(bytes32[] records)
            public constant returns
    (
        string string1,
        string string2,
        string string3,
        string string4,
        string string5,
        string string6,
        string string7
    )
    {
        string1 = getStringValue(records[0]);
        string2 = getStringValue(records[1]);
        string3 = getStringValue(records[2]);
        string4 = getStringValue(records[3]);
        string5 = getStringValue(records[4]);
        string6 = getStringValue(records[5]);
        string7 = getStringValue(records[6]);
        return (string1, string2, string3, string4, string5, string6, string7);
    }

    function booleanToUInt(bool x) constant returns (uint) {
        if (x) {
            return 0;
        } else {
            return 1;
        }
    }

    function getUIntValue(bytes32 record, uint8 uintType) constant returns(uint) {
        if (uintType == 1) {
            booleanToUInt(getBooleanValue(record));
        } else if (uintType == 2) {
            uint(getBytes32Value(record));
        } else if (uintType == 3) {
            uint(getUInt8Value(record));
        } else {
            getUIntValue(record);
        }
    }

    function getEntityList(bytes32[] records, uint8[] uintTypes)
            public constant returns
    (
        uint[] items1,
        uint[] items2,
        uint[] items3,
        uint[] items4,
        uint[] items5,
        uint[] items6,
        uint[] items7
    )
    {
        for (uint i = 0; i < (records.length / 7); i++) {
            items1[i] = getUIntValue(records[i * 7], uintTypes[i]);
            items2[i] = getUIntValue(records[(i * 7) + 1], uintTypes[i + 1]);
            items3[i] = getUIntValue(records[(i * 7) + 2], uintTypes[i + 2]);
            items4[i] = getUIntValue(records[(i * 7) + 3], uintTypes[i + 3]);
            items5[i] = getUIntValue(records[(i * 7) + 4], uintTypes[i + 4]);
            items6[i] = getUIntValue(records[(i * 7) + 5], uintTypes[i + 5]);
            items7[i] = getUIntValue(records[(i * 7) + 6], uintTypes[i + 6]);
        }
        return (items1, items2, items3, items4, items5, items6, items7);
    }
}