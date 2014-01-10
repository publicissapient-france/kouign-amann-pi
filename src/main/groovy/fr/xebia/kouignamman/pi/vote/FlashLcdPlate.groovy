package fr.xebia.kouignamman.pi.vote

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class FlashLcdPlate extends Verticle {

    boolean flashing = true
    def logger = container.logger

    def start() {
        logger.info("Initialize handler");
        [
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.stopFlashing": this.&stopFlashing,
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.startFlashing": this.&startFlashing,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
        logger.info("Done initialize handler");
    }

    void stopFlashing(Message message) {
        flashing = false
    }

    void startFlashing(Message message) {
        flashing = true
        int colorIdx = 0

        while (flashing) {
            Thread.sleep(1000);
            VoteVerticle.lcd.setBacklight(VoteVerticle.lcd.COLORS[colorIdx])
            colorIdx++
            if (colorIdx >= VoteVerticle.lcd.COLORS.length) {
                // Reset
                colorIdx = 0
            }
        }
    }
}
