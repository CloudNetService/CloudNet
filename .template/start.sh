#!/bin/sh

cd "$(dirname "$(readlink -fn "$0")")" || exit 1

# check if java is installed
if [ -x "$(command -v java)" ]; then
  # if screen is present use that
  # this check is elevated as tmux is sometimes present by default
  if [ -x "$(command -v screen)" ]; then
    screen -DRSq CloudNet java -Xms256M -Xmx256M -XX:+UseZGC -XX:+PerfDisableSharedMem -XX:+DisableExplicitGC -jar launcher.jar
  elif [ -x "$(command -v tmux)" ]; then
    tmux new-session -As CloudNet java -Xms256M -Xmx256M -XX:+UseZGC -XX:+PerfDisableSharedMem -XX:+DisableExplicitGC -jar launcher.jar
  else
    echo "No screen or tmux installation found, you need to install at least one of them to run CloudNet"
    exit 1
  fi
else
  echo "No valid java installation was found, please install java in order to run CloudNet"
  exit 1
fi
