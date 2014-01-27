package fr.xebia.kouignamann.pi

import fr.xebia.kouignamann.pi.db.PersistenceVerticle
import fr.xebia.kouignamann.pi.hardwareTest.TestLcd
import fr.xebia.kouignamann.pi.hardwareTest.TestLedBackPack
import fr.xebia.kouignamann.pi.hardwareTest.TestLedButton
import fr.xebia.kouignamann.pi.mqtt.MqttDataManagementVerticle
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

        def centralCommProtocol = DataManagementVerticle.class.name
        if (container.config.useMqtt) {
            centralCommProtocol = MqttDataManagementVerticle.class.name
        }

        container.deployWorkerVerticle('groovy:' + NfcVerticle.class.name, container.config, 1) { asyncResultNfc ->
            if (asyncResultNfc.succeeded) {
                container.deployWorkerVerticle('groovy:' + VoteVerticle.class.name, container.config, 1) { asyncResultVote ->
                    if (asyncResultVote.succeeded) {
                        container.deployWorkerVerticle('groovy:' + centralCommProtocol, container.config, 1) { asyncResultData ->
                            if (asyncResultData.succeeded) {
                                container.deployWorkerVerticle('groovy:' + PersistenceVerticle.class.name, container.config, 1) { asyncResultDB ->
                                    if (asyncResultData.succeeded) {
                                        Map msg = [status: "Init"]
                                        logger.info("Bus -> fr.xebia.kouignamann.pi.${container.config.hardwareUid}.reinitialiseLcd")
                                        vertx.eventBus.send("fr.xebia.kouignamann.pi.${container.config.hardwareUid}.reinitialiseLcd", msg) {
                                            logger.info "****** Ready to process ******"
                                        }
                                    } else {
                                        logger.error asyncResultDB.throwable
                                    }
                                }
                            } else {
                                logger.error asyncResultData.throwable
                            }
                        }
                    } else {
                        logger.error asyncResultVote.throwable
                    }
                }
            } else {
                logger.error asyncResultNfc.throwable
            }
        }


        if (container.config.testLcd) {
            container.deployWorkerVerticle('groovy:' + TestLcd.class.name, container.config, 1)
        }
        if (container.config.testLedBackPack) {
            container.deployWorkerVerticle('groovy:' + TestLedBackPack.class.name, container.config, 1)
        }
        if (container.config.testLedButton) {
            container.deployWorkerVerticle('groovy:' + TestLedButton.class.name, container.config, 1)
        }
    }
}
