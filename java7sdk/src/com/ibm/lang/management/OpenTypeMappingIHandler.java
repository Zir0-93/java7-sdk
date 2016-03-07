/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

/**
 * Concrete instance of the {@link InvocationHandler} interface that is used to
 * handle method invocations on MXBeans that have been obtained using the proxy
 * method.
 * 
 */
public class OpenTypeMappingIHandler implements InvocationHandler {

    private MBeanServerConnection connection;

    private ObjectName mxBeanObjectName;

    private MBeanInfo info;

    /**
     * @param connection
     *            the MBeanServerConnection to forward to.
     * @param mxBeanType
     *            the fully qualified name of an <code>MXBean</code>.
     * @param mxBeanName
     *            the name of a platform MXBean within connection to forward to. 
     * @throws IOException 
     */
    public OpenTypeMappingIHandler(MBeanServerConnection connection,
            String mxBeanType, String mxBeanName) throws IOException {
        this.connection = connection;
        setObjectName(mxBeanName);
        checkBeanIsRegistered();
        setInfo(mxBeanType);
    }

    /**
     * @param mxBeanType
     */
    private void setInfo(String mxBeanType) {
        this.info = ManagementUtils.getMBeanInfo(mxBeanType);
        if (info == null) {
            throw new IllegalArgumentException("Unknown MXBean type : "
                    + mxBeanType);
        }
    }

    /**
     * @param mxBeanName
     */
    private void setObjectName(String mxBeanName) {
        try {
            this.mxBeanObjectName = new ObjectName(mxBeanName);
        } catch (Exception e) {
            if (ManagementUtils.VERBOSE_MODE) {
                e.printStackTrace(System.err);
            }// end if 
            throw new IllegalArgumentException(
                    "Bean name not in valid format.", e);
        }
    }

    /**
     * @throws IOException 
     */
    private void checkBeanIsRegistered() throws IOException {
        if (!this.connection.isRegistered(this.mxBeanObjectName)) {
            throw new IllegalArgumentException("Not registered : "
                    + this.mxBeanObjectName);
        }
    }
    
    enum InvokeType {
        ATTRIBUTE_GETTER, ATTRIBUTE_SETTER, NOTIFICATION_OP, OPERATION
    };

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Object result = null;
        // Carry out the correct operation according to what the caller is
        // trying to do (set/get an attribute or invoke an operation. Each of
        // the below handler methods is expected to manage the conversion of
        // input args to open types and the conversion of return values from
        // an open type to a Java type.
        switch (getInvokeType(method.getName())) {
        case ATTRIBUTE_GETTER:
            result = invokeAttributeGetter(method);
            break;
        case ATTRIBUTE_SETTER:
            result = invokeAttributeSetter(method, args);
            break;
        case NOTIFICATION_OP:
            result = invokeNotificationEmitterOperation(method, args);
            break;
        default:
            if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
                result = "MXBeanProxy(" + connection + "[" + mxBeanObjectName + "])";
            } else if ("getObjectName".equals(method.getName()) && (args == null || args.length == 0)) {
                result = mxBeanObjectName;
            } else {
                result = invokeOperation(method, args);
            }
            break;
        }
        return result;
    }

    /**
     * Invoke the event notification operation described by the 
     * {@link Method}instance <code>method</code> on this handler's target object.
     * @param method
     *            describes the operation to be invoked on the target object
     * @param args
     *            the arguments to be used in the operation call
     * @return a <code>null</code> representing the <code>void</code> return 
     * from all {@link javax.management.NotificationEmitter} methods. 
     * @throws IOException
     * @throws InstanceNotFoundException
     * @throws ListenerNotFoundException
     */
    private Object invokeNotificationEmitterOperation(Method method,
            final Object[] args) throws InstanceNotFoundException, IOException,
            ListenerNotFoundException {
        Object result = null;
        
        if (method.getName().equals("addNotificationListener")) {
            try {
                AccessController
                        .doPrivileged(new PrivilegedExceptionAction<Object>() {
                            public Object run()
                                    throws InstanceNotFoundException,
                                    IOException {
                                connection.addNotificationListener(
                                        mxBeanObjectName,
                                        (NotificationListener) args[0],
                                        (NotificationFilter) args[1], args[2]);
                                return null;
                            }// end method run
                        });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) t;
                } else if (t instanceof IOException) {
                    throw (IOException) t;
                }
            } catch (RuntimeMBeanException e) {
                // RuntimeMBeanException wraps unchecked exceptions from the 
                // MBean server
                throw e.getTargetException();
            }// end catch
        } else if (method.getName().equals("getNotificationInfo")) {
            result = this.info.getNotifications();
        } else if (method.getName().equals("removeNotificationListener")) {
            if (args.length == 1) {
                try {
                    AccessController
                            .doPrivileged(new PrivilegedExceptionAction<Object>() {
                                public Object run()
                                        throws InstanceNotFoundException,
                                        ListenerNotFoundException, IOException {
                                    connection.removeNotificationListener(
                                            mxBeanObjectName,
                                            (NotificationListener) args[0]);
                                    return null;
                                }// end method run
                            });
                } catch (PrivilegedActionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof InstanceNotFoundException) {
                        throw (InstanceNotFoundException) t;
                    } else if (t instanceof ListenerNotFoundException) {
                        throw (ListenerNotFoundException) t;
                    } else if (t instanceof IOException) {
                        throw (IOException) t;
                    }
                } catch (RuntimeMBeanException e) {
                    // RuntimeMBeanException wraps unchecked exceptions from the 
                    // MBean server
                    throw e.getTargetException();
                }// end catch
            } else {
                try {
                    AccessController
                            .doPrivileged(new PrivilegedExceptionAction<Object>() {
                                public Object run()
                                        throws InstanceNotFoundException,
                                        ListenerNotFoundException, IOException {
                                    connection.removeNotificationListener(
                                            mxBeanObjectName,
                                            (NotificationListener) args[0],
                                            (NotificationFilter) args[1],
                                            args[2]);
                                    return null;
                                }// end method run
                            });
                } catch (PrivilegedActionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof InstanceNotFoundException) {
                        throw (InstanceNotFoundException) t;
                    } else if (t instanceof ListenerNotFoundException) {
                        throw (ListenerNotFoundException) t;
                    } else if (t instanceof IOException) {
                        throw (IOException) t;
                    }
                } catch (RuntimeMBeanException e) {
                    // RuntimeMBeanException wraps unchecked exceptions from the 
                    // MBean server
                    throw e.getTargetException();
                }// end catch
            }// end else
        }// end else if removeNotificationListener
        return result;
    }

    /**
     * Invoke the operation described by {@link Method}instance
     * <code>method</code> on this handler's target object.
     * <p>
     * All argument values are automatically converted to their corresponding
     * MBean open types.
     * </p>
     * <p>
     * If the method return is an MBean open type value it will be automatically
     * converted to a Java type by this method.
     * </p>
     * 
     * @param method
     *            describes the operation to be invoked on the target object
     * @param args
     *            the arguments to be used in the operation call
     * @return the returned value from the operation call on the target object
     *         with any MBean open type values being automatically converted to
     *         their Java counterparts.
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InstanceNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    private Object invokeOperation(final Method method, final Object[] args)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException, IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, SecurityException,
            IllegalArgumentException, NoSuchMethodException,
            InvocationTargetException {
        // For Java 5.0 platform MXBeans, no conversion
        // to open MBean types is necessary for any of the arguments.
        // i.e. they are all simple types.
        Object result = null;
        try {
            result = AccessController
                    .doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws InstanceNotFoundException,
                                MBeanException, ReflectionException,
                                IOException {
                            return connection.invoke(mxBeanObjectName, method
                                    .getName(), args,
                                    getOperationSignature(method));
                        }// end method run
                    });
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException)t;
            } else if (t instanceof MBeanException) {
                throw (MBeanException)t;
            } else if (t instanceof ReflectionException) {
                throw (ReflectionException)t;
            } else if (t instanceof IOException) {
                throw (IOException)t;
            }
        } catch (RuntimeMBeanException e) {
            // RuntimeMBeanException wraps unchecked exceptions from the 
            // MBean server
            throw e.getTargetException();
        }// end catch

        String realReturnType = method.getReturnType().getName();
        String openReturnType = getOperationOpenReturnType(method);

        if (!realReturnType.equals(openReturnType)) {
            result = ManagementUtils.convertFromOpenType(result, Class
                    .forName(openReturnType), Class.forName(realReturnType));
        }

        return result;
    }

    /**
     * For the method available on this handler's target object that is
     * described by the {@link Method}instance, return a string representation
     * of the return type after mapping to an open type.
     * 
     * @param method
     *            an instance of <code>Method</code> that describes a method
     *            on the target object.
     * @return a string containing the open return type of the method specified
     *         by <code>method</code>.
     */
    private String getOperationOpenReturnType(Method method) {
        String result = null;
        String[] methodSig = getOperationSignature(method);
        MBeanOperationInfo[] opInfos = this.info.getOperations();
        for (int i = 0; i < opInfos.length; i++) {
            MBeanOperationInfo opInfo = opInfos[i];
            if (opInfo.getName().equals(method.getName())) {
                MBeanParameterInfo[] opParams = opInfo.getSignature();
                if (opParams.length == methodSig.length) {
                    boolean matchFound = true;
                    for (int j = 0; j < opParams.length; j++) {
                        if (!opParams[j].getType().equals(methodSig[j])) {
                            matchFound = false;
                            break;
                        }// end if arg types do not match
                    }// end for each argument
                    if (matchFound) {
                        result = opInfo.getReturnType();
                        break;
                    }
                }// end if number of args matches
            }// end if method match found
        }// end for all operations

        return result;
    }

    /**
     * Obtain a string array of all of the argument types (in the correct order)
     * for the method on this handler's target object. The signature types will
     * all be open types.
     * 
     * @param method
     *            a {@link Method}instance that describes an operation on the
     *            target object.
     * @return an array of strings with each element holding the fully qualified
     *         name of the corresponding argument to the method. The order of
     *         the array elements corresponds exactly with the order of the
     *         method arguments.
     */
    private String[] getOperationSignature(Method method) {
        String[] result = null;
        Class[] args = method.getParameterTypes();
        result = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            Class clazz = args[i];
            result[i] = clazz.getName();
        }
        return result;
    }

    /**
     * Invoke the attribute set operation described by {@link Method}instance
     * <code>method</code> on this handler's target object.
     * <p>
     * All argument values are automatically converted to their corresponding
     * MBean open types.
     * </p>
     * 
     * @param method
     *            describes the operation to be invoked on the target object
     * @param args
     *            the arguments to be used in the operation call
     * @return the returned value from the operation call on the target object
     *         with any MBean open type values being automatically converted to
     *         their Java counterparts.
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InstanceNotFoundException
     * @throws AttributeNotFoundException
     * @throws InvalidAttributeValueException
     */
    private Object invokeAttributeSetter(final Method method,
            final Object[] args) throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, InvalidAttributeValueException {
        // In Java 5.0 platform MXBeans the following applies for all
        // attribute setter methods :
        // 1. no conversion to open MBean types necessary
        // 2. all setter arguments are single value (i.e. not array or
        // collection types).
        // 3. all return null
        try {
            AccessController
                    .doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws InstanceNotFoundException,
                                AttributeNotFoundException,
                                InvalidAttributeValueException, MBeanException,
                                ReflectionException, IOException {
                            connection.setAttribute(mxBeanObjectName,
                                    new Attribute(getAttribName(method),
                                            args[0]));
                            return null;
                        }// end method run
                    });
        } catch (PrivilegedActionException e) {
            // PrivilegedActionException wraps checked exceptions 
            Throwable t = e.getCause();
            if (t instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException) t;
            } else if (t instanceof AttributeNotFoundException) {
                throw (AttributeNotFoundException) t;
            } else if (t instanceof InvalidAttributeValueException) {
                throw (InvalidAttributeValueException) t;
            } else if (t instanceof MBeanException) {
                throw (MBeanException) t;
            } else if (t instanceof ReflectionException) {
                throw (ReflectionException) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            }
        } catch (RuntimeMBeanException e) {
            // RuntimeMBeanException wraps unchecked exceptions from the 
            // MBean server
            throw e.getTargetException();
        }// end catch
        
        return null;
    }

    /**
     * Invoke the attribute get operation described by {@link Method}instance
     * <code>method</code> on this handler's target object.
     * <p>
     * All returned values are automatically converted to their corresponding
     * MBean open types.
     * </p>
     * 
     * @param method
     *            describes the getter operation to be invoked on the target
     *            object
     * @return the returned value from the getter call on the target object with
     *         any MBean open type values being automatically converted to their
     *         Java counterparts.
     * @throws IOException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InstanceNotFoundException
     * @throws AttributeNotFoundException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    private Object invokeAttributeGetter(final Method method)
            throws AttributeNotFoundException, InstanceNotFoundException,
            MBeanException, ReflectionException, IOException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException, SecurityException,
            IllegalArgumentException, NoSuchMethodException,
            InvocationTargetException {
        Object result = null;
        String realReturnType = method.getReturnType().getName();
        String openReturnType = getAttrOpenType(method.getName());

        try {
            result = AccessController
                    .doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws AttributeNotFoundException,
                                InstanceNotFoundException, MBeanException,
                                ReflectionException, IOException {
                            return connection.getAttribute(mxBeanObjectName,
                                    getAttribName(method));
                        }// end method run
                    });
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof AttributeNotFoundException) {
                throw (AttributeNotFoundException)t;
            } else if (t instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException)t;
            } else if (t instanceof MBeanException) {
                throw (MBeanException)t;
            } else if (t instanceof ReflectionException) {
                throw (ReflectionException)t;
            } else if (t instanceof IOException) {
                throw (IOException)t;
            }
        } catch (RuntimeMBeanException e) {
            // RuntimeMBeanException wraps unchecked exceptions from the 
            // MBean server
            throw e.getTargetException();
        }// end catch
        
        if (!realReturnType.equals(openReturnType)) {
            result = ManagementUtils.convertFromOpenType(result, Class
                    .forName(openReturnType), Class.forName(realReturnType));
        }

        return result;
    }

    /**
     * For the target type associated with this invocation handler, returns the
     * name of the attribute that will be queried or updated by the Java method
     * described in <code>method</code>.
     * 
     * @param method
     *            a {@link Method}object describing a method on this handler's
     *            target object.
     * @return the name of the attribute that will be queried or modified by an
     *         invocation of the method described by <code>method</code>.
     */
    private String getAttribName(Method method) {
        String result = null;
        String methodName = method.getName();
        MBeanAttributeInfo[] attribs = info.getAttributes();

        if (methodName.startsWith("get")) {
            String attribName = methodName.substring("get".length());
            for (int i = 0; i < attribs.length; i++) {
                MBeanAttributeInfo attribInfo = attribs[i];
                if (attribInfo.getName().equals(attribName)
                        && attribInfo.isReadable()) {
                    result = attribInfo.getName();
                    break;
                }// end if
            }// end for
        }// end if

        if (result == null) {
            if (methodName.startsWith("is")) {
                String attribName = methodName.substring("is".length());
                for (int i = 0; i < attribs.length; i++) {
                    MBeanAttributeInfo attribInfo = attribs[i];
                    if (attribInfo.getName().equals(attribName)
                            && attribInfo.isReadable() && attribInfo.isIs()) {
                        result = attribInfo.getName();
                        break;
                    }// end if
                }// end for
            }// end if
        }

        if (result == null) {
            if (methodName.startsWith("set")) {
                String attribName = methodName.substring("set".length());
                for (int i = 0; i < attribs.length; i++) {
                    MBeanAttributeInfo attribInfo = attribs[i];
                    if (attribInfo.getName().equals(attribName)
                            && attribInfo.isWritable()) {
                        result = attribInfo.getName();
                        break;
                    }// end if
                }// end for
            }// end if
        }

        return result;
    }

    /**
     * Returns the name of the fully qualified open type of the attribute of the
     * target type that is obtained from a call to the <code>methodName</code>
     * method.
     * 
     * @param methodName
     *            the name of a getter method on an attribute of the target
     *            type.
     * @return the fully qualified name of the implied attribute's <i>open </i>
     *         type.
     */
    private String getAttrOpenType(String methodName) {
        MBeanAttributeInfo attrInfo = getAttribInfo(methodName);
        return attrInfo.getType();
    }

    /**
     * Obtain the {@link MBeanAttributeInfo}meta data for the attribute of this
     * invocation handler's target object that is returned by a call to the
     * method called <code>methodName</code>.
     * 
     * @param methodName
     *            the name of the getter method on the attribute under scrutiny.
     * @return the <code>MBeanAttributeInfo</code> that describes the
     *         attribute.
     */
    private MBeanAttributeInfo getAttribInfo(String methodName) {
        MBeanAttributeInfo result = null;
        MBeanAttributeInfo[] attribs = info.getAttributes();

        if (methodName.startsWith("get")) {
            String attribName = methodName.substring("get".length());
            for (int i = 0; i < attribs.length; i++) {
                MBeanAttributeInfo attribInfo = attribs[i];
                if (attribInfo.getName().equals(attribName)
                        && attribInfo.isReadable()) {
                    result = attribInfo;
                    break;
                }// end if
            }// end for
        }// end if

        if (result == null) {
            if (methodName.startsWith("is")) {
                String attribName = methodName.substring("is".length());
                for (int i = 0; i < attribs.length; i++) {
                    MBeanAttributeInfo attribInfo = attribs[i];
                    if (attribInfo.getName().equals(attribName)
                            && attribInfo.isReadable() && attribInfo.isIs()) {
                        result = attribInfo;
                        break;
                    }// end if
                }// end for
            }// end if
        }

        return result;
    }

    /**
     * Determine the type of invocation being made on the target object.
     * 
     * @param methodName
     * @return an instance of <code>InvokeType</code> corresponding to the
     *         nature of the operation that the caller is attemting to make on
     *         the target object.
     */
    private InvokeType getInvokeType(String methodName) {
        InvokeType result = null;

        if (methodName.startsWith("get")) {
            String attribName = methodName.substring("get".length());
            MBeanAttributeInfo[] attribs = info.getAttributes();
            for (int i = 0; i < attribs.length; i++) {
                MBeanAttributeInfo attribInfo = attribs[i];
                if (attribInfo.getName().equals(attribName)
                        && attribInfo.isReadable()) {
                    result = InvokeType.ATTRIBUTE_GETTER;
                    break;
                }// end if
            }// end for
        }// end if

        if (result == null) {
            if (methodName.startsWith("is")) {
                String attribName = methodName.substring("is".length());
                MBeanAttributeInfo[] attribs = info.getAttributes();
                for (int i = 0; i < attribs.length; i++) {
                    MBeanAttributeInfo attribInfo = attribs[i];
                    if (attribInfo.getName().equals(attribName)
                            && attribInfo.isReadable() && attribInfo.isIs()) {
                        result = InvokeType.ATTRIBUTE_GETTER;
                        break;
                    }// end if
                }// end for
            }// end if
        }

        if (result == null) {
            if (methodName.startsWith("set")) {
                String attribName = methodName.substring("set".length());
                MBeanAttributeInfo[] attribs = info.getAttributes();
                for (int i = 0; i < attribs.length; i++) {
                    MBeanAttributeInfo attribInfo = attribs[i];
                    if (attribInfo.getName().equals(attribName)
                            && attribInfo.isWritable()) {
                        result = InvokeType.ATTRIBUTE_SETTER;
                        break;
                    }// end if
                }// end for
            }// end if
        }

        if (result == null) {
            Method[] neMethods = NotificationEmitter.class.getMethods();
            for (int i = 0; i < neMethods.length; i++) {
                if (neMethods[i].getName().equals(methodName)) {
                    result = InvokeType.NOTIFICATION_OP;
                    break;
                }// end if
            }// end for
        }
        
        // If not a getter or setter or a notification emitter method then
        // must be a vanilla DynamicMXBean operation.
        if (result == null) {
            result = InvokeType.OPERATION;
        }
        return result;
    }
}

/*
 * $Log$
 * Revision 1.9  2005/07/07 15:44:46  gharley
 * Allow runtime exceptions to reach caller (like reference impl)
 *
 * Revision 1.8  2005/06/21 09:47:24  gharley
 * Add in security "doPrivileged" code to get attributes, set attributes and invoke operations on an MBeanServerConnection.
 *
 * Revision 1.7  2005/06/13 10:02:03  gharley
 * Only inform user of non-fatal exceptions if the system property "com.ibm.lang.management" is set.
 *
 * Revision 1.6  2005/05/25 20:23:35  gharley
 * Updated unit test bucket now that 5.0 VM available that enables direct testing of the MXBeans. Shook out one or two bugs in the code. Note that OperatingSystemMXBeans are still broken & that there is a problem with MemoryMXBean.getNonHeapmemoryUsage(). Awaiting new VM before tackling these...
 *
 * Revision 1.5  2005/04/17 20:45:55  gharley
 * Fixed the invokeNotificationEmitterOperation() method to make it capable of returning back to the caller the notifications objects for the proxied bean.
 *
 * Revision 1.4  2005/02/22 11:40:48  gharley
 * Removed a small number of errors flagged by the Eclipse 3.1 M5 Java compiler.
 *
 * Revision 1.3  2005/02/10 13:56:44  gharley
 * Added in support for platform bean dynamic proxies being implementors of the
 * NotificationEmitter interface (e.g. MemoryMXBean)
 *
 * Revision 1.2  2005/02/09 22:25:02  gharley
 * Horrible code for platform beans dynamic proxy support.
 * Revision 1.1 2005/02/02 14:00:07
 * gharley Initial version. Incomplete !!
 * 
 */
