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
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * =======================================================================
 * Module Information:
 *
 * DESCRIPTION: This interface is used to define new networks that will be
 * supported for java.net.Socket.
 * 
 * This package private interface provides RDMA_CAPABILITY.
 * =======================================================================
 */

package java.net;

import java.io.IOException;
import java.util.List;

interface NetworkProvider {
	/**
	 * Initialize the underlying network.
	 * This method should perform all the steps required to initialize
	 * the components specific to this network.
	 * Steps may include -
	 *   loading the underlying system libraries
	 *   checking whether the network interface hardware is enabled
	 * Successful completion of this method indicates that
	 * the network is ready to be used.
	 * 
	 * @throws IOException to indicate failure during initialization.
	 */
	void initialize() throws IOException;
	
	/**
	 * This method frees resources that are allocated during
	 * the network initialization.
	 */
	void cleanup();
	
	/**
	 * Create a socket impl of the type supported by a specific network
	 * provider implementing this interface.
	 */
	SocketImpl createImpl(SocketImpl oldImpl);
	
	/**
	 * Set flags/fields after an accept() call.
	 * 
	 * @throws IOException to indicate failure in setting flags/fields.
	 */
	void postAccept(SocketImpl serverSocket, SocketImpl socket) throws IOException;

        /**
         * Get the name of the provider
         */
        String getName();

        /**
         * Set prefered address for RDMA transfer
         */
        void setPreferredAddress(List<String> addrList, List<String> addrList2);

        /**
         * Get prefered addresses for RDMA transfer. 
         * null if there is no preference
         */
        InetAddress getPreferredAddress();
}
