#!/bin/bash

cd "$(dirname "$0")" || exit 1

# check if java is installed
if [ -x "$(command -v java)" ]; then
  java -Xms256M -Xmx256M -XX:+UseZGC -XX:+PerfDisableSharedMem -XX:+DisableExplicitGC -jar launcher.jar
else
  echo "No valid java installation was found, please install java in order to run CloudNet"
  exit 1
fi
