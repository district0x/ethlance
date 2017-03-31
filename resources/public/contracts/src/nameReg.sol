pragma solidity ^0.4.8;

contract NameReg {
  function register(bytes32 name) {}
  function unregister() {}
  function addressOf(bytes32 name) constant returns (address addr) {}
  function nameOf(address addr) constant returns (bytes32 name) {}
  function kill() {}
}

contract nameRegAware {
  function nameRegAddress() returns (address) {
    return 0x084f6a99003DaE6D3906664fDBf43Dd09930d0e3;
  }

  function getCodeSize(address _addr) constant internal returns(uint _size) {
      assembly {
          _size := extcodesize(_addr)
      }
  }

  function named(bytes32 name) returns (address) {
    var addr = nameRegAddress();
    if (getCodeSize(addr) > 0) {
        return NameReg(nameRegAddress()).addressOf(name);
    } else {
        return 0x0;
    }
  }
}

contract named is nameRegAware {
  function named(bytes32 name) {
    var addr = nameRegAddress();
    if (getCodeSize(addr) > 0) {
        NameReg(addr).register(name);
    }
  }
}
