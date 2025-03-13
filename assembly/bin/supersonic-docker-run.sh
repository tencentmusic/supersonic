#!/usr/bin/env sh

export SUPERSONIC_VERSION=latest

#### Set below DB configs to connect to your own database
# Supported DB_TYPE:  h2, mysql, postgres
export S2_DB_TYPE=h2
export S2_DB_HOST=
export S2_DB_PORT=
export S2_DB_USER=
export S2_DB_PASSWORD=
export S2_DB_DATABASE=

docker run --rm -it -d \
  --name supersonic_standalone \
  -p 9080:9080 \
  -e S2_DB_TYPE=${S2_DB_TYPE} \
  -e S2_DB_HOST=${S2_DB_HOST} \
  -e S2_DB_PORT=${S2_DB_PORT} \
  -e S2_DB_USER=${S2_DB_USER} \
  -e S2_DB_PASSWORD=${S2_DB_PASSWORD} \
  -e S2_DB_DATABASE=${S2_DB_DATABASE} \
  supersonicbi/supersonic:${SUPERSONIC_VERSION}