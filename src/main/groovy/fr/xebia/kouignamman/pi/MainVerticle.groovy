package fr.xebia.kouignamman.pi

import fr.xebia.kouignamman.pi.adafruit.lcd.AdafruitLcdPlate
import fr.xebia.kouignamman.pi.db.PersistenceVerticle
import fr.xebia.kouignamman.pi.hardwareTest.TestLcd
import fr.xebia.kouignamman.pi.hardwareTest.TestLedBackPack
import fr.xebia.kouignamman.pi.vote.FlashLcdPlate
import fr.xebia.kouignamman.pi.vote.VoteVerticle
import org.vertx.groovy.platform.Verticle


class MainVerticle extends Verticle {
    def logger

    def start() {
        logger = container.logger
        logger.info "Starting"
        logger.info "Initialise singleton only if hardware is active"
        container.deployWorkerVerticle('groovy:' + VoteVerticle.class.name, container.config, 1)
        container.deployWorkerVerticle('groovy:' + FlashLcdPlate.class.name, container.config, 1)
        if (container.config.testLcd) {
            container.deployWorkerVerticle('groovy:' + TestLcd.class.name, container.config, 1)
        }
        if (container.config.testLedBackPack) {
            container.deployWorkerVerticle('groovy:' + TestLedBackPack.class.name, container.config, 1)
        }
        // Local persistence
        container.deployWorkerVerticle('groovy:' + PersistenceVerticle.class.name, container.config, 1)

    }


}
