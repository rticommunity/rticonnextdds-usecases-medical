#!/bin/sh

filename=$0
script_dir=`dirname $filename`
absolute_script_dir=`cd $script_dir; pwd`
hmi_dir=$script_dir/../src/BedsideSupervisor/

# Check for NDDSHOME
if [ -z "$NDDSHOME" ]; then
    echo "***************************************************************"
    echo "The environment variable 'NDDSHOME' is not set!"
    echo "To use this makefile you need to set NDDSHOME to the directory"
    echo "where you have RTI Connext installed."
    echo "***************************************************************"
fi

# Guess platform
i86Linux_platforms=`ls $NDDSHOME/lib | grep i86Linux`
x64Linux_platforms=`ls $NDDSHOME/lib | grep x64Linux`
x64Darwin_platforms=`ls $NDDSHOME/lib | grep x64Darwin`
platform_name="unknown"

os=`uname`
arch=`uname -m`

# Define platforms_to_try
case $os in
Linux*)
	if [ "$arch" = "x86_64" ]; then
	    platforms_to_try="$x64Linux_platforms $i86Linux_platforms"
        else
	    platforms_to_try="$i86Linux_platforms"
	fi
	;;
Darwin*)
	platforms_to_try="$x64Darwin_platforms"
esac

# Look for $platforms_to_try in your installation
for platform in $platforms_to_try
do
    if [ -d $NDDSHOME/lib/${platform} ]; then
	platform_name=$platform
	break;
    fi
done

if [ "$platform_name" = "unknown" ]; then
    echo "***************************************************************"
    echo "Error: Could not find the native libraries needed to run"
    echo "$filename."
    echo "Please examine $NDDSHOME/lib/<arch> to find the libraries"
    echo "needed."
    echo "***************************************************************"
else
    if [ "$os" = "Darwin" ]; then
	export DYLD_LIBRARY_PATH=$PWD:$NDDSHOME/lib/${platform}
    else
	export LD_LIBRARY_PATH=$PWD:$NDDSHOME/lib/${platform}
    fi

    export RTI_JAR_DIR=$NDDSHOME/lib/java
    
    # Run command
    cd $hmi_dir
    java -classpath $absolute_script_dir/../src/BedsideSupervisor/bin:$RTI_JAR_DIR/nddsjava.jar \
	com.rti.medical.BedsideSupervisor
fi
