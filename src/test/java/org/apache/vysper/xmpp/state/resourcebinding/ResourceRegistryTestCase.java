/***********************************************************************
 * Copyright (c) 2006-2007 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.vysper.xmpp.state.resourcebinding;

import junit.framework.TestCase;
import org.apache.vysper.xmpp.server.TestSessionContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.addressing.EntityFormatException;

import java.util.List;

/**
 */
public class ResourceRegistryTestCase extends TestCase {
    
    protected ResourceRegistry resourceRegistry = new ResourceRegistry();

    public void testSessionNotWellDefinedForResourceBinding() {
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(null);
        assertNull(sessionContext.getInitiatingEntity());
        try {
            resourceRegistry.bindSession(sessionContext);
            fail("do not accept sessions with no initiating entity");
        } catch (IllegalStateException _) {
            // test succeeded
        }
    }
    
    public void testAddSession() throws EntityFormatException {
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(EntityImpl.parse("me@test"));
        String resourceId = resourceRegistry.bindSession(sessionContext);
        assertNotNull(resourceId);
        List<String> resourceList = resourceRegistry.getResourcesForSessionInternal(sessionContext);
        assertEquals(1, resourceList.size());
        assertTrue(resourceList.contains(resourceId));
    }

    public void testAddMultipleSession() throws EntityFormatException {
        TestSessionContext sessionContext1 = TestSessionContext.createSessionContext(EntityImpl.parse("me1@test"));
        String resourceId1 = resourceRegistry.bindSession(sessionContext1);
        TestSessionContext sessionContext2 = TestSessionContext.createSessionContext(EntityImpl.parse("me2@test"));
        String resourceId2 = resourceRegistry.bindSession(sessionContext2);
        assertNotNull(resourceId2);
        List<String> resourceList = resourceRegistry.getResourcesForSessionInternal(sessionContext1);
        assertEquals(1, resourceList.size());
        assertTrue(resourceList.contains(resourceId1));
        resourceList = resourceRegistry.getResourcesForSessionInternal(sessionContext2);
        assertEquals(1, resourceList.size());
        assertTrue(resourceList.contains(resourceId2));
        
        assertEquals(resourceRegistry.getSessionContext(resourceId1), sessionContext1);
        assertEquals(resourceRegistry.getSessionContext(resourceId2), sessionContext2);
    }
    
    public void testAddOneEntityMultipleResources() throws EntityFormatException {
        EntityImpl entity = EntityImpl.parse("me@test");
        
        TestSessionContext sessionContext1 = TestSessionContext.createSessionContext(entity);
        String resourceId1 = resourceRegistry.bindSession(sessionContext1);
        
        TestSessionContext sessionContext2 = TestSessionContext.createSessionContext(entity);
        String resourceId2 = resourceRegistry.bindSession(sessionContext2);
        
        assertNotNull(resourceId1);
        assertNotNull(resourceId2);
        
        List<String> resourceList = resourceRegistry.getBoundResources(entity);
        assertEquals(2, resourceList.size());
        assertTrue(resourceList.contains(resourceId1));
        assertTrue(resourceList.contains(resourceId2));

        List<SessionContext> sessionList = resourceRegistry.getSessions(entity);
        assertEquals(2, resourceList.size());
        assertTrue(sessionList.contains(sessionContext1));
        assertTrue(sessionList.contains(sessionContext2));
    }

    public void testAddOneEntityMultipleResources_TolerateResourceIds() throws EntityFormatException {
        EntityImpl entity = EntityImpl.parse("me@test");
        
        TestSessionContext sessionContext1 = TestSessionContext.createSessionContext(EntityImpl.parse("me@test/xy"));
        String resourceId1 = resourceRegistry.bindSession(sessionContext1);
        
        TestSessionContext sessionContext2 = TestSessionContext.createSessionContext(EntityImpl.parse("me@test/ab"));
        String resourceId2 = resourceRegistry.bindSession(sessionContext2);
        
        assertNotNull(resourceId1);
        assertNotNull(resourceId2);
        
        List<String> resourceList = resourceRegistry.getBoundResources(entity);
        assertEquals(2, resourceList.size());
        assertTrue(resourceList.contains(resourceId1));
        assertTrue(resourceList.contains(resourceId2));

        List<SessionContext> sessionList = resourceRegistry.getSessions(entity);
        assertEquals(2, resourceList.size());
        assertTrue(sessionList.contains(sessionContext1));
        assertTrue(sessionList.contains(sessionContext2));
    }

    public void testSameEntityMultipleResources() throws EntityFormatException {
        EntityImpl entity = EntityImpl.parse("me@test");
        
        TestSessionContext sessionContext1 = TestSessionContext.createSessionContext(entity);
        String resourceId1 = resourceRegistry.bindSession(sessionContext1);
        
        TestSessionContext sessionContext2 = TestSessionContext.createSessionContext(entity);
        String resourceId2 = resourceRegistry.bindSession(sessionContext2);
        
        // resource ids are different
        assertFalse(resourceId1.equals(resourceId2));
    }
    
    public void testUnbindResourceSimple() throws EntityFormatException {
        EntityImpl entity = EntityImpl.parse("me@test");
        
        TestSessionContext sessionContext1 = TestSessionContext.createSessionContext(entity);
        String resourceId1 = resourceRegistry.bindSession(sessionContext1);
        assertEquals(sessionContext1, resourceRegistry.getSessionContext(resourceId1));
        
        boolean noResourceRemains = resourceRegistry.unbindResource(resourceId1);

        assertTrue(noResourceRemains);
        assertNull(resourceRegistry.getSessionContext(resourceId1));
        assertEquals(0, resourceRegistry.getBoundResources(entity).size());
    }
    
    public void testUnbindSessionSimple() throws EntityFormatException {
        EntityImpl entity = EntityImpl.parse("me@test");
        
        TestSessionContext sessionContext1 = TestSessionContext.createSessionContext(entity);
        String resourceId1 = resourceRegistry.bindSession(sessionContext1);
        assertEquals(sessionContext1, resourceRegistry.getSessionContext(resourceId1));
        
        resourceRegistry.unbindSession(sessionContext1);
        
        assertNull(resourceRegistry.getSessionContext(resourceId1));
        assertEquals(0, resourceRegistry.getBoundResources(entity).size());
    }

    public void testUniqueResourceIsConsistent() throws EntityFormatException {
        EntityImpl entity = EntityImpl.parse("me@test");
        TestSessionContext sessionContext = TestSessionContext.createSessionContext(entity);
        String resourceId1 = resourceRegistry.bindSession(sessionContext);

        String first1 = resourceRegistry.getUniqueResourceForSession(sessionContext);
        assertEquals(resourceId1, first1);
        
        String resourceId2 = resourceRegistry.bindSession(sessionContext);
        assertFalse("resource ids actually differ", resourceId1.equals(resourceId2));
        assertNull("resource id no longer unique", resourceRegistry.getUniqueResourceForSession(sessionContext));
    } 
}
