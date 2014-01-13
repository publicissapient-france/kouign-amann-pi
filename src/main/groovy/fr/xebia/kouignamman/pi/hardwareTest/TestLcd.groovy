package fr.xebia.kouignamman.pi.hardwareTest

import fr.xebia.kouignamman.pi.adafruit.lcd.AdafruitLcdPlate
import org.vertx.groovy.platform.Verticle

class TestLcd extends Verticle {

    def start() {
        def lcd = new AdafruitLcdPlate(1, 0x20);
        lcd.setBacklight(0x01 + 0x04)
        lcd.write("En attente");
        println("En attente");
        // keep program running until user aborts (CTRL-C)
        while (true) {
            Thread.sleep(10);
            int[] result = lcd.readButtonsPressed();
            def multiplevote = false;
            def vote = -1;
            println result
            for (int i = 0; i < 5; i++) {
                if (result[i]) {
                    if (!multiplevote)
                        vote = i;
                    else
                        multiplevote = true
                }
            }
            if (vote > -1 && !multiplevote) {
                lcd.clear()
                lcd.write("Votre note: ${vote}");
                Thread.sleep(2000);
                lcd.clear()
                lcd.write("En attente");
            } else if (multiplevote) {
                lcd.clear()
                lcd.write("Une seule note SVP");
                Thread.sleep(2000);
                lcd.clear()
                lcd.write("En attente");

            }
        }

        lcd.shutdown();
    }
}
