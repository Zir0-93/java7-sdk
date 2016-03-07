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
 * Copyright (c) 1994, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown to indicate some unexpected internal error has occurred in
 * the Java Virtual Machine.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
public
class InternalError extends VirtualMachineError {
    private static final long serialVersionUID = -9062593416125562365L;

    /**
     * Constructs an <code>InternalError</code> with no detail message.
     */
    public InternalError() {
        super();
    }

    /**
     * Constructs an <code>InternalError</code> with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    public InternalError(String s) {
        super(s);
    }
}
