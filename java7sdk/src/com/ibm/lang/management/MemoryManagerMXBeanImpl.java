/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.management.ObjectName;

/**
 * Runtime type for {@link MemoryManagerMXBean}
 * 
 * @author gharley
 * @since 1.5
 */
public class MemoryManagerMXBeanImpl extends DynamicMXBeanImpl implements
        MemoryManagerMXBean {
    protected final String name;

    protected final int id;

    private List<MemoryPoolMXBean> managedPoolList;

    /**
     * Sets the metadata for this bean. 
     * @param objectName
     * @param name 
     * @param id
     * @param memBean 
     */
    MemoryManagerMXBeanImpl(ObjectName objectName, String name, int id, MemoryMXBeanImpl memBean) {
        super(objectName);
        this.name = name;
        this.id = id;
        managedPoolList = new LinkedList<MemoryPoolMXBean>();
        createMemoryPools( id, memBean );
    }
    
    //This instance is only used for ManagementUtils
    private static MemoryManagerMXBeanImpl tempInstance = new MemoryManagerMXBeanImpl();
    
    // This should be only used by ManagementUtils
    MemoryManagerMXBeanImpl() { 
    	super(null);
        id = -1;
        name = null;
    }

  //This method is only used for ManagementUtils
    static MemoryManagerMXBeanImpl getInstanceFromMgmtUtils(){
    	return tempInstance;
    }
    
    /**
     * 
     */
    protected void initializeInfo() {
        setMBeanInfo(ManagementUtils.getMBeanInfo(MemoryManagerMXBean.class
                .getName()));
    }

    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            initializeInfo();
        }
        return info;
    }
    
    /**
     * Instantiate the MemoryPoolMXBeans representing the memory managed by
     * this manager, and store them in the managedPoolList.
     * @param managerID
     * @param memBean
     */
    private native void createMemoryPools(int managerID, MemoryMXBeanImpl memBean);

    private void createMemoryPoolHelper(String name, boolean isHeap, int internalID, MemoryMXBeanImpl memBean) {
        if (isHeap) {
            // Only one Java Heap bean should exist - as a notification thread is created to update it
            // and the underlying native code currently assumes only one such thread would ever exist.
            managedPoolList.add(MemoryPoolMXBeanImpl.getJavaHeapMemoryPoolMXBean(name, internalID, memBean));
        } else {
            managedPoolList.add(new MemoryPoolMXBeanImpl(name, MemoryType.NON_HEAP, internalID, memBean));
        }
    }

    /**
     * Retrieves the list of memory pool beans managed by this manager.
     * 
     * @return the list of <code>MemoryPoolMXBean</code> instances
     */
    List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return managedPoolList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryManagerMXBean#getMemoryPoolNames()
     */
    public String[] getMemoryPoolNames() {
        String[] names = new String[managedPoolList.size()];
		int idx = 0;
		Iterator<MemoryPoolMXBean> iter = managedPoolList.iterator();
		while( iter.hasNext() ) {
			names[idx++] = iter.next().getName();
		}
		return names;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryManagerMXBean#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return <code>true</code> if the memory manager is still valid in the
     *         virtual machine ; otherwise <code>false</code>.
     *         @see #isValid()
     */
    private native boolean isValidImpl();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.MemoryManagerMXBean#isValid()
     */
    public boolean isValid() {
        return this.isValidImpl();
    }
}

/*
 * $Log$
 * Revision 1.6  2005/05/25 20:23:35  gharley
 * Updated unit test bucket now that 5.0 VM available that enables direct testing of the MXBeans. Shook out one or two bugs in the code. Note that OperatingSystemMXBeans are still broken & that there is a problem with MemoryMXBean.getNonHeapmemoryUsage(). Awaiting new VM before tackling these...
 *
 * Revision 1.5  2005/04/29 23:58:49  pschurch
 * Revised and extended memory management beans to support the native implementation.
 *
 * Revision 1.4  2005/02/13 18:19:21  gharley
 * Added in the native method declarations.
 *
 * Revision 1.3  2005/02/11 17:24:36  gharley
 * Removed stubs with bogus values. Now making native calls...
 * Revision 1.2 2005/01/12 21:56:26
 * gharley Now implements DynamicMBean (as an MXBean should be a dynamic MBean).
 * Revision 1.1 2005/01/11 10:56:10 gharley Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
