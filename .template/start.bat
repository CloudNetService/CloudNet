@echo off

@rem check if java is installed
WHERE java >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
  ECHO No valid java installation was found, please install java in order to run CloudNet
  EXIT /b 1
) ELSE (
:: DO NOT CHANGE THE SUPPLIED MEMORY HERE. THIS HAS NO EFFECT ON THE NODE INSTANCE. USE THE launcher.cnl INSTEAD
  java -Xms128M -Xmx128M -XX:+UseZGC -XX:+PerfDisableSharedMem -jar launcher.jar
)
