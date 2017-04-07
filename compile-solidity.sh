#!/usr/bin/env bash
cd resources/public/contracts/src

#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceUser.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceJob.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceContract.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceInvoice.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceConfig.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceViews.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceSearch.sol -o ../build/
#/Users/matus/Downloads/solidity/build/solc/solc --optimize --bin --abi ethlanceDB.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceUser.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceUser2.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceJob.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceContract.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceFeedback.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceInvoice.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceConfig.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceViews.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceSearchJobs.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceSearchFreelancers.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceMessage.sol -o ../build/
solc --overwrite --optimize --bin --abi ethlanceSponsor.sol -o ../build/
solc --overwrite --optimize --bin --abi --add-std ethlanceSponsorWallet.sol -o ../build/
#solc --optimize --bin --abi ethlanceDB.sol -o ../build/
cd ../build
wc -c EthlanceUser.bin | awk '{print "EthlanceUser: " $1}'
wc -c EthlanceUser2.bin | awk '{print "EthlanceUser2: " $1}'
wc -c EthlanceJob.bin | awk '{print "EthlanceJob: " $1}'
wc -c EthlanceContract.bin | awk '{print "EthlanceContract: " $1}'
wc -c EthlanceFeedback.bin | awk '{print "EthlanceFeedback: " $1}'
wc -c EthlanceInvoice.bin | awk '{print "EthlanceInvoice: " $1}'
wc -c EthlanceConfig.bin | awk '{print "EthlanceConfig: " $1}'
wc -c EthlanceViews.bin | awk '{print "EthlanceViews: " $1}'
wc -c EthlanceSearchJobs.bin | awk '{print "EthlanceSearchJobs: " $1}'
wc -c EthlanceSearchFreelancers.bin | awk '{print "EthlanceSearchFreelancers: " $1}'
wc -c EthlanceMessage.bin | awk '{print "EthlanceMessage: " $1}'
wc -c EthlanceSponsor.bin | awk '{print "EthlanceSponsor: " $1}'
wc -c EthlanceSponsorWallet.bin | awk '{print "EthlanceSponsorWallet: " $1}'
wc -c EthlanceDB.bin | awk '{print "EthlanceDB: " $1}'

#rename -f 's/(.*):(.*)/$2/' *:*