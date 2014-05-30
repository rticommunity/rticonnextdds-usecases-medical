@echo off
setlocal

set dir=%~dp0

set Path=%NDDSHOME%\lib\i86Win32jdk;%PATH%

cd %dir%\..\src\HMI

set RTI_JAR_DIR="%NDDSHOME%\class"
set RTI_DLL_DIR="%NDDSHOME%\lib\i86Win32jdk"

REM Remove any existing quotes
set RTI_DLL_DIR=%RTI_DLL_DIR:"=%

set PATH=%RTI_DLL_DIR%;%PATH%

call java -classpath %dir%\..\win32\Java\AlarmHMI.jar;%RTI_JAR_DIR%\nddsjava.jar com.rti.medical.ICEAlarmDisplayApp