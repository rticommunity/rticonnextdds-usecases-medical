@echo off
setlocal

IF []==[%REPLAY_HOME%] (
  @echo The REPLAY_HOME environment variable must be set to Connext_install_dir/RTI Recording Service 5.1.0 to run this example
)

set dir=%~dp0
set executable_name=rtireplay


cd %dir%\..\replay

call %REPLAY_HOME%\scripts\%executable_name% -cfgFile device_replay.xml -cfgName replayDevice
