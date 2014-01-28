package fr.xebia.kouignamann.pi.draft

import fr.xebia.kouignamann.pi.draft.hardware.VotingBoard
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class MainVerticle extends Verticle {


    Logger log

    VotingBoard votingBoard

    def start() {
        log = container.logger

        log.info('START--')

        votingBoard = new VotingBoard(container, vertx)

        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitCard", votingBoard.&waitCard)
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitVote", votingBoard.&waitVote)
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffLedButtons", votingBoard.&switchOffLedButtons)

        log.debug('TODO: Deploy MQTT Verticle')
    }

    def stop() {

        log.info('STOP: Shutting VotingBoard down')

        votingBoard?.stop()
    }
}
