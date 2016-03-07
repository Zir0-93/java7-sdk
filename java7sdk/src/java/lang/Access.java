package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2007, 2014  All Rights Reserved
 */

import java.security.AccessControlContext;

import sun.misc.JavaLangAccess;
import sun.nio.ch.Interruptible;
import sun.reflect.ConstantPool;
import sun.reflect.annotation.AnnotationType;

/**
 * Helper class used by the Sun JDK to allow privileged access to classes
 * from outside the java.lang package.  The sun.misc.SharedSecrets class
 * uses an instance of this class to access private java.lang members.
 *
 * @author		OTI
 * @version		initial
 */

class Access implements JavaLangAccess {

	/** Set thread's blocker field. */
	public void blockedOn(java.lang.Thread thread, Interruptible interruptable) {
		thread.blockedOn(interruptable);
	}

    /**
     * Get the AnnotationType instance corresponding to this class.
     * (This method only applies to annotation types.)
     */
	public AnnotationType getAnnotationType(java.lang.Class arg0) {
		return arg0.getAnnotationType();
	}

	/** Return the constant pool for a class. */
	public native ConstantPool getConstantPool(java.lang.Class arg0);

    /**
     * Returns the elements of an enum class or null if the
     * Class object does not represent an enum type;
     * the result is uncloned, cached, and shared by all callers.
     */
	public <E extends Enum<E>> E[] getEnumConstantsShared(java.lang.Class<E> arg0) {
		return arg0.getEnumConstantsShared();
	}

	public void registerShutdownHook(int arg0, boolean arg1, Runnable arg2) {
		Shutdown.add(arg0, arg1, arg2);
	}

    /**
     * Set the AnnotationType instance corresponding to this class.
     * (This method only applies to annotation types.)
     */
	public void setAnnotationType(java.lang.Class arg0, AnnotationType arg1) {
		arg0.setAnnotationType(arg1);
	}

	public int getStackTraceDepth(java.lang.Throwable arg0) {
		return arg0.getInternalStackTrace().length;
	}

	public java.lang.StackTraceElement getStackTraceElement(java.lang.Throwable arg0, int arg1) {
		return arg0.getInternalStackTrace()[arg1];
	}

	/**
	 * Returns an alternative hash code for a given String.
	 */
	public int getStringHash32(String string) {
		return string.hash32();
	}

	public Thread newThreadWithAcc(Runnable runnable, AccessControlContext acc) {
		return new Thread(runnable, acc);
	}
}
