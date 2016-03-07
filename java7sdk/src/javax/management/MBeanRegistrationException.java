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
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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


/**
 * Wraps exceptions thrown by the preRegister(), preDeregister() methods
 * of the <CODE>MBeanRegistration</CODE> interface.
 *
 * @since 1.5
 */
public class MBeanRegistrationException extends MBeanException   {

    /* Serial version */
    private static final long serialVersionUID = 4482382455277067805L;

    /**
     * Creates an <CODE>MBeanRegistrationException</CODE> that wraps
     * the actual <CODE>java.lang.Exception</CODE>.
     *
     * @param e the wrapped exception.
     */
    public MBeanRegistrationException(java.lang.Exception e) {
        super(e) ;
    }

    /**
     * Creates an <CODE>MBeanRegistrationException</CODE> that wraps
     * the actual <CODE>java.lang.Exception</CODE> with a detailed
     * message.
     *
     * @param e the wrapped exception.
     * @param message the detail message.
     */
    public MBeanRegistrationException(java.lang.Exception e, String message) {
        super(e, message) ;
    }
}