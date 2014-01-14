package fr.xebia.kouignamman.pi.db

import com.sleepycat.je.DatabaseException
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig
import com.sleepycat.persist.EntityStore
import com.sleepycat.persist.StoreConfig
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
        logger.info "Initializing DB"

        [
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.getNameFromNfcId": this.&getNameFromNfcId,
                "fr.xebia.kouignamman.pi.${container.config.hardwareUid}.storeVote": this.&storeVote,
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
            logger.info "Retrieve entity ${voterIdx.get(voter.nfcId)}"

        } catch (DatabaseException dbe) {
            logger.error "Error opening environment and store: ${dbe.toString()}"
        }
        logger.info "Done initializing DB"
    }

    def getNameFromNfcId(Message message) {
        logger.info("Retrieving '${message.body.nfcId}'")
        def voter = voterIdx.get(message.body.nfcId)
        message.reply([
                name: voter.name
        ])
    }

    def storeVote(Message message) {
        Vote vote = new Vote()
        vote.nfcId = message.body.nfcId
        vote.voteTime = message.body.voteTime
        vote.note = message.body.note

        voteIdx.put(vote)
        logger.info("Storing '${vote}'")
    }

    def processStoredVotes(Message message) {
        // TODO test
        def cursor = voterIdx.entities()
        for (Vote vote : cursor) {
            Map outgoingMessage = [
                    "nfcId": vote.nfcId,
                    "voteTime": vote.voteTime,
                    "note": vote.note,
                    "hardwareUid": container.config.hardwareUid
            ]
            // Send to processor
            // TODO timeout send does not exist ?
            /*vertx.eventBus.send(message.body.nextProcessor, outgoingMessage){response ->
                // Success : delete entity
                voterIdx.delete(vote.voteUid)
                // Failure : break
            }*/
        }
        cursor.close()

    }

    def stop() {
        if (store) {
            try {
                store.close();
            } catch (DatabaseException dbe) {
                logger.error "Error closing store: ${dbe.toString()}"
            }
        }

        if (devEnv) {
            try {
                // Finally, close environment.
                devEnv.close();
            } catch (DatabaseException dbe) {
                logger.error "Error closing MyDbEnv: ${dbe.toString()} "
            }
        }
    }
}
