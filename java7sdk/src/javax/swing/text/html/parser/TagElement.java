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
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html.parser;

import javax.swing.text.html.HTML;
/**
 * A generic HTML TagElement class. The methods define how white
 * space is interpreted around the tag.
 *
 * @author      Sunita Mani
 */

public class TagElement {

    Element elem;
    HTML.Tag htmlTag;
    boolean insertedByErrorRecovery;

    public TagElement ( Element elem ) {
        this(elem, false);
    }

    public TagElement (Element elem, boolean fictional) {
        this.elem = elem;
        htmlTag = HTML.getTag(elem.getName());
        if (htmlTag == null) {
            htmlTag = new HTML.UnknownTag(elem.getName());
        }
        insertedByErrorRecovery = fictional;
    }

    public boolean breaksFlow() {
        return htmlTag.breaksFlow();
    }

    public boolean isPreformatted() {
        return htmlTag.isPreformatted();
    }

    public Element getElement() {
        return elem;
    }

    public HTML.Tag getHTMLTag() {
        return htmlTag;
    }

    public boolean fictional() {
        return insertedByErrorRecovery;
    }
}
