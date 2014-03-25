#!/bin/sh

echo 'alias ll="ls -aul"' >> /etc/profile
echo 'blacklist pn533' >> /etc/modprobe.d/nfc-blacklist.conf
echo 'blacklist nfc' >> /etc/modprobe.d/nfc-blacklist.conf
echo 'i2c-bcm2708' >> /etc/modules
echo 'i2c-dev' >> /etc/modules

wget http://repo.mosquitto.org/debian/mosquitto-repo.gpg.key
sudo apt-key add mosquitto-repo.gpg.key

cd /etc/apt/sources.list.d/
wget http://repo.mosquitto.org/debian/mosquitto-stable.list
cd -

apt-get update

apt-get upgrade -y
apt-get install pcscd emacs mosquitto mosquitto-clients -y

cd /home/pi
wget http://dl.bintray.com/vertx/downloads/vert.x-2.1RC1.tar.gz -O vertx.tgz
tar xvzf vertx.tgz
ln -s vert.x-2.1RC1 vertx_home
mkdir vertx_mods
mkdir vertx_mods_conf
chown pi:pi * -R
apt-get install openjdk-7-jdk -y
apt-get remove oracle-java7-jdk --purge -y

# CCID driver
sudo apt-get install libacsccid1 -y
sudo apt-get remove libccid -y

cp scripts/vertx /etc/init.d
chmod u+x /etc/init.d/vertx
update-rc.d vertx defaults add

mv /etc/mosquitto/mosquitto.conf /etc/mosquitto/mosquitto.conf.bak
cp scripts/mosquitto.conf /etc/mosquitto/mosquitto.conf

echo "Mind about setting values in /etc/mosquitto/mosquitto.conf"
