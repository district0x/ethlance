#!/bin/bash

NAME=district0x/ethlance-ui
VERSION=$CIRCLE_SHA1
#VERSION=$(git log -1 --pretty=%h)
IMG=$NAME:$VERSION
TAG=latest

{
  echo "==============================================="
  echo  "Building: "$IMG" with tag "$TAG""
  echo "==============================================="

  # build and tag as latest
  docker build -t $IMG -f docker-builds/Dockerfile .
  docker tag $IMG $NAME:latest

} || {
  echo "EXCEPTION WHEN BUILDING "$IMG""
  exit 1
}

# dockerhub login
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

echo "==============================================="
echo "Pushing: " $NAME
echo "==============================================="

# push to dockerhub
docker push $NAME

echo "==============================================="
echo "DONE"
echo "==============================================="

exit $?
