package fr.xebia.kouignamann.pi.vote

import fr.xebia.kouignamann.pi.mock.RfidReaderMock
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import javax.smartcardio.Card
import javax.smartcardio.CommandAPDU
import javax.smartcardio.ResponseAPDU
import javax.smartcardio.TerminalFactory



class NfcVerticle extends Verticle {
    def logger
    def nfcTerminal
    static final byte[] READ_UID_SEQ = [0xFF, 0xCA, 0x00, 0x00, 0x00]

    def selfHealingTimer
    def SELF_HEALING_PERIOD = 1000 * 60 * 2

    def start() {
        logger = container.logger
        if (!container.config.mockRfid) {
            logger.info "Start -> Finding Nfc reader"
            nfcTerminal = TerminalFactory.getDefault().terminals().list().get(0)
            logger.info "Start -> Nfc reader found : ${nfcTerminal.name}"
        } else {
            logger.info "Start -> Finding MOCK Nfc reader"
            nfcTerminal = new RfidReaderMock()
            nfcTerminal.logger = logger
        }

        logger.info "Start -> Initialize handler";
        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForNfcIdentification": this.&waitForNfcIdentification
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
        logger.info "Start -> Done initialize handler";
    }

    void selfHealing() {
        // Self healing
        if (selfHealingTimer) {
            logger.info("Process -> selfHealing")
            selfHealingTimer = vertx.setPeriodic(SELF_HEALING_PERIOD) { selfHealingTimer ->
                waitForNfcIdentification(null)
            }
        }
    }

    void cancelSelfHealing() {
        logger.info("Process -> cancelSelfHealing")
        if (selfHealingTimer) {
            vertx.cancelTimer(selfHealingTimer)
        }
    }

    void waitForNfcIdentification(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForNfcIdentification")
        cancelSelfHealing()
        if (incomingMsg) {
            incomingMsg.reply([status: "Processing"])
        }
        // Mise en attente bloquante
        logger.info("Waiting -> nfcTerminal.waitForCardPresent")
        nfcTerminal.waitForCardPresent 0

        // Start self healing timer
        selfHealing()

        // Display userName
        try {
            Card card = nfcTerminal.connect("*")
            ResponseAPDU cardResponse = card.getBasicChannel().transmit(new CommandAPDU(READ_UID_SEQ))
            card.disconnect(false)

            // Send message to next processor
            Map outgoingMessage = [
                    "nfcId": byteArrayToNormalizedString(cardResponse),
                    "voteTime": new Date().getTime()
            ]

            logger.info("Bus -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.getNameFromNfcId ${outgoingMessage}")
            vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.getNameFromNfcId", outgoingMessage) { responseDb ->
                logger.info "Process -> Voter ${responseDb.body.name} is ready to vote"

                // Send message to next processor
                outgoingMessage.put("name", responseDb.body.name)
                logger.info("Bus -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForVote ${outgoingMessage}")
                vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForVote", outgoingMessage)
            }
        } catch (Exception e) {
            logger.error e
            waitForNfcIdentification(null)
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
