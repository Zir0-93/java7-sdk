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
 * Copyright (c) 1995, 1998, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Dimension;
import java.awt.TextField;

/**
 * The peer interface for {@link TextField}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface TextFieldPeer extends TextComponentPeer {

    /**
     * Sets the echo character.
     *
     * @param echoChar the echo character to set
     *
     * @see TextField#getEchoChar()
     */
    void setEchoChar(char echoChar);

    /**
     * Returns the preferred size of the text field with the specified number
     * of columns.
     *
     * @param columns the number of columns
     *
     * @return the preferred size of the text field
     *
     * @see TextField#getPreferredSize(int)
     */
    Dimension getPreferredSize(int columns);

    /**
     * Returns the minimum size of the text field with the specified number
     * of columns.
     *
     * @param columns the number of columns
     *
     * @return the minimum size of the text field
     *
     * @see TextField#getMinimumSize(int)
     */
    Dimension getMinimumSize(int columns);

}
