package fr.xebia.kouignamman.pi.vote

import fr.xebia.kouignamman.pi.adafruit.lcd.AdafruitLcdPlate
import fr.xebia.kouignamman.pi.mock.LcdMock
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json

class FlashLcdPlate extends Verticle {

    boolean flashing = true
    def logger
    def lcd
    int colorIdx = 0


    def start() {
        logger = container.logger

        logger.info("Initialize handler");
        [
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.stopFlashing": this.&stopFlashing,
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.startFlashing": this.&startFlashing,
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.flash": this.&flash,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
        if (container.config.mockAll) {
            lcd = LcdMock.instance
        } else {
            lcd = AdafruitLcdPlate.instance
        }
        logger.info "Done initialize handler"
    }

    void stopFlashing(Message message) {
        logger.info "Stop flashing"
        lcd.setBacklight(0x05)
        flashing = false
        message.reply([
                status: "OK"
        ])
    }

    void startFlashing(Message message) {
        logger.info "Start flashing"
        flashing = true
        message.reply([
                status: "OK"
        ])
        flash null

    }

    void flash(Message message) {
        if (flashing) {
            sleep 1000;

            lcd.setBacklight(lcd.COLORS[colorIdx])
            colorIdx++
            if (colorIdx >= lcd.COLORS.length) {
                // Reset
                colorIdx = 0
            }
        } else {
            sleep 100
        }

        vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.flash", null)

    }
}
