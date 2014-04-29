package fr.xebia.kouignamann.pi

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class ParseLogVerticle extends Verticle {

    private Logger log
    def busPrefix

    def start() {

        log = container.logger

        log.info('Start -> Initialize handler')

        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.parseLog": this.&parseLog,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
            log.info("Registered -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.parseLog")
        }

        busPrefix = 'fr.xebia.kouignamann.pi.' + container.config.hardwareUid

        log.info('Start -> Done initialize handler')
    }

    def stop() {
        log.info('Stop method not implemented yet.')
    }

    def parseLog(Message msg) {
        log.info "Start to parse log"
        // Readfile
        def logFile = new File('/home/pi/vertx.log')

        log.info "File found : ${logFile}"
        logFile.eachLine { it ->
            def matcher = it  =~ /.* BUS => fr.xebia.kouignamann.pi.votingboard.*.processVote => \[nfcId:(.*), voteTime:(.*), note:(.*)\]/
            // Continue if it is not a vote
            if (!matcher) {
                //log.info("Not processing ${it}")
                return
            }
            log.info "Match found, sending message to next processor ${it}"
            Map outgoingMessage = [
                    "nfcId": matcher[0][1],
                    "voteTime": Long.valueOf(matcher[0][2]),
                    "note": matcher[0][3]
            ]

            log.info("BUS => ${busPrefix}.processVote => ${outgoingMessage}")
            vertx.eventBus.send("${busPrefix}.processVote", outgoingMessage)
        }
        log.info "File processed : ${logFile}"

    }
}
