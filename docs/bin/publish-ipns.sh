#!/usr/bin/env bash
# Adds the given IPFS hash to IPNS as defined by the key $IPNS_ETHLANCE_DOC_KEY
# publish-ipns.sh <IPFS HASH>
CWD=`pwd`

# Location of the generated IPNS keys
IPFS_KEYSTORE_DIRECTORY=~/.ipfs/keystore

# Name of the unique key
IPNS_ETHLANCE_DOC_KEY=ethlance-docs-public-html.ipns.key

# Gateway to use for published ipfs data
IPNS_GATEWAY="https://ipfs.io"

# Make sure there's only one argument
if [ "$#" -ne 1 ]; then
  echo "Wrong number of arguments: $#" >&2
  echo "publish-ipns.sh <IPFS HASH>" >&2
  exit 1
fi

# Check to make sure the ipns key is in the keystore
if [ ! -f "$IPFS_KEYSTORE_DIRECTORY/$IPNS_ETHLANCE_DOC_KEY" ]; then
  echo "Missing Key within Keystore: $IPFS_KEYSTORE_DIRECTORY/$IPNS_ETHLANCE_DOC_KEY" >&2
  exit 2
fi

# Begin Publishing
echo "Publishing Hash $1 to IPNS <$IPNS_ETHLANCE_DOC_KEY> ..."
IPNS_HASH=`ipfs name publish --key=$IPNS_ETHLANCE_DOC_KEY $1 | cut -d" " -f3 | cut -d":" -f1`
if [ $? -eq 0 ]; then
  echo "Finished Publishing!"
  echo "IPNS HASH: $IPNS_HASH"
  echo "IPNS SITE: $IPNS_GATEWAY/ipns/$IPNS_HASH"
else
  echo "Failed to Publish to IPNS, exiting..." >&2
  exit 3
fi
