#!/bin/bash

set -e

cd $TRAVIS_BUILD_DIR

if [[ $TRAVIS_TAG =~ ^micro.* ]]
then
  sbt docker:publish
else
  echo "Tag doesn't start with micro and isn't a release tag."
fi
