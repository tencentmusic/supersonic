#!/usr/bin/env bash
llm_host=$1
llm_port=$2

baseDir=$(cd "$binDir/.." && pwd -P)

cd $baseDir/llm/sql

${python_path} examples_reload_run.py ${llm_port} ${llm_host}



