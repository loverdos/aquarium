#!/usr/bin/env bash

# For debugging
clear
# set -x
LOG=build.log

fail() {
    echo "failed while $1"
    echo "last log lines are"
    tail -n 15 $LOG
    echo "see file $LOG for details"
    cleanup
    exit 1
}

cleanup() {
    git checkout master 2>>$LOG 1>>$LOG
    if [ -f stashed ]; then
        git stash pop 2>>$LOG 1>>$LOG
        rm stashed
    fi
}

# Check if there are pending changes
status=`git status --porcelain|grep -v "??"`
if [ ! -z "$status" ]; then
    echo "The following files are not committed:"
    echo 
    echo $status
    echo
    echo "Stashing them"
    git stash save 2>&1 >>$LOG
    touch stashed
fi

# Get latest tag
tag=`git tag |tail -n 1`

DIR="$tag"

# Creating dist dirs
mkdir -p $DIR
mkdir -p $DIR/bin
mkdir -p $DIR/lib
mkdir -p $DIR/conf
mkdir -p $DIR/logs

echo "Checking out $tag"
git checkout $tag 2>>$LOG 1>>$LOG || fail "checking out"

echo "Building $tag"
mvn clean install -DskipTests=true >>build.log || fail "building project"

echo "Collecting dependencies"
mvn dependency:copy-dependencies >> build.log ||  fail "collecting dependencies"
cp target/dependency/*.jar $DIR/lib

echo "Creating archive"
tar zcvf $tag.tar.gz $DIR >> build.log 2>&1 || fail "creating archive"

echo "Cleaning up"
rm -Rf $DIR
cleanup
rm $LOG

echo "File $tag.tar.gz created succesfully"
# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :
