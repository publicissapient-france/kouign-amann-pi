package fr.xebia.kouignamann.pi.hardware

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import fr.xebia.kouignamann.pi.util.WrapperEventBus
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Container

import java.util.concurrent.TimeoutException

/**
 * This class abstracts the whole hardware part.
 *
 * should be composed of : LCD screen (color, flash, text) + button (listen, light on, light) + NFC reader
 */
class VotingBoard {

    def log

    private static final String PROMPT_BOOT = '...Demarrage...'
    private static final String PROMPT_CARD = 'Badgez SVP'
    private static final String PROMPT_VOTE = 'Votez SVP'
    private static final Integer I2C_BUS_NUMBER = 1
    private static final Integer I2C_BUS_ADDRESS = 0x20

    private static final Integer BUTTON_PRESSED_DETECTION_TIME = 10
    private static final Integer VOTE_WAIT_TIME = 1000 * 5

    private final I2CBus i2CBus
    private final I2CDevice i2CDevice
    private final GpioController gpio

    private final Container container
    private final Vertx vertx
    private final String busPrefix

    private VotingBoardLcd lcd
    private VotingBoardNfcReader nfcReader
    private VotingBoardButtons buttons

    /**
     * Constructor.
     *
     * @param container
     * @param vertx
     */
    VotingBoard(Container container, Vertx vertx, String busPrefix) {
        log = container.logger
        this.busPrefix = busPrefix
        this.container = container
        this.vertx = vertx

        log.info('START: Initializing i2c bus')
        i2CBus = I2CFactory.getInstance(I2C_BUS_NUMBER)
        i2CDevice = i2CBus.getDevice(I2C_BUS_ADDRESS)
        log.info('START: Done initializing i2c bus')

        log.info('START: Initializing gpio factory')
        gpio = GpioFactory.getInstance()
        log.info('START: Done initializing gpio factory')

        initLcd()
        initNfcReader()
        initButtons()
    }

    private void initLcd() {
        log.info('START: Initializing lcd plate')

        lcd = new VotingBoardLcd(i2CDevice)
        lcd.display(PROMPT_BOOT)

        log.info('START: Done initializing lcd plate')
    }

    private void initNfcReader() {
        log.info('START: Initializing nfc reader')

        nfcReader = new VotingBoardNfcReader(container)

        log.info('START: Done initializing nfc reader')
    }

    private void initButtons() {
        log.info('START: Initializing led buttons')
        buttons = new VotingBoardButtons(gpio, i2CDevice, log)
        log.info('START: Done initializing led buttons')
    }

    void stop() {
        log.info('STOP: Shutting down NfcReader')
        nfcReader?.stop()

        log.info('STOP: Shutting down buttons')
        buttons?.stop()

        log.info('STOP: Shutting down display')
        lcd?.stop()

        log.info('STOP: Shutting down gpio')
        gpio?.shutdown()

        log.info('STOP: Shutting down i2c bus')
        i2CBus?.close()
    }


    private def lightOnAllButtons() {
        buttons.lightOnAll()
    }

    private def lightOffAllButtons() {
        buttons.lightOffAll()
    }

    private def lightOnOneButton(int note) {
        buttons.lightOnOneButton(note)
    }

    /**
     * Entry point for event bus.
     *
     * @param message
     */
    def waitCard(Message message) {

        // TODO: validate message format
        lightOffAllButtons()

        lcd.display(PROMPT_CARD)

        String nfcId = nfcReader.waitForCardId()

        log.info('Card seen => ' + nfcId)

        long voteTime = new Date().getTime()

        try {
            def wrapperBus = new WrapperEventBus(vertx.eventBus.javaEventBus())

            wrapperBus.sendWithTimeout("${busPrefix}.waitVote", 'call', 5000) { AsyncResult result ->

                if (result.succeeded) {
                    int note = result.result.body.toMap().note

                    log.info("Process -> note : " + note)

                    Map outgoingMessage = [
                            "nfcId": nfcId,
                            "voteTime": voteTime,
                            "note": note
                    ]

                    log.info("BUS => ${busPrefix}.processVote => ${outgoingMessage}")

                    vertx.eventBus.send("${busPrefix}.processVote", outgoingMessage)

                } else {
                    log.info("Process -> TIMEOUT - Do nothing")
                    log.error('didnt succeed: ' + result.cause.message, result.cause)
                }
                vertx.eventBus.send("${busPrefix}.waitCard", 'call')
            }


        }

        catch (TimeoutException e) {
            log.info("PROCESS: waited too long for vote, going back to NFC")
            vertx.eventBus.send("${busPrefix}.waitCard", 'call')
        }
    }

/**
 * Entry point for event bus
 * @param message
 */
    def waitVote(Message message) {

        log.info('Wait vote')

        lightOnAllButtons()

        lcd.display(PROMPT_VOTE)

        boolean voteSaved = false

        int note = -1

        def maxLoops = VOTE_WAIT_TIME / BUTTON_PRESSED_DETECTION_TIME

        def loopCount = 0

        while (!voteSaved && loopCount < maxLoops) {

            sleep BUTTON_PRESSED_DETECTION_TIME

            List<Integer> result = buttons.readButtonsPressed()

            //log.info('pressed : ' + result)

            result.eachWithIndex { value, index ->
                if (value) {
                    note = index + 1
                }
            }

            log.info('note : ' + note)

            if (note > -1) {
                lcd.display("Votre note: ${note}")

                message.reply([note: note])

                lightOnOneButton(note)

                return
            }

            loopCount++
        }
    }

}
