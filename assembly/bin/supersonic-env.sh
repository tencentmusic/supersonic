#!/usr/bin/env bash

#### Set below DB configs to connect to your own database
# These use existing env vars if set, otherwise fall back to defaults.
# Supported DB_TYPE:  h2, mysql, postgres
export S2_DB_TYPE=${S2_DB_TYPE:-mysql}
export S2_DB_HOST=${S2_DB_HOST:-127.0.0.1}
export S2_DB_PORT=${S2_DB_PORT:-3306}
export S2_DB_USER=${S2_DB_USER:-root}
export S2_DB_PASSWORD=${S2_DB_PASSWORD:-root}
export S2_DB_DATABASE=${S2_DB_DATABASE:-supersonic}
