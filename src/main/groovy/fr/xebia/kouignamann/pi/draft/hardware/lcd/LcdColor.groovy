package fr.xebia.kouignamann.pi.draft.hardware.lcd

/**
 * Created by amaury on 23/01/2014.
 */
final class LcdColor {

    static final int OFF = 0x00
    static final int RED = 0x01
    static final int GREEN = 0x02
    static final int BLUE = 0x04
    static final int YELLOW = RED + GREEN
    static final int TEAL = GREEN + BLUE
    static final int PURPLE = RED + BLUE
    static final int WHITE = RED + GREEN + BLUE
    static final int ON = RED + GREEN + BLUE

    private LcdColor() {}
}
