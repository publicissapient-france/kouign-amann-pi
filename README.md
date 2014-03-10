kouign-amann-pi
===============

* Based on Raspbian Wheezy image 2014-01-07
* New Raspberry install should follow:
    * Install image on SD card
    * First boot on ethernet
    * To find out the Pi's IP address: nmap -sS -p 22 192.168.1.0/24 | grep Rasp -B 4
    * ssh on it and do: sudo raspi-config to expand filesystem
    * Reboot
    * Install a ssh public key in authorized_key

Look at (https://github.com/pinterb/bootstrap/tree/master/provisioning/ansible/roles/vertx)

# Install the base system

* Based on raspbian image: 2014-01-07-wheezy-raspbian <=> MD5(6d8e5a48ff7c6bdc0bc0983bc32f75b8)
* On first boot, ssh as pi user and do:
    * sudo raspi-config
    * 1 - Expand Filesystem
    * Finish
    * Reboot ? Yes

# Set up the environment with Ansible

* In /etc/ansible, at the beginning of the file, add the following line :
```
[kouign-amann]
```

* Then add all the IPs of the pis, just below the first line written before
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
    wget http://dl.bintray.com/vertx/downloads/vert.x-2.1RC1.tar.gz -O vertx.tgz
    tar xvzf vertx.tgz
    ln -s vert.x-2.1RC1 vertx_home
    mkdir vertx_mods
    mkdir vertx_mods_conf
    chown pi:pi * -R
    apt-get install openjdk-7-jdk
    apt-get remove oracle-java7-jdk --purge
    service vertx start
```
* Install file from this repository raspberry/vertx on the Pi, under /etc/init.d/vertx
* Connect and:
```
    sudo bash -l
    chmod u+x /etc/init.d/vertx
    update-rc.d vertx defaults add
```

* wpa_passphrase ssid mdp >> /etc/wpa_supplicant/wpa_supplicant.conf

* /etc/network/interfaces
```
    auto lo

    iface lo inet loopback
    iface eth0 inet dhcp
    auto eth0
    allow-hotplug eth0

    auto wlan0
    allow-hotplug wlan0
    iface wlan0 inet manual
    wpa-roam /etc/wpa_supplicant/wpa_supplicant.conf
    iface default inet dhcp
```
* scp raspberry/deploy.sh pi@<ip>:

# Deploy

* gradle modzip
* scp conf.json pi@<ip>:/home/pi/vertx_mods_conf/kouign-amann.conf
* scp build/libs/pi-1.0.zip pi@<ip>:
* ssh pi@<ip> "sudo sh deploy.sh"




