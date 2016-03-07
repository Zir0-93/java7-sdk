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
 * Copyright (c) 1996, 1998, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi;

/**
 * An <code>UnexpectedException</code> is thrown if the client of a
 * remote method call receives, as a result of the call, a checked
 * exception that is not among the checked exception types declared in the
 * <code>throws</code> clause of the method in the remote interface.
 *
 * @author  Roger Riggs
 * @since   JDK1.1
 */
public class UnexpectedException extends RemoteException {

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 1800467484195073863L;

    /**
     * Constructs an <code>UnexpectedException</code> with the specified
     * detail message.
     *
     * @param s the detail message
     * @since JDK1.1
     */
    public UnexpectedException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>UnexpectedException</code> with the specified
     * detail message and nested exception.
     *
     * @param s the detail message
     * @param ex the nested exception
     * @since JDK1.1
     */
    public UnexpectedException(String s, Exception ex) {
        super(s, ex);
    }
}