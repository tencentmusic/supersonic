rm -rf supersonic-webapp.tar.gz

npm i

npx lerna add supersonic-chat-sdk --scope supersonic-fe

npx lerna bootstrap

npx lerna exec --scope supersonic-chat-sdk npm run build

npx lerna exec --scope supersonic-fe npm run build:os-local

cd ./packages/supersonic-fe

tar -zcvf supersonic-webapp.tar.gz ./supersonic-webapp

mv supersonic-webapp.tar.gz ../../

cd ../../
