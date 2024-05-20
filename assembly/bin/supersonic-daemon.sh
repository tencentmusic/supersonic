#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
source $sbinDir/supersonic-common.sh

set -a
source $sbinDir/../conf/supersonic-env.sh
set +a

command=$1
service=$2
if [ -z "$service"  ]; then
  service=${STANDALONE_SERVICE}
fi

model_name=$service
if [ "$service" == "pyllm" ]; then
  model_name=${STANDALONE_SERVICE}
  export llmProxy=PythonLLMProxy
fi
cd $baseDir

function setMainClass {
  if [ "$service" == $CHAT_SERVICE ]; then
    main_class="com.tencent.supersonic.ChatLauncher"
  elif [ "$service" == $HEADLESS_SERVICE ]; then
    main_class="com.tencent.supersonic.HeadlessLauncher"
  else
    main_class="com.tencent.supersonic.StandaloneLauncher"
  fi
}

function setAppName {
  if [ "$service" == $CHAT_SERVICE ]; then
    app_name=$CHAT_APP_NAME
  elif [ "$service" == $HEADLESS_SERVICE ]; then
    app_name=$HEADLESS_APP_NAME
  else
    app_name=$STANDALONE_APP_NAME
  fi
}

function reloadExamples {
  cd $baseDir/pyllm/sql
  ${python_path} examples_reload_run.py
}

function runJavaService {
  javaRunDir=$baseDir
  local_app_name=$1
  libDir=$baseDir/lib
  confDir=$baseDir/conf

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
  pythonRunDir=$baseDir/pyllm
  cd $pythonRunDir
  nohup ${python_path} supersonic_pyllm.py  > $pythonRunDir/pyllm.log  2>&1   &
  # add health check
  for i in {1..10}
  do
    echo "pyllm health check attempt $i..."
    response=$(curl -s http://${PYLLM_HOST}:${PYLLM_PORT}/health)
    echo "pyllm health check response: $response"
    status_ok="Healthy"
    if [[ $response == *$status_ok* ]] ; then
      echo "pyllm Health check passed."
      break
    else
      if [ "$i" -eq 10 ]; then
        echo "pyllm Health check failed after 10 attempts."
        echo "May still downloading model files. Please check pyllm.log in runtime directory."
      fi
      echo "Retrying after 5 seconds..."
      sleep 5
    fi
  done
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

setMainClass
setAppName
case "$command" in
  start)
        if [ "$service" == $PYLLM_SERVICE ]; then
          echo  "Starting $PYLLM_APP_NAME"
          start $PYLLM_APP_NAME
        fi
        echo  "Starting ${app_name}"
        start ${app_name}
        echo  "Start success"
        ;;
  stop)
        echo  "Stopping $app_name"
        stop $app_name
        echo  "Stopping $PYLLM_APP_NAME"
        stop $PYLLM_APP_NAME
        echo  "Stop success"
        ;;
  reload)
        echo  "Reloading ${app_name}"
        reload ${app_name}
        echo  "Reload success"
        ;;
  restart)
        if [ "$service" == $PYLLM_SERVICE ]; then
          echo  "Stopping $PYLLM_APP_NAME"
          stop $PYLLM_APP_NAME
          echo  "Starting $PYLLM_APP_NAME"
          start $PYLLM_APP_NAME
        fi
        echo  "Stopping ${app_name}"
        stop ${app_name}
        echo  "Starting ${app_name}"
        start ${app_name}
        echo  "Restart success"
        ;;
  *)
        echo "Use command {start|stop|restart} to run."
        exit 1
esac
