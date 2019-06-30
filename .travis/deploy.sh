#!/bin/bash

set -e

cd $TRAVIS_BUILD_DIR

if [[ $TRAVIS_TAG =~ ^micro.* ]]
then
  git clone git@github.com:snowplow/snowplow.git && cd snowplow/2-collectors/scala-stream-collector/ && sbt publishLocal && cd ../../..
  sbt docker:publish
else
  echo "Tag doesn't start with micro and isn't a release tag."
fi
