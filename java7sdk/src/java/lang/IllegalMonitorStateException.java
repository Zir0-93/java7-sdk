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
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Thrown to indicate that a thread has attempted to wait on an
 * object's monitor or to notify other threads waiting on an object's
 * monitor without owning the specified monitor.
 *
 * @author  unascribed
 * @see     java.lang.Object#notify()
 * @see     java.lang.Object#notifyAll()
 * @see     java.lang.Object#wait()
 * @see     java.lang.Object#wait(long)
 * @see     java.lang.Object#wait(long, int)
 * @since   JDK1.0
 */
public
class IllegalMonitorStateException extends RuntimeException {
    private static final long serialVersionUID = 3713306369498869069L;

    /**
     * Constructs an <code>IllegalMonitorStateException</code> with no
     * detail message.
     */
    public IllegalMonitorStateException() {
        super();
    }

    /**
     * Constructs an <code>IllegalMonitorStateException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public IllegalMonitorStateException(String s) {
        super(s);
    }
}
