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

package javax.swing.event;

import java.util.EventListener;

/**
  * The listener that's notified when a tree expands or collapses
  * a node.
  * For further documentation and examples see
  * <a
  href="http://java.sun.com/docs/books/tutorial/uiswing/events/treeexpansionlistener.html">How to Write a Tree Expansion Listener</a>,
  * a section in <em>The Java Tutorial.</em>
  *
  * @author Scott Violet
  */

public interface TreeExpansionListener extends EventListener
{
    /**
      * Called whenever an item in the tree has been expanded.
      */
    public void treeExpanded(TreeExpansionEvent event);

    /**
      * Called whenever an item in the tree has been collapsed.
      */
    public void treeCollapsed(TreeExpansionEvent event);
}
