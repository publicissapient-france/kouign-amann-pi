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
        log.info "Main -> starting"

        votingBoard = new VotingBoard()

        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitCard", votingBoard.&waitCard)
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.waitVote", votingBoard.&waitVote)
        vertx.eventBus.registerHandler("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffLedButtons", votingBoard.&switchOffLedButtons)

        container.deployWorkerVerticle('groovy:' + DataVerticle.class.name, container.config, 3) { deployDataVerticleResult ->
            if (deployDataVerticleResult.succeeded) {
                log.info('START: DataVerticle deployed')
            } else {
                log.error('START: Failed to deploy DataVerticle', deployDataVerticleResult.throwable)
            }
        }
    }

    def stop() {
        log.info('STOP: Shutting VotingBoard down')
        votingBoard?.stop()
    }
}
