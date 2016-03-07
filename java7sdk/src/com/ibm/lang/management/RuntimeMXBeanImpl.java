/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.ManagementFactory;
import java.lang.management.ManagementPermission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import com.ibm.oti.vm.VM;

/**
 * Runtime type for {@link java.lang.management.RuntimeMXBean}
 * 
 * @author gharley
 * @since 1.5
 */
public final class RuntimeMXBeanImpl extends DynamicMXBeanImpl implements
		RuntimeMXBean {

	private static RuntimeMXBeanImpl instance = new RuntimeMXBeanImpl();
	
	private static OperatingSystemMXBean os = ExtendedOperatingSystem.getInstance();

	/**
	 * Constructor intentionally private to prevent instantiation by others.
	 * Sets the metadata for this bean.
	 */
	RuntimeMXBeanImpl() {
		super(ManagementUtils.createObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME));
	}
	
	 public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils
                         .getMBeanInfo(java.lang.management.RuntimeMXBean.class.getName()));

        }
        return info;
    }

	/**
	 * Singleton accessor method.
	 * 
	 * @return the <code>RuntimeMXBeanImpl</code> singleton.
	 */
	static RuntimeMXBeanImpl getInstance() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getBootClassPath()
	 */
	public String getBootClassPath() {
		if (!isBootClassPathSupported()) {
			throw new UnsupportedOperationException(
					"VM does not support boot classpath.");
		}

		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkPermission(new ManagementPermission("monitor"));
		}

		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty("sun.boot.class.path");
			}// end method run
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getClassPath()
	 */
	public String getClassPath() {
        return System.getProperty("java.class.path");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getLibraryPath()
	 */
	public String getLibraryPath() {
        return System.getProperty("java.library.path");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getManagementSpecVersion()
	 */
	public String getManagementSpecVersion() {
		return "1.0";
	}

	/**
	 * @return the name of this running virtual machine.
	 * @see #getName()
	 */
	private native String getNameImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getName()
	 */
	public String getName() {
		return this.getNameImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSpecName()
	 */
	public String getSpecName() {
        return System.getProperty("java.vm.specification.name");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSpecVendor()
	 */
	public String getSpecVendor() {
        return System.getProperty("java.vm.specification.vendor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSpecVersion()
	 */
	public String getSpecVersion() {
        return System.getProperty("java.vm.specification.version");
	}

	/**
	 * @return the virtual machine start time in milliseconds.
	 * @see #getStartTime()
	 */
	private native long getStartTimeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getStartTime()
	 */
	public long getStartTime() {
		return this.getStartTimeImpl();
	}

	/**
	 * @return the number of milliseconds the virtual machine has been running.
	 * @see #getUptime()
	 */
	private native long getUptimeImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getUptime()
	 */
	public long getUptime() {
		return this.getUptimeImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getVmName()
	 */
	public String getVmName() {
        return System.getProperty("java.vm.name");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getVmVendor()
	 */
	public String getVmVendor() {
        return System.getProperty("java.vm.vendor");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getVmVersion()
	 */
	public String getVmVersion() {
        return System.getProperty("java.vm.version");
	}

	/**
	 * @return <code>true</code> if supported, <code>false</code> otherwise.
	 * @see #isBootClassPathSupported()
	 */
	private native boolean isBootClassPathSupportedImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#isBootClassPathSupported()
	 */
	public boolean isBootClassPathSupported() {
		return this.isBootClassPathSupportedImpl();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getInputArguments()
	 */
	public List<String> getInputArguments() {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkPermission(new ManagementPermission("monitor"));
		}
		return ManagementUtils.convertStringArrayToList(VM.getVMArgs());
	}
	
	/*
	 * return the ID of Process 
	 */
	public long getProcessID(){
	    return getProcessIDImpl();
	}
	
	private native long getProcessIDImpl();
	
	public double getCPULoad(){
	    return os.getSystemLoadAverage();
	}
	
	public double getVMGeneratedCPULoad(){
	    return os.getSystemLoadAverage()/os.getAvailableProcessors();
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.RuntimeMXBean#getSystemProperties()
	 */
	public Map<String, String> getSystemProperties() {
		Map<String, String> result = new HashMap<String, String>();
        Properties props = System.getProperties();
		Enumeration<?> propNames = props.propertyNames();
		while (propNames.hasMoreElements()) {
			String propName = (String) propNames.nextElement();
			String propValue = props.getProperty(propName);
			result.put(propName, propValue);
		}// end while
		return result;
	}
}

/*
 * $Log$
 * Revision 1.18  2005/07/07 15:44:35  gharley
 * Remove unnecessary PrivilegedAction blocks
 *
 * Revision 1.17  2005/06/30 15:50:30  gharley
 * Wrapped all System.getProperty() calls inside new java.security.PrivilegedAction. Note : all of the properties read in this class are, by default, made readable to all by the java.policy file shipped with the Java SDK so perhaps these new security checks are unnecessary ?
 *
 * Revision 1.16  2005/06/21 09:26:04  gharley
 * Add in security "doPrivileged" code to get System property.
 *
 * Revision 1.15  2005/05/17 22:58:00  pschurch
 * implement getManagementSpecVersion
 *
 * Revision 1.14  2005/04/18 10:41:24  gharley
 * Get bootclasspath using the "semi-standard" system property "sun.boot.class.path".
 *
 * Revision 1.13  2005/04/17 20:47:01  gharley
 * Small fix to remove 5.0 generics warning.
 *
 * Revision 1.12  2005/04/13 00:07:05  pschurch
 * Use existing method to implement getInputArguments
 *
 * Revision 1.11  2005/02/11 17:24:36  gharley
 * Removed stubs with bogus values. Now making native calls...
 *
 * Revision 1.10  2005/02/09 22:26:21  gharley
 * Moved getAttribute into superclass as well as moving the Map->Tabulardata
 * conversion convenience method into ManagementUtils.
 *
 * Revision 1.9  2005/02/04 23:13:55  gharley
 * Added in security permission code : either explicitly or added a comment
 * where an invoked method carries out the check on our behalf.
 *
 * Revision 1.8  2005/02/02 14:09:58  gharley
 * Moved MBeanInfo setup into ManagementUtils on the assumption that the
 * metadata is going to be useful for the proxy bean support.
 *
 * Revision 1.7  2005/01/30 22:26:56  gharley
 * Where possible, return properties available from System.
 *
 * Revision 1.6  2005/01/19 20:46:19  gharley
 * Fixed major bug in the construction of a TabularData object which represents
 * the Map<String, String> of system properties. Plus various minor tweaks to
 * ease the unit testing.
 *
 * Revision 1.5  2005/01/19 12:46:03  gharley
 * Implementation of DynamicMBean behaviour. Extends new base class.
 *
 * Revision 1.4  2005/01/16 22:33:12  gharley
 * Added creation of MBeanInfo to the constructor and returned it in the
 * getMBeanInfo method. Seems to be a lot of duplication here : some
 * refactoring (common base class ?) required soon, methinks.
 * Revision 1.3 2005/01/14 11:23:33 gharley
 * Added singleton enforcement code.
 * 
 * Revision 1.2 2005/01/12 21:56:26 gharley Now implements DynamicMBean (as an
 * MXBean should be a dynamic MBean). Revision 1.1 2005/01/11 10:56:10 gharley
 * Initial upload
 * 
 * Revision 1.1 2005/01/07 10:05:53 gharley Initial creation
 * 
 */
