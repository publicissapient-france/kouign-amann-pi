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

        log.info('START: VotingBoard ready')
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitCard", votingBoard.&waitCard) { asyncResult ->
            if (asyncResult.succeeded) {
            log.info('START: WaitCard handler ready: '+asyncResult.succeeded)
            } else {
                log.error('START: WaitCard handler failed', asyncResult.throwable)
            }
        }
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitVote", votingBoard.&waitVote) { asyncResult ->
            if (asyncResult.succeeded) {
                log.info('START: waitVote handler ready: '+asyncResult.succeeded)
            } else {
                log.error('START: waitVote handler failed', asyncResult.throwable)
            }
        }
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffAllButtonButOne", votingBoard.buttons.&switchOffAllButtonButOne) { asyncResult ->
            if (asyncResult.succeeded) {
                log.info('START: switchOffAllButtonButOne handler ready: '+asyncResult.succeeded)
            } else {
                log.error('START: switchOffAllButtonButOne handler failed', asyncResult.throwable)
            }
        }

        votingBoard.waitCard(null)

        log.info('TODO: Deploy MQTT Verticle')
    }

    def stop() {

        log.info('STOP: Shutting VotingBoard down')

        votingBoard?.stop()
    }
}
