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

Look at
(https://github.com/pinterb/bootstrap/tree/master/provisioning/ansible/roles/vertx)


* 3 verticles:
    * Un normal pour les écritures vers le backpack LCD (normalement assez rapides)
    * Un worker de 3 threads pour les traitements des écritures en base, et les envois au central en timer
    * un worker en thread unique qui gèrent les handlers des trucs bloquants et antagonistes (on wait )
    
# Install

* Based on raspbian image: 2014-01-07-wheezy-raspbian <=> MD5(6d8e5a48ff7c6bdc0bc0983bc32f75b8)
* On first boot, ssh as pi user and do:
    * sudo raspi-config
    * 1 - Expand Filesystem
    * Finish
    * Reboot ? Yes

* Reconnect and:

    sudo bash -l
    apt-get update
    apt-get upgrade
    echo 'alias ll="ls -aul"' >> /etc/profile
    echo 'blacklist pn533' >> /etc/modprobe.d/nfc-blacklist.conf
    echo 'blacklist nfc' >> /etc/modprobe.d/nfc-blacklist.conf

* Reboot, reconnect and:

    sudo bash -l
    wget http://dl.bintray.com/vertx/downloads/vert.x-2.1M3.tar.gz -O vertx.tgz
    tar xvzf vertx.tgz
    ln -s vert.x-2.1M3 vertx_home
    mkdir vertx_mods
    mkdir vertx_mods_conf

* Install file from this repository raspberry/vertx on the Pi, under /etc/init.d/vertx
* Connect and:
    sudo bash -l
    chmod u+x /etc/init.d/vertx
    update-rc.d vertx defaults add

# Deploy

* gradle modzip
* copy conf.json on the Pi under /home/pi/vertx_mods_conf and
  configure its content
* copy build/libs/pi-0.1.zip on the Pi under /home/pi
* connect on the pi and:

    sudo bash -l
    service vertx stop
    rm -rf /home/pi/vertx_mods/fr.xebia.kouignamann~pi~0.1
    unzip pi-0.1.zip -d /home/pi/vertx_mods/fr.xebia.kouignamann~pi~0.1
    service vertx start

workflow:

phase d'init des verticles, quand tout le monde a répondu présent on lance le message de mise en route => Attente ID

Attente ID : flash + texte écran + attente NFC
bip NFC => lecture et envoie message à un handler qui déclenche l'attente d'un vote, avec timeout, enregistré sur le même workerVerticle

si timeout => send message Attente ID
si reply (contenant la valeur du vote) => send to store + send to
Attente ID

# Wifi on the Pi

reminder wpa_passphrase => etc/wpa_supplicant/wpa_supplicant.conf

# vertx install

apt-get rmove wolfram-engine --purge

adduser vertx
install vertx home dir under /opt, owned by vertx user
add deploy directory, under which put zips
add conf file under VERTX_HOME/conf
put init.d service file in place and customize to launch our module
update-rc.d vertx defaults add


dans /etc/modules ajouter =>
i2c-bcm2708
i2c-dev

ajouter le user vertx dans le group i2c

reboot

apt-get install libpcsclite1 pcscd

blacklist module => nfc, un autre qui en dépend

sudo nano /etc/modprobe.d/blacklist-libnfc.conf

Et ajouter les deux lignes dans le fichier.

blacklist pn533
blacklist nfc
