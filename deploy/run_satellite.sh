#!/bin/bash
set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

export EMAIL_DOMAIN="<fixme>"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto

unameOut="$(uname -s)"
case "${unameOut}" in
    Darwin*)    export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home;;
    *)          machine="UNKNOWN:${unameOut}"
esac

export LOG_DIR=/tmp/log/slate

cp -r $SCRIPT_DIR/config /tmp/slate

$JAVA_HOME/bin/java -server -Dhibernate.session.events.log=false -Xms4g -Xmx4g -verbosegc \
	-Xlog:gc:/tmp/log/slate/gc.log::filecount=10,filesize=1024\
    -XX:ErrorFile=$LOG_DIR/jvm_error.log \
    -Dloaddemo=true \
    -cp $SCRIPT_DIR/../core/target/slate-core.jar:$SCRIPT_DIR/../slate-satellite-java/target/*:$SCRIPT_DIR/../slate-satellite-java/target/ com.pinterest.slate.satellite.SatelliteServerMain server
# > $LOG_DIR/slate.log 2>&1
