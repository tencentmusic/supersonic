#!/usr/bin/env bash

path=$(pwd)
echo ${path}

python_path=${PYTHON_PATH:-"python3"}
pip_path=${PIP_PATH:-"pip3"}

requirementPath=$path/requirements.txt
${pip_path} install -r ${requirementPath}
echo "install python modules success"
python $path/evaluation.py
