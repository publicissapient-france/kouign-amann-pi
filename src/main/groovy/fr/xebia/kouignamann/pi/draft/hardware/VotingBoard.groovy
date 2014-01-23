package fr.xebia.kouignamann.pi.draft.hardware

import org.vertx.groovy.core.eventbus.Message

/**
 * This class abstracts the whole hardware part.
 *
 * should be composed of : LCD screen (color, flash, text) + button (listen, light on, light off) + NFC reader
 */
class VotingBoard {



    void initLcd() {
        // init the lcd
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def waitCard(Message message) {
        // display user message to engage vote

        String nfcId = votingBoard.waitForNfcCard()

        // send to waitVote with timeout, expecting for vote value
        // reply => send value + nfcId to dataVerticle
        // timeout or not => send to waitCard
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def waitVote(Message message) {
        // waits indefinitely for a vote, should be killed by timeout
        int vote = votingBoard.waitForVote()
    }

    /**
     * Driving hardware
     * @return
     */
    String waitForNfcCard() {

        // wait NFC card forever
        // when read, return as string
    }

    /**
     * Driving hardware
     * @return
     */
    void waitForVote() {
        // wait for vote forever
        // should be killed by timeout when called
    }
}
