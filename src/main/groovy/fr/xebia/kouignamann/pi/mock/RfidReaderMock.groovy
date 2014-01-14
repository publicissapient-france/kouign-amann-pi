package fr.xebia.kouignamann.pi.mock

import javax.smartcardio.Card
import javax.smartcardio.CardChannel
import javax.smartcardio.ResponseAPDU


class RfidReaderMock {

    def logger

    static channelMock = [ transmit: {  command -> new ResponseAPDU(new byte[0xAA]) } ] as CardChannel
    static cardMock = [ getBasicChannel: { id -> channelMock }, disconnect:{  bool -> true }  ] as Card

    def waitForCardPresent(int timeout) {
        sleep 3000
        return true
    }

    def connect(String param){
        return cardMock
    }
}
