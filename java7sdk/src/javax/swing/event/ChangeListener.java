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
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.event;


import java.util.EventListener;


/**
 * Defines an object which listens for ChangeEvents.
 *
 * @author Jeff Dinkins
 */
public interface ChangeListener extends EventListener {
    /**
     * Invoked when the target of the listener has changed its state.
     *
     * @param e  a ChangeEvent object
     */
    void stateChanged(ChangeEvent e);
}
