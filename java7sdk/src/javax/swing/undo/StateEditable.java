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
 * Copyright (c) 1997, 2004, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.undo;

import java.util.Hashtable;


/**
 * StateEditable defines the interface for objects that can have
 * their state undone/redone by a StateEdit.
 *
 * @see StateEdit
 */

public interface StateEditable {

    /** Resource ID for this class. */
    public static final String RCSID = "$Id: StateEditable.java,v 1.2 1997/09/08 19:39:08 marklin Exp $";

    /**
     * Upon receiving this message the receiver should place any relevant
     * state into <EM>state</EM>.
     */
    public void storeState(Hashtable<Object,Object> state);

    /**
     * Upon receiving this message the receiver should extract any relevant
     * state out of <EM>state</EM>.
     */
    public void restoreState(Hashtable<?,?> state);
} // End of interface StateEditable
