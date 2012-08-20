#!/bin/bash
#
PORT=8888
SERVER=localhost
#
usage() {
  echo "Usage: $0 [options]"
  echo ""
  echo "OPTIONS:"
  echo "  -s address Server IP address."
  echo "  -p port    Server port number."
  echo "  -h         Show this message."
  exit 0
}
#
function ping {
   #echo "SERVER : $SERVER PORT : $PORT" 
   OUT=`curl -s http://$SERVER:$PORT/ping/$1  | grep PONG`
   if [ "$OUT"  != "PONG" ] ;  
   then
      exit 1
   fi
}
#
while getopts ":p:s:h" opt
do
  case $opt in
    p) PORT=$OPTARG
    ;;
    s) SERVER=$OPTARG
    ;;
    h) usage
    ;;
    :) ERROR="Option -$OPTARG requires an argument. Aborting..."
    ;;
    \?) ERROR="Invalid option: -$OPTARG"
    ;;
  esac
done

if [ -n "$ERROR" ]; then
  echo $ERROR >&2
  exit 1
fi

#
ping aquarium
ping rabbitmq
ping imstore
ping rcstore
exit 0
