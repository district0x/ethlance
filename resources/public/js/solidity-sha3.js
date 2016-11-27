var SoliditySha3 =
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;
/******/
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ function(module, exports, __webpack_require__) {

	'use strict';

	Object.defineProperty(exports, "__esModule", {
	  value: true
	});
	exports.sha3num = exports.sha3withsize = undefined;

	var _web = __webpack_require__(1);

	var _web2 = _interopRequireDefault(_web);

	var _leftPad = __webpack_require__(2);

	var _leftPad2 = _interopRequireDefault(_leftPad);

	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

	var web3 = new _web2.default();

	// the size of a character in a hex string in bytes
	var HEX_CHAR_SIZE = 4;

	// the size to hash an integer if not explicity provided
	var DEFAULT_SIZE = 256;

	/** Encodes a value in hex and adds padding to the given size if needed. Tries to determine whether it should be encoded as a number or string. Curried args. */
	var encodeWithPadding = function encodeWithPadding(size) {
	  return function (value) {
	    return typeof value === 'string'
	    // non-hex string
	    ? web3.toHex(value)
	    // numbers, big numbers, and hex strings
	    : encodeNum(size)(value);
	  };
	};

	/** Encodes a number in hex and adds padding to the given size if needed. Curried args. */
	var encodeNum = function encodeNum(size) {
	  return function (value) {
	    return (0, _leftPad2.default)(web3.toHex(value < 0 ? value >>> 0 : value).slice(2), size / HEX_CHAR_SIZE, value < 0 ? 'F' : '0');
	  };
	};

	/** Hashes one or more arguments, using a default size for numbers. */

	exports.sha3 = function () {
	  for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
	    args[_key] = arguments[_key];
	  }

	  var paddedArgs = args.map(encodeWithPadding(DEFAULT_SIZE)).join('');
	  return web3.sha3(paddedArgs, { encoding: 'hex' });
	};

	/** Hashes a single value at the given size. */


	var sha3withsize = exports.sha3withsize = function sha3withsize(value, size) {
	  var paddedArgs = encodeWithPadding(size)(value);
	  return web3.sha3(paddedArgs, { encoding: 'hex' });
	};

	var sha3num = exports.sha3num = function sha3num(value) {
	  var size = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : DEFAULT_SIZE;

	  var paddedArgs = encodeNum(size)(value);
	  return web3.sha3(paddedArgs, { encoding: 'hex' });
	};

/***/ },
/* 1 */
/***/ function(module, exports) {

	module.exports = Web3;

/***/ },
/* 2 */
/***/ function(module, exports) {

	/* This program is free software. It comes without any warranty, to
	     * the extent permitted by applicable law. You can redistribute it
	     * and/or modify it under the terms of the Do What The Fuck You Want
	     * To Public License, Version 2, as published by Sam Hocevar. See
	     * http://www.wtfpl.net/ for more details. */
	'use strict';
	module.exports = leftPad;

	var cache = [
	  '',
	  ' ',
	  '  ',
	  '   ',
	  '    ',
	  '     ',
	  '      ',
	  '       ',
	  '        ',
	  '         '
	];

	function leftPad (str, len, ch) {
	  // convert `str` to `string`
	  str = str + '';
	  // `len` is the `pad`'s length now
	  len = len - str.length;
	  // doesn't need to pad
	  if (len <= 0) return str;
	  // `ch` defaults to `' '`
	  if (!ch && ch !== 0) ch = ' ';
	  // convert `ch` to `string`
	  ch = ch + '';
	  // cache common use cases
	  if (ch === ' ' && len < 10) return cache[len] + str;
	  // `pad` starts with an empty string
	  var pad = '';
	  // loop
	  while (true) {
	    // add `ch` to `pad` if `len` is odd
	    if (len & 1) pad += ch;
	    // divide `len` by 2, ditch the remainder
	    len >>= 1;
	    // "double" the `ch` so this operation count grows logarithmically on `len`
	    // each time `ch` is "doubled", the `len` would need to be "doubled" too
	    // similar to finding a value in binary search tree, hence O(log(n))
	    if (len) ch += ch;
	    // `len` is 0, exit the loop
	    else break;
	  }
	  // pad `str`!
	  return pad + str;
	}


/***/ }
/******/ ]);