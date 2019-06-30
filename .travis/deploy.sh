#!/bin/bash

set -e

cd $TRAVIS_BUILD_DIR

if [[ $TRAVIS_TAG =~ ^micro.* ]]
then
  echo "Deploying SSC locally"
  git clone git@github.com:snowplow/snowplow.git && cd snowplow/2-collectors/scala-stream-collector/ && sbt publishLocal && cd ../../..
  echo "Compiling and publishing Micro to Bintray"
  sbt docker:publish
else
  echo "Tag doesn't start with micro and isn't a release tag."
fi
