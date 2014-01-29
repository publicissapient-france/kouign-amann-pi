package fr.xebia.kouignamann.pi.vote

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class LedButtonsVerticle extends Verticle {
    private GpioController gpio
    def buttons = [:]
    def logger


    def start() {
        logger = container.logger
        // create gpio controller
        gpio = GpioFactory.getInstance()

        // provision gpio pin #01 as an output pin and turn on
        // GPIO # 27
        buttons.put("button1", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Button1", PinState.LOW))
        // GPIO # 22
        buttons.put("button2", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Button2", PinState.LOW))
        // GPIO # 23
        buttons.put("button3", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Button3", PinState.LOW))
        // GPIO # 24
        buttons.put("button4", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Button4", PinState.LOW))
        // GPIO # 25
        buttons.put("button5", gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Button5", PinState.LOW))

        logger.info "Start -> Initialize handler";
        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.lightOnAll": this.&illuminateAllButtons,
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.switchOffAllButtonButOne": this.&switchOffAllButtonButOne,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
        logger.info "Start -> Done initialize handler";

        illuminateAllButtons(null)
        sleep 1000
        switchOffAllButtonButOne(null)
    }

    def illuminateAllButtons(Message msgIn) {
        for (i in 1..5) {
            buttons."button${i}".high()
        }
    }

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

    def stop() {
        gpio.shutdown();
    }
}
