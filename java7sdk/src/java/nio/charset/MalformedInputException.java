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
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.charset;


/**
 * Checked exception thrown when an input byte sequence is not legal for given
 * charset, or an input character sequence is not a legal sixteen-bit Unicode
 * sequence.
 *
 * @since 1.4
 */

public class MalformedInputException
    extends CharacterCodingException
{

    private static final long serialVersionUID = -3438823399834806194L;

    private int inputLength;

    public MalformedInputException(int inputLength) {
        this.inputLength = inputLength;
    }

    public int getInputLength() {
        return inputLength;
    }

    public String getMessage() {
        return "Input length = " + inputLength;
    }

}
