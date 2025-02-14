#!/bin/bash
# This script is used to create index pattern for AI SQL
# Usage: ./opensearch_create_index.sh
# Note: Please make sure the opensearch is running and the index pattern is not exist。
# To confirm the vector dimension, the default is 512.
# If you need to modify it, please first adjust the corresponding index configuration.
# configure the following parameters for opensearch
# es_host: the host of opensearch
# es_user: the user of opensearch
# es_password: the password of opensearch
# es_index_prefix: the index prefix of opensearch
es_host="https://opensearch-node:7799"
es_user="admin"
es_password="admin"
es_index_prefix="ai_sql"

echo "Creating index pattern for AI SQL"
echo "creating index ${es_index_prefix}_meta_collection"
curl -X PUT "${es_host}/${es_index_prefix}_meta_collection" \
  -u "${es_user}:${es_password}" \
  -H "Content-Type: application/json" \
  -d @ai_sql_meta_collection.json

echo "creating index ${es_index_prefix}_text2dsl_agent_collection"
curl -X PUT "${es_host}/${es_index_prefix}_text2dsl_agent_collection" \
  -u "${es_user}:${es_password}" \
  -H "Content-Type: application/json" \
  -d @ai_sql_text2dsl_agent_collection.json

echo "creating index ${es_index_prefix}_preset_query_collection"
curl -X PUT "${es_host}/${es_index_prefix}_preset_query_collection" \
  -u "${es_user}:${es_password}" \
  -H "Content-Type: application/json" \
  -d @ai_sql_preset_query_collection.json

for i in {1..10}; do
  echo "creating index ${es_index_prefix}_memory_${i}"
  curl -X PUT "${es_host}/${es_index_prefix}_memory_${i}" \
    -u "${es_user}:${es_password}" \
    -H "Content-Type: application/json" \
    -d @ai_sql_memory.json
done
