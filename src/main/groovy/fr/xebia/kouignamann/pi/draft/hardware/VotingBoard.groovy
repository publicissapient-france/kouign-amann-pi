package fr.xebia.kouignamann.pi.draft.hardware

import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import fr.xebia.kouignamann.pi.mock.LcdMock
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Container
import org.vertx.java.core.logging.Logger

/**
 * This class abstracts the whole hardware part.
 *
 * should be composed of : LCD screen (color, flash, text) + button (listen, light on, light off) + NFC reader
 */
class VotingBoard {

    static final Integer BUS_NUMBER = 1
    static final Integer BUS_ADDRESS = 0x20

    static final String PROMPT_VOTE = 'VotingBoard needs vote'
    static final String PROMPT_CARD = 'VotingBoard needs card'

    Logger log

    VotingBoardLcd lcd

    I2CDevice i2CDevice

    def VotingBoard(Container container) {
        log = container.logger
        log.info "START: Initializing lcd plate"
        i2CDevice = initI2CDevice(BUS_NUMBER, BUS_ADDRESS)
        lcd = new VotingBoardLcd(i2CDevice)
        lcd.display(PROMPT_CARD)
        log.info "Start -> Done initializing lcd plate"
    }

    def stop() {
        lcd?.shutdown()
    }

    private I2CDevice initI2CDevice(Integer busNo, Integer address) {
        I2CBus i2cBus = I2CFactory.getInstance(busNo)
        i2cBus.getDevice(address)
    }

    void waitVote(Message message) {

        // TODO: validate incoming message content

        lcd.display(PROMPT_VOTE)

        boolean voteSaved = false

        int note = -1

        def detectionTime = 10
        def maxWaitTime = 1000 * 10
        def maxLoops = maxWaitTime / detectionTime
        def loopCount = 0

        log.info("Process -> max loops ${maxLoops}")

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

            if (loopCount >= maxLoops) {
                log.info("Process -> waited too long for vote, going back to NFC")
            }

            if (note > -1 && !multiplevote) {
                lcd.display("Votre note: ${note}");
                sleep 1500


                voteSaved = true
            } else if (multiplevote) {
                lcd.display("Une seule note SVP");
                sleep 1500
            }

            loopCount++
        }

        if (voteSaved) {
            message.reply([
                    nfcId: message.body.nfcId,
                    voteTime: message.body.voteTime,
                    note: note
            ])
        }
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def waitCard(Message message) {
        // display user message to engage vote

        String nfcId = votingBoard.waitForNfcCard()

        // send to waitVote with timeout, expecting for vote value
        // reply => send value + nfcId to dataVerticle
        // timeout or not => send to waitCard
    }

    /**
     * Driving hardware
     * @return
     */
    String waitForNfcCard() {

        // wait NFC card forever
        // when read, return as string
    }
}
