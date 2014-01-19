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
    
    
workflow:

phase d'init des verticles, quand tout le monde a répondu présent on lance le message de mise en route => Attente ID

Attente ID : flash + texte écran + attente NFC
bip NFC => lecture et envoie message à un handler qui déclenche l'attente d'un vote, avec timeout, enregistré sur le même workerVerticle

si timeout => send message Attente ID
si reply (contenant la valeur du vote) => send to store + send to Attente ID