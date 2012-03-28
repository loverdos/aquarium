#!/usr/bin/env bash

# Make an Aquarium binary distribution out of current working directory.
# Use at your own risk (i.e. make sure it compiles etc).

WHERE=`dirname $0`
SHA=`git rev-parse HEAD`
DATE_FORMAT=+'%Y%m%d%H%M%S'
NOW=`date $DATE_FORMAT`
DIST=aquarium-$SHA
SERVER_SCRIPTS_SRC=$WHERE/scripts
CONF_SRC=$WHERE/src/main/resources

fail() {
    echo "failed while $1"
    exit 1
}

checkdist() {
    [ -e $DIST ] && {
        echo Folder $DIST exists.
        echo Please remove it before proceeding
        exit 1
    }
    echo Creating dist dirs

    mkdir -pv $DIST
    mkdir -pv $DIST/bin
    mkdir -pv $DIST/lib
    mkdir -pv $DIST/conf
    mkdir -pv $DIST/logs
}

clean() {
    local p="$1"
    [ "$p"="dev" -o "$p"="fast" -o "$p"="noclean" ] || {
        mvn clean || fail "cleaning compilation artifacts"
    }
}

collectdeps() {
    mvn dependency:copy-dependencies && cp target/dependency/*.jar $DIST/lib || fail "collecting dependencies"
}

build() {
    echo "Building Aquarium"
    mvn package -DskipTests && {
        echo "Copying Aquarium classes"
        aquariumjar=`find target -type f|egrep "aquarium-[0-9\.]+(-SNAPSHOT)?\.jar"`
        cp $aquariumjar $DIST/lib || fail "copying $aquariumjar"ß
    } || fail "building"
}

collectconf() {
    echo Copying config files from $CONF_SRC
    cp $CONF_SRC/log4j.properties $DIST/conf|| fail "copying log4j.properties"
    cp $CONF_SRC/aquarium.properties $DIST/conf || fail "copying aquarium.properties"
    cp $CONF_SRC/policy.yaml $DIST/conf || fail "copying policy.yaml"
    cp $CONF_SRC/roles-agreements.map $DIST/conf || fail "copying roles-agreements.map"
}

collectscripts() {
    echo Copying scripts from $SERVER_SCRIPTS_SRC
    cp $SERVER_SCRIPTS_SRC/aquarium.sh $DIST/bin || fail "copying aquarium.sh"
    cp $SERVER_SCRIPTS_SRC/test.sh $DIST/bin || fail "copying test.sh"
}

gitmark() {
    git rev-parse HEAD > $DIST/gitsha.txt
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