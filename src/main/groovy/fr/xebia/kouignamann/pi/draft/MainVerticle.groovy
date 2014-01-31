package fr.xebia.kouignamann.pi.draft

import fr.xebia.kouignamann.pi.draft.hardware.VotingBoard
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class MainVerticle extends Verticle {

    Logger log

    VotingBoard votingBoard

    def start() {
        log = container.logger

        String localBusPrefix = 'fr.xebia.kouignamann.pi.' + container.config.hardwareUid

        log.info('--START--')

        votingBoard = new VotingBoard(container, vertx, localBusPrefix)

        log.info('START: VotingBoard ready')

        EventBus eventBus = vertx.eventBus

        [
                "${localBusPrefix}.waitCard": votingBoard.&waitCard,
                "${localBusPrefix}.waitVote": votingBoard.&waitVote
        ].each { address, handler ->
            eventBus.registerHandler(address, handler) { AsyncResult asyncResult ->
                if (asyncResult.succeeded) {
                    log.info('START: Bus handler ready: ${address}')
                } else {
                    log.error('START: Bus handler failed: ${address}', asyncResult.cause)
                }
            }
        }

        eventBus.send("${localBusPrefix}.waitCard", 'call')

        log.info('TODO: Deploy MQTT Verticle')
    }

    def stop() {
        log.info('STOP: Shutting VotingBoard down')
        votingBoard?.stop()
    }
}
