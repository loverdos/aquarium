#!/bin/bash

`dirname $0`/aquarium-dist/bin/aquarium.sh start
tail -f `dirname $0`/aquarium-dist/logs/aquarium.log

