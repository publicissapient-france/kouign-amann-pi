package fr.xebia.kouignamann.pi.draft

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
        logger.info "Main -> starting"
        container.deployWorkerVerticle('groovy:' + NfcVerticle.class.name, container.config, 1) { asyncResultNfc ->
            if (asyncResultNfc.succeeded) {
                container.deployWorkerVerticle('groovy:' + VoteVerticle.class.name, container.config, 1) { asyncResultVote ->
                    if (asyncResultVote.succeeded) {
                        container.deployWorkerVerticle('groovy:' + DataManagementVerticle.class.name, container.config, 1) { asyncResultData ->
                            if (asyncResultData.succeeded) {
                                container.deployWorkerVerticle('groovy:' + PersistenceVerticle.class.name, container.config, 1) { asyncResultDB ->
                                    if (asyncResultData.succeeded) {
                                        Map msg = [status: "Init"]
                                        logger.info("Bus -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.reinitialiseLcd")
                                        vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.reinitialiseLcd", msg) {
                                            logger.info "****** Ready to process ******"
                                        }
                                    } else {
                                        asyncResultData.cause.printStackTrace()
                                    }
                                }
                            } else {
                                asyncResultData.cause.printStackTrace()
                            }
                        }
                    } else {
                        asyncResultVote.cause.printStackTrace()
                    }
                }
            } else {
                asyncResultNfc.cause.printStackTrace()
            }
        }


        if (container.config.testLcd) {
            container.deployWorkerVerticle('groovy:' + TestLcd.class.name, container.config, 1)
        }
        if (container.config.testLedBackPack) {
            container.deployWorkerVerticle('groovy:' + TestLedBackPack.class.name, container.config, 1)
        }
    }
}
