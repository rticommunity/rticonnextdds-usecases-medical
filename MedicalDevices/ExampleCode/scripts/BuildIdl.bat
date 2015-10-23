@ECHO off

set dir=%~dp0

set PATH=%NDDSHOME%\lib\i86Win32jdk;%NDDSHOME%\bin;%PATH%

cd %dir%..\src\Idl 

call "%NDDSHOME%\bin\rtiddsgen" %1 -replace -language C++ -namespace -d ..\Generated

cd %dir%

