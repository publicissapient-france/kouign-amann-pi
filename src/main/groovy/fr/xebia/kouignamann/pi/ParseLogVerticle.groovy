package fr.xebia.kouignamann.pi

import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class ParseLogVerticle extends Verticle {

    private Logger log
    def busPrefix = 'fr.xebia.kouignamann.pi.' + container.config.hardwareUid

    def start() {

        log = container.logger

        log.info('Start -> Initialize handler')

        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.parseLog": this.&parseLog,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        log.info('Start -> Done initialize handler')
    }

    def stop() {
        log.info('Stop method not implemented yet.')
    }

    def parseLog() {
        // Readfile
        def logFile = new File('/home/pi/vertx.log')

        logFile.eachLine {
            def matcher = it  =~ /.* BUS => fr.xebia.kouignamann.pi.votingboard.*.processVote => \[nfcId:(.*), voteTime:(.*), note:(.*)\]/
            // Continue if it is not a vote
            if (!matcher) {return}
            Map outgoingMessage = [
                    "nfcId": matcher[0][1],
                    "voteTime": matcher[0][2],
                    "note": matcher[0][3]
            ]

            log.info("BUS => ${busPrefix}.processVote => ${outgoingMessage}")
            vertx.eventBus.send("${busPrefix}.processVote", outgoingMessage)
        }
    }
}
