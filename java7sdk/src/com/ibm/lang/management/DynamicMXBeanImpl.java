/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.reflect.Method;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanRegistration;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Abstract implementation of the {@link DynamicMBean} interface.
 * 
 * @author gharley
 */
public abstract class DynamicMXBeanImpl implements DynamicMBean {

    protected ObjectName objectName;
    protected MBeanInfo info;

    protected DynamicMXBeanImpl(ObjectName objectName) {
        this.objectName = objectName;
    }

    /**
     * @param className
     *            The name of the Java class of the MBean
     * @param description
     *            A human readable description of the MBean (optional).
     * @param attributes
     *            The list of exposed attributes of the MBean.
     * @param constructors
     *            The list of public constructors of the MBean.
     * @param operations
     *            The list of operations of the MBean. 
     * @param notifications
     *            The list of notifications emitted.
     */
    protected void initMBeanInfo(String className, String description,
            MBeanAttributeInfo[] attributes,
            MBeanConstructorInfo[] constructors,
            MBeanOperationInfo[] operations,
            MBeanNotificationInfo[] notifications) {
        info = new MBeanInfo(className, description, attributes, constructors,
                operations, notifications);
    }

    /**
     * @param info
     *            The MBeanInfo to be set.
     */
    protected void setMBeanInfo(MBeanInfo info) {
        this.info = info;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
     */
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();
        for (int i = 0; i < attributes.length; i++) {
            try {
                Object value = getAttribute(attributes[i]);
                result.add(new Attribute(attributes[i], value));
            } catch (Exception e) {
                // It is alright if the returned AttributeList is smaller in
                // size than the length of the input array.
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }// end for
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
     */
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList result = new AttributeList();

        for (int i = 0; i < attributes.size(); i++) {
            Attribute attrib = (Attribute) attributes.get(i);
            String attribName = null;
            Object attribVal = null;
            try {
                this.setAttribute(attrib);
                attribName = attrib.getName();
                // Note that the below getAttribute call will throw an
                // AttributeNotFoundException if the named attribute is not
                // readable for this bean. This is perfectly alright - the set
                // has worked as requested - it just means that the caller
                // does not get this information returned to them in the
                // result AttributeList.
                attribVal = getAttribute(attribName);
                result.add(new Attribute(attribName, attribVal));
            } catch (Exception e) {
                if (ManagementUtils.VERBOSE_MODE) {
                    e.printStackTrace(System.err);
                }// end if
            }
        }// end for
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getMBeanInfo()
     */
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    /**
     * TODO Type description
     * 
     * @author gharley
     */
    enum AttributeAccessType {
        READING, WRITING
    };

    /**
     * Tests to see if this <code>DynamicMXBean</code> has an attribute with
     * the name <code>attributeName</code>. If the test is passed, the
     * {@link MBeanAttributeInfo}representing the attribute is returned.
     * 
     * @param attributeName
     *            the name of the attribute being queried
     * @param access
     *            an {@link AttributeAccessType}indication of whether the
     *            caller is looking for a readable or writable attribute.
     * @return if the named attribute exists and is readable or writable
     *         (depending on what was specified in <code>access</code>, an
     *         instance of <code>MBeanAttributeInfo</code> that describes the
     *         attribute, otherwise <code>null</code>.
     */
    protected MBeanAttributeInfo getPresentAttribute(String attributeName,
            AttributeAccessType access) {
        MBeanAttributeInfo[] attribs = info.getAttributes();
        MBeanAttributeInfo result = null;

        for (int i = 0; i < attribs.length; i++) {
            MBeanAttributeInfo attribInfo = attribs[i];
            if (attribInfo.getName().equals(attributeName)) {
                if (access.equals(AttributeAccessType.READING)) {
                    if (attribInfo.isReadable()) {
                        result = attribInfo;
                        break;
                    }
                } else {
                    if (attribInfo.isWritable()) {
                        result = attribInfo;
                        break;
                    }
                }
            }// end if
        }// end for
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
     */
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        Object result = null;
        Method getterMethod = null;
        MBeanAttributeInfo attribInfo = getPresentAttribute(attribute,
                AttributeAccessType.READING);
        if (attribInfo == null) {
            throw new AttributeNotFoundException("No such attribute : "
                    + attribute);
        }

        try {
            String getterPrefix = attribInfo.isIs() ? "is" : "get";
            getterMethod = this.getClass().getMethod(getterPrefix + attribute,
                    (Class[]) null);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        String realReturnType = getterMethod.getReturnType().getName();
        String openReturnType = attribInfo.getType();
        result = invokeMethod(getterMethod, (Object[]) null);
        
        try {
            if (!realReturnType.equals(openReturnType)) {
                result = ManagementUtils
                .convertToOpenType(result, Class
                        .forName(openReturnType), Class
                        .forName(realReturnType));
            }// end if conversion necessary
        } catch (ClassNotFoundException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new MBeanException(e);
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
     */
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {

        // In Java 5.0 platform MXBeans the following applies for all
        // attribute setter methods :
        // 1. no conversion to open MBean types necessary
        // 2. all setter arguments are single value (i.e. not array or
        // collection types).
        // 3. all return null

        Class argType = null;

        // Validate the attribute
        MBeanAttributeInfo attribInfo = getPresentAttribute(
                attribute.getName(), AttributeAccessType.WRITING);
        if (attribInfo == null) {
            throw new AttributeNotFoundException("No such attribute : "
                    + attribute);
        }

        try {
            // Validate supplied parameter is of the expected type
            argType = ManagementUtils.getClassMaybePrimitive(attribInfo
                    .getType());
        } catch (ClassNotFoundException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        if (argType.isPrimitive()) {
            if (!ManagementUtils.isWrapperClass(
                    attribute.getValue().getClass(), argType)) {
                throw new InvalidAttributeValueException(attribInfo.getName()
                        + " is a " + attribInfo.getType() + " attribute");
            }
        } else if (!argType.equals(attribute.getValue().getClass())) {
            throw new InvalidAttributeValueException(attribInfo.getName()
                    + " is a " + attribInfo.getType() + " attribute");
        }

        Method setterMethod = null;
        try {
            setterMethod = this.getClass().getMethod(
                    "set" + attribute.getName(), new Class[] { argType });
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        invokeMethod(setterMethod, attribute.getValue());
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.DynamicMBean#invoke(java.lang.String,
     *      java.lang.Object[], java.lang.String[])
     */
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        Object result = null;

        // If null is passed in for the signature argument (if invoking a
        // method with no args for instance) then avoid any NPEs by working
        // with a zero length String array instead.
        String[] localSignature = signature;
        if (localSignature == null) {
            localSignature = new String[0];
        }

        // Validate that we have the named action
        MBeanOperationInfo opInfo = getPresentOperation(actionName,
                localSignature);
        if (opInfo == null) {
            throw new ReflectionException(
                    new NoSuchMethodException(actionName),
                    "No such operation : " + actionName);
        }

        // For Java 5.0 platform MXBeans, no conversion
        // to open MBean types is necessary for any of the arguments.
        // i.e. they are all simple types.
        Method operationMethod = null;
        try {
            Class[] argTypes = new Class[localSignature.length];
            for (int i = 0; i < localSignature.length; i++) {
                argTypes[i] = ManagementUtils
                        .getClassMaybePrimitive(localSignature[i]);
            }// end for
            operationMethod = this.getClass().getMethod(actionName, argTypes);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new ReflectionException(e);
        }

        String realReturnType = operationMethod.getReturnType().getName();
        String openReturnType = opInfo.getReturnType();
        result = invokeMethod(operationMethod, params);

        try {
            if (!realReturnType.equals(openReturnType)) {
                result = ManagementUtils
                        .convertToOpenType(result, Class
                                .forName(openReturnType), Class
                                .forName(realReturnType));
            }
        } catch (ClassNotFoundException e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            throw new MBeanException(e);
        }// end catch
            
        return result;
    }

    /**
     * Tests to see if this <code>DynamicMXBean</code> has an operation with
     * the name <code>actionName</code>. If the test is passed, the
     * {@link MBeanOperationInfo}representing the operation is returned to the
     * caller.
     * 
     * @param actionName
     *            the name of a possible method on this
     *            <code>DynamicMXBean</code>
     * @param signature
     *            the list of parameter types for the named operation in the
     *            correct order
     * @return if the named operation exists, an instance of
     *         <code>MBeanOperationInfo</code> that describes the operation,
     *         otherwise <code>null</code>.
     */
    protected MBeanOperationInfo getPresentOperation(String actionName,
            String[] signature) {
        MBeanOperationInfo[] operations = info.getOperations();
        MBeanOperationInfo result = null;

        for (int i = 0; i < operations.length; i++) {
            MBeanOperationInfo opInfo = operations[i];
            if (opInfo.getName().equals(actionName)) {
                // Do parameter numbers match ?
                if (signature.length == opInfo.getSignature().length) {
                    // Do parameter types match ?
                    boolean match = true;
                    MBeanParameterInfo[] parameters = opInfo.getSignature();
                    for (int j = 0; j < parameters.length; j++) {
                        MBeanParameterInfo paramInfo = parameters[j];
                        if (!paramInfo.getType().equals(signature[j])) {
                            match = false;
                            break;
                        }
                    }// end for all parameters
                    if (match) {
                        result = opInfo;
                        break;
                    }
                }// end if parameter counts match
            }// end if operation names match
        }// end for all operations

        return result;
    }
    
    /**
     * @param params
     * @param operationMethod
     * @return the result of the reflective method invocation
     * @throws MBeanException
     */
    private Object invokeMethod(Method operationMethod, Object... params)
            throws MBeanException {
        Object result = null;
        try {
            result = operationMethod.invoke(this, params);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if
            Throwable root = e.getCause();
            if (root instanceof Error) {
                throw new MBeanException(null, root.toString());
            } else if (root instanceof RuntimeException) {
                throw (RuntimeException) root;
            } else {
                throw new MBeanException((Exception) root);
            }// end else
        }// end catch
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.management.PlatformManagedObject#getObjectName()
     */
    public ObjectName getObjectName() {
        return objectName;
    }
}

/*
 * $Log$
 * Revision 1.8  2006/09/06 18:40:57  gharley
 * Update to invokeMethod() so that it wraps any errors inside a new MBeanException.
 * Revision 1.7 2005/07/07 15:44:46 gharley
 * Allow runtime exceptions to reach caller (like reference impl)
 * 
 * Revision 1.6 2005/06/13 10:02:03 gharley Only inform user of non-fatal
 * exceptions if the system property "com.ibm.lang.management" is set.
 * 
 * Revision 1.5 2005/02/13 18:18:04 gharley Removed some dead code that was
 * already commented out.
 * 
 * Revision 1.4 2005/02/10 12:14:47 gharley Pushed up invoke method into here
 * from subclasses after replacing the custom implementations with (touch wood)
 * a more generalised mechanism based on reflection. Revision 1.3 2005/02/09
 * 22:23:41 gharley Pushed up getAttribute and setAttribute into here from
 * superclasses after replacing the custom implementations with (touch wood) a
 * more generalised mechanism based on reflection. Revision 1.2 2005/02/02
 * 14:09:58 gharley Moved MBeanInfo setup into ManagementUtils on the assumption
 * that the metadata is going to be useful for the proxy bean support.
 * 
 * Revision 1.1 2005/01/19 12:44:02 gharley Abstract base class for all the
 * MXBean implementation classes. Provides default/common dynamic MBean
 * behaviour.
 * 
 */
