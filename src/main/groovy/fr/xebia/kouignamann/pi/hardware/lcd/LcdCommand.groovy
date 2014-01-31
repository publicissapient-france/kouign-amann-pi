package fr.xebia.kouignamann.pi.hardware.lcd

/**
 * Created by amaury on 23/01/2014.
 */
final class LcdCommand {

    static final int CLEAR_DISPLAY = 0x01
    static final int RETURN_HOME = 0x02
    static final int ENTRY_MODE_SET = 0x04
    static final int DISPLAY_CONTROL = 0x08
    static final int CURSOR_SHIFT = 0x10
    static final int FUNCTION_SET = 0x20
    static final int SET_CGRAM_ADDR = 0x40
    static final int SET_DDRAM_ADDR = 0x80

    private LcdCommand() {}
}
