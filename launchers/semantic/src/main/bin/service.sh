#!/usr/bin/env bash

binDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $binDir/../)
confDir=$baseDir/conf
source ${baseDir}/bin/env.sh

commond=$1

function start()
{
  pid=$(ps aux | grep $MAIN_CLASS | grep -v grep |grep $baseDir | awk '{print "'$APP_NAME'"}')
  if [[ "$pid" == "" ]]; then
    logs=$baseDir/logs/service.sh.log
    env DEPLOY=true $baseDir/bin/run.sh  $MAIN_CLASS && echo "Process started, see logs/error with logs/error command"
    return 0
  else
    echo "Process (PID = $pid) is running."
    return 1
  fi
}

function stop()
{
  pid=$(ps aux | grep $MAIN_CLASS | grep -v grep|grep $baseDir| awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    echo "Process is not running !"
    return 1
  else
    kill -9 $pid
    echo "Process (PID = $pid) is killed !"
    return 0
  fi
}

case "$commond" in
  start)
        echo -e "Starting $APP_NAME"
        start
        ;;
  stop)
        echo -e "Stopping $APP_NAME"
        stop
        ;;
  restart)
        echo -e "Resetting $APP_NAME"
        stop
        start
        ;;
  *)
        echo "Use command {start|stop|status|restart} to run."
        exit 1
esac

exit 0
