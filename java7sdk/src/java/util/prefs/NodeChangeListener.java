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
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

/**
 * A listener for receiving preference node change events.
 *
 * @author  Josh Bloch
 * @see     Preferences
 * @see     NodeChangeEvent
 * @see     PreferenceChangeListener
 * @since   1.4
 */

public interface NodeChangeListener extends java.util.EventListener {
    /**
     * This method gets called when a child node is added.
     *
     * @param evt A node change event object describing the parent
     *            and child node.
     */
    void childAdded(NodeChangeEvent evt);

    /**
     * This method gets called when a child node is removed.
     *
     * @param evt A node change event object describing the parent
     *            and child node.
     */
    void childRemoved(NodeChangeEvent evt);
}
