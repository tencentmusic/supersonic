#!/usr/bin/env bash

# environment parameters
python_path=${PYTHON_PATH:-"python3"}
pip_path=${PIP_PATH:-"pip3"}

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/../runtime
buildDir=$baseDir/build

readonly CHAT_APP_NAME="supersonic_chat"
readonly SEMANTIC_APP_NAME="supersonic_semantic"
readonly LLMPARSER_APP_NAME="supersonic_llmparser"
readonly STANDALONE_APP_NAME="supersonic_standalone"
readonly CHAT_SERVICE="chat"
readonly SEMANTIC_SERVICE="semantic"
readonly LLMPARSER_SERVICE="llmparser"
readonly STANDALONE_SERVICE="standalone"
readonly LLMPARSER_HOST="127.0.0.1"
readonly LLMPARSER_PORT="9092"

function setEnvToWeb {
   model_name=$1
   json='{"env": "'$model_name'"}'
   echo $json > ${runtimeDir}/supersonic-${model_name}/webapp/supersonic.config.json
   echo $json > $baseDir/../launchers/${model_name}/target/classes/webapp/supersonic.config.json
}

function moveToRuntime {
  model_name=$1
  tar -zxvf ${buildDir}/supersonic-${model_name}.tar.gz  -C ${runtimeDir}
  mv ${runtimeDir}/launchers-${model_name}-* ${runtimeDir}/supersonic-${model_name}

  mkdir -p ${runtimeDir}/supersonic-${model_name}/webapp
  cp -fr  ${buildDir}/webapp/* ${runtimeDir}/supersonic-${model_name}/webapp
}

function moveAllToRuntime {
  mkdir -p ${runtimeDir}
  tar xvf  ${buildDir}/supersonic-webapp.tar.gz  -C ${buildDir}
  mv ${buildDir}/supersonic-webapp ${buildDir}/webapp

  moveToRuntime chat
  moveToRuntime semantic
  moveToRuntime standalone
  rm -fr  ${buildDir}/webapp
}

# run java service
function runJavaService {
  javaRunDir=${runtimeDir}/supersonic-${model_name}
  local_app_name=$1
  libDir=$javaRunDir/lib
  confDir=$javaRunDir/conf

  CLASSPATH=""
  CLASSPATH=$CLASSPATH:$confDir

  for jarPath in $libDir/*.jar; do
   CLASSPATH=$CLASSPATH:$jarPath
  done

  export CLASSPATH
  export LANG="zh_CN.UTF-8"

  cd $javaRunDir
  if [[ "$JAVA_HOME" == "" ]]; then
    JAVA_HOME=$(ls /usr/jdk64/jdk* -d 2>/dev/null | xargs | awk '{print "'$local_app_name'"}')
  fi
  export PATH=$JAVA_HOME/bin:$PATH
  command="-Dfile.encoding="UTF-8"  -Duser.language="Zh" -Duser.region="CN" -Duser.timezone="GMT+08" -Dapp_name=${local_app_name} -Xms1024m -Xmx2048m "$main_class

  mkdir -p $javaRunDir/logs
  if [[ "$is_test" == "true" ]]; then
    java -Dspring.profiles.active="dev" $command >/dev/null 2>$javaRunDir/logs/error.log &
  else
    java  $command $javaRunDir >/dev/null 2>$javaRunDir/logs/error.log &
  fi
}

# run python service
function runPythonService {
  pythonRunDir=${runtimeDir}/supersonic-${model_name}/llmparser
  cd $pythonRunDir
  nohup ${python_path} supersonic_llmparser.py  > $pythonRunDir/llmparser.log  2>&1   &
  # add health check
  for i in {1..10}
  do
    echo "llmparser health check attempt $i..."
    response=$(curl -s http://${LLMPARSER_HOST}:${LLMPARSER_PORT}/health)
    echo "llmparser health check response: $response"
    status_ok="Healthy"
    if [[ $response == *$status_ok* ]] ; then
      echo "llmparser Health check passed."
      break
    else
      if [ "$i" -eq 10 ]; then
        echo "llmparser Health check failed after 10 attempts."
        echo "May still downloading model files. Please check llmparser.log in runtime directory."
      fi
      echo "Retrying after 5 seconds..."
      sleep 5
    fi
  done
}
