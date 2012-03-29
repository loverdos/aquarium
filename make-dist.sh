#!/usr/bin/env bash

# Make an Aquarium binary distribution out of current working directory.
# Use at your own risk (i.e. make sure it compiles etc).

WHERE=`dirname $0`
SHAFULL=`git rev-parse HEAD`
SHA=`echo $SHAFULL | cut -c 1-11`
DATE_FORMAT=+'%Y%m%d%H%M%S'
NOW=`date $DATE_FORMAT`
DIST=aquarium-$SHA
SERVER_SCRIPTS_SRC=$WHERE/scripts
CONF_SRC=$WHERE/src/main/resources
ARG1="$1"

fail() {
    echo "failed while $1"
    exit 1
}

checkdist() {
    [ -e $DIST ] && {
        echo Folder $DIST exists. Removing it.
        rm -rf "$DIST"
        echo
    }
    echo Creating dist dirs

    mkdir -pv $DIST
    mkdir -pv $DIST/bin
    mkdir -pv $DIST/lib
    mkdir -pv $DIST/conf
    mkdir -pv $DIST/logs
}

clean() {
    local p="$ARG1"
    if [ -z "$p" ]; then
      echo
      echo "==============================="
      echo "=== mvn clean ================="
      echo "==============================="
      echo
      mvn clean || fail "cleaning compilation artifacts"
      echo
    elif [ "$p"="dev" -o "$p"="fast" -o "$p"="noclean" ]; then
      echo
      echo "==============================="
      echo "=== NOT executing mvn clean ==="
      echo "==============================="
      echo
    fi
}

collectdeps() {
    mvn dependency:copy-dependencies && cp target/dependency/*.jar $DIST/lib || fail "collecting dependencies"
}

build() {
    echo
    echo "==============================="
    echo "=== mvn package ==============="
    echo "==============================="
    echo
    mvn package -DskipTests && {
        echo "Copying Aquarium classes"
        aquariumjar=`find target -type f|egrep "aquarium-[0-9\.]+(-SNAPSHOT)?\.jar"`
        cp $aquariumjar $DIST/lib || fail "copying $aquariumjar"
    } || fail "building"
}

collectconf() {
    echo
    echo Copying config files from $CONF_SRC
    echo
    cp $CONF_SRC/log4j.properties $DIST/conf|| fail "copying log4j.properties"
    cp $CONF_SRC/aquarium.properties $DIST/conf || fail "copying aquarium.properties"
    cp $CONF_SRC/policy.yaml $DIST/conf || fail "copying policy.yaml"
    cp $CONF_SRC/roles-agreements.map $DIST/conf || fail "copying roles-agreements.map"
}

collectscripts() {
    echo
    echo Copying scripts from $SERVER_SCRIPTS_SRC
    echo
    cp $SERVER_SCRIPTS_SRC/aquarium.sh $DIST/bin || fail "copying aquarium.sh"
    cp $SERVER_SCRIPTS_SRC/test.sh $DIST/bin || fail "copying test.sh"
}

gitmark() {
    echo $SHAFULL > $DIST/gitsha.txt
}

archive() {
    ARC=$DIST.tar.gz
    echo "Creating archive"
    tar zcvf $ARC $DIST/ || fail "creating archive"
    echo "File $ARC created succesfully"
    echo "Cleaning up"
    ls -al $ARC
}

checkdist      && \
clean          && \
build          && \
collectdeps    && \
collectconf    && \
collectscripts && \
gitmark        && \
archive