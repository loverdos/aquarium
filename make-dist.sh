#!/usr/bin/env bash

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

removedist() {
  if [ -e $DIST ]; then
    echo Folder $DIST exists. Removing it.
    rm -rf "$DIST"
  fi
}

createdist() {
  echo
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
    echo
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
  echo
  echo "Creating archive"
  tar zcvf $ARC $DIST/ || fail "creating archive"
  echo "File $ARC created succesfully"
  echo "Cleaning up"
  ls -al $ARC
}

removedist     && \
createdist     && \
clean          && \
build          && \
collectdeps    && \
collectconf    && \
collectscripts && \
gitmark        && \
archive        && \
removedist