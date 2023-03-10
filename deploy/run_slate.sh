#!/bin/bash
set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

export EMAIL_DOMAIN="<fixme>"
export CORE_URL="http://localhost:8090"
export LDAP_URL="localhost:636"
export LDAP_SEARCH_SCOPE="fixme"
export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto

unameOut="$(uname -s)"
case "${unameOut}" in
    Darwin*)    export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home;;
    *)          machine="UNKNOWN:${unameOut}"
esac

export LOG_DIR=/tmp/log/slate
export JAR=$SCRIPT_DIR/../core/target/slate-core.jar

cp -r $SCRIPT_DIR/config /tmp/slate

$JAVA_HOME/bin/java -server -Dhibernate.session.events.log=false -Xms4g -Xmx4g -verbosegc \
	-Xlog:gc:/tmp/log/slate/gc.log::filecount=10,filesize=1024\
    -XX:ErrorFile=$LOG_DIR/jvm_error.log \
    -jar $JAR server $SCRIPT_DIR/config/dev-config.yaml
# > $LOG_DIR/slate.log 2>&1
