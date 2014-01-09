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
(https://github.com/pinterb/bootstrap/tree/master/provisioning/ansible/roles/vert

