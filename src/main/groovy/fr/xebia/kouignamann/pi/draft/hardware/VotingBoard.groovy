package fr.xebia.kouignamann.pi.draft.hardware

/**
 * This class abstracts the whole hardware part.
 *
 * should be composed of : LCD screen (color, flash, text) + button (listen, light on, light off) + NFC reader
 */
class VotingBoard {



    void initLcd() {
        // init the lcd
    }

    String waitForNfcCard() {

        // wait NFC card forever
        // when read, return as string
    }

    void waitForVote() {
        // wait for vote forever
        // should be killed by timeout when called
    }
}
