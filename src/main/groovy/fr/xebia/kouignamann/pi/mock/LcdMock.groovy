package fr.xebia.kouignamann.pi.mock

import fr.xebia.kouignamann.pi.adafruit.lcd.AdafruitLcdPlate


class LcdMock {
    static final int[] COLORS = AdafruitLcdPlate.COLORS

    // Singleton
    private static final INSTANCE = new LcdMock()
    static getInstance(){
        return INSTANCE
    }

    def write(String message) {
        println "Mock lcd writes ${message}"
    }

    def setBacklight(int id){
        println "Mock lcd setBacklight ${id}"
    }

    def readButtonsPressed(){
        sleep 1000
        println "Mock lcd readButtonsPressed ${[0, 0, 1, 0, 0]}"
        return [0, 0, 1, 0, 0]
    }

    def clear(){
        println "Mock lcd clear"
    }
}
