package fr.xebia.kouignamman.pi.vote

import fr.xebia.kouignamman.pi.adafruit.lcd.AdafruitLcdPlate
import fr.xebia.kouignamman.pi.mock.LcdMock
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class FlashLcdPlate extends Verticle {

    static final Integer FLASH_PERIOD = 1000

    Logger log

    def lcd

    int colorIdx = 0

    Long flashTimerId

    def start() {
        log = container.logger

        log.info('Initialize handler')

        [
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.stopFlashing": this.&stopFlashing,
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.startFlashing": this.&startFlashing
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        if (container.config.mockAll) {
            lcd = LcdMock.instance
        } else {
            lcd = AdafruitLcdPlate.instance
        }

        log.info('Done initialize handler')
    }

    void stopFlashing(Message message) {

        log.info('Stop flashing')

        if (flashTimerId) {
            vertx.cancelTimer(flashTimerId)
            lcd.setBacklight(0x05)
            flashTimerId = 0
        }

        message.reply([status: 'OK'])
    }

    void startFlashing(Message message) {

        log.info('Start flashing')

        if (!flashTimerId) {
            flashTimerId = vertx.setPeriodic(FLASH_PERIOD, this.&flash)
        }

        message.reply([status: 'OK'])
    }

    private void flash(Long timerId) {

        lcd.setBacklight(lcd.COLORS[colorIdx++])

        if (colorIdx >= lcd.COLORS.length) {
            // Reset
            colorIdx = 0
        }
    }
}
