package fr.xebia.kouignamann.pi.vote

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.eventbus.Message

class LedButtonsVerticle extends Verticle {
    private GpioController gpio
    def buttons = [:]
    def logger


    def start() {
        // create gpio controller
        gpio = GpioFactory.getInstance()

        // provision gpio pin #01 as an output pin and turn on
        buttons.put("button1", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Button1", PinState.LOW))
        buttons.put("button2", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Button2", PinState.LOW))
        buttons.put("button3", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Button3", PinState.LOW))
        buttons.put("button4", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Button4", PinState.LOW))
        buttons.put("button5", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Button5", PinState.LOW))

        logger.info "Start -> Initialize handler";
        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.illuminateAllButtons": this.&illuminateAllButtons,
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffAllButtonButOne": this.&switchOffAllButtonButOne,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
        logger.info "Start -> Done initialize handler";
    }

    def illuminateAllButtons(Message msgIn) {
        for (i in 1..5) {
            buttons."button${i}".high()
        }
    }

    def switchOffAllButtonButOne(Message msgIn) {
        for (i in 1..5) {
            if (i != msgIn.body.note) {
                buttons."button${i}".low()
            }
        }
        sleep 1000
        buttons."button${msgIn.body.note}".low()
    }

    def stop() {
        gpio.shutdown();
    }
}
