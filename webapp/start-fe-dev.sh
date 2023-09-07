#!/bin/bash

node_version=$(node -v)

major_version=$(echo $node_version | cut -d'.' -f1 | tr -d 'v')

if [ $major_version -ge 17 ]; then
  export NODE_OPTIONS=--openssl-legacy-provider
fi

if ! command -v pnpm >/dev/null 2>&1; then
  npm i -g pnpm
fi

rm -rf ./packages/supersonic-fe/src/.umi ./packages/supersonic-fe/src/.umi-production

cd ./packages/chat-sdk

pnpm i

pnpm run build

pnpm link --global

cd ../supersonic-fe

pnpm link ../chat-sdk

pnpm i

pnpm start
