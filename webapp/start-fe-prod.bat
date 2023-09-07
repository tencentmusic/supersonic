
setlocal

@echo off

:: 获取Node.js版本
for /f "delims=" %%i in ('node -v') do set "node_version=%%i"

:: 提取主版本号
for /f "tokens=2 delims=v." %%i in ("%node_version%") do set "major_version=%%i"

if %major_version% GEQ 17 (
  set "NODE_OPTIONS=--openssl-legacy-provider"
  echo Node.js版本大于等于17，已设置NODE_OPTIONS为--openssl-legacy-provider
)

:: 检查pnpm是否未安装
where /q pnpm
if errorlevel 1 (
  echo pnpm未安装，正在进行安装...
  npm install -g pnpm
  if errorlevel 1 (
    echo pnpm安装失败，请检查npm是否已安装并且网络连接正常
  ) else (
    echo pnpm安装成功
  )
) else (
  echo pnpm已安装
)

del /F /Q supersonic-webapp.tar.gz

rmdir /S /Q .\packages\supersonic-fe\src\.umi
rmdir /S /Q .\packages\supersonic-fe\src\.umi-production

cd ./packages/chat-sdk

call pnpm i

call pnpm run build

call pnpm link --global

cd ../supersonic-fe

call pnpm link ../chat-sdk

call pnpm i

call pnpm run build:os-local

tar -zcvf supersonic-webapp.tar.gz supersonic-webapp

move supersonic-webapp.tar.gz ..\..\

cd ..

endlocal