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
package javax.swing.text;

import java.awt.Container;

/**
 * A factory to create a view of some portion of document subject.
 * This is intended to enable customization of how views get
 * mapped over a document model.
 *
 * @author  Timothy Prinzing
 */
public interface ViewFactory {

    /**
     * Creates a view from the given structural element of a
     * document.
     *
     * @param elem  the piece of the document to build a view of
     * @return the view
     * @see View
     */
    public View create(Element elem);

}