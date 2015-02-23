#!/bin/bash
echo "Starting Checkin-Attack Server..."

DIRNAME=`dirname $0`
CHECKIN_ATTACK_HOME=`cd $DIRNAME/.;pwd;`
export CHECKIN_ATTACK_HOME;

vertx runzip $CHECKIN_ATTACK_HOME/build/libs/checkin-attack-1.0.0-final.zip -conf $CHECKIN_ATTACK_HOME/conf.json 