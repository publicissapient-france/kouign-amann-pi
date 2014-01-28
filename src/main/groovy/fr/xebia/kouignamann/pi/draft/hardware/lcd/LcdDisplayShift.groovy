package fr.xebia.kouignamann.pi.draft.hardware.lcd

/**
 * Created by amaury on 23/01/2014.
 */
enum LcdDisplayShift {

    static final int DISPLAY_MOVE = 0x08
    static final int CURSOR_MOVE = 0x00
    static final int MOVE_RIGHT = 0x04
    static final int MOVE_LEFT = 0x00

    private LcdDisplayShift() {}
}
