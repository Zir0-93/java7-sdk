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
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import com.sun.jmx.remote.util.ClassLogger;
import com.ibm.tenant.TenantContext;
import com.ibm.tenant.TenantGlobals;
import com.ibm.tenant.internal.TenantAccessFactory;
import com.ibm.tenant.SlotType;

/**
 * <p>Provides an implementation of {@link
 * javax.management.NotificationEmitter NotificationEmitter}
 * interface.  This can be used as the super class of an MBean that
 * sends notifications.</p>
 *
 * <p>By default, the notification dispatch model is synchronous.
 * That is, when a thread calls sendNotification, the
 * <code>NotificationListener.handleNotification</code> method of each listener
 * is called within that thread. You can override this default
 * by overriding <code>handleNotification</code> in a subclass, or by passing an
 * Executor to the constructor.</p>
 *
 * <p>If the method call of a filter or listener throws an {@link Exception},
 * then that exception does not prevent other listeners from being invoked.  However,
 * if the method call of a filter or of {@code Executor.execute} or of
 * {@code handleNotification} (when no {@code Excecutor} is specified) throws an
 * {@link Error}, then that {@code Error} is propagated to the caller of
 * {@link #sendNotification sendNotification}.</p>
 *
 * <p>Remote listeners added using the JMX Remote API (see JMXConnector) are not
 * usually called synchronously.  That is, when sendNotification returns, it is
 * not guaranteed that any remote listeners have yet received the notification.</p>
 *
 * @since 1.5
 */
public class NotificationBroadcasterSupport implements NotificationEmitter {
    private int slotIndex = -1;
    /*
     This constructor is implemented for MemoryMXBeanImpl for object leak issue.
     SlotIndex is used to 1) judge the caller is MemoryMXBeanImpl, 2) get 
     slotIndex so that it can get reference from VM map, to avoid object leakage
     issue.
     */
    NotificationBroadcasterSupport(int slotIndex) {
        this(null, (MBeanNotificationInfo[]) null);
        this.slotIndex = slotIndex;
    }

    /**
     * Constructs a NotificationBroadcasterSupport where each listener is invoked by the
     * thread sending the notification. This constructor is equivalent to
     * {@link NotificationBroadcasterSupport#NotificationBroadcasterSupport(Executor,
     * MBeanNotificationInfo[] info) NotificationBroadcasterSupport(null, null)}.
     */
    public NotificationBroadcasterSupport() {
        this(null, (MBeanNotificationInfo[]) null);
    }

    /**
     * Constructs a NotificationBroadcasterSupport where each listener is invoked using
     * the given {@link java.util.concurrent.Executor}. When {@link #sendNotification
     * sendNotification} is called, a listener is selected if it was added with a null
     * {@link NotificationFilter}, or if {@link NotificationFilter#isNotificationEnabled
     * isNotificationEnabled} returns true for the notification being sent. The call to
     * <code>NotificationFilter.isNotificationEnabled</code> takes place in the thread
     * that called <code>sendNotification</code>. Then, for each selected listener,
     * {@link Executor#execute executor.execute} is called with a command
     * that calls the <code>handleNotification</code> method.
     * This constructor is equivalent to
     * {@link NotificationBroadcasterSupport#NotificationBroadcasterSupport(Executor,
     * MBeanNotificationInfo[] info) NotificationBroadcasterSupport(executor, null)}.
     * @param executor an executor used by the method <code>sendNotification</code> to
     * send each notification. If it is null, the thread calling <code>sendNotification</code>
     * will invoke the <code>handleNotification</code> method itself.
     * @since 1.6
     */
    public NotificationBroadcasterSupport(Executor executor) {
        this(executor, (MBeanNotificationInfo[]) null);
    }

    /**
     * <p>Constructs a NotificationBroadcasterSupport with information
     * about the notifications that may be sent.  Each listener is
     * invoked by the thread sending the notification.  This
     * constructor is equivalent to {@link
     * NotificationBroadcasterSupport#NotificationBroadcasterSupport(Executor,
     * MBeanNotificationInfo[] info)
     * NotificationBroadcasterSupport(null, info)}.</p>
     *
     * <p>If the <code>info</code> array is not empty, then it is
     * cloned by the constructor as if by {@code info.clone()}, and
     * each call to {@link #getNotificationInfo()} returns a new
     * clone.</p>
     *
     * @param info an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.  Can be null, which is equivalent to
     * an empty array.
     *
     * @since 1.6
     */
    public NotificationBroadcasterSupport(MBeanNotificationInfo... info) {
        this(null, info);
    }

    /**
     * <p>Constructs a NotificationBroadcasterSupport with information about the notifications that may be sent,
     * and where each listener is invoked using the given {@link java.util.concurrent.Executor}.</p>
     *
     * <p>When {@link #sendNotification sendNotification} is called, a
     * listener is selected if it was added with a null {@link
     * NotificationFilter}, or if {@link
     * NotificationFilter#isNotificationEnabled isNotificationEnabled}
     * returns true for the notification being sent. The call to
     * <code>NotificationFilter.isNotificationEnabled</code> takes
     * place in the thread that called
     * <code>sendNotification</code>. Then, for each selected
     * listener, {@link Executor#execute executor.execute} is called
     * with a command that calls the <code>handleNotification</code>
     * method.</p>
     *
     * <p>If the <code>info</code> array is not empty, then it is
     * cloned by the constructor as if by {@code info.clone()}, and
     * each call to {@link #getNotificationInfo()} returns a new
     * clone.</p>
     *
     * @param executor an executor used by the method
     * <code>sendNotification</code> to send each notification. If it
     * is null, the thread calling <code>sendNotification</code> will
     * invoke the <code>handleNotification</code> method itself.
     *
     * @param info an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.  Can be null, which is equivalent to
     * an empty array.
     *
     * @since 1.6
     */
    public NotificationBroadcasterSupport(Executor executor,
                                          MBeanNotificationInfo... info) {
        this.executor = (executor != null) ? executor : defaultExecutor;

        notifInfo = info == null ? NO_NOTIFICATION_INFO : info.clone();
    }

    /*
     * Get listener list. Due to object leakage issue, return listener list per 
     * tenant if tenant is enabled. Otherwise, return the original listener list.
     *
     * @param listenerInfo The listener to receive notifications.
     */
    private List<ListenerInfo> getListenerList(ListenerInfo listenerInfo) {
        if (TenantGlobals.isTenantEnabled() && slotIndex >= 0) {
            // get tenant context for current listener info instance
            TenantContext tc = TenantContext.getTenantForObject(listenerInfo); 
            if (tc != null) {
                // read out listener list from slot
                Object obj = TenantAccessFactory.getAccess().safeReadObj(tc, slotIndex);
                if (obj != null && obj instanceof List) {
                    return (List<ListenerInfo>) obj;
                } else if (obj == null) {
                    // write listener list into slot if not existing
                    List<ListenerInfo> listenerList = new CopyOnWriteArrayList<ListenerInfo>();
                    TenantAccessFactory.getAccess().ensureAllocationAndWriteObj(tc, slotIndex, listenerList);
                    return listenerList;
                }
            }
        }
        return listenerList;
    }

    /**
     * Adds a listener.
     *
     * @param listener The listener to receive notifications.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback An opaque object to be sent back to the
     * listener when a notification is emitted. This object cannot be
     * used by the Notification broadcaster object. It should be
     * resent unchanged with the notification to the listener.
     *
     * @exception IllegalArgumentException thrown if the listener is null.
     *
     * @see #removeNotificationListener
     */
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback) {

        if (listener == null) {
            throw new IllegalArgumentException ("Listener can't be null") ;
        }

        ListenerInfo listenerInfo = new ListenerInfo(listener, filter, handback);
        getListenerList(listenerInfo).add(listenerInfo);
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {

        ListenerInfo wildcard = new WildcardListenerInfo(listener);
        boolean removed =
            getListenerList(wildcard).removeAll(Collections.singleton(wildcard));
        if (!removed)
            throw new ListenerNotFoundException("Listener not registered");
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws ListenerNotFoundException {

        ListenerInfo li = new ListenerInfo(listener, filter, handback);
        boolean removed = getListenerList(li).remove(li);
        if (!removed) {
            throw new ListenerNotFoundException("Listener not registered " +
                                                "(with this filter and " +
                                                "handback)");
            // or perhaps not registered at all
        }
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        if (notifInfo.length == 0)
            return notifInfo;
        else
            return notifInfo.clone();
    }


    /**
     * Sends a notification.
     *
     * If an {@code Executor} was specified in the constructor, it will be given one
     * task per selected listener to deliver the notification to that listener.
     *
     * @param notification The notification to send.
     */
    public void sendNotification(Notification notification) {

        if (notification == null) {
            return;
        }

        List<ListenerInfo> allListenerList = 
            new CopyOnWriteArrayList<ListenerInfo>();
        if (TenantGlobals.isTenantEnabled() && slotIndex >= 0) {
            // get all live tenant
            TenantContext tcs[] = TenantAccessFactory.getAccess().getAllLive();
            for (int i = 0; i < tcs.length; i++) {
                if (tcs[i] != null && slotIndex >= 0) {
                    // read out listener list from slot
                    Object obj = TenantAccessFactory.getAccess()
                                .safeReadObj(tcs[i], slotIndex);
                    if (obj != null && obj instanceof List) {
                        allListenerList.addAll((List<ListenerInfo>) obj);
                    }
                }
            }
        } else {
            allListenerList.addAll(listenerList);
        }

        boolean enabled;

        for (ListenerInfo li : allListenerList) {
            try {
                enabled = li.filter == null ||
                    li.filter.isNotificationEnabled(notification);
            } catch (Exception e) {
                if (logger.debugOn()) {
                    logger.debug("sendNotification", e);
                }

                continue;
            }

            if (enabled) {
                executor.execute(new SendNotifJob(notification, li));
            }
        }
    }

    /**
     * <p>This method is called by {@link #sendNotification
     * sendNotification} for each listener in order to send the
     * notification to that listener.  It can be overridden in
     * subclasses to change the behavior of notification delivery,
     * for instance to deliver the notification in a separate
     * thread.</p>
     *
     * <p>The default implementation of this method is equivalent to
     * <pre>
     * listener.handleNotification(notif, handback);
     * </pre>
     *
     * @param listener the listener to which the notification is being
     * delivered.
     * @param notif the notification being delivered to the listener.
     * @param handback the handback object that was supplied when the
     * listener was added.
     *
     */
    protected void handleNotification(NotificationListener listener,
                                      Notification notif, Object handback) {
        listener.handleNotification(notif, handback);
    }

    // private stuff
    private static class ListenerInfo {
        NotificationListener listener;
        NotificationFilter filter;
        Object handback;

        ListenerInfo(NotificationListener listener,
                     NotificationFilter filter,
                     Object handback) {
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ListenerInfo))
                return false;
            ListenerInfo li = (ListenerInfo) o;
            if (li instanceof WildcardListenerInfo)
                return (li.listener == listener);
            else
                return (li.listener == listener && li.filter == filter
                        && li.handback == handback);
        }
    }

    private static class WildcardListenerInfo extends ListenerInfo {
        WildcardListenerInfo(NotificationListener listener) {
            super(listener, null, null);
        }

        public boolean equals(Object o) {
            assert (!(o instanceof WildcardListenerInfo));
            return o.equals(this);
        }
    }

    private List<ListenerInfo> listenerList =
        new CopyOnWriteArrayList<ListenerInfo>();

    // since 1.6
    private final Executor executor;
    private final MBeanNotificationInfo[] notifInfo;

    private final static Executor defaultExecutor = new Executor() {
            // DirectExecutor using caller thread
            public void execute(Runnable r) {
                r.run();
            }
        };

    private static final MBeanNotificationInfo[] NO_NOTIFICATION_INFO =
        new MBeanNotificationInfo[0];

    private class SendNotifJob implements Runnable {
        public SendNotifJob(Notification notif, ListenerInfo listenerInfo) {
            this.notif = notif;
            this.listenerInfo = listenerInfo;
        }

        public void run() {
            try {
                handleNotification(listenerInfo.listener,
                                   notif, listenerInfo.handback);
            } catch (Exception e) {
                if (logger.debugOn()) {
                    logger.debug("SendNotifJob-run", e);
                }
            }
        }

        private final Notification notif;
        private final ListenerInfo listenerInfo;
    }

    private static final ClassLogger logger =
        new ClassLogger("javax.management", "NotificationBroadcasterSupport");
}
