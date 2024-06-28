#!/usr/bin/env bash

# environment parameters
sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/runtime
buildDir=$baseDir/build
projectDir=$baseDir/..

readonly CHAT_APP_NAME="supersonic_chat"
readonly HEADLESS_APP_NAME="supersonic_headless"
readonly STANDALONE_APP_NAME="supersonic_standalone"

readonly CHAT_SERVICE="chat"
readonly HEADLESS_SERVICE="headless"
readonly STANDALONE_SERVICE="standalone"