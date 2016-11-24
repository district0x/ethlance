pragma solidity ^0.4.4;

import "EternalStorage.sol";
import "safeMath.sol";

library SharedLibrary {
    function getCount(address _storage, string countKey) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3(countKey));
    }

    function createNext(address _storage, string countKey) constant returns(uint index) {
        var count = getCount(_storage, countKey);
        EternalStorage(_storage).addUIntValue(sha3(countKey), 1);
        return count + 1;
    }

    function containsValue(address _storage, uint id, string key, uint8[] array) constant returns(bool) {
        if (array.length == 0) {
            return true;
        }
        var val = EternalStorage(_storage).getUInt8Value(sha3(key, id));
        for (uint i = 0; i < array.length ; i++) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }

    function getArrayItemsCount(address _storage, uint id, string countKey) constant returns(uint) {
        return EternalStorage(_storage).getUIntValue(sha3(countKey, id));
    }

    function addArrayItem(address _storage, uint id, string key, string countKey, uint val) {
        var idx = getArrayItemsCount(_storage, id, countKey) + 1;
        EternalStorage(_storage).setUIntValue(sha3(key, id, idx), val);
        EternalStorage(_storage).setUIntValue(sha3(countKey, id), idx + 1);
    }

    function setUIntArray(address _storage, uint id, string key, string countKey, uint[] array) {
        for (uint i = 0; i < array.length; i++) {
            EternalStorage(_storage).setUIntValue(sha3(key, id, i), array[i]);
        }
        EternalStorage(_storage).setUIntValue(sha3(countKey, id), array.length);
    }
    
    function getUIntArray(address _storage, uint id, string key, string countKey) internal returns(uint[] result) {
        uint count = EternalStorage(_storage).getUIntValue(sha3(countKey, id));
        for (uint i = 0; i < count; i++) {
            result[i] = EternalStorage(_storage).getUIntValue(sha3(key, id, i));
        }
        return result;
    }

    function addRemovableArrayItem(address _storage, uint[] ids, string key, string countKey, string keysKey, uint val) {
        for (uint i = 0; i < ids.length; i++) {
            addArrayItem(_storage, ids[i], keysKey, countKey, val);
            EternalStorage(_storage).setBooleanValue(sha3(key, ids[i], val), true);
        }
    }

    function getRemovableArrayItems(address _storage, uint id, string key, string countKey, string keysKey)
        internal returns (uint[] result)
    {
        var count = getArrayItemsCount(_storage, id, countKey);
        uint j = 0;
        for (uint i = 0; i < count; i++) {
            var itemId = EternalStorage(_storage).getUIntValue(sha3(keysKey, id, i));
            if (EternalStorage(_storage).getBooleanValue(sha3(key, id, itemId))) {
                result[j] = itemId;
                j++;
            }
        }
        return result;
    }

    function removeArrayItem(address _storage, uint[] ids, string key, uint val) {
        for (uint i = 0; i < ids.length; i++) {
            EternalStorage(_storage).setBooleanValue(sha3(key, ids[i], val), false);
        }
    }

    function getPage(uint[] array, uint offset, uint limit) internal returns (uint[] result) {
        uint j = 0;
        for (uint i = offset; i < SafeMath.safeAdd(offset, limit); i++) {
            if (array[i] == 0) break;
            result[j] = array[i];
            j++;
        }
        return result;
    }

    function intersect(uint[] a, uint[] b) internal returns(uint[] c) {
        mapping (uint => bool) _map;
        for (uint i = 0; i < a.length; i++) {
            _map[a[i]] = true;
        }
        uint j = 0;
        for (i = 0; i < b.length; i++) {
            if (_map[b[i]]) {
                c[j] = b[i];
                j++;
            }
        }
        return c;
    }

    function intersect(uint[][] arrays) internal returns(uint[] result) {
        result = arrays[0];
        for (uint i = 1; i < arrays.length; i++) {
            result = intersect(result, arrays[i]);
        }
        return result;
    }

    function diff(uint[] _old, uint[] _new) internal returns(uint[] added, uint[] removed) {
        mapping (uint => uint8) _map;
        for (uint i = 0; i < _old.length; i++) {
            _map[_old[i]] = 1;
        }
        uint j = 0;
        for (i = 0; i < _new.length; i++) {
            if (_map[_new[i]] == 0) {
                added[j] = _new[i];
                j++;
            } else {
                _map[_new[i]] = 2;
            }
        }
        j = 0;
        for (i = 0; i < _old.length; i++) {
            if (_map[_old[i]] == 1) {
                removed[j] = _old[i];
                j++;
            }
        }
        return (added, removed);
    }
    
    function intersectCategoriesAndSkills
    (
        address _storage,
        uint categoryId,
        uint[] skills,
        function(address, uint) returns (uint[] memory) getFromSkills,
        function(address, uint) returns (uint[] memory) getFromCategories
    )
        internal returns (uint[]) 
    {
        uint[][] memory idArrays;
        for (uint i = 0; i < skills.length ; i++) {
            idArrays[i] = getFromSkills(_storage, skills[i]);
        }
        if (categoryId > 0) {
            idArrays[idArrays.length - 1] = getFromCategories(_storage, categoryId);
        }
        return SharedLibrary.intersect(idArrays);
    }

    function filter(
        address _storage,
        function (address, uint[] memory, uint) returns (bool) f,
        uint[] memory args,
        uint[] memory items
    )
        internal returns (uint[] memory r)
    {
        uint j = 0;
        for (uint i = 0; i < items.length; i++) {
            if (f(_storage, args, items[i])) {
                r[j] = items[i];
                j++;
            }
        }
    }
}