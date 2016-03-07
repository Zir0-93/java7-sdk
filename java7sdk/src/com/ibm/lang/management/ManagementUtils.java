/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2008, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.LockInfo;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.PlatformLoggingMXBean;
import java.lang.management.PlatformManagedObject;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.LoggingMXBean;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationEmitter;
import javax.management.StandardMBean;
import javax.management.StandardEmitterMBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
 * Support methods for com.ibm.lang.management classes.
 * 
 * @author gharley
 */
public class ManagementUtils {

    private static Map<String, MBeanInfo> infoMap = buildInfoMap();

    private static CompositeType MEMORYUSAGE_COMPOSITETYPE;

    private static CompositeType MEMORYNOTIFICATIONINFO_COMPOSITETYPE;

    private static CompositeType PROCESSINGCAPACITYNOTIFICATIONINFO_COMPOSITETYPE;

    private static CompositeType TOTALPHYSICALMEMORYNOTIFICATIONINFO_COMPOSITETYPE;

    private static CompositeType AVAILABLEPROCESSORSNOTIFICATIONINFO_COMPOSITETYPE;

    private static CompositeType THREADINFO_COMPOSITETYPE;

    private static CompositeType MONITORINFO_COMPOSITETYPE;

    private static CompositeType LOCKINFO_COMPOSITETYPE;

    private static CompositeType STACKTRACEELEMENT_COMPOSITETYPE;

    /**
     * System property setting used to decide if non-fatal exceptions should be
     * written out to console.
     */
    public static final boolean VERBOSE_MODE = checkVerboseProperty();

    /**
     * @return the singleton <code>ClassLoadingMXBean</code> instance.
     */
    public static ClassLoadingMXBeanImpl getClassLoadingBean() {
        return ClassLoadingMXBeanImpl.getInstance();
    }

    /**
     * @return boolean indication of whether or not the system property
     *         <code>com.ibm.lang.management.verbose</code> has been set.
     */
    private static boolean checkVerboseProperty() {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return System.getProperty("com.ibm.lang.management.verbose") != null;
            }// end method run
        });
    }

    /**
     * Convenenience method to return the {@link MBeanInfo} object that
     * corresponds to the specified <code>MXBean</code> type.
     * 
     * @param name
     *            the fully qualified name of an <code>MXBean</code>
     * @return if <code>name</code> has the value of a known
     *         <code>MXBean</code> type then returns the
     *         <code>MBeanInfo</code> meta data for that type. If
     *         <code>name</code> is not the name of a known
     *         <code>MXBean</code> kind then returns <code>null</code>.
     */
    static MBeanInfo getMBeanInfo(String name) {
    	Class MBeanInterface = null;
    	Class commonInterface = null;
    	Class ibmInterface = null;
    	boolean isIBMInterface = false;
    	StandardMBean smb = null;
    	MBeanInfo mbinfo = null;
    	try{
    		if(!infoMap.containsKey(name)){
        		//add the MBean into infoMap
                HashMap<String,Class> commonInterfaces = getLocalAvailableInterfaces();
                HashMap<String,Class> ibmInterfaces = getLocalIBMAvailableInterfaces();
                HashMap<String,Object> impls = getLocalAvailableImpls();
                
                if(commonInterfaces.containsKey(name) || ibmInterfaces.containsKey(name)){
                	commonInterface = commonInterfaces.get(name);
                	ibmInterface = ibmInterfaces.get(name);
                	if(ibmInterface != null){//this is a com.ibm.lang.management.interface
                		isIBMInterface = true;
                		MBeanInterface = ibmInterface;
                	}else{//this is a java.lang.management interface
                		MBeanInterface = commonInterface;
                	}
                	
                	smb = new StandardMBean(impls.get(name), MBeanInterface, true);
                	if (NotificationBroadcaster.class.isInstance(impls.get(name))) {
                		MBeanInfo mbi = smb.getMBeanInfo();
                	    mbinfo = new MBeanInfo(mbi.getClassName(), mbi.getDescription(), mbi.getAttributes(),
                            mbi.getConstructors(), mbi.getOperations(),
                            ((NotificationBroadcaster)impls.get(name)).getNotificationInfo(),
                            mbi.getDescriptor());
                	    } else {
                	    	mbinfo = smb.getMBeanInfo();
                	    	}
                	infoMap.put(MBeanInterface.getName(), mbinfo);
                	if(isIBMInterface){
                		infoMap.put(commonInterface.getName(), mbinfo);
                	}
                }else{   	
                    List<Class> interfacesFromVMUtils = getInterfacesFromVMUtils();
                    if(interfacesFromVMUtils != null){
                        int index = -1;
                        for(int i = 0; i < interfacesFromVMUtils.size(); i++){
                        	if(name.equalsIgnoreCase(interfacesFromVMUtils.get(i).getName())){
                        		index = i;
                        	}
                        }               
                        if(index != -1){
                        	List<PlatformManagedObject> beansFromVMUtils = getBeansFromVMUtils();
                        	MBeanInterface = interfacesFromVMUtils.get(index);
                        	smb = new StandardMBean(beansFromVMUtils.get(index), MBeanInterface, true);
                        	if (NotificationBroadcaster.class.isInstance(beansFromVMUtils.get(index))) {
                        		MBeanInfo mbi = smb.getMBeanInfo();
                        	    mbinfo = new MBeanInfo(mbi.getClassName(), mbi.getDescription(), mbi.getAttributes(),
                                    mbi.getConstructors(), mbi.getOperations(),
                                    ((NotificationBroadcaster)beansFromVMUtils.get(index)).getNotificationInfo(),
                                    mbi.getDescriptor());
                        	    } else {
                        	    	mbinfo = smb.getMBeanInfo();
                        	    }
                        	infoMap.put(MBeanInterface.getName(), mbinfo);
                        }
                    }

                }
    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}	
           
    	return infoMap.get(name);
    }

    private static List<Class> getInterfacesFromVMUtils(){
    	List<Class> interfacesFromVMUtils = null;
    	//Get a list of all available MXBeans objects using VmManagementUtils
        try{
            //get com.ibm.lang.management.VmManagementUtils from vm.jar
            Class object  =  Class.forName("com.ibm.lang.management.VmManagementUtils");
            PlatformMbeanListProvider pmp = (PlatformMbeanListProvider)object.newInstance();
            interfacesFromVMUtils = pmp.getAllAvailableMBeanInterfaces();
        }catch (ClassNotFoundException e) {
        } catch (IllegalAccessException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
        } catch (InstantiationException e) {
        	if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
        }
        return interfacesFromVMUtils;
        
    	
    }
    
    private static List<PlatformManagedObject> getBeansFromVMUtils(){
    	List<PlatformManagedObject> beansFromVMUtils = null;
    	//Get a list of all available MXBeans objects using VmManagementUtils
    	try{
    		//get com.ibm.lang.management.VmManagementUtils from vm.jar
            Class object  =  Class.forName("com.ibm.lang.management.VmManagementUtils");
            PlatformMbeanListProvider pmp = (PlatformMbeanListProvider)object.newInstance();
            beansFromVMUtils = pmp.getAllAvailableMBeans();
    	}catch (ClassNotFoundException e) { 
        	//if com.ibm.lang.management.VmManagementUtils is not found, no registration is done
    	} catch (IllegalAccessException e) {
    		if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
		} catch (InstantiationException e) {
			if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }
		}
		return beansFromVMUtils;
    }
    /**
     * Builds a <code>Map</code> of all the {@link MBeanInfo} instances for
     * each of the platform beans. The map is keyed off the name of each bean
     * type.
     * 
     * @return a <code>Map</code> of all the platform beans'
     *         <code>MBeanInfo</code> instances.
     */
    private static Map<String, MBeanInfo> buildInfoMap() {
    	HashMap<String, MBeanInfo> map = new HashMap<String, MBeanInfo>();
    	return map;
    }
    
    private static HashMap<String,Class> getLocalAvailableInterfaces() {
        HashMap<String,Class> ret = new HashMap<String,Class>();
        ret.put("java.lang.management.ClassLoadingMXBean", ClassLoadingMXBean.class);
        ret.put("java.lang.management.CompilationMXBean", CompilationMXBean.class);
        ret.put("java.util.logging.LoggingMXBean", LoggingMXBean.class);
        ret.put("java.lang.management.MemoryManagerMXBean", MemoryManagerMXBean.class);
        ret.put("java.lang.management.GarbageCollectorMXBean", GarbageCollectorMXBean.class);
        ret.put("java.lang.management.MemoryMXBean", MemoryMXBean.class);
        ret.put("java.lang.management.MemoryPoolMXBean", MemoryPoolMXBean.class);
        ret.put("java.lang.management.OperatingSystemMXBean", OperatingSystemMXBean.class);
        ret.put("java.lang.management.RuntimeMXBean", RuntimeMXBean.class);
        ret.put("java.lang.management.ThreadMXBean", ThreadMXBean.class);
        
        ret.put("com.ibm.lang.management.GarbageCollectorMXBean", GarbageCollectorMXBean.class);
        ret.put("com.ibm.lang.management.MemoryMXBean", MemoryMXBean.class);
        ret.put("com.ibm.lang.management.MemoryPoolMXBean", MemoryPoolMXBean.class);
        ret.put("com.ibm.lang.management.OperatingSystemMXBean", OperatingSystemMXBean.class);
        ret.put("com.ibm.lang.management.RuntimeMXBean", RuntimeMXBean.class);
        return ret;
    }
    
    private static HashMap<String,Class> getLocalIBMAvailableInterfaces() {
    	HashMap<String,Class> ret = new HashMap<String,Class>();
        ret.put("java.lang.management.GarbageCollectorMXBean", com.ibm.lang.management.GarbageCollectorMXBean.class);
        ret.put("java.lang.management.MemoryMXBean", com.ibm.lang.management.MemoryMXBean.class);
        ret.put("java.lang.management.MemoryPoolMXBean", com.ibm.lang.management.MemoryPoolMXBean.class);
        ret.put("java.lang.management.OperatingSystemMXBean", com.ibm.lang.management.OperatingSystemMXBean.class);
        ret.put("java.lang.management.RuntimeMXBean", com.ibm.lang.management.RuntimeMXBean.class);
        
        ret.put("com.ibm.lang.management.GarbageCollectorMXBean", com.ibm.lang.management.GarbageCollectorMXBean.class);
        ret.put("com.ibm.lang.management.MemoryMXBean", com.ibm.lang.management.MemoryMXBean.class);
        ret.put("com.ibm.lang.management.MemoryPoolMXBean", com.ibm.lang.management.MemoryPoolMXBean.class);
        ret.put("com.ibm.lang.management.OperatingSystemMXBean", com.ibm.lang.management.OperatingSystemMXBean.class);
        ret.put("com.ibm.lang.management.RuntimeMXBean", com.ibm.lang.management.RuntimeMXBean.class);
        return ret;
    }


    private static HashMap<String,Object> getLocalAvailableImpls() {
    	HashMap<String,Object> ret = new HashMap<String,Object>();
        ret.put("java.lang.management.ClassLoadingMXBean", ClassLoadingMXBeanImpl.getInstance());
        ret.put("java.lang.management.CompilationMXBean", CompilationMXBeanImpl.getInstance());
        ret.put("java.util.logging.LoggingMXBean", LoggingMXBeanImpl.getInstance());
        ret.put("java.lang.management.MemoryManagerMXBean", MemoryManagerMXBeanImpl.getInstanceFromMgmtUtils());
        ret.put("java.lang.management.GarbageCollectorMXBean", GarbageCollectorMXBeanImpl.getInstanceFromMgmtUtils());
        ret.put("java.lang.management.MemoryMXBean", MemoryMXBeanImpl.getInstance());
        ret.put("java.lang.management.MemoryPoolMXBean", MemoryPoolMXBeanImpl.getInstanceFromMgmtUtils());
        ret.put("java.lang.management.OperatingSystemMXBean", ExtendedOperatingSystem.getInstance());
        ret.put("java.lang.management.RuntimeMXBean", RuntimeMXBeanImpl.getInstance());
        ret.put("java.lang.management.ThreadMXBean", ThreadMXBeanImpl.getInstance());
        
        ret.put("com.ibm.lang.management.GarbageCollectorMXBean", GarbageCollectorMXBeanImpl.getInstanceFromMgmtUtils());
        ret.put("com.ibm.lang.management.MemoryMXBean", MemoryMXBeanImpl.getInstance());
        ret.put("com.ibm.lang.management.MemoryPoolMXBean", MemoryPoolMXBeanImpl.getInstanceFromMgmtUtils());
        ret.put("com.ibm.lang.management.OperatingSystemMXBean", ExtendedOperatingSystem.getInstance());
        ret.put("com.ibm.lang.management.RuntimeMXBean", RuntimeMXBeanImpl.getInstance());
        return ret;
    }

    

    /**
     * @return the singleton <code>MemoryMXBean</code> instance.
     */
    public static MemoryMXBeanImpl getMemoryBean() {
        return MemoryMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>ThreadMXBean</code> instance.
     */
    public static ThreadMXBeanImpl getThreadBean() {
        return ThreadMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>RuntimeMXBean</code> instance.
     */
    public static RuntimeMXBeanImpl getRuntimeBean() {
        return RuntimeMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>RuntimeMXBean</code> instance.
     */
    public static ExtendedOperatingSystem getOperatingSystemBean() {
        return ExtendedOperatingSystem.getInstance();
    }

    /**
     * @return the singleton <code>CompilationMXBean</code> if available.
     */
    public static CompilationMXBeanImpl getCompliationBean() {
        return CompilationMXBeanImpl.getInstance();
    }

    /**
     * @return the singleton <code>LoggingMXBean</code> instance.
     */
    public static LoggingMXBeanImpl getLoggingBean() {
        return LoggingMXBeanImpl.getInstance();
    }

    /**
     * Returns a list of all of the instances of {@link MemoryManagerMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * 
     * @return a list of all known <code>MemoryManagerMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return new LinkedList<MemoryManagerMXBean>(getMemoryBean()
                .getMemoryManagerMXBeans());
    }

    /**
     * Returns a list of all of the instances of {@link MemoryPoolMXBean}in
     * this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * 
     * @return a list of all known <code>MemoryPoolMXBean</code> s in this
     *         virtual machine.
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        Set<MemoryPoolMXBean> set = new HashSet<MemoryPoolMXBean>();
        List<MemoryPoolMXBean> result = new LinkedList<MemoryPoolMXBean>();

        for (MemoryManagerMXBean bean : getMemoryManagerMXBeans()) {
            MemoryManagerMXBeanImpl beanImpl = (MemoryManagerMXBeanImpl) bean;
            for (MemoryPoolMXBean pool : beanImpl.getMemoryPoolMXBeans()) {
                if (!set.contains(pool)) {
                    result.add(pool);
                    set.add(pool);
                }
            }
        }
        return result;
    }

    /**
     * Returns a list of all of the instances of {@link GarbageCollectorMXBean}
     * in this virtual machine. Owing to the dynamic nature of this kind of
     * <code>MXBean</code>, it is possible that instances may be created or
     * destroyed between the invocation and return of this method.
     * 
     * @return a list of all known <code>GarbageCollectorMXBean</code> s in
     *         this virtual machine.
     */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        List<GarbageCollectorMXBean> result = new LinkedList<GarbageCollectorMXBean>();
        Iterator<MemoryManagerMXBean> iter = getMemoryBean()
                .getMemoryManagerMXBeans().iterator();
        while (iter.hasNext()) {
            MemoryManagerMXBean b = iter.next();
            if (b instanceof GarbageCollectorMXBean) {
                result.add((GarbageCollectorMXBean) b);
            }
        }
        return result;
    }
    
    public static List<BufferPoolMXBean> getBufferPoolMXBeans() {
        return new LinkedList<BufferPoolMXBean>(BufferPoolMXBeanImpl
                .getBufferPoolMXBeans());
    }

    /**
     * Returns a list of all available MXBeans objects (instances of {@link PlatformManagedObject}).
     *
     * @return list of available <code>PlatformManagedObject</code>.
     */
    public static List<PlatformManagedObject> getAllAvailableMXBeans() {
        // We use a set here to avoid adding duplicate entries in the list, as
        // getMemoryManagerMXBeans() is a superset of getGarbageCollectorMXBeans().
        Set<PlatformManagedObject> beanSet = new HashSet<PlatformManagedObject>();

        beanSet.add(getClassLoadingBean());
        beanSet.add(getCompliationBean());
        beanSet.add(getLoggingBean());
        beanSet.add(getMemoryBean());
        beanSet.add(getThreadBean());
        beanSet.add(getRuntimeBean());
        beanSet.add(getOperatingSystemBean());
        
		for (PlatformManagedObject bean : getBufferPoolMXBeans()) {
			beanSet.add(bean);
		}
        
        for (PlatformManagedObject bean : getMemoryPoolMXBeans()) {
            beanSet.add(bean);
        }

        for (PlatformManagedObject bean : getGarbageCollectorMXBeans()) {
            beanSet.add(bean);
        }

        for (PlatformManagedObject bean : getMemoryManagerMXBeans()) {
            beanSet.add(bean);
        }

        // Remove the null entry, if any (e.g. compilation bean may be null).
        beanSet.remove(null);

        return new ArrayList<PlatformManagedObject>(beanSet);
    }

    /**
     * Throws an {@link IllegalArgumentException}if the {@link CompositeData}
     * argument <code>cd</code> contains attributes that are not of the exact
     * types specified in the <code>expectedTypes</code> argument. The
     * attribute types of <code>cd</code> must also match the order of types
     * in <code>expectedTypes</code>.
     * 
     * @param cd
     *            a <code>CompositeData</code> object
     * @param expectedNames
     *            an array of expected attribute names
     * @param expectedTypes
     *            an array of type names
     */
    public static void verifyFieldTypes(CompositeData cd,
            String[] expectedNames, String[] expectedTypes) {
        Object[] allVals = cd.getAll(expectedNames);
        // Check that the number of elements match
        if (allVals.length != expectedTypes.length) {
            throw new IllegalArgumentException(
                    "CompositeData does not contain the expected number of attributes.");
        }

        // Type of corresponding elements must be the same
        for (int i = 0; i < allVals.length; i++) {
            String expectedType = expectedTypes[i];
            Object actualVal = allVals[i];
            // It is permissible that a value in the CompositeData object is
            // null in which case we cannot test its type. Move on.
            if (actualVal == null) {
                continue;
            }
            String actualType = actualVal.getClass().getName();
            if (!actualType.equals(expectedType)) {
                // Handle CompositeData and CompositeDataSupport
                if (expectedType.equals(CompositeData.class.getName())) {
                    if (actualVal instanceof CompositeData) {
                        continue;
                    }
                } else {
                    throw new IllegalArgumentException(
                            "CompositeData contains an attribute not of expected type. "
                                    + "Expected " + expectedType + ", found "
                                    + actualType);
                }
            }
        }// end for
    }

    /**
     * Throws an {@link IllegalArgumentException}if the {@link CompositeData}
     * argument <code>cd</code> does not have any of the attributes named in
     * the <code>expected</code> array of strings.
     * 
     * @param cd
     *            a <code>CompositeData</code> object
     * @param expected
     *            an array of attribute names expected in <code>cd</code>.
     */
    public static void verifyFieldNames(CompositeData cd, String[] expected) {
        for (int i = 0; i < expected.length; i++) {
            if (!cd.containsKey(expected[i])) {
                throw new IllegalArgumentException(
                        "CompositeData object does not contain expected key : " //$NON-NLS-1$
                                + expected[i]);
            }
        }// end for all elements in expected
    }

    /**
     * Throws an {@link IllegalArgumentException}if the {@link CompositeData}
     * argument <code>cd</code> does not have the number of attributes
     * specified in <code>i</code>.
     * 
     * @param cd
     *            a <code>CompositeData</code> object
     * @param i
     *            the number of expected attributes in <code>cd</code>
     */
    public static void verifyFieldNumber(CompositeData cd, int i) {
        if (cd == null) {
            throw new NullPointerException("Null CompositeData");
        }
        if (cd.values().size() != i) {
            throw new IllegalArgumentException(
                    "CompositeData object does not have the expected number of attributes"); //$NON-NLS-1$
        }
    }

    /**
     * @param usage
     *            a {@link MemoryUsage}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>usage</code> object.
     */
    public static CompositeData toMemoryUsageCompositeData(MemoryUsage usage) {
        // Bail out early on null input.
        if (usage == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "init", "used", "committed", "max" };
        Object[] values = { new Long(usage.getInit()),
                new Long(usage.getUsed()), new Long(usage.getCommitted()),
                new Long(usage.getMax()) };
        CompositeType cType = getMemoryUsageCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @return an instance of {@link CompositeType}for the {@link MemoryUsage}
     *         class.
     */
    public static CompositeType getMemoryUsageCompositeType() {
        if (MEMORYUSAGE_COMPOSITETYPE == null) {
            String[] typeNames = { "init", "used", "committed", "max" };
            String[] typeDescs = { "init", "used", "committed", "max" };
            OpenType[] typeTypes = { SimpleType.LONG, SimpleType.LONG,
                    SimpleType.LONG, SimpleType.LONG };
            try {
                MEMORYUSAGE_COMPOSITETYPE = new CompositeType(MemoryUsage.class
                        .getName(), MemoryUsage.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return MEMORYUSAGE_COMPOSITETYPE;
    }

    /**
     * @param info
     *            a {@link java.lang.management.MemoryNotificationInfo}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toMemoryNotificationInfoCompositeData(
            MemoryNotificationInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "poolName", "usage", "count" };
        Object[] values = { new String(info.getPoolName()),
                toMemoryUsageCompositeData(info.getUsage()),
                new Long(info.getCount()) };
        CompositeType cType = getMemoryNotificationInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link MemoryNotificationInfo}class.
     */
    private static CompositeType getMemoryNotificationInfoCompositeType() {
        if (MEMORYNOTIFICATIONINFO_COMPOSITETYPE == null) {
            String[] typeNames = { "poolName", "usage", "count" };
            String[] typeDescs = { "poolName", "usage", "count" };
            OpenType[] typeTypes = { SimpleType.STRING,
                    getMemoryUsageCompositeType(), SimpleType.LONG };
            try {
                MEMORYNOTIFICATIONINFO_COMPOSITETYPE = new CompositeType(
                        MemoryNotificationInfo.class.getName(),
                        MemoryNotificationInfo.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return MEMORYNOTIFICATIONINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link ProcessingCapacityNotificationInfo} class.
     */
    private static CompositeType getProcessingCapacityNotificationInfoCompositeType() {
        if (PROCESSINGCAPACITYNOTIFICATIONINFO_COMPOSITETYPE == null) {
            String[] typeNames = { "newProcessingCapacity" };
            String[] typeDescs = { "newProcessingCapacity" };
            OpenType[] typeTypes = { SimpleType.INTEGER };
            try {
                PROCESSINGCAPACITYNOTIFICATIONINFO_COMPOSITETYPE = new CompositeType(
                        ProcessingCapacityNotificationInfo.class.getName(),
                        ProcessingCapacityNotificationInfo.class.getName(),
                        typeNames, typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return PROCESSINGCAPACITYNOTIFICATIONINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link TotalPhysicalMemoryNotificationInfo} class.
     */
    private static CompositeType getTotalPhysicalMemoryNotificationInfoCompositeType() {
        if (TOTALPHYSICALMEMORYNOTIFICATIONINFO_COMPOSITETYPE == null) {
            String[] typeNames = { "newTotalPhysicalMemory" };
            String[] typeDescs = { "newTotalPhysicalMemory" };
            OpenType[] typeTypes = { SimpleType.LONG };
            try {
                TOTALPHYSICALMEMORYNOTIFICATIONINFO_COMPOSITETYPE = new CompositeType(
                        TotalPhysicalMemoryNotificationInfo.class.getName(),
                        TotalPhysicalMemoryNotificationInfo.class.getName(),
                        typeNames, typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return TOTALPHYSICALMEMORYNOTIFICATIONINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link AvailableProcessorsNotificationInfo} class.
     */
    private static CompositeType getAvailableProcessorsNotificationInfoCompositeType() {
        if (AVAILABLEPROCESSORSNOTIFICATIONINFO_COMPOSITETYPE == null) {
            String[] typeNames = { "newAvailableProcessors" };
            String[] typeDescs = { "newAvailableProcessors" };
            OpenType[] typeTypes = { SimpleType.INTEGER };
            try {
                AVAILABLEPROCESSORSNOTIFICATIONINFO_COMPOSITETYPE = new CompositeType(
                        AvailableProcessorsNotificationInfo.class.getName(),
                        AvailableProcessorsNotificationInfo.class.getName(),
                        typeNames, typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return AVAILABLEPROCESSORSNOTIFICATIONINFO_COMPOSITETYPE;
    }

    /**
     * @param info
     *            a {@link ProcessingCapacityNotificationInfo} object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toProcessingCapacityNotificationInfoCompositeData(
            ProcessingCapacityNotificationInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "newProcessingCapacity" };
        Object[] values = { new Integer(info.getNewProcessingCapacity()) };
        CompositeType cType = getProcessingCapacityNotificationInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param info
     *            a {@link TotalPhysicalMemoryNotificationInfo} object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toTotalPhysicalMemoryNotificationInfoCompositeData(
            TotalPhysicalMemoryNotificationInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "newTotalPhysicalMemory" };
        Object[] values = { new Long(info.getNewTotalPhysicalMemory()) };
        CompositeType cType = getTotalPhysicalMemoryNotificationInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param info
     *            a {@link AvailableProcessorsNotificationInfo} object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toAvailableProcessorsNotificationInfoCompositeData(
            AvailableProcessorsNotificationInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "newAvailableProcessors" };
        Object[] values = { new Integer(info.getNewAvailableProcessors()) };
        CompositeType cType = getAvailableProcessorsNotificationInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param info
     *            a {@link ThreadInfo}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>info</code> object.
     */
    public static CompositeData toThreadInfoCompositeData(ThreadInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;

        // Deal with the array types first
        StackTraceElement[] st = info.getStackTrace();
        CompositeData[] stArray = new CompositeData[st.length];
        for (int i = 0; i < st.length; i++) {
            stArray[i] = toStackTraceElementCompositeData(st[i]);
        }// end for all stack trace elements

        MonitorInfo[] lockedMonitors = info.getLockedMonitors();
        CompositeData[] lmArray = new CompositeData[lockedMonitors.length];
        for (int i = 0; i < lmArray.length; i++) {
            lmArray[i] = toMonitorInfoCompositeData(lockedMonitors[i]);
        }// end for all locked monitors

        LockInfo[] lockedSynchronizers = info.getLockedSynchronizers();
        CompositeData[] lsArray = new CompositeData[lockedSynchronizers.length];
        for (int i = 0; i < lsArray.length; i++) {
            lsArray[i] = toLockInfoCompositeData(lockedSynchronizers[i]);
        }// end for all locked synchronizers

        String[] names = { "threadId", "threadName", "threadState",
                "suspended", "inNative", "blockedCount", "blockedTime",
                "waitedCount", "waitedTime", "lockInfo", "lockName",
                "lockOwnerId", "lockOwnerName", "stackTrace", "lockedMonitors",
                "lockedSynchronizers" };
        Object[] values = {
                new Long(info.getThreadId()),
                new String(info.getThreadName()),
                new String(info.getThreadState().name()),
                new Boolean(info.isSuspended()),
                new Boolean(info.isInNative()),
                new Long(info.getBlockedCount()),
                new Long(info.getBlockedTime()),
                new Long(info.getWaitedCount()),
                new Long(info.getWaitedTime()),
                info.getLockInfo() != null ? toLockInfoCompositeData(info
                        .getLockInfo()) : null,
                info.getLockName() != null ? new String(info.getLockName())
                        : null,
                new Long(info.getLockOwnerId()),
                info.getLockOwnerName() != null ? new String(info
                        .getLockOwnerName()) : null, stArray, lmArray, lsArray };
        CompositeType cType = getThreadInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param info
     *            a <code>MonitorInfo</code> object
     * @return a new <code>CompositeData</code> instance that represents the
     *         supplied <code>info</code> object.
     */
    public static CompositeData toMonitorInfoCompositeData(MonitorInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "className", "identityHashCode", "lockedStackFrame",
                "lockedStackDepth" };
        StackTraceElement frame = info.getLockedStackFrame();
        CompositeData frameCD = toStackTraceElementCompositeData(frame);
        Object[] values = { new String(info.getClassName()),
                new Integer(info.getIdentityHashCode()), frameCD,
                new Integer(info.getLockedStackDepth()) };
        CompositeType cType = getMonitorInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param info
     *            a <code>LockInfo</code> object
     * @return a new <code>CompositeData</code> instance that represents the
     *         supplied <code>info</code> object.
     */
    public static CompositeData toLockInfoCompositeData(LockInfo info) {
        // Bail out early on null input.
        if (info == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "className", "identityHashCode" };
        Object[] values = { new String(info.getClassName()),
                new Integer(info.getIdentityHashCode()) };
        CompositeType cType = getLockInfoCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @param element
     *            a {@link StackTraceElement}object.
     * @return a {@link CompositeData}object that represents the supplied
     *         <code>element</code> object.
     */
    public static CompositeData toStackTraceElementCompositeData(
            StackTraceElement element) {
        // Bail out early on null input.
        if (element == null) {
            return null;
        }

        CompositeData result = null;
        String[] names = { "className", "methodName", "fileName", "lineNumber",
                "nativeMethod" };

        // CMVC 92227 - a file name of null is permissable
        String fileName = element.getFileName();
        String fileNameValue = (fileName == null) ? null : new String(fileName);

        Object[] values = { new String(element.getClassName()),
                new String(element.getMethodName()), fileNameValue,
                new Integer(element.getLineNumber()),
                new Boolean(element.isNativeMethod()) };
        CompositeType cType = getStackTraceElementCompositeType();
        try {
            result = new CompositeDataSupport(cType, names, values);
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * @return an instance of {@link CompositeType} for the {@link MonitorInfo}
     *         class
     */
    private static CompositeType getMonitorInfoCompositeType() {
        if (MONITORINFO_COMPOSITETYPE == null) {
            try {
                String[] typeNames = { "className", "identityHashCode",
                        "lockedStackFrame", "lockedStackDepth" };
                String[] typeDescs = { "className", "identityHashCode",
                        "lockedStackFrame", "lockedStackDepth" };
                OpenType[] typeTypes = { SimpleType.STRING, SimpleType.INTEGER,
                        getStackTraceElementCompositeType(), SimpleType.INTEGER };
                MONITORINFO_COMPOSITETYPE = new CompositeType(MonitorInfo.class
                        .getName(), MonitorInfo.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return MONITORINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType} for the {@link LockInfo}
     *         class
     */
    public static CompositeType getLockInfoCompositeType() {
        if (LOCKINFO_COMPOSITETYPE == null) {
            try {
                String[] typeNames = { "className", "identityHashCode" };
                String[] typeDescs = { "className", "identityHashCode" };
                OpenType[] typeTypes = { SimpleType.STRING, SimpleType.INTEGER };
                LOCKINFO_COMPOSITETYPE = new CompositeType(LockInfo.class
                        .getName(), LockInfo.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return LOCKINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType} for the {@link ThreadInfo}
     *         class.
     */
    public static CompositeType getThreadInfoCompositeType() {
        if (THREADINFO_COMPOSITETYPE == null) {
            try {
                String[] typeNames = { "threadId", "threadName", "threadState",
                        "suspended", "inNative", "blockedCount", "blockedTime",
                        "waitedCount", "waitedTime", "lockInfo", "lockName",
                        "lockOwnerId", "lockOwnerName", "stackTrace",
                        "lockedMonitors", "lockedSynchronizers" };
                String[] typeDescs = { "threadId", "threadName", "threadState",
                        "suspended", "inNative", "blockedCount", "blockedTime",
                        "waitedCount", "waitedTime", "lockInfo", "lockName",
                        "lockOwnerId", "lockOwnerName", "stackTrace",
                        "lockedMonitors", "lockedSynchronizers" };
                OpenType[] typeTypes = {
                        SimpleType.LONG,
                        SimpleType.STRING,
                        SimpleType.STRING,
                        SimpleType.BOOLEAN,
                        SimpleType.BOOLEAN,
                        SimpleType.LONG,
                        SimpleType.LONG,
                        SimpleType.LONG,
                        SimpleType.LONG,
                        getLockInfoCompositeType(),
                        SimpleType.STRING,
                        SimpleType.LONG,
                        SimpleType.STRING,
                        new ArrayType<CompositeType>(1,
                                getStackTraceElementCompositeType()),
                        new ArrayType<CompositeType>(1,
                                getMonitorInfoCompositeType()),
                        new ArrayType<CompositeType>(1,
                                getLockInfoCompositeType()) };
                THREADINFO_COMPOSITETYPE = new CompositeType(ThreadInfo.class
                        .getName(), ThreadInfo.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return THREADINFO_COMPOSITETYPE;
    }

    /**
     * @return an instance of {@link CompositeType}for the
     *         {@link StackTraceElement}class.
     */
    private static CompositeType getStackTraceElementCompositeType() {
        if (STACKTRACEELEMENT_COMPOSITETYPE == null) {
            String[] typeNames = { "className", "methodName", "fileName",
                    "lineNumber", "nativeMethod" };
            String[] typeDescs = { "className", "methodName", "fileName",
                    "lineNumber", "nativeMethod" };
            OpenType[] typeTypes = { SimpleType.STRING, SimpleType.STRING,
                    SimpleType.STRING, SimpleType.INTEGER, SimpleType.BOOLEAN };
            try {
                STACKTRACEELEMENT_COMPOSITETYPE = new CompositeType(
                        StackTraceElement.class.getName(),
                        StackTraceElement.class.getName(), typeNames,
                        typeDescs, typeTypes);
            } catch (OpenDataException e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }
        return STACKTRACEELEMENT_COMPOSITETYPE;
    }

    /**
     * Convenience method to converts an array of <code>String</code> to a
     * <code>List&lt;String&gt;</code>.
     * 
     * @param data
     *            an array of <code>String</code>
     * @return a new <code>List&lt;String&gt;</code>
     */
    public static List<String> convertStringArrayToList(String[] data) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < data.length; i++) {
            result.add(data[i]);
        }// end for
        return result;
    }

    /**
     * Receives an instance of a {@link TabularData}whose data is wrapping a
     * <code>Map</code> and returns a new instance of <code>Map</code>
     * containing the input information.
     * 
     * @param data
     *            an instance of <code>TabularData</code> that may be mapped
     *            to a <code>Map</code>.
     * @return a new {@link Map}containing the information originally wrapped
     *         in the <code>data</code> input.
     * @throws IllegalArgumentException
     *             if <code>data</code> has a <code>CompositeType</code>
     *             that does not contain exactly two items (i.e. a key and a
     *             value).
     */
    @SuppressWarnings("unchecked")
    public static Object convertTabularDataToMap(TabularData data) {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        Map<Object, Object> result = new HashMap<Object, Object>();
        Set<String> cdKeySet = data.getTabularType().getRowType().keySet();
        // The key set for the CompositeData instances comprising each row
        // must contain only two elements.
        if (cdKeySet.size() != 2) {
            throw new IllegalArgumentException(
                    "TabularData's row type is not a CompositeType with two items.");
        }
        String[] keysArray = new String[2];
        int count = 0;
        Iterator<String> keysIt = cdKeySet.iterator();
        while (keysIt.hasNext()) {
            keysArray[count++] = keysIt.next();
        }// end while

        Collection<CompositeData> rows = (Collection<CompositeData>) data
                .values();
        Iterator<CompositeData> rowIterator = rows.iterator();
        while (rowIterator.hasNext()) {
            CompositeData rowCD = rowIterator.next();
            result.put(rowCD.get(keysArray[0]), rowCD.get(keysArray[1]));
        }// end while a row to process
        return result;
    }

    /**
     * Return a new instance of type <code>T</code> from the supplied
     * {@link CompositeData} object whose type maps to <code>T</code>.
     * 
     * @param <T>
     *            the type of object wrapped by the <code>CompositeData</code>.
     * @param data
     *            an instance of <code>CompositeData</code> that maps to an
     *            instance of <code>T</code>
     * @param realClass
     *            the {@link Class} object for type <code>T</code>
     * @return a new instance of <code>T</code>
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertFromCompositeData(CompositeData data,
            Class<T> realClass) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        // See if the realClass has a static for method that takes a
        // CompositeData and returns a new instance of T.
        Method forMethod = realClass.getMethod("from",
                new Class[] { CompositeData.class });
        return (T) forMethod.invoke(null, data);
    }

    /**
     * Receive data of the type specified in <code>openClass</code> and return
     * it in an instance of the type specified in <code>realClass</code>.
     * 
     * @param <T>
     * 
     * @param data
     *            an instance of the type named <code>openTypeName</code>
     * @param openClass
     * @param realClass
     * @return a new instance of the type <code>realTypeName</code> containing
     *         all the state in the input <code>data</code> object.
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertFromOpenType(Object data, Class<?> openClass,
            Class<T> realClass) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, SecurityException,
            IllegalArgumentException, NoSuchMethodException,
            InvocationTargetException {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        T result = null;

        if (openClass.isArray() && realClass.isArray()) {
            Class openElementClass = openClass.getComponentType();
            Class<?> realElementClass = realClass.getComponentType();

            Object[] dataArray = (Object[]) data;
            result = (T) Array.newInstance(realElementClass, dataArray.length);
            for (int i = 0; i < Array.getLength(result); i++) {
                Array.set(result, i, convertFromOpenType(dataArray[i],
                        openElementClass, realElementClass));
            }// end for
        } else if (openClass.equals(CompositeData.class)) {
            result = ManagementUtils.convertFromCompositeData(
                    (CompositeData) data, realClass);
        } else if (openClass.equals(TabularData.class)) {
            if (realClass.equals(Map.class)) {
                result = (T) ManagementUtils
                        .convertTabularDataToMap((TabularData) data);
            }
        } else if (openClass.equals(String[].class)) {
            if (realClass.equals(List.class)) {
                result = (T) ManagementUtils
                        .convertStringArrayToList((String[]) data);
            }
        } else if (openClass.equals(String.class)) {
            if (realClass.equals(MemoryType.class)) {
                result = (T) ManagementUtils
                        .convertStringToMemoryType((String) data);
            }
        }
        return result;
    }

    /**
     * Convenience method that receives a string representation of a
     * <code>MemoryType</code> instance and returns the actual
     * <code>MemoryType</code> that corresponds to that value.
     * 
     * @param data
     *            a string
     * @return if <code>data</code> can be used to obtain an instance of
     *         <code>MemoryType</code> then a <code>MemoryType</code>,
     *         otherwise <code>null</code>.
     */
    private static MemoryType convertStringToMemoryType(String data) {
        MemoryType result = null;
        try {
            result = MemoryType.valueOf(data);
        } catch (IllegalArgumentException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
        }
        return result;
    }

    /**
     * Convenience method to convert an object, <code>data</code> from its
     * Java type <code>realClass</code> to the specified open MBean type
     * <code>openClass</code>.
     * 
     * @param <T>
     *            the open MBean class
     * @param data
     *            the object to be converted
     * @param openClass
     *            the open MBean class
     * @param realClass
     *            the real Java type of <code>data</code>
     * @return a new instance of type <code>openClass</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertToOpenType(Object data, Class<T> openClass,
            Class<?> realClass) {
        // Bail out early on null input.
        if (data == null) {
            return null;
        }

        T result = null;

        if (openClass.isArray() && realClass.isArray()) {
            Class<?> openElementClass = openClass.getComponentType();
            Class<?> realElementClass = realClass.getComponentType();

            Object[] dataArray = (Object[]) data;
            result = (T) Array.newInstance(openElementClass, dataArray.length);
            for (int i = 0; i < Array.getLength(result); i++) {
                Array.set(result, i, convertToOpenType(dataArray[i],
                        openElementClass, realElementClass));
            }// end for
        } else if (openClass.equals(CompositeData.class)) {
            if (realClass.equals(ThreadInfo.class)) {
                result = (T) ManagementUtils
                        .toThreadInfoCompositeData((ThreadInfo) data);
            } else if (realClass.equals(MemoryUsage.class)) {
                result = (T) ManagementUtils
                        .toMemoryUsageCompositeData((MemoryUsage) data);
            } else if (realClass.equals(MonitorInfo.class)) {
                result = (T) ManagementUtils
                        .toMonitorInfoCompositeData((MonitorInfo) data);
            } else if (realClass.equals(LockInfo.class)) {
                result = (T) ManagementUtils
                        .toLockInfoCompositeData((LockInfo) data);
            }
        } else if (openClass.equals(TabularData.class)) {
            if (realClass.equals(Map.class)) {
                result = (T) ManagementUtils
                        .toSystemPropertiesTabularData((Map) data);
            }
        } else if (openClass.equals(String[].class)) {
            if (realClass.equals(List.class)) {
                result = (T) ManagementUtils.convertListToArray((List) data,
                        openClass, openClass.getComponentType());
            }
        } else if (openClass.equals(String.class)) {
            if (realClass.isEnum()) {
                result = (T) ((Enum) data).name();
            }
        }
        return result;
    }

    /**
     * Convenience method to convert a {@link List} instance to an instance of
     * an array. The element type of the returned array will be of the same type
     * as the <code>List</code> component values.
     * 
     * @param <T>
     *            the array type named <code>arrayType</code>
     * @param <E>
     *            the type of the elements in the array,
     *            <code>elementType</code>
     * @param list
     *            the <code>List</code> to be converted
     * @param arrayType
     *            the array type
     * @param elementType
     *            the type of the array's elements
     * @return a new instance of <code>arrayType</code> initialised with the
     *         data stored in <code>list</code>
     */
    @SuppressWarnings("unchecked")
    private static <T, E> T convertListToArray(List<E> list,
            Class<T> arrayType, Class<E> elementType) {
        T result = (T) Array.newInstance(elementType, list.size());
        Iterator<E> it = list.iterator();
        int count = 0;
        while (it.hasNext()) {
            E element = it.next();
            Array.set(result, count++, element);
        }
        return result;
    }

    /**
     * @param propsMap
     *            a <code>Map&lt;String, String%gt;</code> of the system
     *            properties.
     * @return the system properties (e.g. as obtained from
     *         {@link RuntimeMXBean#getSystemProperties()}) wrapped in a
     *         {@link TabularData}.
     */
    public static TabularData toSystemPropertiesTabularData(
            Map<String, String> propsMap) {
        // Bail out early on null input.
        if (propsMap == null) {
            return null;
        }

        TabularData result = null;
        try {
            // Obtain the row type for the TabularType
            String[] rtItemNames = { "key", "value" };
            String[] rtItemDescs = { "key", "value" };
            OpenType[] rtItemTypes = { SimpleType.STRING, SimpleType.STRING };

            CompositeType rowType = new CompositeType(propsMap.getClass()
                    .getName(), propsMap.getClass().getName(), rtItemNames,
                    rtItemDescs, rtItemTypes);

            // Obtain the required TabularType
            TabularType sysPropsType = new TabularType(propsMap.getClass()
                    .getName(), propsMap.getClass().getName(), rowType,
                    new String[] { "key" });

            // Create an empty TabularData
            result = new TabularDataSupport(sysPropsType);

            // Take each entry out of the input propsMap, put it into a new
            // instance of CompositeData and put into the TabularType
            Set<String> keys = propsMap.keySet();
            for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
                String propKey = iter.next();
                String propVal = propsMap.get(propKey);
                result.put(new CompositeDataSupport(rowType, rtItemNames,
                        new String[] { propKey, propVal }));
            }// end for
        } catch (OpenDataException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            result = null;
        }
        return result;
    }

    /**
     * Convenience method that sets out to return the {@link Class}object for
     * the specified type named <code>name</code>. Unlike the
     * {@link Class#forName(java.lang.String)}method, this will work even for
     * primitive types.
     * 
     * @param name
     *            the name of a Java type
     * @return the <code>Class</code> object for the type <code>name</code>
     * @throws ClassNotFoundException
     *             if <code>name</code> does not correspond to any known type
     *             (including primitive types).
     */
    public static Class getClassMaybePrimitive(String name)
            throws ClassNotFoundException {
        int i = name.lastIndexOf('.');
        if (i != -1) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPackageAccess(name.substring(0, i));
            }
        }

        Class result = null;

        try {
            result = Class.forName(name);
        } catch (ClassNotFoundException e) {
            if (name.equals(boolean.class.getName())) {
                result = boolean.class;
            } else if (name.equals(char.class.getName())) {
                result = char.class;
            } else if (name.equals(byte.class.getName())) {
                result = byte.class;
            } else if (name.equals(short.class.getName())) {
                result = short.class;
            } else if (name.equals(int.class.getName())) {
                result = int.class;
            } else if (name.equals(long.class.getName())) {
                result = long.class;
            } else if (name.equals(float.class.getName())) {
                result = float.class;
            } else if (name.equals(double.class.getName())) {
                result = double.class;
            } else if (name.equals(void.class.getName())) {
                result = void.class;
            } else {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
                // Rethrow the original ClassNotFoundException
                throw e;
            }// end else
        }// end catch
        return result;
    }

    /**
     * Convenience method to determine if the <code>wrapper</code>
     * <code>Class</code>
     * object is really the wrapper class for the
     * <code>primitive</code> <code>Class</code> object.
     * 
     * @param wrapper
     * @param primitive
     * @return <code>true</code> if the <code>wrapper</code> class is the
     *         wrapper class for <code>primitive</code>. Otherwise
     *         <code>false</code>.
     */
    public static boolean isWrapperClass(Class<? extends Object> wrapper,
            Class primitive) {
        boolean result = true;
        if (primitive.equals(boolean.class) && !wrapper.equals(Boolean.class)) {
            result = false;
        } else if (primitive.equals(char.class)
                && !wrapper.equals(Character.class)) {
            result = false;
        } else if (primitive.equals(byte.class) && !wrapper.equals(Byte.class)) {
            result = false;
        } else if (primitive.equals(short.class)
                && !wrapper.equals(Short.class)) {
            result = false;
        } else if (primitive.equals(int.class)
                && !wrapper.equals(Integer.class)) {
            result = false;
        } else if (primitive.equals(long.class) && !wrapper.equals(Long.class)) {
            result = false;
        } else if (primitive.equals(float.class)
                && !wrapper.equals(Float.class)) {
            result = false;
        } else if (primitive.equals(double.class)
                && !wrapper.equals(Double.class)) {
            result = false;
        }

        return result;
    }

    /**
     * Convenience method that returns a boolean indication of whether or not
     * concrete instances of the the supplied interface type
     * <code>mxbeanInterface</code> should also be implementors of the
     * interface <code>javax.management.NotificationEmitter</code>.
     * 
     * @param <T>
     * @param mxbeanInterface
     * @return <code>true</code> if instances of type
     *         <code>mxbeanInterface</code> should also implement
     *         <code>javax.management.NotificationEmitter</code>. Otherwise,
     *         <code>false</code>.
     */
    public static <T> boolean isANotificationEmitter(Class<T> mxbeanInterface) {
        boolean result = false;
        MBeanInfo info = getMBeanInfo(mxbeanInterface.getName());
        if (info != null) {
            MBeanNotificationInfo[] notifications = info.getNotifications();
            if ((notifications != null) && (notifications.length > 0)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns an array of {@link StackTraceElement} whose elements have been
     * created from the corresponding elements of the
     * <code>stackTraceDataVal</code> argument.
     * 
     * @param stackTraceDataVal
     *            an array of {@link CompositeData}objects, each one
     *            representing a <code>StackTraceElement</code>.
     * @return an array of <code>StackTraceElement</code> objects built using
     *         the data discovered in the corresponding elements of
     *         <code>stackTraceDataVal</code>.
     * @throws IllegalArgumentException
     *             if any of the elements of <code>stackTraceDataVal</code> do
     *             not correspond to a <code>StackTraceElement</code> with the
     *             following attributes:
     *             <ul>
     *             <li><code>className</code>(<code>java.lang.String</code>)
     *             <li><code>methodName</code>(
     *             <code>java.lang.String</code>)
     *             <li><code>fileName</code>(<code>java.lang.String</code>)
     *             <li><code>lineNumbercode> (<code>java.lang.Integer</code>)
     *             <li><code>nativeMethod</code> (<code>java.lang.Boolean</code>)
     *             </ul>
     */
    public static StackTraceElement[] getStackTracesFromCompositeDataArray(
            CompositeData[] stackTraceDataVal) {
        // Bail out early on null input.
        if (stackTraceDataVal == null) {
            return null;
        }

        StackTraceElement[] result = new StackTraceElement[stackTraceDataVal.length];

        for (int i = 0; i < stackTraceDataVal.length; i++) {
            CompositeData data = stackTraceDataVal[i];

            if (data != null) {
                // Verify the element
                verifyFieldNumber(data, 5);
                String[] attributeNames = { "className", "methodName",
                        "fileName", "lineNumber", "nativeMethod" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                verifyFieldNames(data, attributeNames);
                String[] attributeTypes = { "java.lang.String",
                        "java.lang.String", "java.lang.String",
                        "java.lang.Integer", "java.lang.Boolean" };
                verifyFieldTypes(data, attributeNames, attributeTypes); //$NON-NLS-1$

                // Get hold of the values from the data object to use in the
                // creation of a new StackTraceElement.
                Object[] attributeVals = data.getAll(attributeNames);
                String classNameVal = (String) attributeVals[0];
                String methodNameVal = (String) attributeVals[1];
                String fileNameVal = (String) attributeVals[2];
                int lineNumberVal = ((Integer) attributeVals[3]).intValue();
                boolean nativeMethodVal = ((Boolean) attributeVals[4])
                        .booleanValue();
                StackTraceElement element = new StackTraceElement(classNameVal,
                        methodNameVal, fileNameVal, lineNumberVal);
                result[i] = element;
            } else {
                result[i] = null;
            }

        }

        return result;
    }

    /**
     * Returns an array of {@link LockInfo} whose elements have been created
     * from the corresponding elements of the <code>lockInfosCDArray</code>
     * argument.
     * 
     * @param lockInfosCDArray
     *            an array of {@link CompositeData}objects, each one
     *            representing a <code>LockInfo</code>.
     * @return an array of <code>LockInfo</code> objects built using the data
     *         discovered in the corresponding elements of
     *         <code>lockInfosCDArray</code>.
     * @throws IllegalArgumentException
     *             if any of the elements of <code>lockInfosCDArray</code> do
     *             not correspond to a <code>LockInfo</code> with the
     *             following attributes:
     *             <ul>
     *             <li><code>className</code>(<code>java.lang.String</code>)
     *             <li><code>identityHashCode</code> (<code>java.lang.Integer</code>)
     *             </ul>
     */
    public static LockInfo[] getLockInfosFromCompositeDataArray(
            CompositeData[] lockInfosCDArray) {
        // Bail out early on null input.
        if (lockInfosCDArray == null) {
            return null;
        }

        LockInfo[] result = new LockInfo[lockInfosCDArray.length];

        for (int i = 0; i < lockInfosCDArray.length; i++) {
            CompositeData data = lockInfosCDArray[i];

            // Verify the element
            verifyFieldNumber(data, 2);
            String[] attributeNames = { "className", "identityHashCode" };
            verifyFieldNames(data, attributeNames);
            String[] attributeTypes = { "java.lang.String", "java.lang.Integer" };
            verifyFieldTypes(data, attributeNames, attributeTypes); //$NON-NLS-1$

            // Get hold of the values from the data object to use in the
            // creation of a new LockInfo.
            Object[] attributeVals = data.getAll(attributeNames);
            String className = (String) attributeVals[0];
            int idHashCode = ((Integer) attributeVals[1]).intValue();
            LockInfo element = new LockInfo(className, idHashCode);
            result[i] = element;
        }
        return result;
    }

    /**
     * Returns an array of {@link MonitorInfo} whose elements have been created
     * from the corresponding elements of the <code>monitorInfosCDArray</code>
     * argument.
     * 
     * @param monitorInfosCDArray
     *            an array of {@link CompositeData}objects, each one
     *            representing a <code>MonitorInfo</code>.
     * @return an array of <code>MonitorInfo</code> objects built using the
     *         data discovered in the corresponding elements of
     *         <code>monitorInfosCDArray</code>.
     * @throws IllegalArgumentException
     *             if any of the elements of <code>monitorInfosCDArray</code>
     *             do not correspond to a <code>MonitorInfo</code> with the
     *             following attributes:
     *             <ul>
     *             <li><code>lockedStackFrame</code>(<code>javax.management.openmbean.CompositeData</code>)
     *             <li><code>lockedStackDepth</code>(
     *             <code>java.lang.Integer</code>)
     *             </ul>
     *             The <code>lockedStackFrame</code> attribute must correspond
     *             to a <code>java.lang.StackTraceElement</code> which has the
     *             following attributes:
     *             <ul>
     *             <li><code>className</code> (<code>java.lang.String</code>)
     *             <li><code>methodName</code> (<code>java.lang.String</code>)
     *             <li><code>fileName</code> (<code>java.lang.String</code>)
     *             <li><code>lineNumber</code> (<code>java.lang.Integer</code>)
     *             <li><code>nativeMethod</code> (<code>java.lang.Boolean</code>)
     *             </ul>
     */
    public static MonitorInfo[] getMonitorInfosFromCompositeDataArray(
            CompositeData[] monitorInfosCDArray) {
        // Bail out early on null input.
        if (monitorInfosCDArray == null) {
            return null;
        }

        MonitorInfo[] result = new MonitorInfo[monitorInfosCDArray.length];

        for (int i = 0; i < monitorInfosCDArray.length; i++) {
            CompositeData data = monitorInfosCDArray[i];

            // Verify the element
            verifyFieldNumber(data, 4);
            String[] attributeNames = { "className", "identityHashCode",
                    "lockedStackFrame", "lockedStackDepth" };
            verifyFieldNames(data, attributeNames);
            String[] attributeTypes = { "java.lang.String",
                    "java.lang.Integer", CompositeData.class.getName(),
                    "java.lang.Integer" };
            verifyFieldTypes(data, attributeNames, attributeTypes);
            result[i] = MonitorInfo.from(data);
        }

        return result;
    }

    /**
     * Convenience method that returns a {@link StackTraceElement} created from
     * the corresponding <code>CompositeData</code> argument.
     * 
     * @param stackTraceCD
     *            a <code>CompositeData</code> that wraps a
     *            <code>StackTraceElement</code>
     * @return a <code>StackTraceElement</code> object built using the data
     *         discovered in the <code>stackTraceCD</code>.
     * @throws IllegalArgumentException
     *             if the <code>stackTraceCD</code> does not correspond to a
     *             <code>StackTraceElement</code> with the following
     *             attributes:
     *             <ul>
     *             <li><code>className</code>(<code>java.lang.String</code>)
     *             <li><code>methodName</code>(
     *             <code>java.lang.String</code>)
     *             <li><code>fileName</code>(<code>java.lang.String</code>)
     *             <li><code>lineNumbercode> (<code>java.lang.Integer</code>)
     *             <li><code>nativeMethod</code> (<code>java.lang.Boolean</code>)
     *             </ul>
     */
    public static StackTraceElement getStackTraceFromCompositeData(
            CompositeData stackTraceCD) {
        return getStackTracesFromCompositeDataArray(new CompositeData[] { stackTraceCD })[0];
    }

    /**
     * Convenience method to create an ObjectName from the specified string.
     *
     * @param name
     * @return an ObjectName corresponding to the specified string.
     */
    public static ObjectName createObjectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Convenience method to create an ObjectName with the specified domain and name property.
     *
     * @param domain
     * @param name
     * @return an ObjectName with the specified domain and name property.
     */
    public static ObjectName createObjectName(String domain, String name) {
        try {
            return new ObjectName(domain + ",name=" + name);
        } catch (MalformedObjectNameException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

/*
 * $Log$
 * Revision 1.33  2006/09/06 18:42:50  gharley
 * Updated MBeanInfo for OperatingSystemMXBean. Fixed a couple of warnings from Eclipse 6.0 compiler.
 * Revision 1.32 2006/06/15 11:37:22 gharley Fix
 * bug in getMonitorInfosFromCompositeDataArray() where the opentype of the
 * stack trace element was an array of CompositeData - should just be
 * CompositeData. Revision 1.31 2006/06/09 16:02:20 gharley Implementation of
 * the new ThreadMXBean operations. All over bar the native code. Revision 1.30
 * 2006/06/09 11:39:33 gharley Code updated to incorporate all of the API
 * changes that have occurred in 6.0 RI between Beta b-59 (vintage of Javadoc
 * provided internally for development purposes) and Beta b-86 (latest version
 * of API). Unit tests are broken until native code becomes available. To do :
 * The last couple of ThreadMXBean methods. Revision 1.29 2006/06/06 17:32:26
 * gharley Implementation of ThreadInfo.getLockInfo() method together with all
 * of the surrounding changes required.
 * 
 * Revision 1.28 2006/05/31 12:14:56 gharley For 6.0, added in new LockInfo and
 * MonitorInfo types. Added in new getSystemLoadAverage() method to
 * OperatingSystemMXBean. Test cases added and some minor refactorings carried
 * out. Work still in progress. Next step will entail changes to ThreadInfo and
 * ThreadMXBean. Revision 1.27 2005/09/12 14:55:06 gharley Added new convenience
 * method isANotificationEmitter()
 * 
 * Revision 1.26 2005/08/19 22:41:19 pchurch Tidy up comments and add some
 * javadoc.
 * 
 * Revision 1.25 2005/07/08 13:02:30 gharley CMVC 92227
 * 
 * Revision 1.24 2005/06/29 17:57:47 gharley Removed some warnings related to
 * generics that cropped up on moving to the JDT in Eclipse 3.1 final.
 * 
 * Revision 1.23 2005/06/21 09:27:13 gharley Add in security "doPrivileged" code
 * to get System property.
 * 
 * Revision 1.22 2005/06/20 10:35:34 gharley Update metadata to support new
 * GCMode property on MemoryMXBean extension.
 * 
 * Revision 1.21 2005/06/13 10:02:03 gharley Only inform user of non-fatal
 * exceptions if the system property "com.ibm.lang.management" is set.
 * 
 * Revision 1.20 2005/05/31 10:04:30 gharley New code for exposing shared cache
 * information.
 * 
 * Revision 1.19 2005/05/25 20:23:35 gharley Updated unit test bucket now that
 * 5.0 VM available that enables direct testing of the MXBeans. Shook out one or
 * two bugs in the code. Note that OperatingSystemMXBeans are still broken &
 * that there is a problem with MemoryMXBean.getNonHeapmemoryUsage(). Awaiting
 * new VM before tackling these...
 * 
 * Revision 1.18 2005/05/12 11:02:39 gharley 1) Update MemoryMXBean metadata to
 * include Paul's DLPAR extensions 2) Update OperatingSystemMXBean metadata to
 * include event notifications brought in by DLPAR extension. 3) Several methods
 * to support converting the new notification types to CompositeData objects.
 * 
 * Revision 1.17 2005/04/29 23:58:49 pschurch Revised and extended memory
 * management beans to support the native implementation.
 * 
 * Revision 1.16 2005/04/18 10:37:42 gharley Updates caused by removal of
 * proprietary attributes from IBM interfaces for GarbageCollector and
 * OperatingSystem beans.
 * 
 * Revision 1.15 2005/04/17 20:43:29 gharley Register beans which extend IBM
 * interfaces twice in info map : once with the standard interface name and once
 * with the IBM interface name.
 * 
 * Revision 1.14 2005/03/15 17:45:28 gharley Fixed multiple problems caused by
 * not updating the MXBean meta data declarations when moving extensions around.
 * 
 * Revision 1.13 2005/02/14 12:16:03 gharley Added proprietary attributes to the
 * meta data objects for the GarbageCollectorMXBean and OperatingSystemMXBean
 * types.
 * 
 * Revision 1.12 2005/02/13 18:20:49 gharley Added in native method
 * declarations. Also added code to build the meta-data for the
 * GarbageCollectorMXBean type after I figured out how inheritance works between
 * dynamic MBeans.
 * 
 * Revision 1.11 2005/02/11 17:25:48 gharley Various changes to support
 * MemoryPoolMXBean
 * 
 * Revision 1.10 2005/02/10 12:16:47 gharley Fixed minor array element
 * mis-numbering problem in addLoggingBeanInfo
 * 
 * Revision 1.9 2005/02/09 22:20:58 gharley Lots more static convenience methods
 * that came about through providing for dynamic proxy support and also from
 * re-implementing the DynamicMXBean getAttribute and setAttribute methods using
 * a reflective approach. Revision 1.8 2005/02/02 14:01:38 gharley Moved
 * creation of all of the MBeanInfo metadata objects in here on the assumption
 * that the metadata is going to be useful for the handling of method
 * invocations via the dynamic proxies. Revision 1.7 2005/01/25 15:44:28 gharley
 * Added a public static method to access the LoggingMXBean singleton.
 * 
 * Revision 1.6 2005/01/21 14:56:54 gharley Renamed a couple of CompositeData
 * creational helper methods.
 * 
 * Revision 1.5 2005/01/21 09:14:12 gharley Added a few more convenience
 * methods. Revision 1.4 2005/01/19 12:46:03 gharley Implementation of
 * DynamicMBean behaviour. Extends new base class.
 * 
 * Revision 1.3 2005/01/14 11:25:08 gharley Added singelton access methods.
 * 
 * Revision 1.2 2005/01/11 13:41:47 gharley Add test for attributes of instance
 * type CompositeDataSupport to verify that they implement CompositeData.
 * 
 * Revision 1.1 2005/01/11 10:56:10 gharley Initial upload
 * 
 */
