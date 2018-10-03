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

pragma solidity ^0.4.13;

contract DSAuthority {
  function canCall(
    address src, address dst, bytes4 sig
  ) public view returns (bool);
}

contract DSAuthEvents {
  event LogSetAuthority (address indexed authority);
  event LogSetOwner     (address indexed owner);
}

contract DSAuth is DSAuthEvents {
  DSAuthority  public  authority;
  address      public  owner;

  function DSAuth() public {
    owner = msg.sender;
    LogSetOwner(msg.sender);
  }

  function setOwner(address owner_)
  public
  auth
  {
    owner = owner_;
    LogSetOwner(owner);
  }

  function setAuthority(DSAuthority authority_)
  public
  auth
  {
    authority = authority_;
    LogSetAuthority(authority);
  }

  modifier auth {
    require(isAuthorized(msg.sender, msg.sig));
    _;
  }

  function isAuthorized(address src, bytes4 sig) internal view returns (bool) {
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
