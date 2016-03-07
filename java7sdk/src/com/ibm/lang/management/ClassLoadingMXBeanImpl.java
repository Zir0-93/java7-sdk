/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ManagementPermission;

/**
 * Runtime type for {@link ClassLoadingMXBean}.
 * <p>
 * There is only ever one instance of this class in a virtual machine.
 * </p>
 * 
 * @author gharley
 * @since 1.5
 */
public final class ClassLoadingMXBeanImpl extends DynamicMXBeanImpl implements
        ClassLoadingMXBean {

    private static ClassLoadingMXBeanImpl instance = new ClassLoadingMXBeanImpl();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    ClassLoadingMXBeanImpl() {
        super(ManagementUtils.createObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME));
    }

    @Override
    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils.getMBeanInfo(ClassLoadingMXBean.class.getName()));
        }
        return info;
    }
    
    /**
     * Singleton accessor method.
     * 
     * @return the <code>ClassLoadingMXBeanImpl</code> singleton.
     */
    static ClassLoadingMXBeanImpl getInstance() {
        return instance;
    }

    /**
     * @return the number of loaded classes
     * @see #getLoadedClassCount()
     */
    private native int getLoadedClassCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#getLoadedClassCount()
     */
    public int getLoadedClassCount() {
        return this.getLoadedClassCountImpl();
    }

    /**
     * @return the total number of classes that have been loaded
     * @see #getTotalLoadedClassCount()
     */
    private native long getTotalLoadedClassCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#getTotalLoadedClassCount()
     */
    public long getTotalLoadedClassCount() {
        return this.getTotalLoadedClassCountImpl();
    }

    /**
     * @return the total number of unloaded classes
     * @see #getUnloadedClassCount()
     */
    private native long getUnloadedClassCountImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#getUnloadedClassCount()
     */
    public long getUnloadedClassCount() {
        return this.getUnloadedClassCountImpl();
    }

    /**
     * @return true if running in verbose mode
     * @see #isVerbose()
     */
    private native boolean isVerboseImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#isVerbose()
     */
    public boolean isVerbose() {
        return this.isVerboseImpl();
    }

    /**
     * @param value true to put the class loading system into verbose
     * mode, false to take the class loading system out of verbose mode.
     * @see #setVerbose(boolean)
     */
    private native void setVerboseImpl(boolean value);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.ClassLoadingMXBean#setVerbose(boolean)
     */
    public void setVerbose(boolean value) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new ManagementPermission("control"));
        }
        this.setVerboseImpl(value);
    }
}

/*
 * $Log$
 * Revision 1.9  2005/02/11 17:24:36  gharley
 * Removed stubs with bogus values. Now making native calls...
 * Revision 1.8 2005/02/09 22:21:32
 * gharley Moved getAttribute and setAttribute into superclass Revision 1.7
 * 2005/02/04 23:13:55 gharley Added in security permission code : either
 * explicitly or added a comment where an invoked method carries out the check
 * on our behalf.
 * 
 * Revision 1.6 2005/02/02 14:09:58 gharley Moved MBeanInfo setup into
 * ManagementUtils on the assumption that the metadata is going to be useful for
 * the proxy bean support.
 * 
 * Revision 1.5 2005/01/19 12:46:03 gharley Implementation of DynamicMBean
 * behaviour. Extends new base class. Revision 1.4 2005/01/16 22:33:12 gharley
 * Added creation of MBeanInfo to the constructor and returned it in the
 * getMBeanInfo method. Seems to be a lot of duplication here : some refactoring
 * (common base class ?) required soon, methinks. Revision 1.3 2005/01/14
 * 11:23:33 gharley Added singleton enforcement code.
 * 
 * Revision 1.2 2005/01/12 21:56:26 gharley Now implements DynamicMBean (as an
 * MXBean should be a dynamic MBean). Revision 1.1 2005/01/11 10:56:10 gharley
 * Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
