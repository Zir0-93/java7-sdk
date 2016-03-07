
package java.lang;

import java.lang.ref.SoftReference;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.lang.reflect.Proxy;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2014  All Rights Reserved
 */

class J9VMInternals {

	private static final int j9Version = 0x120E026A;

	private static final long j9Config = 0x7363617237306200L;	// 'scar70b\0'

	private static final int UNINITIALIZED = 0;
	private static final int INITIALIZED = 1;
	private static final int FAILED = 2;
	private static final int UNVERIFIED = 3;
	private static final int UNPREPARED = 4;
	private static final int STATUS_MASK = 255;

	// cannot create any instances in <clinit> in this special class
	private static Map exceptions;

	private static boolean initialized;

	/*
	 * Called by the vm after everything else is initialized.
	 */
	private static void completeInitialization() {
		initialized = true;
		exceptions = new WeakHashMap();
		ClassLoader.completeInitialization();
		Thread.currentThread().completeInitialization();
	}

	private static native void sendClassPrepareEvent(Class clazz);

	/**
	 * Check whether -Xaggressive is option is on or not.
	 *
	 * @return true if -Xaggressive is on.
	 * 		   false if -Xaggressive is off.
	 */
	static native boolean isXaggressiveImpl();

	/**
	 * Verify the specified Class using the VM byte code verifier.
	 *
	 * @param clazz the Class to verify.
	 *
	 * @throws VerifyError if the Class cannot be verified
	 */
	static void verify(Class clazz) {
		ClassInitializationLock initializationLock = clazz.initializationLock;
		if (initializationLock == null) {
			/* initializationLock == null means initialization successfully completed */
			return;
		}

		boolean isPendingInterrupt = false;
		try {
		while (true) {
			switch (getInitStatus(clazz)) {
				case INITIALIZED:
				case UNINITIALIZED:
				case FAILED:
				case UNPREPARED:
					return;

				case UNVERIFIED: {
					Class superclass;

					synchronized (initializationLock) {
						if (getInitStatus(clazz) != UNVERIFIED) break;
						setInitThread(clazz);
					}
					superclass = getSuperclass(clazz);
					boolean succeeded = false;
					try {
						// verify the superclass
						if (superclass != null)
							verify(superclass);
						// verify this class
						verifyImpl(clazz);
						succeeded = true;
					} finally {
						if (!succeeded) {
							setInitStatus(clazz, UNVERIFIED);
						}
					}
					synchronized (initializationLock) {
						if (getInitThread(clazz) && ((getInitStatus(clazz) & STATUS_MASK) == UNVERIFIED)) {
							setInitStatus(clazz, UNPREPARED);
						}
					}
					return;
				}

				default: // INPROGRESS
					synchronized (initializationLock) {
						int status = getInitStatus(clazz);

						if (((status & ~STATUS_MASK) == 0)) break;
						if ((status & STATUS_MASK) != UNVERIFIED) return;
						if (!getInitThread(clazz)) {
							try { initializationLock.wait(); } catch (InterruptedException e) {
								isPendingInterrupt = true;
							}
							break;
						}
					}

					boolean succeeded = false;
					try {
						verifyImpl(clazz);
						succeeded = true;
					} finally {
						if (!succeeded) {
							setInitStatus(clazz, UNVERIFIED);
						}
					}
					synchronized (initializationLock) {
						if (getInitThread(clazz) && ((getInitStatus(clazz) & STATUS_MASK) == UNVERIFIED)) {
							setInitStatus(clazz, UNPREPARED);
						}
					}
			}
		}
		} finally {
			if (isPendingInterrupt) {
				Thread.currentThread().interrupt();
			}
		}
	}

	// verifyImpl may throw a Throwable, however its undeclared so the Throwable
	// does not need to be wrapped in an Error or RuntimeException
	private static native void verifyImpl(Class clazz);

	/**
	 * Sent internally by the VM to initiatiate
	 * initialization of the receiver.  See chapter
	 * 2.17.5 of the JVM Specification (2nd ed)
	 *
	 * @throws		Throwable
	 */
	private static void initialize(Class clazz) throws Throwable {
		ClassInitializationLock initializationLock = clazz.initializationLock;
		if (initializationLock == null) {
			/* initializationLock == null means initialization successfully completed */
			return;
		}

		boolean isPendingInterrupt = false;
		try {
		while (true) {
			switch (getInitStatus(clazz)) {
				case INITIALIZED:
					return;
				case UNVERIFIED:
					verify(clazz);
					break;
				case UNPREPARED:
					prepare(clazz);
					break;
				case FAILED:
					NoClassDefFoundError notFound = new NoClassDefFoundError(clazz.getName() + " (initialization failure)"); //$NON-NLS-1$
					// if exceptions is null, we're initializing and running single threaded
					if (exceptions != null) {
						synchronized(exceptions) {
							SoftReference weakReason = (SoftReference)exceptions.get(clazz);
							if (weakReason != null) {
								Throwable reason = (Throwable)weakReason.get();
								if (reason != null) {
									reason = copyThrowable(reason);
									notFound.initCause(reason);
								}
							}
						}
					}
					throw notFound;
				case UNINITIALIZED: {
					Class superclass;

					synchronized (initializationLock) {
						if (getInitStatus(clazz) != UNINITIALIZED) break;
						setInitThread(clazz);
					}

					// initialize the superclass
					superclass = getSuperclass(clazz);
					if (superclass != null) {
						try {
							initialize(superclass);
						} catch (Error err) {
							setInitStatus(clazz, FAILED);
							if (initialized) {
								// if exceptions is null, we're initializing and running single threaded
								if (exceptions == null)
									exceptions = new WeakHashMap();
								synchronized(exceptions) {
									Throwable cause = err;
									if (err instanceof ExceptionInInitializerError) {
										cause = ((ExceptionInInitializerError)err).getException();
									}
									exceptions.put(clazz, new SoftReference(copyThrowable(cause)));
								}
							}
							throw err;
						}
					}

					// initialize this class
					try {
						/* When we are initializing the statics of the class
						 * we want to be in the correct memory space for the class loader.
						 * If the class loader does not have a memory space, then we want
						 * to initialize it in the base memory space.  In the situation where
						 * we are not in multi-memory space mode, we will get null back when
						 * we request the memory space for the class loader, and we won't need
						 * to switch memory spaces.  If we have the system class loader then we
						 * want to initialize in the base memory space.  Their are problems treating
						 * the system class loader the same as others because not everything required
						 * by MemorySpace.getMemorySpaceForClassLoader has been initialized the first
						 * time the method is called.
						 */
						initializeImpl(clazz);
					} catch (Error err) {
						setInitStatus(clazz, FAILED);
						if (initialized) {
							// if exceptions is null, we're initializing and running single threaded
							if (exceptions == null)
								exceptions = new WeakHashMap();
							synchronized(exceptions) {
								exceptions.put(clazz, new SoftReference(copyThrowable(err)));
							}
						}
						throw err;
					} catch (Throwable t) {
						setInitStatus(clazz, FAILED);
						if (initialized) {
							// if exceptions is null, we're initializing and running single threaded
							if (exceptions == null)
								exceptions = new WeakHashMap();
							synchronized(exceptions) {
								exceptions.put(clazz, new SoftReference(copyThrowable(t)));
							}
						}
						throw new ExceptionInInitializerError(t);
					}

					setInitStatus(clazz, INITIALIZED);
					clazz.initializationLock = null;
					return;
				}

				default: // INPROGRESS
					synchronized (initializationLock) {
						int status = getInitStatus(clazz);

						if ((status & ~STATUS_MASK) == 0) break;
						if ((status & STATUS_MASK) == UNINITIALIZED) {
							if (getInitThread(clazz)) return;
							try { initializationLock.wait(); } catch (InterruptedException e) {
								isPendingInterrupt = true;
								}
							break;
						}
					}
					verify(clazz);
			}
		}
		} finally {
			if (isPendingInterrupt) {
				Thread.currentThread().interrupt();
			}

		}
	}

	private static native Throwable newInstance(Class exceptionClass, Class constructorClass);

	private static Throwable cloneThrowable(final Throwable throwable, final HashMap hashMapThrowable) {
		return (Throwable)AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				Throwable clone;
				try {
					Class cls = throwable.getClass();
					clone = newInstance(cls, Object.class);
					while (cls != null) {
						Field[] fields = cls.getDeclaredFields();
						for (int i=0; i<fields.length; i++) {
							if (!Modifier.isStatic(fields[i].getModifiers()) &&
									!(cls == Throwable.class && fields[i].getName().equals("walkback")))
							{
								fields[i].setAccessible(true);
								Object value;
								if (cls == Throwable.class && fields[i].getName().equals("cause")) {
									value = clone;
								} else {
									value = fields[i].get(throwable);
									//	Only copy throwable fields whose stacktrace might be kept within Map exceptions
									//	The throwable stored within Map exceptions as WeakReference could be retrieved (before being GCed) later
									if (value instanceof Throwable) {
										value = copyThrowable((Throwable)value, hashMapThrowable);
									}
								}
								fields[i].set(clone, value);
							}
						}
						cls = getSuperclass(cls);
					}
				} catch (Throwable e) {
					clone = new Throwable("Error cloning Throwable (" + e + "). The original exception was: " + throwable.toString());
				}
				return clone;
			}
		});
	}

	/**
	 * Entry method to copy the specified Throwable, invoke copyThrowable(Throwable, HashMap)
	 * to check loop such that we don't go infinite.
	 *
	 * @param throwable the Throwable to copy
	 *
	 * @return a copy of the Throwable
	 */
	private static Throwable copyThrowable(Throwable throwable) {
		HashMap hashMapThrowable = new HashMap();
		return copyThrowable(throwable, hashMapThrowable);
	}

	/**
	 * Copy the specified Throwable, wrapping the stack trace for each
	 * Throwable. Check for loops so we don't go infinite.
	 *
	 * @param throwable the Throwable to copy
	 * @param hashMapThrowable the Throwables already cloned
	 *
	 * @return a copy of the Throwable or itself if it has been cloned already
	 */
	private static Throwable copyThrowable(Throwable throwable, HashMap hashMapThrowable) {
		if (hashMapThrowable.get(throwable) != null) {
			//	stop recursive call here when the throwable has been cloned
			return	throwable;
		}
		hashMapThrowable.put(throwable, throwable);
		Throwable root = cloneThrowable(throwable, hashMapThrowable);
		root.setStackTrace(throwable.getStackTrace());
		Throwable parent = root;
		Throwable cause = throwable.getCause();
		//	looking for causes recursively which will be part of stacktrace	stored into Map exceptions
		while (cause != null && hashMapThrowable.get(cause) == null) {
			hashMapThrowable.put(cause, cause);
			Throwable child = cloneThrowable(cause, hashMapThrowable);
			child.setStackTrace(cause.getStackTrace());
			parent.setCause(child);
			parent = child;
			cause = cause.getCause();
		}
		return root;
	}

	/**
	 * Used to indicate the end of class initialization.
	 * Sets the initialization status and notifies any
	 * threads which may have been waiting for
	 * initialization to complete
	 *
	 * @param		status
	 *					INITIALIZED (1)
	 *					FAILED (2)
	 *
	 */
	private static void setInitStatus(Class clazz, int status) {
		ClassInitializationLock initializationLock = clazz.initializationLock;
		/* initializationLock == null means initialization successfully completed.
		 * Trying to change the init status after this is an error, so no need for
		 * a null check here.
		 *
		 */

		synchronized(initializationLock) {
			setInitStatusImpl(clazz, status);
			initializationLock.notifyAll();
		}
	}

	/**
	 * Answers the receiver's initialization status
	 *
	 * @return		status
	 *					UNINITIALIZED (0)
	 *					INITIALIZED (1)
	 *					FAILED (2)
	 *					INPROGRESS (0xnnnnnnn[048C])
	 *
	 */
	private static native int getInitStatus(Class clazz);

	/**
	 * Set the receiver's initialization status
	 *
	 * @param		status
	 *					INITIALIZED (1)
	 *					FAILED (2)
	 *
	 */
	private static native void setInitStatusImpl(Class clazz, int status);

	/**
	 * Run the receiver's <clinit> method and initialize
	 * any static variables
	 *
	 * @throws		Throwable Any exception may be thrown
	 */
	private static native void initializeImpl(Class clazz) throws Throwable;

	/**
	 * Answers true if the current thread is currently
	 * initializing this class
	 *
	 * @return		true if the current thread is initializing the receiver
	 *
	 */
	private static native boolean getInitThread(Class clazz);

	/**
	 * Set the receiver's initialize status to be 'in
	 * progress' and save the current thread as the
	 * initializing thread.
	 */
	private static native void setInitThread(Class clazz);

	/**
	 * Private method to be called by the VM after a Threads dies and throws ThreadDeath
	 * It has to <code>notifyAll()</code> so that <code>join</code> can work properly.
	 * However, it has to be done when the Thread is "thought of" as being dead by other
	 * observer Threads (<code>isAlive()</code> has to return false for the Thread
	 * running this method or <code>join</code> may never return)
	 *
	 * @author		OTI
	 * @version		initial
	 */
	private static void threadCleanup(Thread thread) {
		// don't synchronize the remove! Otherwise deadlock may occur
		try {
			// Leave the ThreadGroup. This is why remove can't be private
			thread.group.remove(thread);
		}
		finally {
			thread.cleanup();

			synchronized(thread) {
				thread.notifyAll();
			}
		}
	}

	private static void checkPackageAccess(final Class clazz, ProtectionDomain pd) {
		final SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					sm.checkPackageAccess(clazz.getPackageName());
					if (Proxy.isProxyClass(clazz)) {
						ClassLoader	cl = getClassLoader(clazz);
						sun.reflect.misc.ReflectUtil.checkProxyPackageAccess(cl, clazz.getInterfaces());
					}
					return null;
				}
			}, new AccessControlContext(new ProtectionDomain[]{pd}));
		}
	}

	private static void runFinalize(Object obj) {
		try {
			obj.finalize();
		} catch(Throwable e) {}
	}

	static native StackTraceElement[] getStackTrace(Throwable throwable, boolean pruneConstructors);

	/**
	 * Prepare the specified Class. Fill in initial field values, and send
	 * the class prepare event.
	 *
	 * @param clazz the Class to prepare
	 */
	static void prepare(Class clazz) {
		ClassInitializationLock initializationLock = clazz.initializationLock;
		if (initializationLock == null) {
			/* initializationLock == null means initialization successfully completed */
			return;
		}

		while (true) {
			switch (getInitStatus(clazz)) {
				case INITIALIZED:
				case UNINITIALIZED:
				case FAILED:
					return;

				case UNVERIFIED:
					verify(clazz);
					break;

				case UNPREPARED: {
					Class superclass;

					superclass = getSuperclass(clazz);
					// prepare the superclass and direct superinterfaces
					if (superclass != null)
						prepare(superclass);
					Class interfaces[] = getInterfaces(clazz);
					for (int i = 0; i < interfaces.length; ++i) {
						prepare(interfaces[i]);
					}

					synchronized (initializationLock) {
						if (getInitStatus(clazz) != UNPREPARED) break;
						setInitStatus(clazz, UNINITIALIZED);
					}
					sendClassPrepareEvent(clazz);
					return;
				}

				default: // INPROGRESS
					// This cannot happen
					return;
			}
		}
	}

	/**
	 * Determines the superclass of specified <code>clazz</code>.
	 * @param clazz The class to introspect (must not be null).
	 * @return The superclass, or null for primitive types and interfaces.
	 */
	static native Class getSuperclass(Class clazz);

	/**
	 * Determines the interfaces implemented by <code>clazz</code>.
	 * @param clazz The class to introspect (must not be null).
	 * @return An array of all interfaces supported by <code>clazz</code>.
	 */
	static native Class[] getInterfaces(Class clazz);

	/**
	 * Determines the ClassLoader for <code>clazz</code>.
	 * @param clazz The class to introspect (must not be null).
	 * @return The ClassLoader which loaded <code>clazz</code>.
	 */
	static native ClassLoader getClassLoader(Class clazz);

	/**
	 * Answers a new instance of the class represented by the
	 * <code>clazz</code>, created by invoking the default (i.e. zero-argument)
	 * constructor. If there is no such constructor, or if the
	 * creation fails (either because of a lack of available memory or
	 * because an exception is thrown by the constructor), an
	 * InstantiationException is thrown. If the default constructor
	 * exists, but is not accessible from the context where this
	 * message is sent, an IllegalAccessException is thrown.
	 *
	 * @param clazz The class to create an instance of.
	 * @return		a new instance of the class represented by the receiver.
	 * @throws		IllegalAccessException if the constructor is not visible to the sender.
	 * @throws		InstantiationException if the instance could not be created.
	 */
	native static Object newInstanceImpl(Class clazz)
		throws IllegalAccessException, InstantiationException;

	/**
	 * A class used for synchronizing Class initialization.
	 *
	 * The instance field is currently only for debugging purposes.
	 */
	static class ClassInitializationLock {
		Class theClass;
	}
}
