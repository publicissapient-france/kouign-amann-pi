package fr.xebia.kouignamann.pi.adafruit.led

import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory

class LedBackpack {

    // Registers
    private static final int HT16K33_REGISTER_DISPLAY_SETUP = 0x80;
    private static final int HT16K33_REGISTER_SYSTEM_SETUP = 0x20;
    private static final int HT16K33_REGISTER_DIMMING = 0xE0;

    // Blink rate
    public static final int HT16K33_BLINKRATE_DISPLAY_OFF = 0x00;
    public static final int HT16K33_BLINKRATE_OFF = 0x01;
    public static final int HT16K33_BLINKRATE_2HZ = 0x03;
    public static final int HT16K33_BLINKRATE_1HZ = 0x05;
    public static final int HT16K33_BLINKRATE_HALFHZ = 0x07;

    // Display buffer (8x16-bits)
    private int[] BUFFER = [0, 0, 0, 0, 0, 0, 0, 0]

    private final I2CBus i2CBus;
    private final I2CDevice i2cDevice = null

    /**
     * constructs an Adafruit LED Backpack
     *
     * @param busNr the bus nr, 1 on current Pi revision, 0 for older revisions
     * @param address the I2C address of the backback (default is 0x0070)
     *
     * @throws IOException
     */
    public LedBackpack(int busNr, int address) throws IOException {
        println "Get Device\n"
        i2CBus = I2CFactory.getInstance(busNr);
        i2cDevice = i2CBus.getDevice(address);

        println "Turn the oscillator on\n"
        i2cDevice.write(HT16K33_REGISTER_SYSTEM_SETUP | 0x01, (byte) 0x00);

        println "Turn display on and blink rate off\n"
        setBlinkRate(HT16K33_BLINKRATE_OFF);

        println "Set max brightness\n"
        setBrightness(15);

        println "Init done"

        // clear display
        clear(true);

    }

    public void setBlinkRate(int blinkrateValue) {
        try {
            i2cDevice.write(HT16K33_REGISTER_DISPLAY_SETUP | blinkrateValue, (byte) 0x00);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setBrightness(int brightness) {
        if (brightness < 0) brightness = 0;
        else if (brightness > 15) brightness = 15;
        try {
            i2cDevice.write(HT16K33_REGISTER_DIMMING | brightness, (byte) 0x00);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear(boolean flush) {
        BUFFER = [0, 0, 0, 0, 0, 0, 0, 0];
        if (flush) {
            writeDisplay();
        }
    }

    /**
     * write a row to the buffer
     *
     * @param row the row number, 0..7
     * @param value the value
     */
    protected void setBufferRow(int row, int value) {
        setBufferRow(row, value, false);
    }

    /**
     * write a row to the buffer
     *
     * @param row the row number, 0..7
     * @param value the value
     * @param flush write buffer to display immediately
     */
    protected void setBufferRow(int row, int value, boolean flush) {

        if (!isBetween0And7(row)) return;
        BUFFER[row] = value;
        if (flush) {
            writeDisplay();
        }
    }

    protected boolean isBetween0And7(int x) {
        return x >= 0 && x <= 7;
    }

    /**
     * get the current buffer (might not be displayed yet)
     *
     * @return current buffer
     */
    protected int[] getBuffer() {
        return BUFFER;
    }

    public void writeDisplay() {

        byte[] bytes = new byte[16]; // we need 2 bytes for each row
        for (int i = 0; i < 8; i++) { // iterate rows
            bytes[i * 2] = (byte) (BUFFER[i] & 0xFF); // lower byte for green LED
            bytes[i * 2 + 1] = (byte) ((BUFFER[i] >> 8) & 0xFF); // higher byte for red LED
        }
        //println bytes
        if (i2cDevice != null)
            i2cDevice.write(0x00, bytes, 0, 16);
    }
}