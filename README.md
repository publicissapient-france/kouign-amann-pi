kouign-amann-pi
===============

* Based on Raspbian Wheezy image 2013-12-20, available from [here](http://www.raspberrypi.org/downloads)
* New Radpberry install should follow:
    * Install image on SD card
    * First boot on ethernet
    * To find out the Pi's IP address: nmap -sS -p 22 192.168.1.0/24 |
      grep Rasp -B 4
    * ssh on it and do: sudo raspi-config to expand filesystem
    * Reboot
    * Install a ssh public key in authorized_key
    * Add Pi's IP to your ansible hosts
    * Have fun

Look at (https://github.com/pinterb/bootstrap/tree/master/provisioning/ansible/roles/vertx)

# Install the base system

* Based on raspbian image: 2014-01-07-wheezy-raspbian <=> MD5(6d8e5a48ff7c6bdc0bc0983bc32f75b8)
* On first boot, ssh as pi user and do:
    * sudo raspi-config
    * 1 - Expand Filesystem
    * Finish
    * Reboot ? Yes

# Set up the environment with Ansible

* In /etc/ansible, at the beginning of the file, add all the ips of the aimed pis
* Then run the command :
```
ansible-playbook ansible/init_pi.yaml
```

# Set up the environment manually
* Reconnect and:
```
    sudo bash -l
    apt-get update
    apt-get upgrade
    echo 'alias ll="ls -aul"' >> /etc/profile
    echo 'blacklist pn533' >> /etc/modprobe.d/nfc-blacklist.conf
    echo 'blacklist nfc' >> /etc/modprobe.d/nfc-blacklist.conf
    echo 'i2c-bcm2708' >> /etc/modules
    echo 'i2c-dev' >> /etc/modules
    apt-get install pcscd emacs -y
```
* Reboot, reconnect and:
```
    sudo bash -l
    wget http://dl.bintray.com/vertx/downloads/vert.x-2.1M5.tar.gz -O vertx.tgz
    tar xvzf vertx.tgz
    ln -s vert.x-2.1M5 vertx_home
    mkdir vertx_mods
    mkdir vertx_mods_conf
    chown pi:pi * -R
```
* Install file from this repository raspberry/vertx on the Pi, under /etc/init.d/vertx
* Connect and:
```
    sudo bash -l
    chmod u+x /etc/init.d/vertx
    update-rc.d vertx defaults add
```
# Deploy

* gradle modzip
* copy conf.json on the Pi under /home/pi/vertx_mods_conf and
  configure its content
* copy build/libs/pi-0.1.zip on the Pi under /home/pi
* connect on the pi and:
```
    sudo bash -l
    service vertx stop
    rm -rf /home/pi/vertx_mods/fr.xebia.kouignamann~pi~0.1
    unzip pi-0.1.zip -d /home/pi/vertx_mods/fr.xebia.kouignamann~pi~0.1
    apt-get install openjdk-7-jdk
    apt-get remove oracle-java7-jdk --purge
    service vertx start
```
* This is not a mistake, at this stage of system configuration, only
openjdk-7 can hook on the nfc reader, not the oraclejdk-7

* If you want to connect to a wifi network, use wpa_passphrase to
  generate a section to put into /etc/wpa_supplicant/wpa_supplicant.conf



