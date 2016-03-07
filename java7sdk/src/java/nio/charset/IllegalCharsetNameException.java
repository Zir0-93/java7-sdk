/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
 * ===========================================================================
 *  Licensed Materials - Property of IBM
 *  "Restricted Materials of IBM"
 *  
 *  IBM SDK, Java(tm) Technology Edition, v7
 *  (C) Copyright IBM Corp. 2010, 2014. All Rights Reserved
 * 
 *  US Government Users Restricted Rights - Use, duplication or disclosure
 *  restricted by GSA ADP Schedule Contract with IBM Corp.
 * ===========================================================================
 *
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
 *
 */

// -- This file was mechanically generated: Do not edit! -- //

package java.nio.charset;


/**
 * Unchecked exception thrown when a string that is not a
 * <a href=Charset.html#names>legal charset name</a> is used as such.
 *
 * @since 1.4
 */

public class IllegalCharsetNameException
    extends IllegalArgumentException
{

    private static final long serialVersionUID = 1457525358470002989L;

    private String charsetName;

    /**
     * Constructs an instance of this class. </p>
     *
     * @param  charsetName
     *         The illegal charset name
     */
    public IllegalCharsetNameException(String charsetName) {
        super(String.valueOf(charsetName));
	this.charsetName = charsetName;
    }

    /**
     * Retrieves the illegal charset name. </p>
     *
     * @return  The illegal charset name
     */
    public String getCharsetName() {
        return charsetName;
    }

}
