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
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

/**
 * Thrown when an invalid string operation is passed
 * to a method for constructing a query.
 *
 * @since 1.5
 */
public class BadStringOperationException extends Exception   {


    /* Serial version */
    private static final long serialVersionUID = 7802201238441662100L;

    /**
     * @serial The description of the operation that originated this exception
     */
    private String op;

    /**
     * Constructs a <CODE>BadStringOperationException</CODE> with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public BadStringOperationException(String message) {
        this.op = message;
    }


    /**
     * Returns the string representing the object.
     */
    public String toString()  {
        return "BadStringOperationException: " + op;
    }

 }
