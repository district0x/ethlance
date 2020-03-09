#!/usr/bin/env bash
# Adds the given directory to IPFS, and returns the HASH and URL.
# publish-ipfs.sh <DIRECTORY>
#
# Also creates two files:
# - $IPFS_HASH_FILE: Contains the ipfs hash of the given directory
# - $IPFS_URL_FILE:  Contains the URL of the given directory

CWD=`pwd`

IPFS_HASH_FILE=.site-hash
IPFS_URL_FILE=.site-url

IPFS_GATEWAY="https://ipfs.io"

# Make sure there's only one argument
if [ "$#" -ne 1 ]; then
  echo "Wrong number of arguments: $#" >&2
  echo "publish-ipfs.sh <DIRECTORY>" >&2
  exit 1
fi

echo "Site:      $CWD/$1"

HASH=`ipfs add -qr $1 | tail -n 1 | cut -d" " -f1`
if [ $? -eq 0 ]; then
  URL="$IPFS_GATEWAY/ipfs/$HASH"

  echo "HASH:      $HASH"
  echo "$HASH" > $IPFS_HASH_FILE
  echo "HASH FILE: $CWD/$IPFS_HASH_FILE"

  echo "URL:       $URL"
  echo "$URL" > $IPFS_URL_FILE
  echo "URL FILE:  $CWD/$IPFS_URL_FILE"
else
  echo "ERROR: Failed to add site" >&2
  exit 2
fi
