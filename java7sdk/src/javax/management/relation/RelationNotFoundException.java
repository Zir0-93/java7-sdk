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
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.relation;

/**
 * This exception is raised when there is no relation for a given relation id
 * in a Relation Service.
 *
 * @since 1.5
 */
public class RelationNotFoundException extends RelationException {

    /* Serial version */
    private static final long serialVersionUID = -3793951411158559116L;

    /**
     * Default constructor, no message put in exception.
     */
    public RelationNotFoundException() {
        super();
    }

    /**
     * Constructor with given message put in exception.
     *
     * @param message the detail message.
     */
    public RelationNotFoundException(String message) {
        super(message);
    }
}
