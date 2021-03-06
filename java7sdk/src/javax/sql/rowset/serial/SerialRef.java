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
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.sql.*;
import java.io.*;
import java.util.*;

/**
 * A serialized mapping of a <code>Ref</code> object, which is the mapping in the
 * Java programming language of an SQL <code>REF</code> value.
 * <p>
 * The <code>SerialRef</code> class provides a constructor  for
 * creating a <code>SerialRef</code> instance from a <code>Ref</code>
 * object and provides methods for getting and setting the <code>Ref</code> object.
 */
public class SerialRef implements Ref, Serializable, Cloneable {

    /**
     * String containing the base type name.
     * @serial
     */
    private String baseTypeName;

    /**
     * This will store the type <code>Ref</code> as an <code>Object</code>.
     */
    private Object object;

    /**
     * Private copy of the Ref reference.
     */
    private Ref reference;

    /**
     * Constructs a <code>SerialRef</code> object from the given <code>Ref</code>
     * object.
     *
     * @param ref a Ref object; cannot be <code>null</code>
     * @throws SQLException if a database access occurs; if <code>ref</code>
     *     is <code>null</code>; or if the <code>Ref</code> object returns a
     *     <code>null</code> value base type name.
     * @throws SerialException if an error occurs serializing the <code>Ref</code>
     *     object
     */
    public SerialRef(Ref ref) throws SerialException, SQLException {
        if (ref == null) {
            throw new SQLException("Cannot instantiate a SerialRef object " +
                "with a null Ref object");
        }
        reference = ref;
        object = ref;
        if (ref.getBaseTypeName() == null) {
            throw new SQLException("Cannot instantiate a SerialRef object " +
                "that returns a null base type name");
        } else {
            baseTypeName = ref.getBaseTypeName();
        }
    }

    /**
     * Returns a string describing the base type name of the <code>Ref</code>.
     *
     * @return a string of the base type name of the Ref
     * @throws SerialException in no Ref object has been set
     */
    public String getBaseTypeName() throws SerialException {
        return baseTypeName;
    }

    /**
     * Returns an <code>Object</code> representing the SQL structured type
     * to which this <code>SerialRef</code> object refers.  The attributes
     * of the structured type are mapped according to the given type map.
     *
     * @param map a <code>java.util.Map</code> object containing zero or
     *        more entries, with each entry consisting of 1) a <code>String</code>
     *        giving the fully qualified name of a UDT and 2) the
     *        <code>Class</code> object for the <code>SQLData</code> implementation
     *        that defines how the UDT is to be mapped
     * @return an object instance resolved from the Ref reference and mapped
     *        according to the supplied type map
     * @throws SerialException if an error is encountered in the reference
     *        resolution
     */
    public Object getObject(java.util.Map<String,Class<?>> map)
        throws SerialException
    {
        map = new Hashtable(map);
        if (object != null) {
            return map.get(object);
        } else {
            throw new SerialException("The object is not set");
        }
    }

    /**
     * Returns an <code>Object</code> representing the SQL structured type
     * to which this <code>SerialRef</code> object refers.
     *
     * @return an object instance resolved from the Ref reference
     * @throws SerialException if an error is encountered in the reference
     *         resolution
     */
    public Object getObject() throws SerialException {

        if (reference != null) {
            try {
                return reference.getObject();
            } catch (SQLException e) {
                throw new SerialException("SQLException: " + e.getMessage());
            }
        }

        if (object != null) {
            return object;
        }


        throw new SerialException("The object is not set");

    }

    /**
     * Sets the SQL structured type that this <code>SerialRef</code> object
     * references to the given <code>Object</code> object.
     *
     * @param obj an <code>Object</code> representing the SQL structured type
     *        to be referenced
     * @throws SerialException if an error is encountered generating the
     * the structured type referenced by this <code>SerialRef</code> object
     */
    public void setObject(Object obj) throws SerialException {
        try {
            reference.setObject(obj);
        } catch (SQLException e) {
            throw new SerialException("SQLException: " + e.getMessage());
        }
        object = obj;
    }

    /**
         * The identifier that assists in the serialization of this <code>SerialRef</code>
     * object.
     */
    static final long serialVersionUID = -4727123500609662274L;


}
