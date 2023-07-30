#!/usr/bin/env bash

binDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(readlink -f $binDir/../)
source ${baseDir}/bin/env.sh

command=$1

function start()
{
  pid=$(ps aux |grep ${start_name} | grep -v grep )
  if [[ "$pid" == "" ]]; then
    $baseDir/bin/run.sh && echo "Process started."
    return 0
  else
    echo "Process (PID = $pid) is running."
    return 1
  fi
}

function stop()
{
  pid=$(ps aux | grep ${start_name} | grep -v grep | awk '{print $2}')
  if [[ "$pid" == "" ]]; then
    echo "Process is not running !"
    return 1
  else
    kill -9 $pid
    echo "Process (PID = $pid) is killed !"
    return 0
  fi
}

case "$command" in
  start)
        echo -e "Starting ${start_name}"
        start
        ;;
  stop)
        echo -e "Stopping ${start_name}"
        stop
        ;;
  restart)
        echo -e "Resetting ${start_name}E"
        stop
        start
        ;;
  *)
        echo "Use command {start|stop|status|restart} to run."
        exit 1
esac

exit 0
