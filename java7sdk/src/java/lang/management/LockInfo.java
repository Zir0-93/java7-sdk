/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2008, 2012  All Rights Reserved.
 */

package java.lang.management;

import java.beans.ConstructorProperties;

/**
 * TODO : Type description
 * 
 * @author gharley
 * 
 * @since 1.6
 */
public class LockInfo {

    String className;

    int identityHashCode;
    
    private LockInfo(Object object){
        this.className = object.getClass().getName();
        this.identityHashCode = System.identityHashCode(object);
    }
    
    /**
     * Creates a new <code>LockInfo</code> instance.
     * 
     * @param className
     *            the name (including the package prefix) of the associated lock
     *            object's class
     * @param identityHashCode
     *            the value of the associated lock object's identity hash code.
     *            This amounts to the result of calling
     *            {@link System#identityHashCode(Object)} with the lock object
     *            as the sole argument.
     * @throws NullPointerException
     *             if <code>className</code> is <code>null</code>
     */
    @ConstructorProperties(value = { "className", "identityHashCode" })
    public LockInfo(String className, int identityHashCode) {
        if (className == null) {
            throw new NullPointerException("className cannot be null");
        }
        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    /**
     * Returns the name of the lock object's class in fully qualified form (i.e.
     * including the package prefix).
     * 
     * @return the associated lock object's class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the value of the associated lock object's identity hash code
     * 
     * @return the identity hash code of the lock object
     */
    public int getIdentityHashCode() {
        return identityHashCode;
    }

    /**
     * Provides callers with a string value that represents the associated lock.
     * The string will hold both the name of the lock object's class and it's
     * identity hash code expressed as an unsigned hexidecimal. i.e.<br>
     * <p>
     * {@link #getClassName()}&nbsp;+&nbsp;&commat;&nbsp;+&nbsp;Integer.toHexString({@link #getIdentityHashCode()})
     * </p>
     * 
     * @return a string containing the key details of the lock
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append('@');
        sb.append(Integer.toHexString(identityHashCode));
        return sb.toString();
    }
}
