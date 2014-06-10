package fr.xebia.kouignamann.pi

import fr.xebia.kouignamann.pi.hardware.VotingBoard
import org.slf4j.LoggerFactory
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class MainVerticle extends Verticle {

    Logger log
    def richLogger = LoggerFactory.getLogger(MainVerticle.class);

    VotingBoard votingBoard

    def start() {
        log = container.logger

        String localBusPrefix = 'fr.xebia.kouignamann.pi.' + container.config.hardwareUid

        log.info('--START--')
        richLogger.info('--START--')

        EventBus eventBus = vertx.eventBus

        // HTTP
        if (container.config.modeAdmin) {

            // Reprise sur incident
            container.deployWorkerVerticle('groovy:fr.xebia.kouignamann.pi.ParseLogVerticle', container.config) { parseLogResult ->
                if (parseLogResult.succeeded) {
                    log.info "The verticle has been deployed, deployment ID is ${parseLogResult.result}"
                } else {
                    parseLogResult.cause.printStackTrace()
                }
            }

            log.info("Main : HTTP Server")

            def server = vertx.createHttpServer()
            server.requestHandler { request ->
                log.info "A request has arrived on the server!"
                eventBus.send("${localBusPrefix}.parseLog", 'call')
                log.info("Bus -> ${localBusPrefix}.parseLog")
            }
            server.listen(8080) { AsyncResult asyncResult ->
                if (asyncResult.succeeded) {
                    log.info("START: HTTPServer ready")
                } else {
                    log.error("START: HTTPServer failed", asyncResult.cause)
                }
            }
        }

        if (container.config.modeRun) {
            votingBoard = new VotingBoard(container, vertx, localBusPrefix)

            log.info('START: VotingBoard ready')

            [
                    "${localBusPrefix}.waitCard": votingBoard.&waitCard,
                    "${localBusPrefix}.waitVote": votingBoard.&waitVote
            ].each { address, handler ->
                eventBus.registerHandler(address, handler) { AsyncResult asyncResult ->
                    if (asyncResult.succeeded) {
                        log.info("START: Bus handler ready: ${address}")
                    } else {
                        log.error("START: Bus handler failed: ${address}", asyncResult.cause)
                    }
                }
            }

            eventBus.send("${localBusPrefix}.waitCard", 'call')
        }

        container.deployWorkerVerticle('groovy:fr.xebia.kouignamann.pi.MqttVerticle', container.config)

        log.info("START: Initialisation done")
    }


    def stop() {
        log.info('STOP: Shutting VotingBoard down')
        votingBoard?.stop()
    }
}
