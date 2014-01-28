package fr.xebia.kouignamann.pi.draft.hardware

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.platform.Container
import org.vertx.java.core.logging.Logger

import java.util.concurrent.TimeoutException

/**
 * This class abstracts the whole hardware part.
 *
 * should be composed of : LCD screen (color, flash, text) + button (listen, light on, light off) + NFC reader
 */
class VotingBoard {

    static final Integer BUS_NUMBER = 1
    static final Integer BUS_ADDRESS = 0x20

    static final String PROMPT_CARD = 'Badgez SVP'
    static final String PROMPT_VOTE = 'Votez SVP'

    static final Integer BUTTON_PRESSED_DETECTION_TIME = 10
    static final Integer VOTE_WAIT_TIME = 1000 * 10

    Logger log

    VotingBoardLcd lcd

    VotingBoardNfcReader nfcReader
    VotingBoardButtons buttons


    private I2CDevice i2CDevice

    private GpioController gpio

    private Container container

    private Vertx vertx


    def VotingBoard(Container container, Vertx vertx) {
        this.container = container
        this.vertx = vertx

        log = container.logger

        initLcd()
        //initNfcReader()

        log.info "START: Initializing led buttons"
        gpio = GpioFactory.getInstance()
        buttons = new VotingBoardButtons(gpio)
        log.info "START: Done initializing led buttons"
    }

    private void initNfcReader() {
        log.info('START: Initializing nfc reader')

        nfcReader = new VotingBoardNfcReader(container)

        log.info('START: Done initializing nfc reader')
    }

    private void initLcd() {
        log.info('START: Initializing lcd plate')

        i2CDevice = initI2CDevice(BUS_NUMBER, BUS_ADDRESS)
        lcd = new VotingBoardLcd(i2CDevice)
        lcd.display(PROMPT_CARD)

        log.info('START: Done initializing lcd plate')
    }

    def stop() {
        lcd?.stop()
    }

    private I2CDevice initI2CDevice(Integer busNo, Integer address) {
        I2CBus i2cBus = I2CFactory.getInstance(busNo)
        i2cBus.getDevice(address)
    }
    /**
     * Entry point for event bus
     * @param message
     */
    def waitVote() {
        // expecting for vote value
        // reply => send value + nfcId to dataVerticle
        // timeout or not => send to waitCard

        // TODO light button leds
        // TODO stop flashing lcd
        lcd.display(PROMPT_VOTE)

        boolean voteSaved = false

        int note = -1

        def maxLoops = VOTE_WAIT_TIME / BUTTON_PRESSED_DETECTION_TIME
        def loopCount = 0

        log.info("PROCESS: max loops ${maxLoops}")

        // Wait for button to be pressed
        while (!voteSaved && loopCount < maxLoops) {
            sleep BUTTON_PRESSED_DETECTION_TIME
            int[] result = lcd.readButtonsPressed()

            for (int i = 0; i < 5; i++) {
                if (result[i]) {
                    note = i;
                }
            }

            if (loopCount >= maxLoops) {
                throw new TimeoutException()
            }

            if (note > -1) {
                lcd.display("Votre note: ${note}")
                // TODO switch off all buttons led
                return note
            }
            loopCount++
        }
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def waitCard() {
        // display user message to engage vote
        lcd.display(PROMPT_CARD)
        // TODO start flashing lcd

        String nfcId = waitForNfcCard()
        long voteTime = new Date().getTime()
        try {
            int note = waitVote()
            Map outgoingMessage = [
                    "nfcId": nfcId,
                    "voteTime": voteTime,
                    "note": note
            ]
            // TODO send to data verticle

        } catch (TimeoutException e) {
            log.info("PROCESS: waited too long for vote, going back to NFC")
        }

        log.info("BUS: -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitCard")

        vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitCard", [])
    }

    String waitForNfcCard() {
        // wait NFC card forever
        // when read, return as string
        return nfcReader.waitForCardId()
    }
}
