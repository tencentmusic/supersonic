#!/usr/bin/env bash

python_path="/usr/local/bin/python3"
readonly CHAT_APP_NAME="supersonic_chat"
readonly SEMANTIC_APP_NAME="supersonic_semantic"
readonly LLMPARSER_APP_NAME="supersonic_llmparser"
readonly STANDALONE_APP_NAME="supersonic_standalone"
readonly CHAT_SERVICE="chat"
readonly SEMANTIC_SERVICE="semantic"
readonly LLMPARSER_SERVICE="llmparser"
readonly STANDALONE_SERVICE="standalone"

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/../runtime
buildDir=$baseDir/build

command=$1
service=$2

if [ -z "$service"  ]; then
  service=${STANDALONE_SERVICE}
fi

app_name=$STANDALONE_APP_NAME
main_class="com.tencent.supersonic.StandaloneLauncher"
model_name=$service

if [ "$service" == "llmparser" ]; then
  model_name=${STANDALONE_SERVICE}
fi

cd $baseDir

function setMainClass {
  if [ "$service" == $CHAT_SERVICE ]; then
    main_class="com.tencent.supersonic.ChatLauncher"
  elif [ "$service" == $SEMANTIC_SERVICE ]; then
    main_class="com.tencent.supersonic.SemanticLauncher"
  fi
}

function setAppName {
  if [ "$service" == $CHAT_SERVICE ]; then
    app_name=$CHAT_APP_NAME
  elif [ "$service" == $SEMANTIC_SERVICE ]; then
    app_name=$SEMANTIC_APP_NAME
  elif [ "$service" == $LLMPARSER_SERVICE ]; then
    app_name=$LLMPARSER_APP_NAME
  fi
}

setAppName
setMainClass

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

function runPythonService {
  pythonRunDir=${runtimeDir}/supersonic-${model_name}/llmparser
  cd $pythonRunDir
  nohup ${python_path} supersonic_llmparser.py  > $pythonRunDir/llmparser.log  2>&1   &
  sleep 4
}

function reloadExamples {
  pythonRunDir=${runtimeDir}/supersonic-${model_name}/llmparser
  cd $pythonRunDir/sql
  ${python_path} examples_reload_run.py
}

function getProcesName {
  process_name=$main_class
  if [[ ${app_name} == $LLMPARSER_APP_NAME ]]; then
    process_name=$app_name
  fi
  echo $process_name
}

function start()
{
  local_app_name=$1
  pid=$(ps aux |grep ${local_app_name} | grep -v grep | awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    if [[ ${local_app_name} == $LLMPARSER_APP_NAME ]]; then
      runPythonService ${local_app_name}
    else
      runJavaService ${local_app_name}
    fi
  else
    echo "Process (PID = $pid) is running."
    return 1
  fi
}

function stop()
{
  pid=$(ps aux | grep $1 | grep -v grep | awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    echo "Process $1 is not running !"
    return 1
  else
    kill -9 $pid
    echo "Process (PID = $pid) is killed !"
    return 0
  fi
}

function reload()
{
  if [[ $1 == $LLMPARSER_APP_NAME ]]; then
    reloadExamples
  fi
}

case "$command" in
  start)
        if [ "$service" == $STANDALONE_SERVICE ]; then
          echo  "Starting $LLMPARSER_APP_NAME"
          start $LLMPARSER_APP_NAME
          echo  "Starting $app_name"
          start $app_name
        else
          echo  "Starting $app_name"
          start $app_name
        fi
        echo  "Start success"
        ;;
  stop)
        if [ "$service" == $STANDALONE_SERVICE ]; then
          echo  "Stopping $LLMPARSER_APP_NAME"
          stop $LLMPARSER_APP_NAME
          echo  "Stopping $app_name"
          stop $app_name
        else
          echo  "Stopping $app_name"
          stop ${app_name}
        fi
        echo  "Stop success"
        ;;
  reload)
        echo  "Reloading ${app_name}"
        reload ${app_name}
        echo  "Reload success"
        ;;
  restart)
        if [ "$service" == $STANDALONE_SERVICE ]; then
          echo  "Stopping ${app_name}"
          stop ${app_name}
          echo  "Stopping ${LLMPARSER_APP_NAME}"
          stop $LLMPARSER_APP_NAME
          echo  "Starting ${LLMPARSER_APP_NAME}"
          start $LLMPARSER_APP_NAME
          echo  "Starting ${app_name}"
          start ${app_name}
        else
          echo  "Stopping ${app_name}"
          stop ${app_name}
          echo  "Starting ${app_name}"
          start ${app_name}
        fi
        echo  "Restart success"
        ;;
  *)
        echo "Use command {start|stop|restart} to run."
        exit 1
esac
