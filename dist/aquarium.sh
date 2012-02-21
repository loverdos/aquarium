#!/usr/bin/env bash
#
# Aquarium init script 

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

# Only set CATALINA_HOME if not already set
[ -z "$AQUARIUM_HOME" ] && AQUARIUM_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

AQMAIN=gr.grnet.aquarium.Main
PID=$AQUARIUM_HOME/bin/aquarium.pid
LIB=$AQUARIUM_HOME/lib
LOG=$AQUARIUM_HOME/logs/aquarium.log
CONF=$AQUARIUM_HOME/conf

# Check the application status
check_status() {

    if [ -f $PID ]; then
        aqrunning=`ps -ef|grep java|grep aquarium`
        if [ -z "$aqrunning" ]; then
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

# Starts the application
start() {
    check_status
    if [ $? -ne 0 ] ; then
        echo "Aquarium is running"
        exit 1
    fi

    echo "Starting Aquarium"

    # Build classpath
    CLASSPATH=`find $LIB -type f|grep jar$|tr '\n' ':'|sed -e 's/\:$//'`
    
    # load log4j from classpath
    CLASSPATH=$CONF:$CLASSPATH

    # default properties
    PROPS="-Dlog4j.debug=true"

    echo "Using AQUARIUM_HOME $AQUARIUM_HOME"
    echo "Using CLASSPATH $CLASSPATH"
    echo "Using configuration files in $CONF"
    echo "Using MAIN $AQMAIN"
    java -cp $CLASSPATH $PROPS $AQMAIN >> $LOG 2>&1 &
    echo $! > $PID 
    echo "OK [pid = $!]"
}

# Stops the application
stop() {
    check_status
    if [ $? -eq 0 ] ; then
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
    if [ $? -ne 0 ] ; then
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
    restart|reload)
        stop
        start
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|reload|status}"
        exit 1
esac

exit 0

# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :

