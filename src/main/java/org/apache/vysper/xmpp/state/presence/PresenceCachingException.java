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

package org.apache.vysper.xmpp.state.presence;

/**
 * thrown when a caching a presence fails
 */
public class PresenceCachingException extends RuntimeException {
    public PresenceCachingException() {
        super();
    }

    public PresenceCachingException(String string) {
        super(string);
    }

    public PresenceCachingException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public PresenceCachingException(Throwable throwable) {
        super(throwable);
    }
}
