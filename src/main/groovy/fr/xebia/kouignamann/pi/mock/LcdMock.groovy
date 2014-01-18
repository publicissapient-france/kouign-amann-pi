package fr.xebia.kouignamann.pi.mock

import fr.xebia.kouignamann.pi.adafruit.lcd.AdafruitLcdPlate


class LcdMock {
    static final int[] COLORS = AdafruitLcdPlate.COLORS

    def logger

    def write(String message) {
        logger.debug "Mock lcd writes ${message}"
    }

    def setBacklight(int id){
        logger.debug "Mock lcd setBacklight ${id}"
    }

    def readButtonsPressed(){
        sleep 20000
        logger.debug "Mock lcd readButtonsPressed ${[0, 0, 1, 0, 0]}"
        return [0, 0, 1, 0, 0]
    }

    def clear(){
        logger.debug "Mock lcd clear"
    }
}
