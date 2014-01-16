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

    void waitForNfcIdentification(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForNfcIdentification")
        if (incomingMsg) {
            incomingMsg.reply([status: "Processing"])
        }
        // Mise en attente bloquante
        logger.info("Waiting -> nfcTerminal.waitForCardPresent")
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

        logger.info("Bus -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.getNameFromNfcId ${outgoingMessage}")
        vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.getNameFromNfcId", outgoingMessage) { responseDb ->
            logger.info "Process -> Voter ${responseDb.body.name} is ready to vote"

            // Send message to next processor
            outgoingMessage.put("name", responseDb.body.name)
            logger.info("Bus -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForVote ${outgoingMessage}")
            vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitForVote", outgoingMessage)
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
