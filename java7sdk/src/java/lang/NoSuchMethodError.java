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
 * Thrown if an application tries to call a specified method of a
 * class (either static or instance), and that class no longer has a
 * definition of that method.
 * <p>
 * Normally, this error is caught by the compiler; this error can
 * only occur at run time if the definition of a class has
 * incompatibly changed.
 *
 * @author  unascribed
 * @since   JDK1.0
 */
public
class NoSuchMethodError extends IncompatibleClassChangeError {
    private static final long serialVersionUID = -3765521442372831335L;

    /**
     * Constructs a <code>NoSuchMethodError</code> with no detail message.
     */
    public NoSuchMethodError() {
        super();
    }

    /**
     * Constructs a <code>NoSuchMethodError</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public NoSuchMethodError(String s) {
        super(s);
    }
}
