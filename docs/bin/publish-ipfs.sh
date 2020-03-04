#!/usr/bin/env bash

CWD=`pwd`
IPFS_HASH_FILE=.site-hash
IPFS_GATEWAY="https://ipfs.io"
echo "Site: $CWD/$1"
echo "File: $CWD/$IPFS_HASH_FILE"
HASH=`ipfs add -qr $1 | tail -n 1 | cut -d" " -f1`
URL="$IPFS_GATEWAY/ipfs/$HASH"
echo "HASH: $HASH"
echo "URL:  $URL"
echo "$HASH" > $IPFS_HASH_FILE
