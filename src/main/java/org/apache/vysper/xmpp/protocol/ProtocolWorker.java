/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.protocol;

import org.apache.vysper.xmpp.parser.ParsingException;
import org.apache.vysper.xmpp.parser.StreamParser;
import org.apache.vysper.xmpp.protocol.exception.TLSException;
import org.apache.vysper.xmpp.protocol.worker.AuthenticatedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.EncryptedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.EncryptionStartedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.EndOrClosedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.InitiatedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.StartedProtocolWorker;
import org.apache.vysper.xmpp.protocol.worker.UnconnectedProtocolWorker;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.SessionState;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.writer.DenseStanzaLogRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * responsible for high-level XMPP protocol logic.
 * determines start, end and jabber conditions.
 * reads the stream and cuts it into stanzas,
 * holds state and invokes stanza execution,
 * separates stream reading from actual execution.
 * stateless.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class ProtocolWorker implements StanzaProcessor {

    final Logger logger = LoggerFactory.getLogger(ProtocolWorker.class);

    private final Map<SessionState, StateAwareProtocolWorker> stateWorker = new HashMap<SessionState, StateAwareProtocolWorker>();

    private final ResponseWriter responseWriter = new ResponseWriter();

    public ProtocolWorker() {

        stateWorker.put(SessionState.UNCONNECTED, new UnconnectedProtocolWorker());
        stateWorker.put(SessionState.INITIATED, new InitiatedProtocolWorker());
        stateWorker.put(SessionState.STARTED, new StartedProtocolWorker());
        stateWorker.put(SessionState.ENCRYPTION_STARTED, new EncryptionStartedProtocolWorker());
        stateWorker.put(SessionState.ENCRYPTED, new EncryptedProtocolWorker());
        stateWorker.put(SessionState.AUTHENTICATED, new AuthenticatedProtocolWorker());
        stateWorker.put(SessionState.ENDED, new EndOrClosedProtocolWorker());
        stateWorker.put(SessionState.CLOSED, new EndOrClosedProtocolWorker());
    }

    /**
     * reads next stanza from stream, if the worker is used in a pull szenario (testing).
     * @param sessionContext
     * @param streamParser
     * @return new stanza
     */
    public Stanza aquireStanza(SessionContext sessionContext, StreamParser streamParser) {
        Stanza stanza = null;
        try {
            stanza = streamParser.getNextStanza();
        } catch (ParsingException e) {
            responseWriter.handleParsingException(sessionContext, e);
            return null;
        }

        if (stanza == null) return null; // no next stanza for the moment

        return stanza;
    }

    /**
     * executes the handler for a stanza, handles Protocol exceptions.
     * also writes a response, if the handler implements ResponseStanzaContainer
     * @param serverRuntimeContext
     * @param sessionContext
     * @param stanza
     * @param sessionStateHolder
     */
    public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza, SessionStateHolder sessionStateHolder) {
        if (stanza == null) throw new RuntimeException("cannot process NULL stanzas");

        StanzaHandler stanzaHandler = serverRuntimeContext.getHandler(stanza);

        if (stanzaHandler == null) {
            responseWriter.handleUnsupportedStanzaType(sessionContext, stanza);
            return;
        }
        if (sessionContext == null && stanzaHandler.isSessionRequired()) {
            throw new IllegalStateException("handler requires session context");
        }

        StateAwareProtocolWorker stateAwareProtocolWorker = stateWorker.get(sessionContext.getState());
        if (stateAwareProtocolWorker == null) {
            throw new IllegalStateException("no protocol worker for state " + sessionContext.getState().toString());
        }

        // check as of RFC3920/4.3:
        if (sessionStateHolder.getState() != SessionState.AUTHENTICATED)
        {
            // is not authenticated...
            if (XMPPCoreStanza.getWrapper(stanza) != null)
            {
                // ... and is a IQ/PRESENCE/MESSAGE stanza!
                responseWriter.handleNotAuthorized(sessionContext, stanza);
                return;
            }
        }

        // make sure that 'from' (if present) matches the bare authorized entity
        // else repond with a stanza error 'unknown-sender'
        // see rfc3920_draft-saintandre-rfc3920bis-04.txt#8.5.4
        Entity from = stanza.getFrom();
        if (from != null && sessionContext.getInitiatingEntity() != null) {
            Entity fromBare = from.getBareJID();
            Entity initiatingEntity = sessionContext.getInitiatingEntity();
            if (!initiatingEntity.equals(fromBare)) {
                responseWriter.handleWrongFromJID(sessionContext, stanza);
                return;
            }
        }
        // make sure that there is a bound resource entry for that from's resource id attribute!
        if (from != null && from.getResource() != null) {
            List<String> boundResources = sessionContext.getServerRuntimeContext().getResourceRegistry().getBoundResources(from, false);
            if (boundResources.size() == 0) {
                responseWriter.handleWrongFromJID(sessionContext, stanza);
                return;
            }
        }
        // make sure that there is a full from entity given in cases where more than one resource is bound
        // in the same session.
        // see rfc3920_draft-saintandre-rfc3920bis-04.txt#8.5.4
        if (from != null && from.getResource() == null) {
            List<String> boundResources = sessionContext.getServerRuntimeContext().getResourceRegistry().getResourcesForSession(sessionContext);
            if (boundResources.size() > 1) {
                responseWriter.handleWrongFromJID(sessionContext, stanza);
                return;
            }
        }

        try {
            stateAwareProtocolWorker.processStanza(sessionContext, sessionStateHolder,
                                                   stanza, stanzaHandler);
        } catch (Exception e) {
            logger.error("error executing handler {} with stanza {}", stanzaHandler.getClass().getName(), DenseStanzaLogRenderer.render(stanza));
            e.printStackTrace();
        }
    }

    public void processTLSEstablished(SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        processTLSEstablishedInternal(sessionContext, sessionStateHolder, responseWriter);
    }

    static void processTLSEstablishedInternal(SessionContext sessionContext, SessionStateHolder sessionStateHolder, ResponseWriter responseWriter) {
        if (sessionContext.getState() != SessionState.ENCRYPTION_STARTED) {
            responseWriter.handleProtocolError(new TLSException(), sessionContext, null);
            return;
        }
        sessionStateHolder.setState(SessionState.ENCRYPTED);
        sessionContext.setIsReopeningXMLStream();
    }
}
