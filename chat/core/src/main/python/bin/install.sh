#!/usr/bin/env bash

binDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $binDir/../)
echo $binDir

source ${binDir}/env.sh

${pip_path}  install -r $baseDir/requirements.txt

if ${python_path} -c "import langchain,fastapi,chromadb,tiktoken,uvicorn" >/dev/null 2>&1
then
    echo "install ok, will pass"
else
  if [ $? -eq 0 ]; then
     echo "install ok"
  else
     echo "install fail ,pls check your install error log. cmd is : "
     echo ${pip_path}  install -r $baseDir/requirements.txt
     exit 1
  fi
fi