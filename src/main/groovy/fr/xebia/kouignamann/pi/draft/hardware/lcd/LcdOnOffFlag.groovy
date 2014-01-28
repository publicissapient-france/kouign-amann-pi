package fr.xebia.kouignamann.pi.draft.hardware.lcd

/**
 * Created by amaury on 23/01/2014.
 */
final class LcdOnOffFlag {

    static final int DISPLAY_ON = 0x04
    static final int DISPLAY_OFF = 0x00
    static final int CURSOR_ON = 0x02
    static final int CURSOR_OFF = 0x00
    static final int BLINK_ON = 0x01
    static final int BLINK_OFF = 0x00

    private LcdOnOffFlag() {}
}
