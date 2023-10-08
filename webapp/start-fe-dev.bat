@echo off

for /f "delims=" %%i in ('node -v') do set "node_version=%%i"

for /f "tokens=2 delims=v." %%i in ("%node_version%") do set "major_version=%%i"

if %major_version% GEQ 17 (
  set "NODE_OPTIONS=--openssl-legacy-provider"
  echo Node.js version is greater than or equal to 17. NODE_OPTIONS has been set to --openssl-legacy-provider.
)
where /q pnpm
if errorlevel 1 (
  echo pnpm is not installed. Installing...
  npm install -g pnpm
  if errorlevel 1 (
    echo Failed to install pnpm. Please check if npm is installed and the network connection is working.
  ) else (
    echo pnpm installed successfully.
  )
) else (
  echo pnpm is already installed.
)

rmdir /s /q ".\packages\supersonic-fe\src\.umi"
rmdir /s /q ".\packages\supersonic-fe\src\.umi-production"

cd ./packages/chat-sdk

call pnpm i

call pnpm run build

call pnpm link --global

cd ../supersonic-fe

call pnpm link ../chat-sdk

call pnpm i

call pnpm start
