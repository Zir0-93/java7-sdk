package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012, 2014  All Rights Reserved
 */

import com.ibm.oti.vm.*;

/**
 * Helper class to allow privileged access to classes
 * from outside the java.lang package. Based on sun.misc.SharedSecrets
 * implementation.
 */
class VMAccess implements VMLangAccess {

	private static ClassLoader extClassLoader;

	/**
	 * Set the extension class loader. It can only be set once.
	 *
	 * @param loader the extension class loader
	 */
	static void setExtClassLoader(ClassLoader loader) {
		if (null == extClassLoader) {
			extClassLoader = loader;
		}
	}

	/**
	 * Answer the extension class loader.
	 */
	public ClassLoader getExtClassLoader() {
		return extClassLoader;
	}

	/**
	 * Returns true if parent is the ancestor of child.
	 * Parent and child must not be null.
	 */
	public boolean isAncestor(java.lang.ClassLoader parent, java.lang.ClassLoader child) {
		return parent.isAncestorOf(child);
	}

	/**
	 * Returns the ClassLoader off clazz.
	 */
	public java.lang.ClassLoader getClassloader(java.lang.Class clazz) {
		return J9VMInternals.getClassLoader(clazz);
	}

	/**
	 * Returns the package name for a given class.
	 * clazz must not be null.
	 */
	public java.lang.String getPackageName(java.lang.Class clazz){
		return clazz.getPackageName();
	}
}
