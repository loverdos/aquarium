#!/usr/bin/env bash
#
# Copyright 2012 GRNET S.A. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#   1. Redistributions of source code must retain the above copyright
#      notice, this list of conditions and the following disclaimer.
#
#  2. Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer in the
#     documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
# OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# The views and conclusions contained in the software and documentation are
# those of the authors and should not be interpreted as representing official
# policies, either expressed or implied, of GRNET S.A.

#
# Init script for Aquarium
#
#-----------------------
# Supported environment variables
#
# JAVA_OPTS       Runtime options for the JVM that runs Aquarium
#                 (default: -Xms1024M -Xmx4096M)
#
# AQUARIUM_PROP   Java system properties understood by Aquarium
#                 (default: -Dlog4j.debug=true)
#
# AQUARIUM_OPTS   Runtime options for Aquarium
#                 (default: "")
#
# AQUARIUM_HOME   Location of the top level Aquarium dir
#                 (default: .)
#----------------------

#set -x

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set supported environment variables if not already set
[ -z "$AQUARIUM_HOME" ] && AQUARIUM_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`
[ -z "$AQUARIUM_PROP" ] && AQUARIUM_PROP="-Dlog4j.debug=true"
[ -z "$AQUARIUM_OPTS" ] && AQUARIUM_OPTS=""
[ -z "$JAVA_OPTS" ]     && JAVA_OPTS="-Xms1024M -Xmx4096M"


AQMAIN=gr.grnet.aquarium.Main
PID=$AQUARIUM_HOME/bin/aquarium.pid
LIB=$AQUARIUM_HOME/lib
LOG=$AQUARIUM_HOME/logs/aquarium.log
CONF=$AQUARIUM_HOME/conf

# Check the application status
check_status() {
    if [ -f $PID ]
    then
        aqrunning=`ps -ef|grep java|grep aquarium`
        if [ -z "$aqrunning" ]
        then
            return 0
            echo "Aquarium running, but no pid file found"
        else
            return 1
        fi 
    else
       return 0
    fi
    
    return 1
}

# Starts the application. If "debug" is passed as argument, aquarium starts
# in debug mode
start() {
    check_status
    if [ $? -ne 0 ]
    then
        echo "Aquarium is running"
        exit 1
    fi

    echo "Starting Aquarium"

    # Build classpath
    CLASSPATH=`find $LIB -type f|grep jar$|tr '\n' ':'|sed -e 's/\:$//'`
    
    # load log4j from classpath
    CLASSPATH=$CONF:$CLASSPATH

    echo "Using CLASSPATH $CLASSPATH"
    echo "Using AQUARIUM_HOME $AQUARIUM_HOME"
    echo "Using MAIN $AQMAIN"
    echo "Using AQUARIUM_PROP $AQUARIUM_PROP"
    echo "Using JAVA_OPTS $JAVA_OPTS"

    java $JAVA_OPTS -cp $CLASSPATH $AQUARIUM_PROP $AQMAIN >> $LOG 2>&1 &
    echo $! > $PID
    echo "OK [pid = $!]"
}

# Stops the application
stop() {
    check_status
    if [ $? -eq 0 ]
    then
        echo "Aquarium is not running"
        exit 1
    fi

    # Kills the application process
    echo -n "Stopping Aquarium: "
    kill `cat $PID`
    rm $PID
    echo "OK"
}

# Show the application status
status() {
    check_status
    if [ $? -ne 0 ]
    then
        echo "Aquarium is running (pid=$pid)"
    else
        echo "Aquarium is stopped"
    fi
}

# Main logic, a simple case to call functions
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    debug)
        start "debug"
        ;;
    restart)
        stop
        start
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|debug}"
        exit 1
esac

exit 0

# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :

