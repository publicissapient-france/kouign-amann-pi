package fr.xebia.kouignamann.pi.vote

import fr.xebia.kouignamann.pi.adafruit.lcd.AdafruitLcdPlate
import fr.xebia.kouignamann.pi.mock.LcdMock
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class VoteVerticle extends Verticle {

    def logger
    def static lcd

    def start() {
        logger = container.logger

        if (!container.config.mockLcd) {
            logger.info "Initializing lcd plate"
            lcd = new AdafruitLcdPlate(1, 0x20)
            lcd.setBacklight(0x01 + 0x04)
            logger.info "Done initializing lcd plate"
        } else {
            logger.info "Initializing MOCK lcd plate"
            lcd = new LcdMock()
            lcd.logger = logger
        }


        logger.info "Initialize handler";
        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.reinitialiseLcd": this.&reinitialiseLcd,
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForVote": this.&waitForVote,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";

        startFlashing()
        reinitialiseLcd(null)
    }

    def stop() {
        if (lcd) {
            lcd.shutdown()
        }
    }

    void reinitialiseLcd(Message incomingMsg) {
        // Wait for NFC identification
        lcd.clear()
        lcd.write("En attente NFC")
        startFlashing()

        // Send message to next processor
        logger.info("Send message to next processor fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForNfcIdentification")
        vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForNfcIdentification", null)
    }



    void waitForVote(Message incomingMsg) {
        logger.info("Message received by next processor fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForVote")
        stopFlashing()
        lcd.clear()
        lcd.write("${incomingMsg.body.name}")

        boolean voteSaved = false
        int note = -1

        // Extract NFC id from message received
        def nfcId = incomingMsg.body.nfcId
        def voteTime = incomingMsg.body.voteTime

        def detectionTime = 10
        def maxLoops = 10000 / detectionTime
        def loopCount = 0

        // Wait for button to be pressed
        while (!voteSaved && loopCount < maxLoops) {
            sleep detectionTime;
            int[] result = lcd.readButtonsPressed()

            def multiplevote = false;
            for (int i = 0; i < 5; i++) {
                if (result[i]) {
                    if (!multiplevote)
                        note = i;
                    else
                        multiplevote = true
                }
            }
            if (note > -1 && !multiplevote) {
                lcd.clear()
                lcd.write("Votre note: ${note}");
                sleep 1500


                voteSaved = true
            } else if (multiplevote) {
                lcd.clear()
                lcd.write("Une seule note SVP");
                sleep 1500
            }
            loopCount++
        }
        if (note > -1) {
            // Send vote to next processor
            Map outgoingMessage = [
                    "nfcId": nfcId,
                    "voteTime": voteTime,
                    "note": note
            ]
            // Proceed to data process
            logger.info("Sending message to next processor fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote ${outgoingMessage}")
            vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote", outgoingMessage)
        }
        // Return to NFC waiting
        reinitialiseLcd(null)

    }

    static final Integer FLASH_PERIOD = 1000
    int colorIdx = 0
    long flashTimerId

    void stopFlashing() {
        logger.info('Stop flashing')

        if (flashTimerId) {
            vertx.cancelTimer(flashTimerId)
            lcd.setBacklight(0x05)
            flashTimerId = 0
        }
    }

    void startFlashing() {
        logger.info('Start flashing')
        if (!flashTimerId) {
            flashTimerId = vertx.setPeriodic(FLASH_PERIOD) { flashTimerId ->
                lcd.setBacklight(lcd.COLORS[colorIdx++])

                if (colorIdx >= lcd.COLORS.length) {
                    // Reset
                    colorIdx = 0
                }
            }
        }

    }

}
