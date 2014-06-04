kouign-amann-pi
===============

* Based on Raspbian Wheezy image 2014-01-07
* New Raspberry install should follow:
    * Install image on SD card
        * insert card + detect dev device via 'mount'
        * sudo diskutil umount /dev/disk2s1
        * sudo dd if=~/Downloads/2014-01-07-wheezy-raspbian.img of=/dev/rdisk2 bs=1m
        * eject
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

---
curl -s get.gvmtool.net | bash
gvm install vertx

gradle modZip ==> 
shasum -a 256 build/libs/pi-1.0.zip ==> mettre de coté la valeur
./run.sh ==> dans un terminal séparé

mettre l'ip du pi à installer dans le fichier votingboards_hosts
configurer votre .ssh/config avec un truc comme ça :

Host 10.150.102.223   <-- les ip de mes Pi
User pi
IdentityFile /Users/amaury/.ssh/id_rsa.ansibox

# Set up the environment with Ansible

## WARN: Ansible deploy is not up-to-date, do it manually (remove when updated)
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

* Change password for user pi
* Add every public key you need in ```.ssh/authorized_keys```
* Copy the script directory to the raspberry :
```
    scp -r scripts pi@rpi:
```
* Connect and:
```
    sudo sh scripts/sysconf.sh
```
* To get the wifi up and running:
```
    wpa_passphrase ssid mdp >> /etc/wpa_supplicant/wpa_supplicant.conf
```
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
* Add to /etc/profile:
```
    export NUKE_SERVER=<SERVER_IP>
```
* Add network interface listing to cron:
```
    crontab -e
    * * * * * sh /home/pi/scripts/send_ip.sh
```

# Deploy
```
    gradle modzip
    scp conf.json pi@rpi:/home/pi/vertx_mods_conf/kouign-amann.conf
    scp build/libs/pi-1.0.zip pi@rpi:
    ssh pi@rpi "sudo sh scripts/apply_version.sh"
```
