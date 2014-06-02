@ECHO off

set dir=%~dp0

set PATH=%NDDSHOME%\lib\i86Win32jdk;%NDDSHOME%\scripts;%PATH%

cd %dir%..\src\Idl 

call "%NDDSHOME%\scripts\rtiddsgen" %1 -replace -language C++ -namespace -d ..\Generated

cd %dir%

