#!/usr/bin/env bash

llm_host=$1
llm_port=$2

binDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$binDir/.." && pwd -P)

source ${baseDir}/bin/env.sh

if [ "${llm_host}" = "" ] || [ "${llm_port}" = "" ]
then
  echo "llm_host llm_port is not set"
  exit 1
fi


if [ "${python_path}" = "" ] || [ "${pip_path}" = "" ]
then
  echo "please set env value python_path , pip_path to  python, pip path by export cmd "
  exit 1
fi

chmod +x $binDir/install.sh
$binDir/install.sh
if [ $? -ne 0 ]; then
    exit 1
fi

cd $baseDir/llm
nohup ${python_path} -m uvicorn ${start_name}:app --port ${llm_port} --host ${llm_host} > $baseDir/llm.log  2>&1   &