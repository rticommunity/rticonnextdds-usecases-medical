#!/bin/sh

filename=$0
script_dir=`dirname $filename`
executable_name="PatientDeviceGenerator"
platform=`uname`
bin_dir=$script_dir/../objs/$platform/PatientDevices

if [ -f "$bin_dir/$executable_name" ]
then
    cd "$bin_dir"
    ./$executable_name $*
else
    echo "***************************************************************"
    echo $executable_name executable does not exist in:
    echo $bin_dir
    echo ""
    echo Please, try to recompile the application using the command:
    echo " $ make -f make/Makefile.<architecture>"
    echo "***************************************************************"
fi
