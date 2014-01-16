package fr.xebia.kouignamann.pi.util

import groovy.transform.CompileStatic
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.EventBus as JEventBus
import org.vertx.java.core.json.JsonObject

@CompileStatic
class WrapperEventBus {

    private final JEventBus jEventBus

    public WrapperEventBus(JEventBus jEventBus) {
        this.jEventBus = jEventBus
    }

    WrapperEventBus sendWithTimeout(String address, def message, long timeout, Closure replyHandler = null) {
        if (message != null) {
            jEventBus.sendWithTimeout(address, convertMessage(message), timeout, wrapHandler(replyHandler))
        } else {
            // Just choose an overloaded method...
            jEventBus.sendWithTimeout(address, (String) null, timeout, wrapHandler(replyHandler))
        }
        this
    }


    public static convertMessage(message) {
        if (message instanceof Map) {
            message = new JsonObject(message)
        } else if (message instanceof Buffer) {
            message = ((Buffer) message).toJavaBuffer()
        }
        message
    }

    public static Handler wrapHandler(Closure handler) {
        if (handler != null) {
            return { handler(new AsyncResult(it as org.vertx.java.core.AsyncResult)) } as Handler
        } else {
            return null
        }
    }
}
