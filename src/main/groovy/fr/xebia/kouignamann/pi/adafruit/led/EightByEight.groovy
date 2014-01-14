package fr.xebia.kouignamann.pi.adafruit.led


class EightByEight {
    LedBackpack display

    // Constructor
    public EightByEight(int busNo, int address) {
        print "Initializing a new instance of LEDBackpack at ${address}\n"
        display = new LedBackpack(busNo, address)
    }

    public void setPixel(int row, int column) {
        if (!isBetween0And7(row) || !isBetween0And7(column)) {
            return;
        }

        int[] buffer = display.getBuffer();
        int oldRow = buffer[row];
        //if (color == LedColor.GREEN) {
        //    display.setBufferRow(row, oldRow | 1 << column); // lower byte is for green LED
        //} else if (color == LedColor.RED) {
        //    display.setBufferRow(row, oldRow | 1 << (column + 8)); // higher byte is for red LED
        //} else if (color == LedColor.YELLOW) {
        //println "Row ${row} Value ${oldRow | (1 << (column + 8) | (1 << column))}"
        display.setBufferRow(row, oldRow | (1 << (column + 8) | (1 << column)), true); // both LEDs = yellow
        //} else if (color == LedColor.OFF) {
        //    display.setBufferRow(row, oldRow & ~(1 << column) & ~(1 << (column + 8))); // switch off both
        //}

    }

    boolean isBetween0And7(int i) {
        return i >= 0 && i <= 7
    }

    public void clear(){
        display.clear(true)
    }
}
