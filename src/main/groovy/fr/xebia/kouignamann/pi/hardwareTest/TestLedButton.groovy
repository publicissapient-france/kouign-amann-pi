package fr.xebia.kouignamann.pi.hardwareTest

import com.pi4j.io.gpio.*
import org.vertx.groovy.platform.Verticle

/**
 * Created with IntelliJ IDEA.
 * User: pablolopez
 * Date: 25/01/14
 * Time: 23:29
 * To change this template use File | Settings | File Templates.
 */
class TestLedButton extends Verticle {

    def start() {
        println "<--Pi4J--> GPIO Control Example ... started. -> GPIO#24"

        // create gpio controller
        GpioController gpio = GpioFactory.getInstance();

        // provision gpio pin #01 as an output pin and turn on
        GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "MyLED", PinState.HIGH)
        println "--> GPIO state should be: ON"

        sleep 2000

        // turn off gpio pin #01
        pin.low();
        println "--> GPIO state should be: OFF"

        sleep 2000

        // toggle the current state of gpio pin #01 (should turn on)
        pin.toggle();
        println "--> GPIO state should be: ON"

        sleep 2000

        // toggle the current state of gpio pin #01  (should turn off)
        pin.toggle();
        println "--> GPIO state should be: OFF"

        sleep 2000

        // turn on gpio pin #01 for 1 second and then off
        println "--> GPIO state should be: ON for only 1 second"
        pin.pulse(1000, true); // set second argument to 'true' use a blocking call

        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
        gpio.shutdown();
    }
}
