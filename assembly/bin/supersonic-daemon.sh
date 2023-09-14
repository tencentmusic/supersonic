#!/usr/bin/env bash

sbinDir=$(cd "$(dirname "$0")"; pwd)
baseDir=$(cd "$sbinDir/.." && pwd -P)
runtimeDir=$baseDir/../runtime
buildDir=$baseDir/build

command=$1
service=$2

cd $baseDir
if [[ "$service" == "semantic" || -z "$service"  ]] && [ "$command" != "stop"  ]; then
   #1. clear file
   mkdir -p ${runtimeDir}
   rm -fr ${runtimeDir}/*

   #2. package lib
   tar -zxvf ${buildDir}/supersonic.tar.gz  -C ${runtimeDir}
   mv ${runtimeDir}/launchers-standalone-* ${runtimeDir}/supersonic-standalone
   tar -zxvf ${buildDir}/supersonic-webapp.tar.gz  -C ${buildDir}
   mkdir -p ${runtimeDir}/supersonic-standalone/webapp
   cp -fr  ${buildDir}/supersonic-webapp/* ${runtimeDir}/supersonic-standalone/webapp
   rm -fr  ${buildDir}/supersonic-webapp
fi
if [[ "$service" == "semantic"  ]]; then
   json=$(cat ${runtimeDir}/supersonic-semantic/webapp/supersonic.config.json)
   json=$(echo $json | jq '.env="semantic"')
   echo $json > ${runtimeDir}/supersonic-semantic/webapp/supersonic.config.json
fi

if [[ "$service" == "chat"  ]]; then
   json=$(cat ${runtimeDir}/supersonic-chat/webapp/supersonic.config.json)
   json=$(echo $json | jq '.env="chat"')
   echo $json > ${runtimeDir}/supersonic-chat/webapp/supersonic.config.json
fi
echo $command
echo $service
case "$command" in
  start)
        if [[ "$service" == "semantic"  ]];then
          echo -e "Starting semantic java service"
          sh ${runtimeDir}/supersonic-semantic/bin/service.sh start
        elif [[ "$service" == "chat"  ]];then
          echo -e "Starting chat java service"
          sh ${runtimeDir}/supersonic-chat/bin/service.sh start
        elif [[ "$service" == "llmparser"  ]];then
          echo -e "Starting llmparser python service"
          sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh  start
        elif [[ -z "$service"  ]]; then
          echo -e "Starting supersonic services"
          sh ${runtimeDir}/supersonic-standalone/bin/service.sh start
          sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh start
        else
          echo "Use command {chat|semantic|llmparser} to run."
        fi
        ;;
  stop)
        if [[ "$service" == "semantic"  ]];then
          echo -e "Stopping semantic java service"
          sh ${runtimeDir}/supersonic-semantic/bin/service.sh stop
        elif [[ "$service" == "chat"  ]];then
          echo -e "Stopping chat java service"
          sh ${runtimeDir}/supersonic-chat/bin/service.sh stop
        elif [[ "$service" == "llmparser"  ]];then
          echo -e "Stopping llmparser python service"
          sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh  stop
        elif [[ -z "$service"  ]]; then
          echo -e "Stopping supersonic services"
          sh ${runtimeDir}/supersonic-standalone/bin/service.sh stop
          sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh stop
        else
          echo "Use command {chat|semantic|llmparser} to run."
        fi
        ;;
  restart)
        if [[ "$service" == "semantic"  ]];then
          echo -e "Restarting semantic java service"
          sh ${runtimeDir}/supersonic-semantic/bin/service.sh restart
        elif [[ "$service" == "chat"  ]];then
          echo -e "Restarting chat java service"
          sh ${runtimeDir}/supersonic-chat/bin/service.sh restart
        elif [[ "$service" == "llmparser"  ]];then
          echo -e "Restarting llmparser python service"
          sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh  restart
        elif [[ -z "$service"  ]]; then
          echo -e "Restarting supersonic services"
          sh ${runtimeDir}/supersonic-standalone/bin/service.sh restart
          sh ${runtimeDir}/supersonic-standalone/llm/bin/service.sh restart
        else
          echo "Use command {chat|semantic|llmparser} to run."
        fi
        ;;
  *)
        echo "Use command {start|stop|status|restart} to run."
        exit 1
esac

exit 0
