package fr.xebia.kouignamann.pi.draft.hardware

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import fr.xebia.kouignamann.pi.draft.hardware.lcd.LcdColor
import org.codehaus.groovy.control.messages.Message
import fr.xebia.kouignamann.pi.draft.hardware.mock.MockVotingBoardButtons
import groovy.transform.CompileStatic
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.platform.Container
import org.vertx.java.core.logging.Logger

import java.util.concurrent.TimeoutException

/**
 * This class abstracts the whole hardware part.
 *
 * should be composed of : LCD screen (color, flash, text) + button (listen, light on, light) + NFC reader
 */
@CompileStatic
class VotingBoard {

    private static final String PROMPT_CARD = 'Badgez SVP'
    private static final String PROMPT_VOTE = 'Votez SVP'
    private static final Integer BUS_NUMBER = 1
    private static final Integer BUS_ADDRESS = 0x20
    private static final Integer BUTTON_PRESSED_DETECTION_TIME = 10
    private static final Integer VOTE_WAIT_TIME = 1000 * 10

    private final I2CBus i2CBus
    private final I2CDevice i2CDevice
    private final GpioController gpio
    private final Container container
    private final Vertx vertx

    private static Logger log

    private VotingBoardLcd lcd
    private VotingBoardNfcReader nfcReader
    private VotingBoardButtons buttons

    /**
     * Constructor.
     *
     * @param container
     * @param vertx
     */
    VotingBoard(Container container, Vertx vertx) {
        log = container.logger

        this.container = container
        this.vertx = vertx

        log.info('START: Initializing i2c bus')
        i2CBus = I2CFactory.getInstance(BUS_NUMBER)
        i2CDevice = i2CBus.getDevice(BUS_ADDRESS)
        log.info('START: Done initializing i2c bus')

        log.info('START: Initializing gpio factory')
        gpio = GpioFactory.getInstance()
        log.info('START: Done initializing gpio factory')

        initLcd()
        initNfcReader()
        initButtons()
        initNfcReader()
    }

    private void initButtons() {
        log.info('START: Initializing led buttons')

        if (container.config.mockButtonsLight) {
            buttons = new MockVotingBoardButtons(log) as VotingBoardButtons
        } else {
            buttons = new VotingBoardButtons(gpio)
        }
        log.info('START: Done initializing led buttons')
    }

    private void initNfcReader() {
        log.info('START: Initializing nfc reader')

        nfcReader = new VotingBoardNfcReader(container)

        log.info('START: Done initializing nfc reader')
    }

    private void initLcd() {
        log.info('START: Initializing lcd plate')

        lcd = new VotingBoardLcd(i2CDevice)
        lcd.display(PROMPT_CARD)

        log.info('START: Done initializing lcd plate')
    }

    private void initLedButtons() {
        log.info "START: Initializing led buttons"

        gpio = GpioFactory.getInstance()
        buttons = new VotingBoardButtons(gpio, i2CDevice)

        log.info "START: Done initializing led buttons"
    }

    def stop() {
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

    /**
     * Entry point for event bus
     * @param message
     */
    def waitVote(Message message) {
        // expecting for vote value
        // reply => send value + nfcId to dataVerticle
        // timeout or not => send to waitCard

        buttons.illuminateAllButtons()
        stopFlashing()
        lcd.display(PROMPT_VOTE)

        boolean voteSaved = false

        int note = -1

        def maxLoops = VOTE_WAIT_TIME / BUTTON_PRESSED_DETECTION_TIME
        def loopCount = 0

        log.info("PROCESS: max loops ${maxLoops}")

        // Wait for button to be pressed
        while (!voteSaved && loopCount < maxLoops) {
            sleep BUTTON_PRESSED_DETECTION_TIME
            int[] result = buttons.readButtonsPressed()

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

                // TODO switch all buttons led

                return note
            }
            loopCount++
        }
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def waitCard(Message message) {
        // display user message to engage vote
        lcd.display(PROMPT_CARD)
        startFlashing()

        String nfcId = waitForNfcCard()
        long voteTime = new Date().getTime()
        try {
            int note = waitVote()

            Map outgoingMessage = [
                    "nfcId": nfcId,
                    "voteTime": voteTime,
                    "note": note
            ]

            log.info("BUS: -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffAllButtonButOne ${outgoingMessage}")
            vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffAllButtonButOne", outgoingMessage)

            log.info("BUS: -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.data.store ${outgoingMessage}")
            vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.data.store", outgoingMessage)
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

    static final Integer FLASH_PERIOD = 1000
    int colorIdx = 0
    long flashTimerId

    void stopFlashing() {
        log.info('PROCESS: Stop flashing')

        if (flashTimerId) {
            vertx.cancelTimer(flashTimerId)
            lcd.setBacklight(0x05)
            flashTimerId = 0
        }
    }

    void startFlashing() {
        log.info('PROCESS: Start flashing')
        if (!flashTimerId) {
            flashTimerId = vertx.setPeriodic(FLASH_PERIOD) { flashTimerId ->
                lcd.setBacklight(LcdColor.COLORS[colorIdx++])

                if (colorIdx >= LcdColor.COLORS.length) {
                    // Reset
                    colorIdx = 0
                }
            }
        }

    }
}
