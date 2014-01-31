package fr.xebia.kouignamann.pi.hardware.plate

/**
 * Created by amaury on 23/01/2014.
 */
class MCP23017 {
    static final int IOCON_BANK0 = 0x0A  // IOCON when Bank 0 active
    static final int IOCON_BANK1 = 0x15  // IOCON when Bank 1 active

    // These are register addresses when in Bank 1 only:
    static final int GPIOA = 0x09
    static final int GPIOB = 0x19
    static final int IODIRB = 0x10
}
