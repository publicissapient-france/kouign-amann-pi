package fr.xebia.kouignamann.pi.db

import com.sleepycat.je.DatabaseException
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig
import com.sleepycat.persist.EntityStore
import com.sleepycat.persist.StoreConfig
import fr.xebia.kouignamann.pi.util.WrapperEventBus
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class PersistenceVerticle extends Verticle {
    static File envHome = new File("/tmp")

    private Environment devEnv
    private EntityStore store

    def voterIdx
    def voteIdx

    def logger

    def start() {
        boolean readOnly = false
        logger = container.logger
        logger.info "Start -> Initializing DB"

        [
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.getNameFromNfcId": this.&getNameFromNfcId,
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.storeVote": this.&storeVote,
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processStoredVotes": this.&processStoredVotes,
                "fr.xebia.kouignamann.pi.${container.config.hardwareUid}.deleteVoteFromLocal": this.&deleteVoteFromLocal,
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        try {
            EnvironmentConfig myEnvConfig = new EnvironmentConfig()
            StoreConfig storeConfig = new StoreConfig()

            myEnvConfig.setAllowCreate(!readOnly)
            storeConfig.setAllowCreate(!readOnly)

            // Open the environment and entity store
            devEnv = new Environment(envHome, myEnvConfig)
            store = new EntityStore(devEnv, "EntityStore", storeConfig)

            // Create the index
            voterIdx = store.getPrimaryIndex(String.class, Voter.class);
            voteIdx = store.getPrimaryIndex(Long.class, Vote.class);

            // Load Test data
            Voter voter = new Voter()
            voter.name = "Pablo Lopez"
            voter.nfcId = "1D A8 7E ED"
            voterIdx.put(voter)

            voter = new Voter()
            voter.name = "Merle Moqueur"
            voter.nfcId = "00 00 00 00"
            voterIdx.put(voter)

        } catch (DatabaseException dbe) {
            logger.error "Start -> Error opening environment and store: ${dbe.toString()}"
        }
        logger.info "Start -> Done initializing DB"
    }

    def getNameFromNfcId(Message message) {
        logger.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.getNameFromNfcId ${message}")
        logger.info("Process -> Retrieving '${message.body.nfcId}'")
        def voter = voterIdx.get(message.body.nfcId)

        def name = "Anonyme"
        if (voter) {
            name = voter.name
        }
        message.reply([
                name: name
        ])
    }

    def storeVote(Message message) {
        logger.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.storeVote ${message}")
        Vote vote = new Vote()
        vote.nfcId = message.body.nfcId
        vote.voteTime = message.body.voteTime
        vote.note = message.body.note

        voteIdx.put(vote)
        logger.info("Process -> Storing '${vote}'")

        message.reply([
                status: "OK"
        ])
    }

    def processStoredVotes(Message message) {
        logger.info("Bus <- fr.xebia.kouignamann.pi.${container.config.hardwareUid}.processStoredVotes ${message}")
        def cursor = voteIdx.entities()
        for (Vote vote : cursor) {
            Map outgoingMessage = [
                    "nfcId": vote.nfcId,
                    "voteTime": vote.voteTime,
                    "note": vote.note,
                    "hardwareUid": container.config.hardwareUid
            ]
            // End point must exists ?
            // TODO Need further testing
            // Send to processor
            logger.info("Bus -> ${message.body.nextProcessor} ${message}")
            def eventBus = vertx.eventBus
            def wrapperBus = new WrapperEventBus(eventBus.javaEventBus())
            wrapperBus.sendWithTimeout(message.body.nextProcessor, outgoingMessage, 1000) { result ->
                if (result.succeeded) {
                    logger.info("Process -> ${outgoingMessage} successfully processed by central")
                    deleteVoteFromLocal(vote.voteUid)
                } else {
                    logger.info("Process -> TIMEOUT from central - Do nothing")
                }

            }
        }
        cursor.close()

    }

    def deleteVoteFromLocal(long voteUid) {
        logger.info "Process -> Remove from local base vote ${voteUid}"
        voteIdx.delete(voteUid)
    }

    def stop() {
        if (store) {
            try {
                store.close();
            } catch (DatabaseException dbe) {
                logger.error "Stop -> Error closing store: ${dbe.toString()}"
            }
        }

        if (devEnv) {
            try {
                // Finally, close environment.
                devEnv.close();
            } catch (DatabaseException dbe) {
                logger.error "Stop -> Error closing MyDbEnv: ${dbe.toString()} "
            }
        }
    }
}
