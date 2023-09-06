setlocal


call npm i

call npx lerna add supersonic-chat-sdk --scope supersonic-fe

call npx lerna bootstrap

call npx lerna exec --scope supersonic-chat-sdk npm run build

call npx lerna exec --scope supersonic-fe npm run build:os-local

cd packages\supersonic-fe

tar -zcvf supersonic-webapp.tar.gz supersonic-webapp

move supersonic-webapp.tar.gz ..\..\

cd ..

endlocal