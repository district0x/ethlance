#!/usr/bin/env bash
# Adds the given directory to IPFS, and returns the HASH and URL.
# publish-ipfs.sh <directory>
#
# Also creates two files:
# - $IPFS_HASH_FILE: Contains the ipfs hash of the given directory
# - $IPFS_URL_FILE:  Contains the URL of the given directory

CWD=`pwd`

IPFS_HASH_FILE=.site-hash
IPFS_URL_FILE=.site-url

IPFS_GATEWAY="https://ipfs.io"

echo "Site: $CWD/$1"
echo "File: $CWD/$IPFS_HASH_FILE"

HASH=`ipfs add -qr $1 | tail -n 1 | cut -d" " -f1`
URL="$IPFS_GATEWAY/ipfs/$HASH"

echo "HASH: $HASH"
echo "$HASH" > $IPFS_HASH_FILE

echo "URL:  $URL"
echo "$URL" > $IPFS_URL_FILE
