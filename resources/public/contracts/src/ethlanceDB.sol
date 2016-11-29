pragma solidity ^0.4.4;

import "Ownable.sol";
import "safeMath.sol";


contract EthlanceDB is Ownable {

    address[] public allowedContractsKeys;
    mapping(address => bool) public allowedContracts;

    modifier onlyAllowedContractOrOwner {
      if (allowedContracts[msg.sender] != true && msg.sender != owner) throw;
      _;
    }

    function EthlanceDB(){
    }

    function addAllowedContracts(address[] addresses)
    onlyOwner {
        for (uint i = 0; i < addresses.length; i++) {
            allowedContracts[addresses[i]] = true;
            allowedContractsKeys.push(addresses[i]);
        }
    }

    function removeAllowedContracts(address[] addresses)
    onlyOwner {
        for (uint i = 0; i < addresses.length; i++) {
            allowedContracts[addresses[i]] = false;
        }
    }

    function allowedContractsCount() constant returns(uint count) {
        for (uint i = 0; i < allowedContractsKeys.length; i++) {
            if (allowedContracts[allowedContractsKeys[i]]) {
                count++;
            }
        }
        return count;
    }

    function getAllowedContracts() constant returns(address[] addresses) {
        addresses = new address[](allowedContractsCount());
        for (uint i = 0; i < allowedContractsKeys.length; i++) {
            if (allowedContracts[allowedContractsKeys[i]]) {
                addresses[i] = allowedContractsKeys[i];
            }
        }
        return addresses;
    }

    mapping(bytes32 => uint8) UInt8Storage;
    
    function getUInt8Value(bytes32 record) constant returns (uint8){
        return UInt8Storage[record];
    }

    function setUInt8Value(bytes32 record, uint8 value)
    onlyAllowedContractOrOwner
    {
        UInt8Storage[record] = value;
    }

    function deleteUInt8Value(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete UInt8Storage[record];
    }

    mapping(bytes32 => uint) UIntStorage;

    function getUIntValue(bytes32 record) constant returns (uint){
        return UIntStorage[record];
    }

    function setUIntValue(bytes32 record, uint value)
    onlyAllowedContractOrOwner
    {
        UIntStorage[record] = value;
    }

    function addUIntValue(bytes32 record, uint value)
    onlyAllowedContractOrOwner
    {
        UIntStorage[record] = SafeMath.safeAdd(UIntStorage[record], value);
    }

    function subUIntValue(bytes32 record, uint value)
    onlyAllowedContractOrOwner
    {
        UIntStorage[record] = SafeMath.safeSub(UIntStorage[record], value);
    }

    function deleteUIntValue(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete UIntStorage[record];
    }

    mapping(bytes32 => string) StringStorage;

    function getStringValue(bytes32 record) constant returns (string){
        return StringStorage[record];
    }

    function setStringValue(bytes32 record, string value)
    onlyAllowedContractOrOwner
    {
        StringStorage[record] = value;
    }

    function deleteStringValue(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete StringStorage[record];
    }

    mapping(bytes32 => address) AddressStorage;

    function getAddressValue(bytes32 record) constant returns (address){
        return AddressStorage[record];
    }

    function setAddressValue(bytes32 record, address value)
    onlyAllowedContractOrOwner
    {
        AddressStorage[record] = value;
    }

    function deleteAddressValue(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete AddressStorage[record];
    }

    mapping(bytes32 => bytes) BytesStorage;

    function getBytesValue(bytes32 record) constant returns (bytes){
        return BytesStorage[record];
    }

    function setBytesValue(bytes32 record, bytes value)
    onlyAllowedContractOrOwner
    {
        BytesStorage[record] = value;
    }

    function deleteBytesValue(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete BytesStorage[record];
    }

    mapping(bytes32 => bytes32) Bytes32Storage;

    function getBytes32Value(bytes32 record) constant returns (bytes32){
        return Bytes32Storage[record];
    }

    function setBytes32Value(bytes32 record, bytes32 value)
    onlyAllowedContractOrOwner
    {
        Bytes32Storage[record] = value;
    }

    function deleteBytes32Value(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete Bytes32Storage[record];
    }

    mapping(bytes32 => bool) BooleanStorage;

    function getBooleanValue(bytes32 record) constant returns (bool){
        return BooleanStorage[record];
    }

    function setBooleanValue(bytes32 record, bool value)
    onlyAllowedContractOrOwner
    {
        BooleanStorage[record] = value;
    }

    function deleteBooleanValue(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete BooleanStorage[record];
    }

    mapping(bytes32 => int) IntStorage;

    function getIntValue(bytes32 record) constant returns (int){
        return IntStorage[record];
    }

    function setIntValue(bytes32 record, int value)
    onlyAllowedContractOrOwner
    {
        IntStorage[record] = value;
    }

    function deleteIntValue(bytes32 record)
    onlyAllowedContractOrOwner
    {
      delete IntStorage[record];
    }

    function getTypesCounts(uint8[] types) constant returns(uint[]) {
        var counts = new uint[](7);
        for (uint i = 0; i < types.length ; i++) {
            counts[types[i] - 1]++;
        }
        return counts;
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
        var counts = getTypesCounts(types);
        bools = new bool[](counts[0]);
        uint8s = new uint8[](counts[1]);
        uints = new uint[](counts[2]);
        addresses = new address[](counts[3]);
        bytes32s = new bytes32[](counts[4]);
        ints = new int[](counts[5]);
        counts = new uint[](7);

        for (uint i = 0; i < records.length; i++) {
            var recordType = types[i];
            var record = records[i];
            if (recordType == 1) {
                bools[counts[0]] = getBooleanValue(record);
                counts[0]++;
            } else if (recordType == 2) {
                uint8s[counts[1]] = getUInt8Value(record);
                counts[1]++;
            } else if (recordType == 3) {
                uints[counts[2]] = getUIntValue(record);
                counts[2]++;

            } else if (recordType == 4) {
                addresses[counts[3]] = getAddressValue(record);
                counts[3]++;

            } else if (recordType == 5) {
                bytes32s[counts[4]] = getBytes32Value(record);
                counts[4]++;

            } else if (recordType == 6) {
                ints[counts[5]] = getIntValue(record);
                counts[5]++;

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
            return 1;
        } else {
            return 0;
        }
    }

    function getUIntValueConverted(bytes32 record, uint8 uintType) constant returns(uint) {
        if (uintType == 1) {
            return booleanToUInt(getBooleanValue(record));
        } else if (uintType == 2) {
            return uint(getUInt8Value(record));
        } else if (uintType == 3) {
            return getUIntValue(record);
        } else if (uintType == 5) {
            return uint(getBytes32Value(record));
        } else {
            return 0;
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
        uint itemsCount = records.length / uintTypes.length;
        uint typesLength = uintTypes.length;
        items1 = new uint[](itemsCount);

        if (typesLength > 1) {
            items2 = new uint[](itemsCount);
        }
        if (typesLength > 2) {
            items3 = new uint[](itemsCount);
        }
        if (typesLength > 3) {
            items4 = new uint[](itemsCount);
        }
        if (typesLength > 4) {
            items5 = new uint[](itemsCount);
        }
        if (typesLength > 5) {
            items6 = new uint[](itemsCount);
        }
        if (typesLength > 6) {
            items7 = new uint[](itemsCount);
        }

        for (uint i = 0; i < itemsCount; i++) {
            items1[i] = getUIntValueConverted(records[i * typesLength], uintTypes[0]);

            if (typesLength > 1) {
                items2[i] = getUIntValueConverted(records[(i * typesLength) + 1], uintTypes[1]);
            }
            if (typesLength > 2) {
                items3[i] = getUIntValueConverted(records[(i * typesLength) + 2], uintTypes[2]);
            }
            if (typesLength > 3) {
                items4[i] = getUIntValueConverted(records[(i * typesLength) + 3], uintTypes[3]);
            }
            if (typesLength > 4) {
                items5[i] = getUIntValueConverted(records[(i * typesLength) + 4], uintTypes[4]);
            }
            if (typesLength > 5) {
                items6[i] = getUIntValueConverted(records[(i * typesLength) + 5], uintTypes[5]);
            }
            if (typesLength > 6) {
                items7[i] = getUIntValueConverted(records[(i * typesLength) + 6], uintTypes[6]);
            }
        }
        return (items1, items2, items3, items4, items5, items6, items7);
    }
}