#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
buildDir=$baseDir/build

cd $baseDir/bin
sh build-standalone.sh

cd $buildDir
tar xvf supersonic-webapp.tar.gz
mv supersonic-webapp webapp
mv webapp ../../launchers/standalone/target/classes