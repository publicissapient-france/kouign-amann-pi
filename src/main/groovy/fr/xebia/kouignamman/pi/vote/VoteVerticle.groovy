package fr.xebia.kouignamman.pi.vote

import fr.xebia.kouignamman.pi.adafruit.lcd.AdafruitLcdPlate
import fr.xebia.kouignamman.pi.mock.LcdMock
import fr.xebia.kouignamman.pi.mock.RfidReaderMock
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import javax.smartcardio.Card
import javax.smartcardio.CommandAPDU
import javax.smartcardio.ResponseAPDU
import javax.smartcardio.TerminalFactory

class VoteVerticle extends Verticle {

    def logger
    def static lcd
    def nfcTerminal
    static final byte[] READ_UID_SEQ = [0xFF, 0xCA, 0x00, 0x00, 0x00]

    def start() {
        logger = container.logger

        if (!container.config.mockLcd) {
            logger.info "Initializing lcd plate"
            lcd = AdafruitLcdPlate.instance
            lcd.setBacklight(0x01 + 0x04)
            logger.info "Done initializing lcd plate"
        } else {
            logger.info "Initializing MOCK lcd plate"
            lcd = LcdMock.instance
        }
        if (!container.config.mockRfid) {
            logger.info "Finding Nfc reader"
            nfcTerminal = TerminalFactory.getDefault().terminals().list().get(0)
            logger.info "Nfc reader found : ${nfcTerminal.name}"
        } else {
            logger.info "Finding MOCK Nfc reader"
            nfcTerminal = new RfidReaderMock()
        }

        logger.info "Initialize handler";
        [
                "  fr.xebia.kouignamman.pi.  ${ container.config.hardwareUid } .waitForNfcIdentification  ": this.&waitForNfcIdentification,
                "  fr.xebia.kouignamman.pi.  ${ container.config.hardwareUid } .waitForVote  ": this.&waitForVote,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";

        vertx.eventBus.send("  fr.xebia.kouignamman.pi.  ${ container.config.hardwareUid } .startFlashing  ", null)

        waitForNfcIdentification(null)
    }

    def stop() {
        if (lcd) {
            lcd.shutdown()
        }
    }

    void waitForNfcIdentification(Message incomingMsg) {
        // Wait for NFC identification
        lcd.write("En attente NFC")

        // Mise en attente bloquante
        nfcTerminal.waitForCardPresent 0

        // Display userName
        Card card = nfcTerminal.connect("*")
        ResponseAPDU cardResponse = card.getBasicChannel().transmit(new CommandAPDU(READ_UID_SEQ))
        card.disconnect(false)

        // Send message to next processor
        Map outgoingMessage = [
                "nfcId": byteArrayToNormalizedString(cardResponse),
                "voteTime": new Date().getTime()
        ]

        vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.getNameFromNfcId", outgoingMessage) { responseDb ->
            logger.info "Voter ${responseDb.body.name} is ready to vote"

            vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.stopFlashing", null) { responsePlate ->
                // Send message to next processor
                vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.waitForVote", outgoingMessage)
            }
        }

    }

    void waitForVote(Message incomingMsg) {
        lcd.write("En attente Vote")

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
            println result
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
                Thread.sleep(1500);

                // TODO proceed to agregation server
                // Return to NFC waiting
                voteSaved = true

            } else if (multiplevote) {
                lcd.clear()
                lcd.write("Une seule note SVP");
                Thread.sleep(1500);
            }
            loopCount++
        }
        if (note > -1) {// Send vote to next processor
            Map outgoingMessage = [
                    "nfcId": nfcId,
                    "voteTime": voteTime,
                    "note": note
            ]
            vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.startFlashing", outgoingMessage) { response ->
                vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.waitForNfcIdentification", outgoingMessage)
            }

        }

    }

    private String byteArrayToNormalizedString(ResponseAPDU cardReponse) {
        cardReponse.bytes.encodeHex().toString()
                .toUpperCase()
                .replaceAll('(..)', '$0 ')
                .trim()
                .substring(0, 11)
    }


}
