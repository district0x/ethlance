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

/// @title Interface for a defined District Authority Handler
contract DSAuthority {
  function canCall(
    address src, address dst, bytes4 sig
  ) public view returns (bool);
}


/// @title District Authority Events
contract DSAuthEvents {
  event LogSetAuthority (address indexed authority);
  event LogSetOwner     (address indexed owner);
}


/// @title District User Authority @dev A properly constructed DSAuth
/// contract requires ANY of this.setOwner(...) with an appropriate
/// authority address to be set, or this.setAuthority(...) to be set
/// to a contract containing the DSAuthority.canCall interface method
/// defined. see ./auth/DSGuard for an example of a defined
/// DSAuthority.canCall method.
contract DSAuth is DSAuthEvents {
  DSAuthority  public  authority;
  address      public  owner;


  /// @dev 
  constructor() public {
    owner = msg.sender;
    emit LogSetOwner(msg.sender);
  }

  //
  // Methods
  //

  function setOwner(address owner_)
  public
  auth
  {
    owner = owner_;
    emit LogSetOwner(owner);
  }

  function setAuthority(DSAuthority authority_)
  public
  auth
  {
    authority = authority_;
    emit LogSetAuthority(authority);
  }

  //
  // Modifiers
  //

  /// @dev Checks if the given message sender is authorized
  modifier auth {
    require(isAuthorized(msg.sender, msg.sig),
	    "Unauthorized Access");
    _;
  }

  //
  // Views
  //

  /// @dev Returns true, if the given address and signature pair are
  /// authorized as designed by DSAuthority.canCall interface.
  /// @param src The address we are checking authority against.
  /// @param sig The 4-byte function signature we are checking authority against.
  /// @return Returns true, if the given address/sig pair is
  /// authorized, false otherwise.
  function isAuthorized(address src, bytes4 sig)
      internal view returns (bool)
  {
    if (src == address(this)) {
      return true;
    } else if (src == owner) {
      return true;
    } else if (authority == DSAuthority(0)) {
      return false;
    } else {
      return authority.canCall(src, this, sig);
    }
  }
}
