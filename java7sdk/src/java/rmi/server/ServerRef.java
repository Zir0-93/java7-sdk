/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v7
 * (C) Copyright IBM Corp. 2014, 2014. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1996, 1999, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.rmi.server;

import java.rmi.*;

/**
 * A ServerRef represents the server-side handle for a remote object
 * implementation.
 *
 * @author  Ann Wollrath
 * @since   JDK1.1
 */
public interface ServerRef extends RemoteRef {

    /** indicate compatibility with JDK 1.1.x version of class. */
    static final long serialVersionUID = -4557750989390278438L;

    /**
     * Creates a client stub object for the supplied Remote object.
     * If the call completes successfully, the remote object should
     * be able to accept incoming calls from clients.
     * @param obj the remote object implementation
     * @param data information necessary to export the object
     * @return the stub for the remote object
     * @exception RemoteException if an exception occurs attempting
     * to export the object (e.g., stub class could not be found)
     * @since JDK1.1
     */
    RemoteStub exportObject(Remote obj, Object data)
        throws RemoteException;

    /**
     * Returns the hostname of the current client.  When called from a
     * thread actively handling a remote method invocation the
     * hostname of the client is returned.
     * @return the client's host name
     * @exception ServerNotActiveException if called outside of servicing
     * a remote method invocation
     * @since JDK1.1
     */
    String getClientHost() throws ServerNotActiveException;
}
