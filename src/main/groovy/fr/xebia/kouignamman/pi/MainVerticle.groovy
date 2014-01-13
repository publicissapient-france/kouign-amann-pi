package fr.xebia.kouignamman.pi

import fr.xebia.kouignamman.pi.db.PersistenceVerticle
import fr.xebia.kouignamman.pi.hardwareTest.TestLcd
import fr.xebia.kouignamman.pi.hardwareTest.TestLedBackPack
import fr.xebia.kouignamman.pi.vote.FlashLcdPlate
import fr.xebia.kouignamman.pi.vote.VoteVerticle
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger


class MainVerticle extends Verticle {
    Logger logger

    def start() {
        logger = container.logger
        logger.info "Starting"
        logger.info "Initialise singleton only if hardware is active"


        container.deployWorkerVerticle('groovy:' + VoteVerticle.class.name, container.config, 1) { AsyncResult asyncResult ->
            if (asyncResult.succeeded) {
                logger.info "VoteVerticle deployed: ID is ${asyncResult.result}"
                container.deployWorkerVerticle('groovy:' + FlashLcdPlate.class.name, container.config, 1)
            } else {
                logger.error('Error deploying VoteVerticle', asyncResult.cause)
            }
        }

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
