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
 * ========================================================================
 * Module Information:
 *
 * DESCRIPTION: This enumerated type is used to predefine the list of
 * allowed socket actions.
 * 
 * This package private class provides RDMA_CAPABILITY.
 * ========================================================================
 */

package java.net;

enum SocketAction {
	BIND,
	ACCEPT,
	CONNECT;
	
	/**
	 * Convert an action string into a predefined action constant.
	 */
	static SocketAction parse(String actionStr){
		SocketAction action = null;
		for (SocketAction a: SocketAction.values()) {
			if (actionStr.equalsIgnoreCase(a.name())) {
				action = a;
				break;
			}
		}
		return action;
	}

	// convert this action to a string
	public static String toString(SocketAction action) {
		if (action == SocketAction.BIND) {
			return new String("bind");
		} else if (action == SocketAction.ACCEPT) {
			return new String("accept");
		} else if (action == SocketAction.CONNECT) {
			return new String("connect");
		} else {
			return null;
		}
	}
}
