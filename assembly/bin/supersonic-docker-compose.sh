#!/usr/bin/env bash

# Start SuperSonic via Docker Compose
# Reads configuration from docker/.env file

sbinDir=$(cd "$(dirname "$0")"; pwd)
source "$sbinDir/supersonic-common.sh"

cd "$projectDir/docker"
docker compose up -d
