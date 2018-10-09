// guard.sol -- simple whitelist implementation of DSAuthority

// Copyright (C) 2017  DappHub, LLC

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

pragma solidity ^0.4.24;

import "./DSAuth.sol";

/// @title DSGuard Events
contract DSGuardEvents {
  event LogPermit(
    bytes32 indexed src,
    bytes32 indexed dst,
    bytes32 indexed sig
  );
  event LogForbid(
    bytes32 indexed src,
    bytes32 indexed dst,
    bytes32 indexed sig
  );
}


/// @title Simple whitelist implementation of DSAuthority
contract DSGuard is DSAuth, DSAuthority, DSGuardEvents {
  bytes32 constant public ANY = bytes32(uint(- 1));
  mapping(bytes32 => mapping(bytes32 => mapping(bytes32 => bool))) acl;

  function canCall(
    address src_, address dst_, bytes4 sig
  ) public view returns (bool) {
    bytes32 src = bytes32(src_);
    bytes32 dst = bytes32(dst_);

    return acl[src][dst][sig]
    || acl[src][dst][ANY]
    || acl[src][ANY][sig]
    || acl[src][ANY][ANY]
    || acl[ANY][dst][sig]
    || acl[ANY][dst][ANY]
    || acl[ANY][ANY][sig]
    || acl[ANY][ANY][ANY];
  }


  /// @dev Permits the authority of `src` to `dst` for the given
  /// function identifier `sig`. Note that DSGuard.ANY can be
  /// substituted in `src, `dst`, or `sig` to slacken authority
  /// further.
  /// @param src The byte representation of a source address
  /// @param dst The byte representation of a destination address
  /// @param sig The calldata function signature
  function permit(bytes32 src, bytes32 dst, bytes32 sig) public auth {
    acl[src][dst][sig] = true;
    emit LogPermit(src, dst, sig);
  }


  /// @dev Forbids the authority of `src` to `dst` for the given
  /// function identifier `sig`.
  /// @param src The byte representation of a source address
  /// @param dst The byte representation of a destination address
  /// @param sig The calldata function signature
  function forbid(bytes32 src, bytes32 dst, bytes32 sig) public auth {
    acl[src][dst][sig] = false;
    emit LogForbid(src, dst, sig);
  }


  /// @dev Permits the authority of `src` to `dst` for the given
  /// function identifier `sig`. Note that DSGuard.ANY can be
  /// substituted in `src, `dst`, or `sig` to slacken authority
  /// further.
  /// @param src The address representation of a source address
  /// @param dst The address representation of a destination address
  /// @param sig The calldata function signature
  function permit(address src, address dst, bytes32 sig) public {
    permit(bytes32(src), bytes32(dst), sig);
  }


  /// @dev Forbids the authority of `src` to `dst` for the given
  /// function identifier `sig`.
  /// @param src The address representation of a source address
  /// @param dst The address representation of a destination address
  /// @param sig The calldata function signature
  function forbid(address src, address dst, bytes32 sig) public {
    forbid(bytes32(src), bytes32(dst), sig);
  }
}


/// @title DSGuard Authority Factory
/// @dev Maintains a listing of active Guard Authorities.
contract DSGuardFactory {
  mapping(address => bool)  public  isGuard;

  /// @dev Create a new DSGuard, containing a DSAuthority Implementation.
  /// @return The newly created DSGuard contract
  function newGuard() public returns (DSGuard guard) {
    guard = new DSGuard();
    guard.setOwner(msg.sender);
    isGuard[guard] = true;
  }
}
