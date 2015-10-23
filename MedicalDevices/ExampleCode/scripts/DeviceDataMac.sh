#!/bin/sh

filename=$0
script_dir=`dirname $filename`
executable_name="rtireplay"

if [ -f "$NDDSHOME/bin/$executable_name" ]
then
    cd "$script_dir/../replay"
    bash -c "$NDDSHOME/bin/$executable_name \
        -cfgFile device_replay.xml \
        -cfgName replayECGDevice &"
    bash -c "$NDDSHOME/bin/$executable_name \
        -cfgFile device_replay.xml \
        -cfgName replayPODevice"

else
    echo "*****************************************************************"
    echo "The NDDSHOME environment variable must be set to"
    echo "Connext_install_dir/ to run this example"
    echo "*****************************************************************"
fi

