#!/usr/bin/env bash

IPFS_GATEWAY="https://ipfs.io"
echo "Site: $1"
HASH=`ipfs add -qr $1 | tail -n 1 | cut -d" " -f1`
echo "HASH: $HASH"
echo "URL:  $IPFS_GATEWAY/ipfs/$HASH/index.html"

