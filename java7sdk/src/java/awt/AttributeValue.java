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
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import sun.util.logging.PlatformLogger;

abstract class AttributeValue {
    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.AttributeValue");
    private final int value;
    private final String[] names;

    protected AttributeValue(int value, String[] names) {
        if (log.isLoggable(PlatformLogger.FINEST)) {
            log.finest("value = " + value + ", names = " + names);
        }

        if (log.isLoggable(PlatformLogger.FINER)) {
            if ((value < 0) || (names == null) || (value >= names.length)) {
                log.finer("Assertion failed");
            }
        }
        this.value = value;
        this.names = names;
    }
    // This hashCode is used by the sun.awt implementation as an array
    // index.
    public int hashCode() {
        return value;
    }
    public String toString() {
        return names[value];
    }
}
