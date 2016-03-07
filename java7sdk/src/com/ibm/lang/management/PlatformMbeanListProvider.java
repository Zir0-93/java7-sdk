
/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2012, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.PlatformManagedObject;
import java.util.List;

/**
 * The interface to get all the available MBean implementation and MBean interfaces.
 * 
 */
public interface PlatformMbeanListProvider {
	
	public List<PlatformManagedObject> getAllAvailableMBeans();
	
	public List<Class> getAllAvailableMBeanInterfaces();

}
