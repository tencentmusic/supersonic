#!/bin/bash

start=$(date +%s)

node_version=$(node -v)

major_version=$(echo $node_version | cut -d'.' -f1 | tr -d 'v')

if [ $major_version -ge 17 ]; then
  export NODE_OPTIONS=--openssl-legacy-provider
fi

if ! command -v pnpm >/dev/null 2>&1; then
  npm i -g pnpm
fi

rm -rf supersonic-webapp.tar.gz

rm -rf ./packages/supersonic-fe/src/.umi ./packages/supersonic-fe/src/.umi-production

cd ./packages/chat-sdk

pnpm i

pnpm run build

pnpm link --global

cd ../supersonic-fe

pnpm link ../chat-sdk

pnpm i

pnpm run build:os-local

tar -zcvf supersonic-webapp.tar.gz ./supersonic-webapp

mv supersonic-webapp.tar.gz ../../

cd ../../

end=$(date +%s)

take=$(( end - start ))

echo Time taken to execute commands is ${take} seconds.
