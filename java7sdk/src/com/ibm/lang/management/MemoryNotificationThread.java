/* 
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */
package com.ibm.lang.management;

import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryUsage;

import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.management.Notification;

/**
 * A thread that monitors and dispatches memory usage notifications from
 * an internal queue.
 * 
 * @author Paul Church
 * @since 1.5
 */
class MemoryNotificationThread extends Thread {

	private MemoryMXBeanImpl memBean;

	private MemoryPoolMXBeanImpl memPool;

	int internalID;
	
	/**
	 * Basic constructor
	 * @param mem The memory bean to send notifications through
	 * @param myPool The memory pool bean we are sending notifications on behalf of
	 * @param id The internal ID of the notification queue being monitored
	 */
	MemoryNotificationThread( MemoryMXBeanImpl mem, MemoryPoolMXBeanImpl myPool, int id ) {
		memBean = mem;
		memPool = myPool;
		internalID = id;
	}
	
	/**
	 * Register a shutdown handler that will signal this thread to terminate,
	 * then enter the native that services an internal notification queue.
	 */
	public void run() {
        final Thread myShutdownNotifier = new MemoryNotificationThreadShutdown( this );
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                try {
                    Runtime.getRuntime().addShutdownHook(myShutdownNotifier);
                    return null;
                } catch (IllegalStateException e) {
                    /* if by chance we are already shutting down when we try to
                     * register the shutdown hook, allow this thread to terminate
                     * silently
                     */
                    return null;
                }
            }
        });

		processNotificationLoop( internalID );
	}

	/**
	 *  Process notifications on an internal VM queue until a shutdown
	 *  request is received.
	 *  @param internalID The internal ID of the queue to service 
	 *  */
	private native void processNotificationLoop( int internalID );
	
	/**
     * A helper used by processNotificationLoop to construct and dispatch
     * notification objects
     * 
     * @param min
     *            the initial amount in bytes of memory that can be allocated by
     *            this virtual machine
     * @param used
     *            the number of bytes currently used for memory
     * @param committed
     *            the number of bytes of committed memory
     * @param max
     *            the maximum number of bytes that can be used for memory
     *            management purposes
     * @param count
     *            the number of times that the memory usage of the memory pool
     *            in question has met or exceeded the relevant threshold
     * @param sequenceNumber
     *            the sequence identifier of the current notification
     * @param isCollectionUsageNotification
     *            a <code>boolean</code> indication of whether or not the new
     *            notification is as a result of the collection threshold being
     *            exceeded. If this value is <code>false</code> then the
     *            implication is that a memory threshold has been exceeded.
     */
    private void dispatchNotificationHelper(long min, long used,
            long committed, long max, long count, long sequenceNumber,
            boolean isCollectionUsageNotification) {
        MemoryNotificationInfo info = new MemoryNotificationInfo(memPool
                .getName(), new MemoryUsage(min, used, committed, max), count);
        Notification n = new Notification(
                isCollectionUsageNotification ? MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED
                        : MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED,
                "java.lang:type=Memory", sequenceNumber);
        n.setUserData(ManagementUtils
                .toMemoryNotificationInfoCompositeData(info));
        memBean.sendNotification(n);
    }
}
