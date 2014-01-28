package fr.xebia.kouignamann.pi.draft.hardware

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.io.i2c.I2CDevice
import fr.xebia.kouignamann.pi.draft.hardware.plate.MCP23017

/**
 * Created by amaury on 23/01/2014.
 */
class VotingBoardButtons {

    Map<Integer, GpioPinDigitalOutput> buttons = [:]
    GpioController gpio
    I2CDevice i2cDevice

    VotingBoardButtons(GpioController gpio, I2CDevice i2cDevice) {
        this.gpio = gpio
        this.i2cDevice = i2cDevice

        // GPIO # 27
        buttons.put(1, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Button1", PinState.LOW))
        // GPIO # 22
        buttons.put(2, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Button2", PinState.LOW))
        // GPIO # 23
        buttons.put(3, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Button3", PinState.LOW))
        // GPIO # 24
        buttons.put(4, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Button4", PinState.LOW))
        // GPIO # 25
        buttons.put(5, gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Button5", PinState.LOW))
        illuminateAllButtons()
        sleep 1000
        switchOffAllButtonButOne(null)
    }

    def illuminateAllButtons() {
        for (i in 1..5) {
            buttons."button${i}".high()
        }
    }

    def switchOffAllButtonButOne(Integer note) {
        for (i in 1..5) {
            if (note || i != note) {
                buttons.note.low()
            }
        }
        if (note) {
            sleep 1000
            buttons.note.low()
        }
    }


    byte[] _button_pins = [0, 1, 2, 3, 4]

    private void writa(byte b) {
        i2cDevice.write(b)
    }

    private byte[] reada(int len) {
        byte[] b = new byte[len]
        i2cDevice.read(b, 0, len)
        return b
    }

    private byte reada() {
        byte[] b = reada(1)
        return b[0]
    }

    byte digitalRead(byte p) {
        byte gpioaddr
        // only 16 bits!
        if (p > 15)
            return 0
        if (p < 8) {
            gpioaddr = MCP23017.GPIOA
        } else {
            gpioaddr = MCP23017.GPIOB
            p -= 8
        }
        // read the current GPIO
        writa(gpioaddr)
        byte rec = reada()
        byte step1 = (byte) (rec >> p)
        return (byte) (step1 & (byte) 0x1)
    }

    public byte readButtons() {
        byte reply = 0x1F
        for (byte i = 0; i < 5; i++) {
            reply &= ~((digitalRead(_button_pins[i])) << i)
        }
        return reply

    }

    public int[] readButtonsPressed() {
        int[] result = [0, 0, 0, 0, 0]
        for (byte i = 0; i < 5; i++) {
            result[i] = (int) digitalRead(_button_pins[i])
        }
        return result
    }

    def shutdown() {
        gpio.shutdown();
    }

}
