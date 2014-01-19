package fr.xebia.kouignamann.pi.draft

import fr.xebia.kouignamann.pi.draft.hardware.VotingBoard
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

/**
 * Created by amaury on 17/01/2014.
 */
class VotingVerticle extends Verticle {

    Logger log

    VotingBoard votingBoard

    /**
     * Init hardware, then send register a handler for startup.complete
     */
    def start() {
        log = container.logger

        initBackPack()
    }

    void initBackPack() {

    }

    def startupComplete(Message message) {
        // when called, unregister handler to this method, then send to waitCard
    }

    def waitCard(Message message) {
        // display user message to engage vote

        String nfcId = votingBoard.waitForNfcCard()

        // send to waitVote with timeout, expecting for vote value
        // reply => send value + nfcId to dataVerticle
        // timeout or not => send to waitCard
    }

    def waitVote(Message message) {
        // waits indefinitely for a vote, should be killed by timeout
        int vote = votingBoard.waitForVote()
    }
}
