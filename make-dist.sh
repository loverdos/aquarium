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
if [ ! -z "$status" ]
then
    echo "The following files are not committed:"
    echo 
    echo $status
    echo
    echo "Stashing them"
    git stash save 2>&1 >>$LOG
    touch stashed
fi

# Get version or tag to checkout from cmd-line
if [ ! -z $1 ]
then
    echo -n "checking commit $1... "
    out=`git show $1 2>&1|grep "fatal: ambiguous argument '$1'"`
    if [ -z "$out" ]
    then
        tag=$1
        echo "success"
    else
        fail "retrieving info about commit $1"
    fi
else
    echo -n "checking out latest tag ("
    tag=`git tag |tail -n 1`
    echo "$tag)"
fi

# Tags are marked as aquarium-*
if [[ "$tag" =~ "aquarium-" ]];
then
    DIR=$tag
else
    DIR="aquarium-$tag"
fi

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
cp target/dependency/*.jar $DIR/lib || fail "copying dependencies"

echo "Copying Aquarium classes"
aquariumjar=`find target -type f|egrep "aquarium-[0-9\.]+(-SNAPSHOT)?\.jar"`
cp $aquariumjar $DIR/lib || fail "copying $aquariumjar"

echo "Copying scripts and config files"
cp dist/aquarium.sh $DIR/bin || fail "copying aquarium.sh"
cp dist/log4j.properties $DIR/conf|| fail "copying log4j.properties"
cp dist/aquarium.properties $DIR/conf || fail "copying aquarium.properties"
cp dist/policy.yaml $DIR/conf || fail "copying policy.yaml"

echo "Creating archive"
tar zcvf $DIR.tar.gz $DIR >> build.log 2>&1 || fail "creating archive"

echo "Cleaning up"
rm -Rf $DIR
cleanup
rm $LOG

echo "File $tag.tar.gz created succesfully"

# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :
