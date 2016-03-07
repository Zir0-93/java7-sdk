/* 
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */
package com.ibm.lang.management;

import javax.management.Notification;

/**
 * A thread that monitors and dispatches notifications for changes in
 * the number of CPUs, processing capacity, and total physical memory.
 * 
 * @author pchurch
 * @since 1.5
 */
class OperatingSystemNotificationThread extends Thread {
	private final OperatingSystemMXBeanImpl osBean;
	
	public OperatingSystemNotificationThread( OperatingSystemMXBeanImpl osBean ) {
		this.osBean = osBean;
	}
	
	/**
	 * Register a shutdown handler that will signal this thread to terminate,
	 * then enter the native that services an internal notification queue.
	 */
	public void run() {
		Thread myShutdownNotifier = new OperatingSystemNotificationThreadShutdown( this );
		try {
			Runtime.getRuntime().addShutdownHook( myShutdownNotifier );
		} catch( IllegalStateException e ) {
			/* if by chance we are already shutting down when we try to
			 * register the shutdown hook, allow this thread to terminate
			 * silently
			 */
			return;
		}
		processNotificationLoop();
	}

	/**
	 * Registers a signal handler for SIGRECONFIG, then processes notifications
	 * on an internal VM queue until a shutdown request is received. 
	 */
	private native void processNotificationLoop();
	
	private void dispatchNotificationHelper( int type, long data, long sequenceNumber ) {
		if( type == 1 ) {
			// #CPUs changed
			AvailableProcessorsNotificationInfo info = new AvailableProcessorsNotificationInfo( (int)data );
			Notification n = new Notification( AvailableProcessorsNotificationInfo.AVAILABLE_PROCESSORS_CHANGE, "java.lang:type=OperatingSystem", sequenceNumber );
			n.setUserData( ManagementUtils.toAvailableProcessorsNotificationInfoCompositeData( info ) );
			osBean.sendNotification( n );
		} else if( type == 2 ) {
			// processing capacity changed
			ProcessingCapacityNotificationInfo info = new ProcessingCapacityNotificationInfo( (int)data );
			Notification n = new Notification( ProcessingCapacityNotificationInfo.PROCESSING_CAPACITY_CHANGE, "java.lang:type=OperatingSystem", sequenceNumber );
			n.setUserData( ManagementUtils.toProcessingCapacityNotificationInfoCompositeData( info ) );
			osBean.sendNotification( n );
		} else if( type == 3 ) {
			// total physical memory changed
			TotalPhysicalMemoryNotificationInfo info = new TotalPhysicalMemoryNotificationInfo( data );
			Notification n = new Notification( TotalPhysicalMemoryNotificationInfo.TOTAL_PHYSICAL_MEMORY_CHANGE, "java.lang:type=OperatingSystem", sequenceNumber );
			n.setUserData( ManagementUtils.toTotalPhysicalMemoryNotificationInfoCompositeData( info ) );
			osBean.sendNotification( n );
		}
	}
}
