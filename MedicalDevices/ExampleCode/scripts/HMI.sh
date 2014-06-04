#!/bin/sh

filename=$0
script_dir=`dirname $filename`
absolute_script_dir=`cd $script_dir; pwd`
hmi_dir=$script_dir/../src/HMI/

cd $hmi_dir
export LD_LIBRARY_PATH=$PWD:$NDDSHOME/lib/x64Linux3.xgcc4.6.3jdk
export RTI_JAR_DIR=$NDDSHOME/class

java -classpath $absolute_script_dir/../src/HMI/bin:$RTI_JAR_DIR/nddsjava.jar com.rti.medical.ICEAlarmDisplayApp
