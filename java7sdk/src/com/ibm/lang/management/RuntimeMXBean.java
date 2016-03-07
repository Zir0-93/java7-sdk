/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2012, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

/**
 * The IBM-specific interface for the runtime system of the virtual
 * machine.
 */
public interface RuntimeMXBean extends
        java.lang.management.RuntimeMXBean {

	public double getCPULoad();
	
	public long getProcessID();
	
	public double getVMGeneratedCPULoad();

    
}


