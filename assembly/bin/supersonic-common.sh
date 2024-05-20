#!/usr/bin/env bash

# environment parameters
python_path=${PYTHON_PATH:-"python3"}
pip_path=${PIP_PATH:-"pip3"}

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/runtime
buildDir=$baseDir/build
projectDir=$baseDir/..

readonly CHAT_APP_NAME="supersonic_chat"
readonly HEADLESS_APP_NAME="supersonic_headless"
readonly PYLLM_APP_NAME="supersonic_pyllm"
readonly STANDALONE_APP_NAME="supersonic_standalone"

readonly CHAT_SERVICE="chat"
readonly HEADLESS_SERVICE="headless"
readonly PYLLM_SERVICE="pyllm"
readonly STANDALONE_SERVICE="standalone"

readonly PYLLM_HOST="127.0.0.1"
readonly PYLLM_PORT="9092"
