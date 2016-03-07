/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Runtime type for {@link CompilationMXBean}
 * 
 * @author gharley
 * @since 1.5
 */
public final class CompilationMXBeanImpl extends DynamicMXBeanImpl implements
        CompilationMXBean {

    private static CompilationMXBeanImpl instance = createInstance();

    /**
     * Conditionally returns the singleton instance of this type of MXBean.
     * 
     * @return if the virtual machine has a compilation system, returns a new
     *         instance of <code>CompilationMXBean</code>, otherwise returns
     *         <code>null</code>.
     */
    private static CompilationMXBeanImpl createInstance() {
        CompilationMXBeanImpl result = null;

        if (isJITEnabled()) {
            result = new CompilationMXBeanImpl();
        }
        return result;
    }

    /**
     * Query whether the VM is running with a JIT compiler enabled.
     * 
     * @return true if a JIT is enabled, false otherwise
     */
    private static native boolean isJITEnabled();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean. 
     */
    CompilationMXBeanImpl() {
        super(ManagementUtils.createObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME));
    }
    
    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils.getMBeanInfo(CompilationMXBean.class
                .getName()));
        }
        return info;
    }
    	     

    /**
     * Singleton accessor method.
     * 
     * @return the <code>ClassLoadingMXBeanImpl</code> singleton.
     */
    static CompilationMXBeanImpl getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.CompilationMXBean#getName()
     */
    public String getName() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("java.compiler");
            }// end method run
        });
    }

    /**
     * @return the compilation time in milliseconds
     * @see #getTotalCompilationTime()
     */
    private native long getTotalCompilationTimeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.CompilationMXBean#getTotalCompilationTime()
     */
    public long getTotalCompilationTime() {
        if (!isCompilationTimeMonitoringSupported()) {
            throw new UnsupportedOperationException(
                    "VM does not support monitoring of compilation time.");
        }
        return this.getTotalCompilationTimeImpl();
    }

    /**
     * @return <code>true</code> if compilation timing is supported, otherwise
     *         <code>false</code>.
     * @see #isCompilationTimeMonitoringSupported()
     */
    private native boolean isCompilationTimeMonitoringSupportedImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.CompilationMXBean#isCompilationTimeMonitoringSupported()
     */
    public boolean isCompilationTimeMonitoringSupported() {
        return this.isCompilationTimeMonitoringSupportedImpl();
    }
}

/*
 * $Log$
 * Revision 1.11  2005/06/21 09:23:33  gharley
 * Add in security "doPrivileged" code to get System property.
 *
 * Revision 1.10  2005/04/13 00:06:08  pschurch
 * Changed getName to get the java.compiler property.
 * Added a native for isJITEnabled and used it to gate the instantiation of this bean.
 *
 * Revision 1.9  2005/02/11 17:24:36  gharley
 * Removed stubs with bogus values. Now making native calls...
 *
 * Revision 1.8  2005/02/09 22:21:49  gharley
 * Moved getAttribute into superclass
 *
 * Revision 1.7  2005/02/02 14:09:58  gharley
 * Moved MBeanInfo setup into ManagementUtils on the assumption that the
 * metadata is going to be useful for the proxy bean support.
 *
 * Revision 1.6  2005/01/30 22:24:48  gharley
 * Quick check on whether call supported by VM or not.
 *
 * Revision 1.5  2005/01/19 12:46:03  gharley
 * Implementation of DynamicMBean behaviour. Extends new base class.
 * Revision 1.4 2005/01/16 22:33:12 gharley
 * Added creation of MBeanInfo to the constructor and returned it in the
 * getMBeanInfo method. Seems to be a lot of duplication here : some refactoring
 * (common base class ?) required soon, methinks.
 * 
 * Revision 1.3 2005/01/14 11:24:42 gharley Added singleton enforcement code.
 * Need to interogate the VM to see if there is a JIT compiler available before
 * the singleton should be created.
 * 
 * Revision 1.2 2005/01/12 21:56:26 gharley Now implements DynamicMBean (as an
 * MXBean should be a dynamic MBean). Revision 1.1 2005/01/11 10:56:10 gharley
 * Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
