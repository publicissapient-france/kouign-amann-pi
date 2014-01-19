package fr.xebia.kouignamann.pi.draft

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

/**
 * Should be deployed as a WorkerVerticle
 */
class DataVerticle extends Verticle {

    def start() {

    }

    private void initDb() {

    }

    def store(Message message) {
        // stores a vote
    }

    def delete(Message message) {

    }

    def findNameByNfcId(Message message) {

    }

    /**
     * Should be launched by timer
     */
    def backupAndFlush() {

    }

    def stop() {

    }
}
