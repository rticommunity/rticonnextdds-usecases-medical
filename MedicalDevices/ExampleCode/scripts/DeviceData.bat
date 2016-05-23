@echo off
setlocal

IF []==[%NDDSHOME%] (
  @echo The NDDSHOME environment variable must be set to install_dir\rti_connext_dds-x.x.x to run this example
)

set dir=%~dp0
set executable_name=rtireplay


cd %dir%\..\replay

REM Remove quotation marks
set NDDSHOME=%NDDSHOME:"=%


start "ECG Replay" "%NDDSHOME%\bin\%executable_name%" -cfgFile device_replay.xml -cfgName replayECGDevice
start "Pulse Oximeter Replay" "%NDDSHOME%\bin\%executable_name%" -cfgFile device_replay.xml -cfgName replayPODevice
