package fr.xebia.kouignamann.pi

import fr.xebia.kouignamann.pi.db.PersistenceVerticle
import fr.xebia.kouignamann.pi.hardwareTest.TestLcd
import fr.xebia.kouignamann.pi.hardwareTest.TestLedBackPack
import fr.xebia.kouignamann.pi.vote.DataManagementVerticle
import fr.xebia.kouignamann.pi.vote.NfcVerticle
import fr.xebia.kouignamann.pi.vote.VoteVerticle
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.logging.Logger

class MainVerticle extends Verticle {
    Logger logger

    def start() {
        logger = container.logger
        logger.info "Starting"
        container.deployWorkerVerticle('groovy:' + NfcVerticle.class.name, container.config, 1) { asyncResult ->
            if (asyncResult.succeeded) {
                container.deployWorkerVerticle('groovy:' + VoteVerticle.class.name, container.config, 1)
            } else {
                asyncResult.cause.printStackTrace()
            }
        }
        container.deployWorkerVerticle('groovy:' + DataManagementVerticle.class.name, container.config, 1)
        // Local persistence
        container.deployWorkerVerticle('groovy:' + PersistenceVerticle.class.name, container.config, 1)

        if (container.config.testLcd) {
            container.deployWorkerVerticle('groovy:' + TestLcd.class.name, container.config, 1)
        }
        if (container.config.testLedBackPack) {
            container.deployWorkerVerticle('groovy:' + TestLedBackPack.class.name, container.config, 1)
        }


    }
}
