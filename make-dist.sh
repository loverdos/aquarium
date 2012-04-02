#!/bin/bash

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

ERROR=""
P_VERBOSE="no"
P_PROPS=""
P_BUILD="normal"
P_KEEPDIST="no"
P_FAKEIT="no"

verbose() {
  if [ "$P_VERBOSE" = "yes" ]; then
    echo "$@"
  fi
}

verbose_p() {
  verbose "Build type          :" $P_BUILD
  verbose "Custom configuration:" $P_PROPS
  verbose "Keep dist/ folder   :" $P_KEEPDIST
  verbose "Fake it             :" $P_FAKEIT
}

fail() {
    echo "failed while $1"
    exit 1
}

removedist() {
  local KEEP=$1
  if [ "$KEEP" = "no" -a -e $DIST ]; then
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
  if [ "$P_BUILD" = "normal" ]; then
    echo
    echo "==============================="
    echo "=== mvn clean ================="
    echo "==============================="
    echo
    mvn clean || fail "cleaning compilation artifacts"
    echo
  elif [ "$P_BUILD"="fast" ]; then
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
  cp $CONF_SRC/logback.xml $DIST/conf|| fail "copying logback.xml"
  cp $CONF_SRC/policy.yaml $DIST/conf || fail "copying policy.yaml"
  cp $CONF_SRC/roles-agreements.map $DIST/conf || fail "copying roles-agreements.map"

  if [ -n "$P_PROPS" ]; then
    cp $P_PROPS $DIST/conf/aquarium.properties || fail "copying $P_PROPS"
  else
    cp $CONF_SRC/aquarium.properties $DIST/conf || fail "copying aquarium.properties"
  fi
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

usage() {
  echo "Usage: $0 [options]"
  echo ""
  echo "OPTIONS:"
  echo "  -b TYPE   Use build TYPE. One of 'normal', 'fast'."
  echo "            'normal' is the default and can be omitted."
  echo "            'fast' means that it will not run mvn clean."
  echo "  -c FILE   Use FILE as aquarium.properties configuration."
  echo "  -k        Keep generated dist folder."
  echo "  -h        Show this message."
  echo "  -n        As in make -n."
  echo "  -v        Be verbose."

  exit 0
}

while getopts ":b:hkc:nv" opt
do
  case $opt in
    b) P_BUILD=$OPTARG
    ;;
    c) P_PROPS=$OPTARG
    ;;
    h) usage
    ;;
    k) P_KEEPDIST="yes"
    ;;
    n) P_FAKEIT="yes"
    ;;
    v) P_VERBOSE="yes"
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

if [ -n "$P_PROPS" -a ! -f "$P_PROPS" ]; then
  echo $P_PROPS is not a file. Aborting... >&2
  exit 1
fi

if [ ! "$P_BUILD" = "normal" -a ! "$P_BUILD" = "fast" ]; then
  echo Build type must be one of normal, fast. $P_BUILD was given. Aborting... >&2
  exit 1
fi

if [ "$P_FAKEIT" = "yes" ]; then
  P_VERBOSE=yes verbose_p
  exit 0
fi

verbose_p         && \
removedist     no && \
createdist        && \
clean             && \
build             && \
collectdeps       && \
collectconf       && \
collectscripts    && \
gitmark           && \
archive           && \
removedist $P_KEEPDIST