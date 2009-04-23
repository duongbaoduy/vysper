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
package org.apache.vysper.xmpp.server;

import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.jcr.JcrStorage;
import org.apache.vysper.storage.jcr.user.JcrUserManagement;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityFormatException;

import java.io.File;

/**
 * starts the server as a standalone application
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Revision$ , $Date: 2009-04-21 13:13:19 +0530 (Tue, 21 Apr 2009) $
 */
public class ServerMain {

    /**
     * boots the server as a standalone application
     * found on the classpath
     * @param args
     */
    public static void main(String[] args) throws AccountCreationException, EntityFormatException {

        final JcrStorage jcrStorage = JcrStorage.getInstance();
        final JcrUserManagement userManagement = new JcrUserManagement(jcrStorage);

        //SimpleUserAuthorization userManagement = new SimpleUserAuthorization();

        if(!userManagement.verifyAccountExists(EntityImpl.parse("user1@vysper.org"))) userManagement.addUser("user1@vysper.org", "password1");
        if(!userManagement.verifyAccountExists(EntityImpl.parse("user2@vysper.org"))) userManagement.addUser("user2@vysper.org", "password1");
        if(!userManagement.verifyAccountExists(EntityImpl.parse("user3@vysper.org"))) userManagement.addUser("user3@vysper.org", "password1");

        XMPPServer server = new XMPPServer("vysper.org");
        server.addEndpoint(new TCPEndpoint());
        //server.addEndpoint(new StanzaSessionFactory());
        server.setUserAuthorization(userManagement);
        server.setAccountVerification(userManagement);

        server.setTLSCertificateInfo(new File("src/main/config/bogus_mina_tls.cert"), "boguspw");

        try {
            server.start();
            System.out.println("vysper server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }

        server.addModule(new SoftwareVersionModule());
        server.addModule(new EntityTimeModule());
        server.addModule(new VcardTempModule());
    }
}
