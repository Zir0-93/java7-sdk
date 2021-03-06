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
 * Copyright (c) 1995, 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;
import java.awt.MenuComponent;

/**
 * The base interface for all kinds of menu components. This is used by
 * {@link MenuComponent}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface MenuComponentPeer {

    /**
     * Disposes the menu component.
     *
     * @see MenuComponent#removeNotify()
     */
    void dispose();

    /**
     * Sets the font for the menu component.
     *
     * @param f the font to use for the menu component
     *
     * @see MenuComponent#setFont(Font)
     */
    void setFont(Font f);
}
