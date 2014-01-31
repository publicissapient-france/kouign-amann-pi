package fr.xebia.kouignamann.pi.hardware.mock

import org.vertx.groovy.core.eventbus.Message
import org.vertx.java.core.logging.Logger

/**
 * Created by amaury on 23/01/2014.
 */
class MockVotingBoardButtons {

    Logger log

    MockVotingBoardButtons(Logger log) {
        this.log = log
        log.info('START: Mocking buttons light behavior, will see log only')
        lightOnAll()
        sleep 1000
        switchOffAllButtonButOne(null)
    }

    def lightOnAll() {
        log.info('Switching on all button lights')
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def switchOffAllButtonButOne(Message msgIn) {
        log.info('Switching off all button lights but ' + msgIn?.body.note)
    }

    void stop() {

    }
}
