#!/usr/bin/env bash
cd resources/public/contracts/src

#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceUser.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceJob.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceContract.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceInvoice.sol -o ../build/
/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceConfig.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceViews.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceSearch.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceDB.sol -o ../build/
wc -c ../build/EthlanceUser.bin | awk '{print "EthlanceUser: " $1}'
wc -c ../build/EthlanceJob.bin | awk '{print "EthlanceJob: " $1}'
wc -c ../build/EthlanceContract.bin | awk '{print "EthlanceContract: " $1}'
wc -c ../build/EthlanceInvoice.bin | awk '{print "EthlanceInvoice: " $1}'
wc -c ../build/EthlanceConfig.bin | awk '{print "EthlanceConfig: " $1}'
wc -c ../build/EthlanceViews.bin | awk '{print "EthlanceViews: " $1}'
wc -c ../build/EthlanceSearch.bin | awk '{print "EthlanceSearch: " $1}'
wc -c ../build/EthlanceDB.bin | awk '{print "EthlanceDB: " $1}'