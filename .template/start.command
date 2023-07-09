#!/bin/bash
cd "$(dirname "$0")" || exit 1

# check if java is installed
if [ -x "$(command -v java)" ]; then
  # DO NOT CHANGE THE SUPPLIED MEMORY HERE. THIS HAS NO EFFECT ON THE NODE INSTANCE. USE THE launcher.cnl INSTEAD
  java -Xms128M -Xmx128M -XX:+UseZGC -XX:+PerfDisableSharedMem -XX:+DisableExplicitGC -jar launcher.jar
else
  echo "No valid java installation was found, please install java in order to run CloudNet"
  exit 1
fi
