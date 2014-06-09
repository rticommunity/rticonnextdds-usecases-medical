@echo off
setlocal

IF []==[%REPLAY_HOME%] (
  @echo The REPLAY_HOME environment variable must be set to Connext_install_dir/RTI Recording Service 5.1.0 to run this example
)

set dir=%~dp0
set executable_name=rtireplay


cd %dir%\..\replay

REM Remove quotation marks
set REPLAY_HOME=%REPLAY_HOME:"=%


start "ECG Replay" "%REPLAY_HOME%\scripts\%executable_name%" -cfgFile device_replay.xml -cfgName replayECGDevice
start "Pulse Oximeter Replay" "%REPLAY_HOME%\scripts\%executable_name%" -cfgFile device_replay.xml -cfgName replayPODevice
