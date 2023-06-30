#!/bin/bash
npm i

if [ $? -ne 0 ]; then
    echo "npm i failed"
    exit 1
fi

npm run build
if [ $? -ne 0 ]; then
  echo "build failed"
  exit 1
fi

rm -rf dist.zip
zip -r dist.zip ./dist/
mkdir -p bin
mv dist.zip bin/
tar czf dist.tar.gz bin/dist.zip
