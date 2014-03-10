#!/bin/sh
#!/bin/sh

echo "Stopping vertx"
service vertx stop

echo "Deleting existing version"
rm -rf /home/pi/vertx_mods/fr.xebia.kouignamann~pi~1.0

echo "Deploying new version"
unzip pi-1.0.zip -d /home/pi/vertx_mods/fr.xebia.kouignamann~pi~1.0

echo "Starting vertx"
nohup /etc/init.d/vertx restart

echo "Vertx started"


