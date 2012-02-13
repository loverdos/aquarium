#!/usr/bin/env bash

# For debugging
clear
# set -x

# Check if there are pending changes
status=`git status --porcelain|grep -v "??"`
if [ ! -z "$status" ]; then
    echo "The following files are not committed:"
    echo 
    echo $status
    echo
    echo "Stashing them"
    git stash save
    touch stashed
fi

# Get latest tag
tag=`git tag |tail -n 1`

DIR="aquarium-$tag"

# Creating dist dirs
mkdir -p $DIR
mkdir -p $DIR/bin
mkdir -p $DIR/lib
mkdir -p $DIR/conf
mkdir -p $DIR/logs

echo "Checking out $tag"
git checkout $tag >/dev/null || exit 1

echo "Building $tag"
mvn clean install 1>/dev/null || exit 1

echo "Gathering dependencies"
mvn dependency:copy-dependencies > /dev/null || exit 1
cp target/dependency/*.jar $DIR/lib

tar zcvf aquarium-$tag.tar.gz $DIR > /dev/null || exit 1

echo "Cleaning up"
rm -Rf $DIR

# Revert working dir
git checkout master
if [ -f stashed ]; then
    git stash pop >/dev/null
    rm stashed
fi

# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :
