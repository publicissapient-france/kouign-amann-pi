package fr.xebia.kouignamann.pi.draft.hardware

import com.pi4j.io.i2c.I2CDevice
import fr.xebia.kouignamann.pi.draft.hardware.lcd.LcdColor
import fr.xebia.kouignamann.pi.draft.hardware.lcd.LcdDisplayEntry
import fr.xebia.kouignamann.pi.draft.hardware.lcd.LcdDisplayShift
import fr.xebia.kouignamann.pi.draft.hardware.lcd.LcdOnOffFlag
import fr.xebia.kouignamann.pi.draft.hardware.plate.MCP23017
import groovy.transform.CompileStatic

import static fr.xebia.kouignamann.pi.draft.hardware.lcd.LcdCommand.*

/**
 * Created by amaury on 23/01/2014.
 */
@CompileStatic
class VotingBoardLcd {

    static final int[] SHIFT_REVERSE = [
            0x00, 0x10, 0x08, 0x18,
            0x04, 0x14, 0x0C, 0x1C,
            0x02, 0x12, 0x0A, 0x1A,
            0x06, 0x16, 0x0E, 0x1E
    ]

    // I2C is relatively slow.  MCP output port states are cached
    // so we don't need to constantly poll-and-change bit states.
    private int portA = 0x00
    private int portB = 0x00
    private int ioDirB = 0x10

    private int displayControl = LcdOnOffFlag.DISPLAY_ON | LcdOnOffFlag.CURSOR_OFF | LcdOnOffFlag.BLINK_OFF

    protected int[] rowOffsets = [0x00, 0x40, 0x14, 0x54]

    static final Integer ROW_NUMBER = 2
    static final Integer COL_NUMBER = 16

    private I2CDevice device

    public VotingBoardLcd(I2CDevice device) {

        this.device = device

        // Set MCP23017 IOCON register to Bank 0 with sequential operation.
        // If chip is already set for Bank 0, this will just write to OLATB,
        // which won't seriously bother anything on the plate right now
        // (blue backlight LED will come on, but that's done in the next
        // step anyway).
        device.write(MCP23017.IOCON_BANK1, (byte) 0)

        // Brute force reload ALL registers to known state.  This also
        // sets up all the input pins, pull-ups, etc. for the Pi Plate.
        byte[] piPlateSetupRegisters = [
                0x3F,            // IODIRA    R+G LEDs=outputs, buttons=inputs
                (byte) ioDirB,    // IODIRB    LCD D7=input, Blue LED=output
                0x3F,            // IPOLA     Invert polarity on button inputs
                0x00,            // IPOLB
                0x00,            // GPINTENA  Disable interrupt-on-change
                0x00,            // GPINTENB
                0x00,            // DEFVALA
                0x00,            // DEFVALB
                0x00,            // INTCONA
                0x00,            // INTCONB
                0x00,            // IOCON
                0x00,            // IOCON
                0x3F,            // GPPUA     Enable pull-ups on buttons
                0x00,            // GPPUB
                0x00,            // INTFA
                0x00,            // INTFB
                0x00,            // INTCAPA
                0x00,            // INTCAPB
                (byte) portA,    // GPIOA
                (byte) portB,    // GPIOB
                (byte) portA,    // OLATA     0 on all outputs side effect of
                (byte) portB        // OLATB     turning on R+G+B backlight LEDs.
        ]
        device.write(0, piPlateSetupRegisters, 0, piPlateSetupRegisters.length)

        // Switch to Bank 1 and disable sequential operation.
        // From this point forward, the register addresses do NOT match
        // the list immediately above.  Instead, use the constants defined
        // at the start of the class.  Also, the address register will no
        // longer increment automatically after this -- multi-byte
        // operations must be broken down into single-byte calls.
        device.write(MCP23017.IOCON_BANK0, (byte) 0xA0)

        writeCmd(0x33) // Init
        writeCmd(0x32) // Init
        writeCmd(0x28) // 2 line 5x8 matrix

        clearDisplay()
        shiftCursorRight()
        initEntryMode()
        initDisplayControl()
        setCursorHome()

        setBacklight(LcdColor.PURPLE)
    }

    private void initDisplayControl() {
        writeCmd(DISPLAY_CONTROL | LcdOnOffFlag.DISPLAY_ON | LcdOnOffFlag.CURSOR_OFF | LcdOnOffFlag.BLINK_OFF)
    }

    private void initEntryMode() {
        writeCmd(ENTRY_MODE_SET | LcdDisplayEntry.ENTRY_LEFT | LcdDisplayEntry.ENTRY_SHIFT_DECREMENT)
    }

    private void shiftCursorRight() {
        writeCmd(CURSOR_SHIFT | LcdDisplayShift.CURSOR_MOVE | LcdDisplayShift.MOVE_RIGHT)
    }

    public void clearDisplay() {
        writeCmd(CLEAR_DISPLAY)
    }

    private void setCursorHome() {
        writeCmd(RETURN_HOME)
    }

    public void setCursorPosition(int row, int column) {
        writeCmd(SET_DDRAM_ADDR | (column + rowOffsets[row]))
    }

    private void writeCmd(int cmd) {

        pollWait()

        int bitmask = portB & 0x01   // Mask out PORTB LCD control bits

        byte[] data = ShiftAndMap4(bitmask, cmd)
        device.write(MCP23017.GPIOB, data, 0, 4)
        portB = data[3]

        // If a poll-worthy instruction was issued, reconfigure D7
        // pin as input to indicate need for polling on next call.
        if (cmd == CLEAR_DISPLAY || cmd == RETURN_HOME) {
            ioDirB |= 0x10
            device.write(MCP23017.IODIRB, (byte) ioDirB)
        }
    }

    public void display(String messageToDisplay) {

        clearDisplay()

        int sLen = messageToDisplay.length()

        if (sLen < 1) {
            return
        }

        int bytesLen = 4 * sLen

        pollWait()

        int bitmask = portB & 0x01   // Mask out PORTB LCD control bits

        bitmask |= 0x80 // Set data bit

        byte[] bytes = new byte[4 * sLen]

        sLen.times { int index ->
            byte[] data = ShiftAndMap4(bitmask, (byte) messageToDisplay.charAt(index))

            4.times { int secondaryIndex ->
                bytes[(index * 4) + secondaryIndex] = data[secondaryIndex]
            }
        }

        device.write(MCP23017.GPIOB, bytes, 0, bytesLen)

        portB = bytes[bytesLen - 1]
    }

    protected void pollWait() {

        // The speed of LCD accesses is inherently limited by I2C through the
        // port expander.  A 'well behaved program' is expected to poll the
        // LCD to know that a prior instruction completed.  But the timing of
        // most instructions is a known uniform 37 mS.  The enable strobe
        // can't even be twiddled that fast through I2C, so it's a safe bet
        // with these instructions to not waste time polling (which requires
        // several I2C transfers for reconfiguring the port direction).
        // The D7 pin is set as input when a potentially time-consuming
        // instruction has been issued (e.g. screen clearDisplay), as well as on
        // startup, and polling will then occur before more commands or data
        // are issued.

        // If pin D7 is in input state, poll LCD busy flag until clearDisplay.
        if ((ioDirB & 0x10) != 0) {
            int lo = (portB & 0x01) | 0x40
            int hi = lo | 0x20 // E=1 (strobe)
            device.write(MCP23017.GPIOB, (byte) lo)
            while (true) {
                device.write((byte) hi) // Strobe high (enable)
                int bits = device.read() // First nybble contains busy state
                byte[] array = [(byte) lo, (byte) hi, (byte) lo]
                device.write(MCP23017.GPIOB, array, 0, 3) // Strobe low, high, low.  Second nybble (A3) is ignored.
                if ((bits & 0x02) == 0) break // D7=0, not busy
            }
            portB = lo
            ioDirB &= 0xEF // Polling complete, change D7 pin to output
            device.write(MCP23017.IODIRB, (byte) ioDirB)
        }
    }

    protected byte[] ShiftAndMap4(int bitmask, int value) {

        // The LCD data pins (D4-D7) connect to MCP pins 12-9 (PORTB4-1), in
        // that order.  Because this sequence is 'reversed,' a direct shift
        // won't work.  This table remaps 4-bit data values to MCP PORTB
        // outputs, incorporating both the reverse and shift.

        int hi = bitmask | SHIFT_REVERSE[value >> 4]

        int lo = bitmask | SHIFT_REVERSE[value & 0x0F]

        byte[] data = [(byte) (hi | 0x20), (byte) hi, (byte) (lo | 0x20), (byte) lo]

        return data
    }

    public void setCursor(boolean on) {
        if (on) {
            displayControl |= LcdOnOffFlag.CURSOR_ON
        } else {
            displayControl &= ~LcdOnOffFlag.CURSOR_ON
        }
        writeCmd(DISPLAY_CONTROL | displayControl)
    }

    public void setDisplay(boolean on) {
        if (on) {
            displayControl |= LcdOnOffFlag.DISPLAY_ON
        } else {
            displayControl &= ~LcdOnOffFlag.DISPLAY_ON
        }
        writeCmd(DISPLAY_CONTROL | displayControl)
    }

    void setBacklight(int color) {
        int c = ~color
        portA = (byte) ((portA & 0x3F) | ((c & 0x03) << 6))
        portB = (byte) ((portB & 0xFE) | ((c & 0x04) >> 2))
        // Has to be done as two writes because sequential operation is off.
        device.write(MCP23017.GPIOA, (byte) portA)
        device.write(MCP23017.GPIOB, (byte) portB)
    }

    void stop() {
        clearDisplay()
        setDisplay(false)
        setBacklight(LcdColor.OFF)
    }
}
