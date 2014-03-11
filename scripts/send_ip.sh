#!/bin/sh

. /etc/profile

echo $(date) - $(cat /home/pi/vertx_mods_conf/kouign-amann.conf | grep hardwareUid | cut -d ":" -f2) $(ifconfig | grep inet | tr -s ' ' | cut -d ' ' -f 3 | grep -v "127.0.0.1")  | mosquitto_pub -t admin -h $NUKE_SERVER -l