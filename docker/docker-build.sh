#!/usr/bin/env bash
# Please execute the "supersonic-build.sh" command first
# to generate the corresponding zip package in the "assembly/build/" directory.
docker build --no-cache --build-arg SUPERSONIC_VERSION=$1 -t supersonic:$1 -f docker/Dockerfile .