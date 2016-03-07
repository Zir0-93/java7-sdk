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
 * Copyright (c) 1997, 1999, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.geom;

/**
 * The <code>IllegalPathStateException</code> represents an
 * exception that is thrown if an operation is performed on a path
 * that is in an illegal state with respect to the particular
 * operation being performed, such as appending a path segment
 * to a {@link GeneralPath} without an initial moveto.
 *
 */

public class IllegalPathStateException extends RuntimeException {
    /**
     * Constructs an <code>IllegalPathStateException</code> with no
     * detail message.
     *
     * @since   1.2
     */
    public IllegalPathStateException() {
    }

    /**
     * Constructs an <code>IllegalPathStateException</code> with the
     * specified detail message.
     * @param   s   the detail message
     * @since   1.2
     */
    public IllegalPathStateException(String s) {
        super (s);
    }
}
