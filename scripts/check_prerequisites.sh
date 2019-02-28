#!/bin/bash
# Check the commandline prerequisites

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color


check_command() {
    command -v $1 >/dev/null 2>&1
    if [ $? -eq 0 ]; then
	printf "$1 --> ${GREEN}Success!${NC}\n"
	exit 0
    else
	printf "$1 --X ${RED}Missing!${NC}\n"
	exit -1
    fi
}


check_command sed
check_command lein
check_command npm
check_command ipfs
check_command solc
check_command ganache-cli
check_command lessc
check_command less-watch-compiler
