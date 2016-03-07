package org.apache.harmony.kernel.vm;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2006, 2014  All Rights Reserved
 */

/**
 * This class must be implemented by the vm vendor.
 *
 * Represents the running virtual machine. All VM specific API
 * are implemented on this class.
 * <p>
 * Note that all methods in VM are static. There is no singleton
 * instance which represents the actively running VM.
 */
public final class VM {

/**
 * Prevents this class from being instantiated.
 */
private VM() {
}

/**
 * Returns the ClassLoader of the method (including natives) at the
 * specified depth on the stack of the calling thread. Frames representing
 * the VM implementation of java.lang.reflect are not included in the list.
 *
 * This is not a public method as it can return the bootstrap class
 * loader, which should not be accessed by non-bootstrap classes.
 *
 * Notes: <ul>
 * 	 <li> This method operates on the defining classes of methods on stack.
 *		NOT the classes of receivers. </li>
 *
 *	 <li> The item at depth zero is the caller of this method </li>
 * </ul>
 *
 * @param depth the stack depth of the requested ClassLoader
 * @return the ClassLoader at the specified depth
 *
 * @see java.lang.ClassLoader#getStackClassLoader
 */
@sun.reflect.CallerSensitive
private static final native ClassLoader getStackClassLoader(int depth);

/**
 * This method must be included, as it is used by ResourceBundle.getBundle(),
 * and other places as well. The reference implementation of this method uses
 * the getStackClassLoader() method.
 *
 * Returns the ClassLoader of the method that called the caller.
 * i.e. A.x() calls B.y() calls callerClassLoader(),
 * A's ClassLoader will be returned. Returns null for the
 * bootstrap ClassLoader.
 *
 * @return 		a ClassLoader or null for the bootstrap ClassLoader
 *
 * @throws SecurityException when called from a non-bootstrap Class
 */
@sun.reflect.CallerSensitive
public static ClassLoader callerClassLoader() {
	ClassLoader loader = getStackClassLoader(2);
	ClassLoader caller = getStackClassLoader(1);
	ClassLoader bootLoader = getStackClassLoader(0);
	if (caller != bootLoader)
		throw new SecurityException();
	if (loader == getStackClassLoader(0)) return null;
	return loader;
}

/**
 * This method must be provided by the vm vendor, as it is used
 * by org.apache.harmony.luni.vm.MsgHelp.setLocale() to get the bootstrap
 * ClassLoader. MsgHelp uses the bootstrap ClassLoader to find the
 * resource bundle of messages packaged with the bootstrap classes.
 * The reference implementation of this method uses the getStackClassLoader()
 * method.
 *
 * Returns the ClassLoader of the method that called the caller.
 * i.e. A.x() calls B.y() calls callerClassLoader(),
 * A's ClassLoader will be returned. Returns null for the
 * bootstrap ClassLoader.
 *
 * @return 		a ClassLoader
 *
 * @throws SecurityException when called from a non-bootstrap Class
 */
@sun.reflect.CallerSensitive
public static ClassLoader bootCallerClassLoader() {
	ClassLoader loader = getStackClassLoader(2);
	ClassLoader caller = getStackClassLoader(1);
	ClassLoader bootLoader = getStackClassLoader(0);
	if (caller != bootLoader)
		throw new SecurityException();
	return loader;
}

}

