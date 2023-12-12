#!/usr/bin/env bash

set -x
sbinDir=$(cd "$(dirname "$0")"; pwd)
chmod +x $sbinDir/supersonic-common.sh
source $sbinDir/supersonic-common.sh

# 1.init environment parameters
if [ ! -d "$runtimeDir" ]; then
    echo "the runtime dir does not exist move all to runtime"
    moveAllToRuntime
fi
set +x

command=$1
service=$2
if [ -z "$service"  ]; then
  service=${STANDALONE_SERVICE}
fi

app_name=$STANDALONE_APP_NAME
main_class="com.tencent.supersonic.StandaloneLauncher"
model_name=$service

if [ "$service" == "pyllm" ]; then
  model_name=${STANDALONE_SERVICE}
  export llmProxy=PythonLLMProxy
fi

cd $baseDir

# 2.set main class
function setMainClass {
  if [ "$service" == $CHAT_SERVICE ]; then
    main_class="com.tencent.supersonic.ChatLauncher"
  elif [ "$service" == $SEMANTIC_SERVICE ]; then
    main_class="com.tencent.supersonic.SemanticLauncher"
  fi
}
setMainClass
# 3.set app name
function setAppName {
  if [ "$service" == $CHAT_SERVICE ]; then
    app_name=$CHAT_APP_NAME
  elif [ "$service" == $SEMANTIC_SERVICE ]; then
    app_name=$SEMANTIC_APP_NAME
  elif [ "$service" == $PYLLM_SERVICE ]; then
    app_name=$PYLLM_APP_NAME
  fi
}
setAppName

function reloadExamples {
  pythonRunDir=${runtimeDir}/supersonic-${model_name}/pyllm
  cd $pythonRunDir/sql
  ${python_path} examples_reload_run.py
}


function start()
{
  local_app_name=$1
  pid=$(ps aux |grep ${local_app_name} | grep -v grep | awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    if [[ ${local_app_name} == $PYLLM_APP_NAME ]]; then
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
  if [[ $1 == $PYLLM_APP_NAME ]]; then
    reloadExamples
  fi
}

# 4. execute command operation
case "$command" in
  start)
        if [ "$service" == $PYLLM_SERVICE ]; then
          echo  "Starting $app_name"
          start $app_name
          echo  "Starting $STANDALONE_APP_NAME"
          start $STANDALONE_APP_NAME
        else
          echo  "Starting $app_name"
          start $app_name
        fi
        echo  "Start success"
        ;;
  stop)
        if [ "$service" == $PYLLM_SERVICE ]; then
          echo  "Stopping $app_name"
          stop $app_name
          echo  "Stopping $STANDALONE_APP_NAME"
          stop $STANDALONE_APP_NAME
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
        if [ "$service" == $PYLLM_SERVICE ]; then
          echo  "Stopping ${app_name}"
          stop ${app_name}
          echo  "Stopping ${STANDALONE_APP_NAME}"
          stop $STANDALONE_APP_NAME
          echo  "Starting ${app_name}"
          start ${app_name}
          echo  "Starting ${STANDALONE_APP_NAME}"
          start $STANDALONE_APP_NAME
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
