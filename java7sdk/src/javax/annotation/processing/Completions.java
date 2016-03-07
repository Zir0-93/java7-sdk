/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v7
 * (C) Copyright IBM Corp. 2010, 2014. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.annotation.processing;

import java.util.Arrays;

/**
 * Utility class for assembling {@link Completion} objects.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public class Completions {
    // No instances for you.
    private Completions() {}

    private static class SimpleCompletion implements Completion {
        private String value;
        private String message;

        SimpleCompletion(String value, String message) {
            if (value == null || message == null)
                throw new NullPointerException("Null completion strings not accepted.");
            this.value = value;
            this.message = message;
        }

        public String getValue() {
            return value;
        }


        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "[\"" + value + "\", \"" + message + "\"]";
        }
        // Default equals and hashCode are fine.
    }

    /**
     * Returns a completion of the value and message.
     *
     * @param value the text of the completion
     * @param message a message about the completion
     * @return a completion of the provided value and message
     */
    public static Completion of(String value, String message) {
        return new SimpleCompletion(value, message);
    }

    /**
     * Returns a completion of the value and an empty message
     *
     * @param value the text of the completion
     * @return a completion of the value and an empty message
     */
    public static Completion of(String value) {
        return new SimpleCompletion(value, "");
    }
}