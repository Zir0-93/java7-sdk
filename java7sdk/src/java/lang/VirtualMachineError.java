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
 * Copyright (c) 1995, 1997, Oracle and/or its affiliates. All rights reserved.
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
 * Thrown to indicate that the Java Virtual Machine is broken or has
 * run out of resources necessary for it to continue operating.
 *
 *
 * @author  Frank Yellin
 * @since   JDK1.0
 */
abstract public
class VirtualMachineError extends Error {
    /**
     * Constructs a <code>VirtualMachineError</code> with no detail message.
     */
    public VirtualMachineError() {
        super();
    }

    /**
     * Constructs a <code>VirtualMachineError</code> with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    public VirtualMachineError(String s) {
        super(s);
    }
}
