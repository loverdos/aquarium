#!/bin/bash
rm -f logs/aquarium.log  aquarium.home_IS_UNDEFINED/logs/aquarium.log
mongo aquarium --eval "db.resevents.remove(); db.imevents.remove() ; db.policies.remove() ; db.userstates.remove()"

BILLREQUEST="http://localhost:8888/user/loverdos@grnet.gr/bill/1341090000000/1343768399999"

USERCREATE="{
    \"id\": \"im.1.create.loverdos\",
    \"clientID\": \"astakos\", 
    \"details\": {
        \"__aquarium_comment_1\": \"user is created\"
    },
    \"eventType\": \"create\", 
    \"eventVersion\": \"1\", 
    \"isActive\": false, 
    \"occurredMillis\":  1342731600000, 
    \"receivedMillis\": 1342731600000, 
    \"role\": \"default\", 
    \"userID\": \"loverdos@grnet.gr\"
}"

ADDCREDITS="{
 \"id\": \"im1400\",
 \"clientID\": \"astakos\",
 \"details\": {
           \"credits\": \"5000\"
  },
 \"eventType\": \"addcredits\",
 \"eventVersion\": \"1\",
 \"isActive\": false,
 \"occurredMillis\": 1344345437000,
 \"receivedMillis\": 1344345437000,
 \"role\": \"default\",
 \"userID\": \"loverdos@grnet.gr\"
}"

RESOURCE1="{
  \"id\": \"rc.1.loverdos\",
  \"occurredMillis\": 1342735200000,
  \"receivedMillis\": 1342735200000,
  \"userID\": \"loverdos@grnet.gr\",
  \"clientID\":   \"pithos\",
  \"resource\":   \"diskspace\",
  \"instanceID\": \"1\",
  \"value\":  1000.0,
  \"eventVersion\":   \"1.0\",
  \"details\": {
    \"action\":   \"object update\",
    \"total\":    \"1000.0\",
    \"user\":     \"loverdos@grnet.gr\",
    \"path\":     \"/Papers/GOTO_HARMFUL.PDF\"
  }
}"

#rabbitmqadmin -H dev82.dev.grnet.gr -P 55672 -u rabbit -p r@bb1t publish  routing_key=astakos.user exchange=astakos < __im.1.create.loverdos.json
#rabbitmqadmin -H dev82.dev.grnet.gr -P 55672 -u rabbit -p r@bb1t publish  routing_key=astakos.user exchange=astakos < __addcredits.json
#rabbitmqadmin -H dev82.dev.grnet.gr -P 55672 -u rabbit -p r@bb1t publish  routing_key=pithos.resource.diskspace exchange=pithos < __rc.1.loverdos.json

 echo $USERCREATE | rabbitmqadmin -H dev82.dev.grnet.gr -P 55672 -u rabbit -p r@bb1t publish  routing_key=astakos.user exchange=astakos
 echo $ADDCREDITS | rabbitmqadmin -H dev82.dev.grnet.gr -P 55672 -u rabbit -p r@bb1t publish  routing_key=astakos.user exchange=astakos 
 echo $RESOURCE1  | rabbitmqadmin -H dev82.dev.grnet.gr -P 55672 -u rabbit -p r@bb1t publish  routing_key=pithos.resource.diskspace exchange=pithos

#read
MAINCLASS=gr.grnet.aquarium.charging.bill.BillEntry
/home/pgerakios/jdk1.6.0_33/bin/java -Dfile.encoding=UTF-8 -classpath /home/pgerakios/jdk1.6.0_33/jre/lib/rt.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/resources.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/jce.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/plugin.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/deploy.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/jsse.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/management-agent.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/javaws.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/charsets.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/ext/sunpkcs11.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/ext/localedata.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/ext/dnsns.jar:/home/pgerakios/jdk1.6.0_33/jre/lib/ext/sunjce_provider.jar:/home/pgerakios/aquarium/target/classes:/home/pgerakios/.m2/repository/org/scala-lang/scala-library/2.9.1/scala-library-2.9.1.jar:/home/pgerakios/.m2/repository/org/slf4j/slf4j-api/1.6.1/slf4j-api-1.6.1.jar:/home/pgerakios/.m2/repository/ch/qos/logback/logback-classic/0.9.29/logback-classic-0.9.29.jar:/home/pgerakios/.m2/repository/ch/qos/logback/logback-core/0.9.29/logback-core-0.9.29.jar:/home/pgerakios/.m2/repository/org/scala-lang/scala-compiler/2.9.1/scala-compiler-2.9.1.jar:/home/pgerakios/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.0.2/jackson-core-2.0.2.jar:/home/pgerakios/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.0.2/jackson-databind-2.0.2.jar:/home/pgerakios/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.0.2/jackson-annotations-2.0.2.jar:/home/pgerakios/.m2/repository/com/fasterxml/jackson/module/jackson-module-scala/2.0.2/jackson-module-scala-2.0.2.jar:/home/pgerakios/.m2/repository/org/scalastuff/scalabeans/0.2/scalabeans-0.2.jar:/home/pgerakios/.m2/repository/com/thoughtworks/paranamer/paranamer/2.3/paranamer-2.3.jar:/home/pgerakios/.m2/repository/com/google/guava/guava/12.0/guava-12.0.jar:/home/pgerakios/.m2/repository/net/liftweb/lift-json_2.9.1/2.4/lift-json_2.9.1-2.4.jar:/home/pgerakios/.m2/repository/org/scala-lang/scalap/2.9.1/scalap-2.9.1.jar:/home/pgerakios/.m2/repository/net/liftweb/lift-json-ext_2.9.1/2.4/lift-json-ext_2.9.1-2.4.jar:/home/pgerakios/.m2/repository/commons-codec/commons-codec/1.4/commons-codec-1.4.jar:/home/pgerakios/.m2/repository/net/liftweb/lift-common_2.9.1/2.4/lift-common_2.9.1-2.4.jar:/home/pgerakios/.m2/repository/org/yaml/snakeyaml/1.9/snakeyaml-1.9.jar:/home/pgerakios/.m2/repository/com/kenai/crontab-parser/crontab-parser/1.0.1/crontab-parser-1.0.1.jar:/home/pgerakios/.m2/repository/com/rabbitmq/amqp-client/2.8.4/amqp-client-2.8.4.jar:/home/pgerakios/.m2/repository/com/ckkloverdos/jbootstrap/3.0.0/jbootstrap-3.0.0.jar:/home/pgerakios/.m2/repository/com/ckkloverdos/streamresource/0.5.1/streamresource-0.5.1.jar:/home/pgerakios/.m2/repository/com/ckkloverdos/maybe/0.5.0/maybe-0.5.0.jar:/home/pgerakios/.m2/repository/com/ckkloverdos/sysprop/0.5.1/sysprop-0.5.1.jar:/home/pgerakios/.m2/repository/com/ckkloverdos/converter/0.5.0/converter-0.5.0.jar:/home/pgerakios/.m2/repository/com/ckkloverdos/typedkey/0.5.0/typedkey-0.5.0.jar:/home/pgerakios/.m2/repository/com/thoughtworks/xstream/xstream/1.4.1/xstream-1.4.1.jar:/home/pgerakios/.m2/repository/xmlpull/xmlpull/1.1.3.1/xmlpull-1.1.3.1.jar:/home/pgerakios/.m2/repository/xpp3/xpp3_min/1.1.4c/xpp3_min-1.1.4c.jar:/home/pgerakios/.m2/repository/org/mongodb/mongo-java-driver/2.7.2/mongo-java-driver-2.7.2.jar:/home/pgerakios/.m2/repository/com/typesafe/akka/akka-actor/2.0.2/akka-actor-2.0.2.jar:/home/pgerakios/.m2/repository/com/typesafe/akka/akka-remote/2.0.2/akka-remote-2.0.2.jar:/home/pgerakios/.m2/repository/io/netty/netty/3.3.0.Final/netty-3.3.0.Final.jar:/home/pgerakios/.m2/repository/com/google/protobuf/protobuf-java/2.4.1/protobuf-java-2.4.1.jar:/home/pgerakios/.m2/repository/net/debasishg/sjson_2.9.1/0.15/sjson_2.9.1-0.15.jar:/home/pgerakios/.m2/repository/net/databinder/dispatch-json_2.9.1/0.8.5/dispatch-json_2.9.1-0.8.5.jar:/home/pgerakios/.m2/repository/org/apache/httpcomponents/httpclient/4.1/httpclient-4.1.jar:/home/pgerakios/.m2/repository/org/apache/httpcomponents/httpcore/4.1/httpcore-4.1.jar:/home/pgerakios/.m2/repository/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar:/home/pgerakios/.m2/repository/org/objenesis/objenesis/1.2/objenesis-1.2.jar:/home/pgerakios/.m2/repository/commons-io/commons-io/1.4/commons-io-1.4.jar:/home/pgerakios/.m2/repository/voldemort/store/compress/h2-lzf/1.0/h2-lzf-1.0.jar:/home/pgerakios/.m2/repository/com/typesafe/akka/akka-slf4j/2.0.2/akka-slf4j-2.0.2.jar:/home/pgerakios/.m2/repository/com/twitter/finagle-core_2.9.1/4.0.2/finagle-core_2.9.1-4.0.2.jar:/home/pgerakios/.m2/repository/com/twitter/util-core_2.9.1/4.0.1/util-core_2.9.1-4.0.1.jar:/home/pgerakios/.m2/repository/com/twitter/util-collection_2.9.1/4.0.1/util-collection_2.9.1-4.0.1.jar:/home/pgerakios/.m2/repository/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar:/home/pgerakios/.m2/repository/com/twitter/util-hashing_2.9.1/4.0.1/util-hashing_2.9.1-4.0.1.jar:/home/pgerakios/.m2/repository/com/twitter/finagle-http_2.9.1/4.0.2/finagle-http_2.9.1-4.0.2.jar:/home/pgerakios/.m2/repository/com/twitter/util-codec_2.9.1/4.0.1/util-codec_2.9.1-4.0.1.jar:/home/pgerakios/.m2/repository/com/twitter/util-logging_2.9.1/4.0.1/util-logging_2.9.1-4.0.1.jar:/home/pgerakios/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar:/home/pgerakios/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar:/home/pgerakios/.m2/repository/joda-time/joda-time/2.0/joda-time-2.0.jar:/home/pgerakios/.m2/repository/org/joda/joda-convert/1.1/joda-convert-1.1.jar:/home/pgerakios/.m2/repository/org/quartz-scheduler/quartz/2.1.5/quartz-2.1.5.jar:/home/pgerakios/.m2/repository/c3p0/c3p0/0.9.1.1/c3p0-0.9.1.1.jar:/home/pgerakios/.m2/repository/org/quartz-scheduler/quartz-oracle/2.1.5/quartz-oracle-2.1.5.jar:/home/pgerakios/.m2/repository/org/quartz-scheduler/quartz-weblogic/2.1.5/quartz-weblogic-2.1.5.jar:/home/pgerakios/.m2/repository/org/quartz-scheduler/quartz-jboss/2.1.5/quartz-jboss-2.1.5.jar:/home/pgerakios/idea-IC-117.418/lib/idea_rt.jar $MAINCLASS & 
PID=$!
echo "PID: $PID"
sleep 2
curl -s "$BILLREQUEST"
kill -9 $!
echo -e "\nKilling aquarium. Success (0=ok)  => $?" 
#sleep 1
