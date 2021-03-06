package com.tinkerpop.rexster.protocol.message;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class SessionRequestMessageTest {
    @Test
    public void constructEmptyConstructorEnsureFormat() {
        RexProMessage msg = new SessionRequestMessage(SessionRequestMessage.FLAG_NEW_SESSION, (byte) 1);

        Assert.assertEquals(RexProMessage.EMPTY_SESSION, msg.getSessionAsUUID());

        // the session is empty for a session request
        Assert.assertFalse(msg.hasSession());
        Assert.assertEquals(SessionRequestMessage.FLAG_NEW_SESSION, msg.getFlag());
        Assert.assertEquals(5, msg.getBodyLength());
        Assert.assertEquals(MessageType.SESSION_REQUEST, msg.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructCopyRexProMessageWrongType() {
        RexProMessage msgToConvert = new SessionRequestMessage(SessionRequestMessage.FLAG_NEW_SESSION, (byte) 1);
        msgToConvert.setType(MessageType.SESSION_RESPONSE);

        new com.tinkerpop.rexster.protocol.message.SessionRequestMessage(msgToConvert);
    }

    @Test
    public void constructCopyRexProMessage() {
        RexProMessage msgToConvert = new SessionRequestMessage(SessionRequestMessage.FLAG_NEW_SESSION, (byte) 1);
        SessionRequestMessage convertedMsg = new SessionRequestMessage(msgToConvert);

        Assert.assertNotNull(convertedMsg);
        Assert.assertTrue(Arrays.equals(msgToConvert.getSession(), convertedMsg.getSession()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getHeaderIdentification(), convertedMsg.getHeaderIdentification()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getRequest(), convertedMsg.getRequest()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getBody(), convertedMsg.getBody()));
        Assert.assertEquals(msgToConvert.getBodyLength(), convertedMsg.getBodyLength());
        Assert.assertEquals(msgToConvert.getFlag(), convertedMsg.getFlag());
        Assert.assertEquals(msgToConvert.getType(), convertedMsg.getType());
        Assert.assertEquals(msgToConvert.getVersion(), convertedMsg.getVersion());
        Assert.assertEquals((byte) 1, convertedMsg.getChannel());
        Assert.assertEquals(SessionRequestMessage.DEFAULT_CHUNK_SIZE, convertedMsg.getChunkSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructAKillRequestWithBodyShouldFail() {
        new SessionRequestMessage(SessionRequestMessage.FLAG_KILL_SESSION, (byte) 1);
    }

}
