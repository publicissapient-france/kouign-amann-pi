package fr.xebia.kouignamman.pi.vote

import fr.xebia.kouignamman.pi.adafruit.lcd.AdafruitLcdPlate
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import javax.smartcardio.*

class VoteVerticle extends Verticle {

    def logger = container.logger
    def static lcd = new AdafruitLcdPlate(1, 0x20);
    CardTerminal nfcTerminal
    static final byte[] READ_UID_SEQ = [0xFF, 0xCA, 0x00, 0x00, 0x00]


    def start() {
        logger.info("Initializing led plate");
        lcd.setBacklight(0x01 + 0x04)
        logger.info("Done initializing led plate");

        logger.info("Finding Nfc reader");
        nfcTerminal = TerminalFactory.getDefault().terminals().list().get(0);
        logger.info("Nfc reader found : ${nfcTerminal.name}");

        logger.info("Initialize handler");
        [
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.waitForNfcIdentification": this.&waitForNfcIdentification,
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.waitForVote": this.&waitForVote,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
        logger.info("Done initialize handler");

        //lcd.shutdown();
    }

    void waitForNfcIdentification(Message incomingMsg) {
        // Wait for NFC identification
        lcd.write("En attente")

        // Mise en attente bloquante
        nfcTerminal.waitForCardPresent(0)

        // Display userName
        Card card = nfcTerminal.connect("*")
        ResponseAPDU cardResponse = card.basicChannel.transmit(new CommandAPDU(READ_UID_SEQ))
        card.disconnect(false)

        // Send message to next processor
        Map outgoingMessage = [
                "nfcId": byteArrayToNormalizedString(cardResponse),
                "voteTime": new Date().getTime()
        ]

        vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.stopFlashing", outgoingMessage)
        vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.waitForVote", outgoingMessage)
    }

    void waitForVote(Message incomingMsg) {
        lcd.setBacklight(0x01 + 0x04)

        boolean buttonPressed = false
        int note = -1

        // Extract NFC id from message received
        def nfcId = incomingMsg.nfcId
        def voteTime = incomingMsg.voteTime

        def detectionTime = 10
        def maxLoops = 10000 / detectionTime
        def loopCount = 0

        // Wait for button to be pressed
        while (!buttonPressed && loopCount < maxLoops) {
            Thread.sleep(detectionTime);
            int[] result = lcd.readButtonsPressed();

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
                // Send vote to next processor
                Map outgoingMessage = [
                        "nfcId": nfcId,
                        "voteTime": voteTime,
                        "note": note
                ]
                // TODO proceed to agregation server
                // Return to NFC waiting
                vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.startFlashing", outgoingMessage)
                vertx.eventBus.send("fr.xebia.kouignamman.pi.${container.config.hardwareUid}.waitForNfcIdentification", outgoingMessage)

            } else if (multiplevote) {
                lcd.clear()
                lcd.write("Une seule note SVP");
                Thread.sleep(1500);
            }
            loopCount++
        }

    }

    private String byteArrayToNormalizedString(ResponseAPDU cardReponse) {
        cardReponse.bytes.encodeHex().toString()
                .toUpperCase()
                .replaceAll('(..)', '$0 ')
                .trim()
                .substring(0, 12)
    }


}
