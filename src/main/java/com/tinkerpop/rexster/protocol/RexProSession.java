package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.ScriptRequestMessage;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Server-side rexster session.
 */
public class RexProSession {

    private final Bindings bindings = new SimpleBindings();

    private final UUID sessionKey;

    private final byte channel;

    private final int chunkSize;

    protected Date lastTimeUsed = new Date();

    public RexProSession(final UUID sessionKey, final RexsterApplication rexsterApplication, final byte channel, final int chunkSize) {
        this.sessionKey = sessionKey;
        this.channel = channel;
        this.chunkSize = chunkSize;

        this.bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, rexsterApplication);
    }

    public UUID getSessionKey() {
        return this.sessionKey;
    }

    public Bindings getBindings() {
        return this.bindings;
    }

    public byte getChannel() {
        return this.channel;
    }

    public long getIdleTime() {
        return (new Date()).getTime() - this.lastTimeUsed.getTime();
    }

    public Object evaluate(String script, String languageName, RexsterBindings rexsterBindings) throws ScriptException {
        EngineController controller = EngineController.getInstance();

        Object result = null;
        try {
            EngineHolder engine = controller.getEngineByLanguageName(languageName);

            if (rexsterBindings != null) {
                this.bindings.putAll(rexsterBindings);
            }

            result = engine.getEngine().eval(script, this.bindings);
        } catch (ScriptException se) {
            throw se;
        } finally {
            this.lastTimeUsed = new Date();
        }

        return result;
    }
}
