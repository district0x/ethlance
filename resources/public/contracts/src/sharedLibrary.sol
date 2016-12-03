pragma solidity ^0.4.4;

import "ethlanceDB.sol";
import "safeMath.sol";
import "strings.sol";

library SharedLibrary {
    function getCount(address db, string countKey) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3(countKey));
    }

    function createNext(address db, string countKey) internal returns(uint index) {
        var count = getCount(db, countKey);
        EthlanceDB(db).addUIntValue(sha3(countKey), 1);
        return count + 1;
    }

    function containsValue(address db, uint id, string key, uint8[] array) internal returns(bool) {
        if (array.length == 0) {
            return true;
        }
        var val = EthlanceDB(db).getUInt8Value(sha3(key, id));
        for (uint i = 0; i < array.length ; i++) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }

    function getArrayItemsCount(address db, uint id, string countKey) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3(countKey, id));
    }

    function addArrayItem(address db, uint id, string key, string countKey, uint val) internal {
        var idx = getArrayItemsCount(db, id, countKey);
        EthlanceDB(db).setUIntValue(sha3(key, id, idx), val);
        EthlanceDB(db).setUIntValue(sha3(countKey, id), idx + 1);
    }

    function setUIntArray(address db, uint id, string key, string countKey, uint[] array) internal{
        for (uint i = 0; i < array.length; i++) {
            EthlanceDB(db).setUIntValue(sha3(key, id, i), array[i]);
        }
        EthlanceDB(db).setUIntValue(sha3(countKey, id), array.length);
    }
    
    function getUIntArray(address db, uint id, string key, string countKey) internal returns(uint[] result) {
        uint count = getArrayItemsCount(db, id, countKey);
        result = new uint[](count);
        for (uint i = 0; i < count; i++) {
            result[i] = EthlanceDB(db).getUIntValue(sha3(key, id, i));
        }
        return result;
    }

    function addRemovableArrayItem(address db, uint[] ids, string key, string countKey, string keysKey, uint val) internal {
        if (ids.length == 0) {
            return;
        }
        for (uint i = 0; i < ids.length; i++) {
            if (EthlanceDB(db).getUInt8Value(sha3(key, ids[i], val)) == 0) { // never seen before
                addArrayItem(db, ids[i], keysKey, countKey, val);
            }
            EthlanceDB(db).setUInt8Value(sha3(key, ids[i], val), 1); // 1 == active
        }
    }


    function getRemovableArrayItems(address db, uint id, string key, string countKey, string keysKey)
        internal returns (uint[] result)
    {
        var count = getArrayItemsCount(db, id, countKey);
        result = new uint[](count);
        uint j = 0;
        for (uint i = 0; i < count; i++) {
            var itemId = EthlanceDB(db).getUIntValue(sha3(keysKey, id, i));
            if (EthlanceDB(db).getUInt8Value(sha3(key, id, itemId)) == 1) { // 1 == active
                result[j] = itemId;
                j++;
            }
        }
        return take(j, result);
    }

    function removeArrayItem(address db, uint[] ids, string key, uint val) internal {
        if (ids.length == 0) {
            return;
        }
        for (uint i = 0; i < ids.length; i++) {
            EthlanceDB(db).setUInt8Value(sha3(key, ids[i], val), 2); // 2 == blocked
        }
    }


    function getPage(uint[] array, uint offset, uint limit) internal returns (uint[] result) {
        uint j = 0;
        if (offset >= array.length) {
            return result;
        }

        result = new uint[](limit);
        for (uint i = offset; i < (offset + limit); i++) {
            if (array.length == i) {
                break;
            }
            result[j] = array[i];
            j++;
        }
        return take(j, result);
    }

    function intersect(uint[] a, uint[] b) internal returns(uint[] c) {
        if (a.length == 0 || b.length == 0) {
            return c;
        }
        c = new uint[](a.length);
//        a = sort(a);
//        b = sort(b);
        uint i = 0;
        uint j = 0;
        uint k = 0;
        while (i < a.length && j < b.length) {
            if (a[i] > b[j]) {
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                c[k] = a[i];
                i++;
                j++;
                k++;
            }
        }
        return take(k, c);
    }
    
    function diff(uint[] _old, uint[] _new) internal returns(uint[] added, uint[] removed) {
        if (_old.length == 0 && _new.length == 0) {
            return (added, removed);
        }
        var maxCount = _old.length + _new.length;
        added = new uint[](maxCount);
        removed = new uint[](maxCount);
        
        _old = sort(_old);
        _new = sort(_new);
        uint ol_i = 0;
        uint ne_i = 0;
        uint ad_i = 0;
        uint re_i = 0;
        while (ol_i < _old.length && ne_i < _new.length) {
            if (_old[ol_i] > _new[ne_i]) {
                added[ad_i] = _new[ne_i];
                ne_i++;
                ad_i++;
            } else if (_old[ol_i] < _new[ne_i]) {
                removed[re_i] = _old[ol_i];
                ol_i++;
                re_i++;
            } else {
                ol_i++;
                ne_i++;
            }
        }
        if (_old.length > ol_i) {
            while (ol_i < _old.length) {
                removed[re_i] = _old[ol_i];
                ol_i++;
                re_i++;
            }
        }
        if (_new.length > ne_i) {
            while (ne_i < _new.length) {
                added[ad_i] = _new[ne_i];
                ne_i++;
                ad_i++;
            }
        }
        return (take(ad_i, added), take(re_i, removed));
    }    

    function sort(uint[] array) internal returns(uint[]) {
        uint n = array.length;
        if (array.length == 0) {
            return array;
        }

        for (uint c = 0 ; c < ( n - 1 ); c++) {
            for (uint d = 0 ; d < n - c - 1; d++) {
                if (array[d] >= array[d + 1]) {
                    (array[d], array[d + 1]) = (array[d + 1], array[d]);
                }
            }
        }
        return array;
    }


    function take(uint n, uint[] array) internal returns(uint[] result) {
        result = new uint[](n);
        for (uint i = 0; i < n ; i++) {
            result[i] = array[i];
        }
        return result;
    }
    
    function intersectCategoriesAndSkills
    (
        address db,
        uint categoryId,
        uint[] skills,
        function(address, uint) returns (uint[] memory) getFromSkills,
        function(address, uint) returns (uint[] memory) getFromCategories,
        function(address) returns (uint) getMaxCount
    )
        internal returns (uint[] result)
    {
        var maxCount = getMaxCount(db);
        uint i;
        if (maxCount == 0) {
            return result;
        }
        if (skills.length == 0 && categoryId == 0) {
            result = new uint[](maxCount);
            for (i = 0; i < maxCount ; i++) {
                result[i] = i + 1;
            }
        }

        if (skills.length > 0) {
            result = sort(getFromSkills(db, skills[0]));
            for (i = 1; i < skills.length ; i++) {
                result = intersect(result, sort(getFromSkills(db, skills[i])));
            }
        }

        if (categoryId > 0) {
            var catResult = sort(getFromCategories(db, categoryId));
            if (skills.length == 0) {
                result = catResult;
            } else {
                result = intersect(result, catResult);
            }
        }
        return result;
    }

    function filter(
        address db,
        function (address, uint[] memory, uint) returns (bool) f,
        uint[] memory items,
        uint[] memory args
    )
        internal returns (uint[] memory r)
    {
        uint j = 0;
        r = new uint[](items.length);
        for (uint i = 0; i < items.length; i++) {
            if (f(db, args, items[i])) {
                r[j] = items[i];
                j++;
            }
        }
        return take(j, r);
    }
}