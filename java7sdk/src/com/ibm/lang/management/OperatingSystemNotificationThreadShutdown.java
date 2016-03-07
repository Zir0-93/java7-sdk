/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

/**
 * A thread hooked as a VM shutdown hook to tell an OperatingSystemNotificationThread
 * to terminate.
 * 
 * @author pchurch
 * @since 1.5
 */
class OperatingSystemNotificationThreadShutdown extends Thread {
	private final OperatingSystemNotificationThread myVictim;
	
	/**
	 * Basic constructor
	 * @param victim The thread to notify on shutdown
	 */
	OperatingSystemNotificationThreadShutdown( OperatingSystemNotificationThread victim ) {
		myVictim = victim;
	}
	
	/**
	 * Shutdown hook code that coordinates the termination of a memory
	 * notification thread.
	 */
	public void run() {
		sendShutdownNotification();
		try {
			// wait for the notification thread to terminate
			myVictim.join();
		} catch( InterruptedException e ) {
			// don't care
		}
	}

	/**
	 * Unregisters the SIGRECONFIG handler, then wipes any pending
	 * notifications and puts a shutdown request on an internal notification
	 * queue.
	 */
	private native void sendShutdownNotification();

}
