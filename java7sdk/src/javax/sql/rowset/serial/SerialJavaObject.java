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
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset.serial;

import java.io.*;
import java.lang.reflect.*;
import javax.sql.rowset.RowSetWarning;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;

/**
 * A serializable mapping in the Java programming language of an SQL
 * <code>JAVA_OBJECT</code> value. Assuming the Java object
 * implements the <code>Serializable</code> interface, this class simply wraps the
 * serialization process.
 * <P>
 * If however, the serialization is not possible because
 * the Java object is not immediately serializable, this class will
 * attempt to serialize all non-static members to permit the object
 * state to be serialized.
 * Static or transient fields cannot be serialized; an attempt to serialize
 * them will result in a <code>SerialException</code> object being thrown.
 *
 * @author Jonathan Bruce
 */
public class SerialJavaObject implements Serializable, Cloneable {

    /**
     * Placeholder for object to be serialized.
     */
    private final Object obj;


   /**
    * Placeholder for all fields in the <code>JavaObject</code> being serialized.
    */
    private transient Field[] fields;

    /**
     * Constructor for <code>SerialJavaObject</code> helper class.
     * <p>
     *
     * @param obj the Java <code>Object</code> to be serialized
     * @throws SerialException if the object is found not to be serializable
     */
    public SerialJavaObject(Object obj) throws SerialException {

        // if any static fields are found, an exception
        // should be thrown


        // get Class. Object instance should always be available
        Class<?> c = obj.getClass();

        // determine if object implements Serializable i/f
        if (!(obj instanceof java.io.Serializable)) {
            setWarning(new RowSetWarning("Warning, the object passed to the constructor does not implement Serializable"));
        }

        // can only determine public fields (obviously). If
        // any of these are static, this should invalidate
        // the action of attempting to persist these fields
        // in a serialized form

        boolean anyStaticFields = false;
        fields = c.getFields();

        for (int i = 0; i < fields.length; i++ ) {
            if ( fields[i].getModifiers() == Modifier.STATIC ) {
                anyStaticFields = true;
            }
        }


        if (anyStaticFields) {
            throw new SerialException("Located static fields in " +
                "object instance. Cannot serialize");
        }

        this.obj = obj;
    }

    /**
     * Returns an <code>Object</code> that is a copy of this <code>SerialJavaObject</code>
     * object.
     *
     * @return a copy of this <code>SerialJavaObject</code> object as an
     *         <code>Object</code> in the Java programming language
     * @throws SerialException if the instance is corrupt
     */
    public Object getObject() throws SerialException {
        return this.obj;
    }

    /**
     * Returns an array of <code>Field</code> objects that contains each
     * field of the object that this helper class is serializing.
     *
     * @return an array of <code>Field</code> objects
     * @throws SerialException if an error is encountered accessing
     * the serialized object
     * @see Class#getFields
     */
    @CallerSensitive
    public Field[] getFields() throws SerialException {
        if (fields != null) {
            Class<?> c = this.obj.getClass();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                /*
                 * Check if the caller is allowed to access the specified class's package.
                 * If access is denied, throw a SecurityException.
                 */
                Class<?> caller = sun.reflect.Reflection.getCallerClass();
                if (ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(),
                                                        c.getClassLoader())) {
                    ReflectUtil.checkPackageAccess(c);
                }
            }
            return c.getFields();
        } else {
            throw new SerialException("SerialJavaObject does not contain" +
                " a serialized object instance");
        }
    }

    /**
         * The identifier that assists in the serialization of this
     * <code>SerialJavaObject</code> object.
     */
    static final long serialVersionUID = -1465795139032831023L;

    /**
     * A container for the warnings issued on this <code>SerialJavaObject</code>
     * object. When there are multiple warnings, each warning is chained to the
     * previous warning.
     */
    java.util.Vector chain;

    /**
     * Registers the given warning.
     */
    private void setWarning(RowSetWarning e) {
        if (chain == null) {
            chain = new java.util.Vector();
        }
        chain.add(e);
    }
}
