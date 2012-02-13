#!/usr/bin/env bash

# For debugging
#clear
#set -x

# Check if there are pending changes
status=`git status --porcelain|grep -v "??"`
if [ ! -z "$status" ]; then
    echo "The following files are not committed:"
    echo 
    echo $status
    echo
    echo "Commit them first"
    exit 1
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
git checkout  $tag

git checkout master
tar zcvf aquarium-$tag.tar.gz $DIR
rm -Rf $DIR

# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :
