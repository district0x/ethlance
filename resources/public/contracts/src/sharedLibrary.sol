pragma solidity ^0.4.8;

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

    function getIdArrayItemsCount(address db, uint id, string countKey) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3(countKey, id));
    }

    function getIdArrayItemsCount(address db, address id, string countKey) internal returns(uint) {
        return EthlanceDB(db).getUIntValue(sha3(countKey, id));
    }

    function addIdArrayItem(address db, uint id, string key, string countKey, uint val) internal {
        var idx = getIdArrayItemsCount(db, id, countKey);
        EthlanceDB(db).setUIntValue(sha3(key, id, idx), val);
        EthlanceDB(db).setUIntValue(sha3(countKey, id), idx + 1);
    }

    function addIdArrayItem(address db, address id, string key, string countKey, uint val) internal {
        var idx = getIdArrayItemsCount(db, id, countKey);
        EthlanceDB(db).setUIntValue(sha3(key, id, idx), val);
        EthlanceDB(db).setUIntValue(sha3(countKey, id), idx + 1);
    }

    function setIdArray(address db, uint id, string key, string countKey, uint[] array) internal {
        for (uint i = 0; i < array.length; i++) {
            if (array[i] == 0) throw;
            EthlanceDB(db).setUIntValue(sha3(key, id, i), array[i]);
        }
        EthlanceDB(db).setUIntValue(sha3(countKey, id), array.length);
    }

    function getIdArray(address db, uint id, string key, string countKey) internal returns(uint[] result) {
        uint count = getIdArrayItemsCount(db, id, countKey);
        result = new uint[](count);
        for (uint i = 0; i < count; i++) {
            result[i] = EthlanceDB(db).getUIntValue(sha3(key, id, i));
        }
        return result;
    }

    function getIdArray(address db, address id, string key, string countKey) internal returns(uint[] result) {
        uint count = getIdArrayItemsCount(db, id, countKey);
        result = new uint[](count);
        for (uint i = 0; i < count; i++) {
            result[i] = EthlanceDB(db).getUIntValue(sha3(key, id, i));
        }
        return result;
    }

    function setIdArray(address db, uint id, string key, string countKey, address[] array) internal {
        for (uint i = 0; i < array.length; i++) {
            require(array[i] != 0x0);
            EthlanceDB(db).setAddressValue(sha3(key, id, i), array[i]);
        }
        EthlanceDB(db).setUIntValue(sha3(countKey, id), array.length);
    }

    function getAddressIdArray(address db, uint id, string key, string countKey) internal returns(address[] result) {
        uint count = getIdArrayItemsCount(db, id, countKey);
        result = new address[](count);
        for (uint i = 0; i < count; i++) {
            result[i] = EthlanceDB(db).getAddressValue(sha3(key, id, i));
        }
        return result;
    }

    function addRemovableIdArrayItem(address db, uint[] ids, string key, string countKey, string keysKey, uint val) internal {
        if (ids.length == 0) {
            return;
        }
        for (uint i = 0; i < ids.length; i++) {
            if (EthlanceDB(db).getUInt8Value(sha3(key, ids[i], val)) == 0) { // never seen before
                addIdArrayItem(db, ids[i], keysKey, countKey, val);
            }
            EthlanceDB(db).setUInt8Value(sha3(key, ids[i], val), 1); // 1 == active
        }
    }


    function getRemovableIdArrayItems(address db, uint id, string key, string countKey, string keysKey)
        internal returns (uint[] result)
    {
        var count = getIdArrayItemsCount(db, id, countKey);
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

    function removeIdArrayItem(address db, uint[] ids, string key, uint val) internal {
        if (ids.length == 0) {
            return;
        }
        for (uint i = 0; i < ids.length; i++) {
            EthlanceDB(db).setUInt8Value(sha3(key, ids[i], val), 2); // 2 == blocked
        }
    }


    function getPage(uint[] array, uint offset, uint limit, bool cycle) internal returns (uint[] result) {
        uint j = 0;
        uint length = array.length;
        if (offset >= length || limit == 0) {
            return result;
        }

        result = new uint[](limit);
        for (uint i = offset; i < (offset + limit); i++) {
            if (length == i) {
                break;
            }
            result[j] = array[i];
            j++;
        }

        if (cycle && j < limit) {
            var k = limit - j;
            for (i = 0; i <= k; i++) {
                if (limit == j) {
                    break;
                }
                result[j] = array[i];
                j++;
            }
        }
        return take(j, result);
    }

    /* Assumes sorted a & b */
    function intersect(uint[] a, uint[] b) internal returns(uint[] c) {
        uint aLen = a.length;
        uint bLen = b.length;
        if (aLen == 0 || bLen == 0) {
            return c;
        }
        c = new uint[](aLen);
        uint i = 0;
        uint j = 0;
        uint k = 0;
        while (i < aLen && j < bLen) {
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

    /* Assumes sorted a & b */
    function union(uint[] a, uint[] b) internal returns(uint[] c) {
        uint aLen = a.length;
        uint bLen = b.length;
        c = new uint[](aLen + bLen);
        uint i = 0;
        uint j = 0;
        uint k = 0;
        while (i < aLen && j < bLen) {
            if (a[i] < b[j]) {
                c[k] = a[i];
                i++;
            } else if (b[j] < a[i]) {
                c[k] = b[j];
                j++;
            } else {
                c[k] = a[i];
                i++;
                j++;
            }
            k++;
        }

        while (i < aLen) {
            c[k] = a[i];
            i++;
            k++;
        }

        while (j < bLen) {
            c[k] = b[j];
            j++;
            k++;
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

    function sort(uint[] array) internal returns (uint[]) {
        for (uint i = 1; i < array.length; i++) {
            var t = array[i];
            var j = i;
            while(j > 0 && array[j - 1] > t) {
                array[j] = array[j - 1];
                j--;
            }
            array[j] = t;
        }
        return array;
    }

    function sortDescBy(uint[] array, uint[] compareArray) internal returns (uint[]) {
        for (uint i = 1; i < array.length; i++) {
            var t = array[i];
            var t2 = compareArray[i];
            var j = i;
            while(j > 0 && compareArray[j - 1] < t2) {
                array[j] = array[j - 1];
                compareArray[j] = compareArray[j - 1];
                j--;
            }
            array[j] = t;
            compareArray[j] = t2;
        }
        return array;
    }


    function take(uint n, uint[] array) internal returns(uint[] result) {
        if (n > array.length) {
            return array;
        }
        result = new uint[](n);
        for (uint i = 0; i < n ; i++) {
            result[i] = array[i];
        }
        return result;
    }

    function take(uint n, bytes32[] array) internal returns(bytes32[] result) {
        if (n > array.length) {
            return array;
        }
        result = new bytes32[](n);
        for (uint i = 0; i < n ; i++) {
            result[i] = array[i];
        }
        return result;
    }

    function findTopNValues(uint[] values, uint n) internal returns(uint[]) {
        uint length = values.length;

        for (uint i = 0; i <= n; i++) {
            uint maxPos = i;
            for (uint j = i + 1; j < length; j++) {
                if (values[j] > values[maxPos]) {
                    maxPos = j;
                }
            }

            if (maxPos != i) {
                uint maxValue = values[maxPos];
                values[maxPos] = values[i];
                values[i] = maxValue;
            }
        }
        return take(n, values);
    }

    function intersectCategoriesAndSkills
    (
        address db,
        uint categoryId,
        uint[] skillsAnd,
        uint[] skillsOr,
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
        if (skillsAnd.length == 0 && skillsOr.length == 0 && categoryId == 0) {
            result = new uint[](maxCount);
            for (i = 0; i < maxCount ; i++) {
                result[i] = i + 1;
            }
        }

        if (skillsAnd.length > 0) {
            result = sort(getFromSkills(db, skillsAnd[0]));
            for (i = 1; i < skillsAnd.length ; i++) {
                result = intersect(result, sort(getFromSkills(db, skillsAnd[i])));
            }
        }

        if (skillsOr.length > 0) {
            if (skillsAnd.length > 0) {
                result = unionSkills(db, skillsOr, getFromSkills, result);
            } else {
                result = unionSkills(db, skillsOr, getFromSkills);
            }
        }

        if (categoryId > 0) {
            var catResult = sort(getFromCategories(db, categoryId));
            if (skillsAnd.length == 0 && skillsOr.length == 0) {
                result = catResult;
            } else {
                result = intersect(result, catResult);
            }
        }
        return result;
    }

    function unionSkills
    (
        address db,
        uint[] skillsOr,
        function(address, uint) returns (uint[] memory) getFromSkills,
        uint[] fromItems
    )
        internal returns (uint[] result)
    {
        result = intersect(fromItems, sort(getFromSkills(db, skillsOr[0])));
        for (uint i = 1; i < skillsOr.length ; i++) {
            result = union(result, intersect(fromItems, sort(getFromSkills(db, skillsOr[i]))));
        }
        return result;
    }

    function unionSkills
    (
        address db,
        uint[] skillsOr,
        function(address, uint) returns (uint[] memory) getFromSkills
    )
        internal returns (uint[] result)
    {
        result = sort(getFromSkills(db, skillsOr[0]));
        for (uint i = 1; i < skillsOr.length ; i++) {
            result = union(result, sort(getFromSkills(db, skillsOr[i])));
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

    function filter(
        address db,
        function (address, uint[] memory, uint[] memory, uint) returns (bool) f,
        uint[] memory items,
        uint[] memory args,
        uint[] memory args2
    )
        internal returns (uint[] memory r)
    {
        uint j = 0;
        r = new uint[](items.length);
        for (uint i = 0; i < items.length; i++) {
            if (f(db, args, args2, items[i])) {
                r[j] = items[i];
                j++;
            }
        }
        return take(j, r);
    }

    function contains(address[] array, address val) internal returns(bool) {
        for (uint i = 0; i < array.length ; i++) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }

    function contains(uint[] array, uint val) internal returns(bool) {
        for (uint i = 0; i < array.length ; i++) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }
}