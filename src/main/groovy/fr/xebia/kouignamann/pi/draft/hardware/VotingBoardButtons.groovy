package fr.xebia.kouignamann.pi.draft.hardware

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import org.vertx.groovy.core.eventbus.Message

/**
 * Created by amaury on 23/01/2014.
 */
class VotingBoardButtons {

    Map<Integer, GpioPinDigitalOutput> buttons = [:]
    GpioController gpio

    VotingBoardButtons(GpioController gpio) {
        this.gpio = gpio

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
        illuminateAllButtons(null)
        sleep 1000
        switchOffAllButtonButOne(null)
    }

    def illuminateAllButtons() {
        for (i in 1..5) {
            buttons."button${i}".high()
        }
    }

    /**
     * Entry point for event bus
     * @param message
     */
    def switchOffAllButtonButOne(Message msgIn) {
        for (i in 1..5) {
            if (!msgIn || i != msgIn.body.note) {
                buttons."button${i}".low()
            }
        }
        if (msgIn) {
            sleep 1000
            buttons."button${msgIn.body.note}".low()
        }
    }

    def shutdown() {
        gpio.shutdown();
    }
}
