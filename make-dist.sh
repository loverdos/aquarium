#!/usr/bin/env bash

# For debugging
#clear
#set -x

# Check if there are pending changes
status=`git status --porcelain|grep -v "??"`
if [ ! -z "$status" ]; then
    echo "The following files are not committed:"
    echo $status
    echo
    echo "Commit them first"
    exit 1
fi

# Get latest tag
tag=`git tag |tail -n 1`

DIR="aquarium-dist-$tag"

# Create tmp dir
mkdir -p $DIR

echo "Checking out $tag"



cd -

# vim: set sta sts=4 shiftwidth=4 sw=4 et ai :
