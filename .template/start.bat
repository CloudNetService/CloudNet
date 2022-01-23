@echo off

@rem check if java is installed
WHERE java >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
  ECHO No valid java installation was found, please install java in order to run CloudNet
  EXIT /b 1
) ELSE (
  java -Xms256M -Xmx256M -XX:+UseZGC -XX:+PerfDisableSharedMem -XX:+DisableExplicitGC -jar launcher.jar
)
