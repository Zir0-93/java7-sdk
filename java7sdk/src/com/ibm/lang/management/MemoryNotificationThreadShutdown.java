/* 
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */
package com.ibm.lang.management;

/**
 * A thread hooked as a VM shutdown hook to tell a MemoryNotificationThread
 * to terminate.
 * 
 * @author Paul Church
 * @since 1.5
 */
class MemoryNotificationThreadShutdown extends Thread {
	private MemoryNotificationThread myVictim;
	
	/**
	 * Basic constructor
	 * @param victim The thread to notify on shutdown
	 */
	MemoryNotificationThreadShutdown( MemoryNotificationThread victim ) {
		myVictim = victim;
	}
	
	/**
	 * Shutdown hook code that coordinates the termination of a memory
	 * notification thread.
	 */
	public void run() {
		sendShutdownNotification( myVictim.internalID );
		try {
			// wait for the notification thread to terminate
			myVictim.join();
		} catch( InterruptedException e ) {
			// don't care
		}
	}

	/**
	 * Wipes any pending notifications and puts a shutdown request
	 * notification on an internal notification queue.
	 * @param id The internal id of the queue to shut down
	 */
	private native void sendShutdownNotification( int id );
}
