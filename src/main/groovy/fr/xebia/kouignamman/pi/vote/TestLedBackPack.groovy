package fr.xebia.kouignamman.pi.vote

import fr.xebia.kouignamman.pi.adafruit.led.EightByEight
import org.vertx.groovy.platform.Verticle

class TestLedBackPack extends Verticle {


    def start() {
        print "Press CTRL+Z to exit"
        def grid = new EightByEight(1, 0x0070)
        def grid2 = new EightByEight(1, 0x0071)
        def grid3 = new EightByEight(1, 0x0072)

        // Continually update the 8x8 display one pixel at a time
        while (true) {
            for (y in 0..8) {
                for (x in 0..8) {
                    grid.setPixel(x, y)
                    grid2.setPixel(x, y)
                    grid3.setPixel(x, y)
                    Thread.sleep(5)
                }
                Thread.sleep(5)

            }
            grid.clear()
            grid2.clear()
            grid3.clear()
            Thread.sleep(50)
        }
    }
}
