@echo off

setlocal

set dir=%~dp0

cd %dir%\..\src\HMI\

set libDir=%NDDSHOME%\lib

set i86Win32Libs=i86Win32VS2015 i86Win32VS2013 i86Win32VS2012 i86Win32VS2010 i86Win32VS2008 i86Win32VS2005 i86Win32VS2003 i86Win32VC70 i86Win32VC60
set x64Win64Libs=x64Win64VS2015 x64Win64VS2013 x64Win64VS2012 x64Win64VS2010 x64Win64VS2008 x64Win64VS2005 %i86Win32Libs%
if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
    set "platformsToTry=%x64Win64Libs%"
) else if "%PROCESSOR_ARCHITEW6432%"=="AMD64" (
    set "platformsToTry=%x64Win64Libs%"
) else if "%PROCESSOR_ARCHITECTURE%"=="x86" (
    set "platformsToTry=%i86Win32Libs%"
) else (
    echo Processor "%PROCESSOR_ARCHITECTURE%" not supported. Please contact support@rti.com.
    exit /b 1
)

for %%a in (%platformsToTry%) do (
    if exist "%libDir%\%%a\nddscore.dll" (
                set platformName=%%a
                goto break
    )
)

@REM We need to use goto to be able to break in the for loop.
:break

@REM Return error if platformName is not set. That is, the script could not
@REM find libraries for your environment in the install directory..
if not defined platformName (
    echo Cannot find libraries for your system under "%libDir%". Please install a target package.
	exit /b 1
)

set RTI_JAR_DIR="%NDDSHOME%\lib\java"
set RTI_DLL_DIR="%NDDSHOME%\lib\%platformName%"

REM Remove any existing quotes
set RTI_DLL_DIR=%RTI_DLL_DIR:"=%

set PATH=%RTI_DLL_DIR%;%PATH%

call java -classpath %dir%\..\src\HMI\bin;%RTI_JAR_DIR%\nddsjava.jar com.rti.medical.ICEAlarmDisplayApp


cd %dir%