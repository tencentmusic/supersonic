
rmdir /s /q ".\packages\supersonic-fe\src\.umi"
rmdir /s /q ".\packages\supersonic-fe\src\.umi-production"

call npm i

call npx lerna add supersonic-chat-sdk --scope supersonic-fe

call npx lerna bootstrap

call npx lerna exec --scope supersonic-chat-sdk npm run build

call npx lerna exec --scope supersonic-fe npm start