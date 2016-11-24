#!/usr/bin/env bash
cd resources/public/contracts/src
/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi --combined-json bin,abi ethlance.sol -o ../build/ > ../build/ethlance.json
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi --combined-json bin,abi ethlanceViews.sol -o ../build/ > ../build/ethlanceViews.json
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi --combined-json bin,abi ethlanceSearch.sol -o ../build/ > ../build/ethlanceSearch.json
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi --combined-json bin,abi eternalStorage.sol -o ../build/ > ../build/eternalStorage.json
wc -c ../build/Ethlance.bin | awk '{print $1}'
wc -c ../build/EthlanceViews.bin | awk '{print $1}'
wc -c ../build/EthlanceSearch.bin | awk '{print $1}'
#wc -c ../build/EternalStorage.bin | awk '{print $1}'