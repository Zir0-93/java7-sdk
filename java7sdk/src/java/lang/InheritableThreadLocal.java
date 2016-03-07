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
 * Copyright (c) 1998, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;
import java.lang.ref.*;
import com.ibm.tenant.TenantGlobals;                                            //IBM-multitenancy_management
import com.ibm.tenant.TenantContext;                                            //IBM-multitenancy_management
import com.ibm.tenant.internal.TenantAccessFactory;                             //IBM-multitenancy_management

/**
 * This class extends <tt>ThreadLocal</tt> to provide inheritance of values
 * from parent thread to child thread: when a child thread is created, the
 * child receives initial values for all inheritable thread-local variables
 * for which the parent has values.  Normally the child's values will be
 * identical to the parent's; however, the child's value can be made an
 * arbitrary function of the parent's by overriding the <tt>childValue</tt>
 * method in this class.
 *
 * <p>Inheritable thread-local variables are used in preference to
 * ordinary thread-local variables when the per-thread-attribute being
 * maintained in the variable (e.g., User ID, Transaction ID) must be
 * automatically transmitted to any child threads that are created.
 *
 * @author  Josh Bloch and Doug Lea
 * @see     ThreadLocal
 * @since   1.2
 */

public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    /**
     * Computes the child's initial value for this inheritable thread-local
     * variable as a function of the parent's value at the time the child
     * thread is created.  This method is called from within the parent
     * thread before the child is started.
     * <p>
     * This method merely returns its input argument, and should be overridden
     * if a different behavior is desired.
     *
     * @param parentValue the parent thread's value
     * @return the child thread's initial value
     */
    protected T childValue(T parentValue) {
        return parentValue;
    }

    /**
     * Get the map associated with a ThreadLocal.
     *
     * @param t the current thread
     */
    ThreadLocalMap getMap(Thread t) {
       if (TenantGlobals.isTenantEnabled()) {                                   //IBM-multitenancy_management
            TenantContext tc = TenantContext.current();                         //IBM-multitenancy_management
            if (tc != null) {                                                   //IBM-multitenancy_management
                Object oMap = TenantAccessFactory.getAccess()                   //IBM-multitenancy_management
                         .safeReadObj(tc, t.inheritableThreadLocalsIndex);      //IBM-multitenancy_management
                if (oMap != null && oMap instanceof ThreadLocalMap) {           //IBM-multitenancy_management
                    return (ThreadLocalMap) oMap;                               //IBM-multitenancy_management
                }                                                               //IBM-multitenancy_management
                return null;                                                    //IBM-multitenancy_management
            }                                                                   //IBM-multitenancy_management
       }                                                                        //IBM-multitenancy_management
       return t.inheritableThreadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the table.
     * @param map the map to store.
     */
    void createMap(Thread t, T firstValue) {
        if (TenantGlobals.isTenantEnabled()) {                                  //IBM-multitenancy_management
            TenantContext tc = TenantContext.current();                         //IBM-multitenancy_management
            if (tc != null) {                                                   //IBM-multitenancy_management
                ThreadLocalMap map = new ThreadLocalMap(this, firstValue);      //IBM-multitenancy_management
                TenantAccessFactory.getAccess().ensureAllocationAndWriteObj(tc, 
                                          t.inheritableThreadLocalsIndex, map); //IBM-multitenancy_management
                return;                                                         //IBM-multitenancy_management
            }                                                                   //IBM-multitenancy_management
        }                                                                       //IBM-multitenancy_management
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
//IBM-multitenancy_management
