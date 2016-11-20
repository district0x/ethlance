#!/usr/bin/env bash
cd resources/public/contracts/src
solc --optimize --bin --abi --combined-json bin,abi ethlance.sol -o ../build/ > ../build/ethlance.json