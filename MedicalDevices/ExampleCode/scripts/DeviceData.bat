@echo off
setlocal

IF []==[%NDDSHOME%] GOTO NeedNDDSHome

set dir=%~dp0
set executable_name=rtireplay


cd %dir%\..\replay

REM Remove quotation marks
set NDDSHOME=%NDDSHOME:"=%


start "ECG Replay" "%NDDSHOME%\bin\%executable_name%" -cfgFile device_replay.xml -cfgName replayECGDevice
start "Pulse Oximeter Replay" "%NDDSHOME%\bin\%executable_name%" -cfgFile device_replay.xml -cfgName replayPODevice
EXIT /B 0

:NeedNDDSHome
@echo NDDSHOME must be set to the RTI Connext install dir to run this example
EXIT /B 1
