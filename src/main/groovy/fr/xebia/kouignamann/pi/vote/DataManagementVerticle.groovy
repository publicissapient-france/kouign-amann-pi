package fr.xebia.kouignamann.pi.vote

import fr.xebia.kouignamann.pi.util.WrapperEventBus
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class DataManagementVerticle extends Verticle {
    def logger

    long purgeLocalTimer
    static final Integer PURGE_PERIOD = 1000 * 60 //* 5

    def start() {
        logger = container.logger
        logger.info "Initialize handler";
        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processVote": this.&processVote,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";

        logger.info "Set periodic retry to central";
        purgeLocalTimer = vertx.setPeriodic(PURGE_PERIOD) { purgeLocalTimer ->
            Map outgoingMessage = [
                    "nextProcessor": "fr.xebia.kouignamann.pi.central.processSingleVote"
            ]
            vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processStoredVotes", outgoingMessage)
        }
    }

    def processVote(Message incomingMsg) {
        Map outgoingMessage = [
                "nfcId": incomingMsg.body.nfcId,
                "voteTime": incomingMsg.body.voteTime,
                "note": incomingMsg.body.note,
                "hardwareUid": container.config.hardwareUid
        ]

        def eventBus = vertx.eventBus
        def wrapperBus = new WrapperEventBus(eventBus.javaEventBus())

        // End point must exists ?
        // TODO Need further testing
        wrapperBus.sendWithTimeout("fr.xebia.kouignamann.pi.central.processSingleVote", outgoingMessage, 1000) { result ->
            if (result.succeeded) {
                // If success do nothing
                logger.info("${outgoingMessage} successfully processed by central")
            } else {
                // If failed, store in local DB
                logger.info (result.cause)
                logger.info("TIMEOUT - Send message to next processor fr.xebia.kouignamann.pi.${container.config.hardwareUid}.storeVote for local storage ${outgoingMessage}")
                vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.storeVote", outgoingMessage)
            }

        }
    }
}
