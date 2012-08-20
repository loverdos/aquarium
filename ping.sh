#!/bin/bash
function ping {
   OUT=`curl -s http://localhost:8888/ping/$1  | grep PONG`
   if [ "$OUT"  != "PONG" ] ;  
   then
      exit 1
   fi
}
ping aquarium
ping rabbitmq
ping imstore
ping rcstore
exit 0
