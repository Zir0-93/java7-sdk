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
 * Copyright (c) 1995, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Label;

/**
 * The peer interface for {@link Label}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface LabelPeer extends ComponentPeer {

    /**
     * Sets the text to be displayed on the label.
     *
     * @param label the text to be displayed on the label
     *
     * @see Label#setText
     */
    void setText(String label);

    /**
     * Sets the alignment of the label text.
     *
     * @param alignment the alignment of the label text
     *
     * @see Label#setAlignment(int)
     * @see Label#CENTER
     * @see Label#RIGHT
     * @see Label#LEFT
     */
    void setAlignment(int alignment);
}
