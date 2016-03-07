package java.lang;

import java.io.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import com.ibm.oti.vm.AbstractClassLoader;
import java.net.URL;
import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.WeakHashMap;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.security.cert.Certificate;
import java.lang.ref.SoftReference;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2014  All Rights Reserved
 */

/**
 * ClassLoaders are used to dynamically load, link and install
 * classes into a running image.
 *
 * @version		initial
 */
public abstract class ClassLoader {
	private static CodeSource defaultCodeSource = new CodeSource(null, (Certificate[])null);

	/**
	 * This is the bootstrap ClassLoader
	 */
	static ClassLoader bootstrapClassLoader;
	/*
	 * This is the application ClassLoader
	 */
	private static ClassLoader applicationClassLoader;
	private static boolean initSystemClassLoader;
	private static boolean reflectCacheEnabled;
	private static boolean reflectCacheAppOnly = true;
	private static boolean reflectCacheDebug;

	private long vmRef;
	ClassLoader parent;

	private boolean initDone;

	private static boolean checkAssertionOptions;
	private static class AssertionLock {}
	private Object assertionLock = new AssertionLock();
  	private boolean defaultAssertionStatus;
  	private Map packageAssertionStatus;
  	private Map classAssertionStatus;
	private volatile Hashtable genericRepository;
	private volatile Hashtable annotationCache;
  	private volatile Hashtable packages;
	private static class LazyInitLock {}
  	Object lazyInitLock = new LazyInitLock();
	private volatile Hashtable classSigners; // initialized if needed
	private volatile Hashtable packageSigners;
	private static Certificate[] emptyCertificates = new Certificate[0];
	private volatile ProtectionDomain defaultProtectionDomain;

	//	store parallel capable classloader classes
	private static Map<Class, Object> parallelCapableCollection;
	//	store class binary name based lock
	private volatile Hashtable	classNameBasedLock;
	//	for performance purpose, only check once if registered as parallel capable
	//	assume customer classloader follow Oracle requirement
	//	in which registerAsParallelCapable shall be invoked during initialization
	private boolean isParallelCapable;
	private static class ClassNameBasedLock {}

	private static boolean allowArraySyntax;
	private static boolean lazyClassLoaderInit = false;

	static final void initializeClassLoaders() {
		if (bootstrapClassLoader != null) return;

			parallelCapableCollection = Collections.synchronizedMap(new WeakHashMap<Class, Object>());

		String allowValue = System.getProperty("sun.lang.ClassLoader.allowArraySyntax");
		if (allowValue != null) allowValue = allowValue.toLowerCase();
		if ("true".equals(allowValue)) {
			allowArraySyntax = true;
		}

		String propValue = System.getProperty("reflect.cache");
		if (propValue != null) propValue = propValue.toLowerCase();
		/* Do not enable reflect cache if -Dreflect.cache=false is in commandline */
		if (!"false".equals(propValue)) {
			reflectCacheEnabled = true;
			if (propValue != null) {
				java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(propValue, ",");
				while (tokenizer.hasMoreTokens()) {
					String value = tokenizer.nextToken();
					/* reflect.cache=boot is handled in completeInitialization() */
					if ("debug".equals(value)) reflectCacheDebug = true;
				}
			}
		}

		try {
			/* CMVC 179008 - b143 needs ProtectionDomain initialized here */
			Class.forName("java.security.ProtectionDomain");
		} catch(ClassNotFoundException e) {
		}

		ClassLoader sysTemp = null;
		// Proper initialization requires BootstrapLoader is the first loader instantiated
		String systemLoaderString = System.getProperty("systemClassLoader");
		if(null == systemLoaderString) {
			sysTemp = com.ibm.oti.vm.BootstrapClassLoader.singleton();
		} else {
			try {
				sysTemp = (ClassLoader)Class.forName(systemLoaderString,true,null).newInstance();
			} catch(Throwable x) {
				x.printStackTrace();
				System.exit(1);
			}
		}
		bootstrapClassLoader = sysTemp;
		AbstractClassLoader.setBootstrapClassLoader(bootstrapClassLoader);
		applicationClassLoader = bootstrapClassLoader;

		applicationClassLoader = sun.misc.Launcher.getLauncher().getClassLoader();

		/* Find the extension class loader */
		ClassLoader tempLoader = applicationClassLoader;
		while (tempLoader.parent != null) {
			tempLoader = tempLoader.parent;
		}
		VMAccess.setExtClassLoader(tempLoader);

		if (reflectCacheEnabled) Class.initCacheIds();

		lazyClassLoaderInit = true;
		String lazyValue = System.getProperty("java.lang.ClassLoader.lazyInitialization");
		if (lazyValue != null) {
			lazyValue = lazyValue.toLowerCase();
			if ("false".equals(lazyValue)) {
				lazyClassLoaderInit = false;
			}
		}
	}

/**
 * Constructs a new instance of this class with the system
 * class loader as its parent.
 *
 * @exception	SecurityException
 *					if a security manager exists and it does not
 *					allow the creation of new ClassLoaders.
 */
protected ClassLoader() {
	this(applicationClassLoader);
}

/**
 * Constructs a new instance of this class with the given
 * class loader as its parent.
 *
 * @param		parentLoader ClassLoader
 *					the ClassLoader to use as the new class
 *					loaders parent.
 * @exception	SecurityException
 *					if a security manager exists and it does not
 *					allow the creation of new ClassLoaders.
 * @exception	NullPointerException
 *					if the parent is null.
 */
protected ClassLoader(ClassLoader parentLoader) {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkCreateClassLoader();

	if (parallelCapableCollection.containsKey(this.getClass())) {
		isParallelCapable = true;
	}

	// VM Critical: must set parent before calling initializeInternal()
	// K0546 = Uninitialized class loader
	if (parentLoader != null ) {
		if (parentLoader.initDone == false) {
			throw new SecurityException(com.ibm.oti.util.Msg.getString("K0546")); //$NON-NLS-1$
		}
	}
	parent = parentLoader;
	if (bootstrapClassLoader != null) {
		if (!lazyClassLoaderInit) {
			com.ibm.oti.vm.VM.initializeClassLoader(this, false, isParallelCapable);
		}
	}
	initializeClassLoaderAssertStatus();

	initDone = true;
}

/**
 * Constructs a new class from an array of bytes containing a
 * class definition in class file format.
 *
 * @param 		classRep byte[]
 *					a memory image of a class file.
 * @param 		offset int
 *					the offset into the classRep.
 * @param 		length int
 *					the length of the class file.
 *
 * @return	the newly defined Class
 *
 * @throws ClassFormatError when the bytes are invalid
 *
 * @deprecated Use defineClass(String, byte[], int, int)
 */
@Deprecated
protected final Class<?> defineClass (byte [] classRep, int offset, int length) throws ClassFormatError {
	return defineClass ((String) null, classRep, offset, length);
}

/**
 * Constructs a new class from an array of bytes containing a
 * class definition in class file format.
 *
 * @param 		className java.lang.String
 *					the name of the new class
 * @param 		classRep byte[]
 *					a memory image of a class file
 * @param 		offset int
 *					the offset into the classRep
 * @param 		length int
 *					the length of the class file
 *
 * @return	the newly defined Class
 *
 * @throws ClassFormatError when the bytes are invalid
 */
protected final Class<?> defineClass(String className, byte[] classRep, int offset, int length) throws ClassFormatError {
	return defineClass(className, classRep, offset, length, null);
}

private String checkClassName(String className) {
	int index;
	if((index = className.lastIndexOf('.')) >= 0) {
		String packageName = className.substring(0, index);
		if (className.startsWith("java.")) {
			// K01d2 = {1} - protected system package '{0}'
			throw new SecurityException(com.ibm.oti.util.Msg.getString("K01d2", packageName, className));
		}
		return packageName;
	}
	return "";
}

/**
 * Constructs a new class from an array of bytes containing a
 * class definition in class file format and assigns the new
 * class to the specified protection domain.
 *
 * @param 		className java.lang.String
 *					the name of the new class.
 * @param 		classRep byte[]
 *					a memory image of a class file.
 * @param 		offset int
 *					the offset into the classRep.
 * @param 		length int
 *					the length of the class file.
 * @param 		protectionDomain ProtectionDomain
 *					the protection domain this class should
 *					belong to.
 *
 * @return	the newly defined Class
 *
 * @throws ClassFormatError when the bytes are invalid
 */
protected final Class<?> defineClass (
	final String className,
	final byte[] classRep,
	final int offset,
	final int length,
	ProtectionDomain protectionDomain)
	throws java.lang.ClassFormatError
{
	Certificate[] certs = null;
	if (protectionDomain != null) {
		final CodeSource cs = protectionDomain.getCodeSource();
		if (cs != null) certs = cs.getCertificates();
	}
	if (className != null) {
		String packageName = checkClassName(className);
		checkPackageSigners(packageName, className, certs);
	}

	if (offset < 0 || length < 0 || offset > classRep.length || length > classRep.length - offset) {
		throw new ArrayIndexOutOfBoundsException();
	}

	if (protectionDomain == null)
		protectionDomain = getDefaultProtectionDomain();

	final ProtectionDomain pd = protectionDomain;
	Class answer = defineClassImpl(className, classRep, offset, length, pd);
	if (isVerboseImpl()) {
		String location = "<unknown>"; //$NON-NLS-1$
		if (pd != null) {
			CodeSource cs = pd.getCodeSource();
			if (cs != null) {
				URL url = cs.getLocation();
				if (url != null) {
					location = url.toString();
				}
			}
		}
		com.ibm.oti.vm.VM.dumpString("class load: " + answer.getName() + " from: " + location + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	if (certs != null) setSigners(answer, certs);
	return answer;
}

private native boolean isVerboseImpl();

private void checkPackageSigners(final String packageName, String className, final Certificate[] classCerts) {
	Certificate[] packageCerts = null;
	synchronized(lazyInitLock) {
		if (packageSigners != null) {
			packageCerts = (Certificate[])packageSigners.get(packageName);
		}
		else {
			packageSigners = new Hashtable();
		}
	}
	if (packageCerts == null) {
			if (classCerts == null) {
				packageSigners.put(packageName, emptyCertificates);
			} else {
				packageSigners.put(packageName, classCerts);
			}
	} else {
		if ((classCerts == null && packageCerts.length == 0) || classCerts == packageCerts)
			return;
		if (classCerts != null && classCerts.length == packageCerts.length) {
			boolean foundMatch = true;
			test: for (int i=0; i<classCerts.length; i++) {
				if (classCerts[i] == packageCerts[i]) continue;
				if (classCerts[i].equals(packageCerts[i])) continue;
				for (int j=0; j<packageCerts.length; j++) {
					if (j == i) continue;
					if (classCerts[i] == packageCerts[j]) continue test;
					if (classCerts[i].equals(packageCerts[j])) continue test;
				}
				foundMatch = false;
				break;
			}
			if (foundMatch) return;
		}
		// K01d1 = Signers of '{0}' do not match signers of other classes in package
		throw new SecurityException(com.ibm.oti.util.Msg.getString("K01d1", className));
	}
}

/**
 * Gets the current default protection domain. If there isn't
 * one, it attempts to construct one based on the currently
 * in place security policy.
 * <p>
 * If the default protection domain can not be determined,
 * answers null.
 * <p>
 *
 * @return 		ProtectionDomain or null
 *					the default protection domain.
 */
private final ProtectionDomain getDefaultProtectionDomain () {
	if (isParallelCapable) {
		if (defaultProtectionDomain == null) {
			synchronized(lazyInitLock) {
				return	getDefaultProtectionDomainHelper();
			}
		}
		return defaultProtectionDomain;
	} else {
		// no need for synchronisation when not parallel capable
		return	getDefaultProtectionDomainHelper();
	}
}

private final ProtectionDomain getDefaultProtectionDomainHelper() {
	if (defaultProtectionDomain == null) {
		defaultProtectionDomain = new ProtectionDomain(defaultCodeSource, null, this, null);
	}
	return defaultProtectionDomain;
}

/*
 * VM level support for constructing a new class. Should not
 * be called by subclasses.
 */
private final native Class defineClassImpl (String className, byte [] classRep, int offset, int length, Object protectionDomain);

/**
 * Overridden by subclasses, by default throws ClassNotFoundException.
 * This method is called by loadClass() after the parent ClassLoader
 * has failed to find a loaded class of the same name.
 *
 * @return 		java.lang.Class
 *					the class or null.
 * @param 		className String
 *					the name of the class to search for.
 * @exception	ClassNotFoundException
 *					always, unless overridden.
 */
protected Class<?> findClass (String className) throws ClassNotFoundException {
    throw new ClassNotFoundException();
}

/**
 * Attempts to find and return a class which has already
 * been loaded by the virtual machine. Note that the class
 * may not have been linked and the caller should call
 * resolveClass() on the result if necessary.
 *
 * @return 		java.lang.Class
 *					the class or null.
 * @param 		className String
 *					the name of the class to search for.
 */
protected final Class<?> findLoadedClass (String className) {
	if (!allowArraySyntax) {
		if (className != null && className.length() > 0 && className.charAt(0) == '[') {
			return null;
		}
	}
	return findLoadedClassImpl(className);
}

private native Class findLoadedClassImpl(String className);

/**
 * Attempts to load a class using the system class loader.
 * Note that the class has already been been linked.
 *
 * @return 		java.lang.Class
 *					the class which was loaded.
 * @param 		className String
 *					the name of the class to search for.
 * @exception	ClassNotFoundException
 *					if the class can not be found.
 */
protected final Class<?> findSystemClass (String className) throws ClassNotFoundException {
	return applicationClassLoader.loadClass(className);
}

/**
 * Returns the specified ClassLoader's parent.
 *
 * @return 		java.lang.ClassLoader
 *					the class or null.
 * @exception	SecurityException
 *					if a security manager exists and it does not
 *					allow the parent loader to be retrieved.
 */
@sun.reflect.CallerSensitive
public final ClassLoader getParent() {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callersClassLoader = callerClassLoader();
		if (callersClassLoader != null && callersClassLoader != this
			&& !callersClassLoader.isAncestorOf(this))
				security.checkPermission(
					RuntimePermission.permissionToGetClassLoader);
	}
	return parent;
}

/**
 * Answers an URL which can be used to access the resource
 * described by resName, using the class loader's resource lookup
 * algorithm. The default behavior is just to return null.
 *
 * @return		URL
 *					the location of the resource.
 * @param		resName String
 *					the name of the resource to find.
 *
 * @see			Class#getResource
 */
public URL getResource (String resName) {
	URL url = parent == null
		? bootstrapClassLoader.findResource(resName)
		: parent.getResource(resName);
	if (url != null) return url;

	return findResource(resName);
}

/**
 * Answers an Enumeration of URL which can be used to access the resources
 * described by resName, using the class loader's resource lookup
 * algorithm.
 *
 * @param		resName String
 *					the name of the resource to find.

 * @return		Enumeration
 *					the locations of the resources.
 *
 * @throws IOException when an error occurs
 */
public Enumeration<URL> getResources (String resName) throws IOException {
	// The Vector holds at most 2 elements (local enumerations and the parent's)
	final Vector resources = new Vector(2);

	Enumeration<URL> parentResources = null;

	if (parent != null) {
		parentResources = parent.getResources(resName);
	} else if (this != bootstrapClassLoader) {
		parentResources = bootstrapClassLoader.getResources(resName);
	}

	Enumeration<URL> localResources = findResources(resName);

	// Check to see if both are valid and add them to the array
	if (parentResources != null) {
		resources.add(parentResources);
	}
	if (localResources != null) {
		resources.add(localResources);
	}

	return new Enumeration<URL>() {
		int index = 0;
		public boolean hasMoreElements() {
			while (index < resources.size()) {
				if (((Enumeration)resources.elementAt(index)).hasMoreElements())
					return true;
				index++;
			}
			return false;
		}
		public URL nextElement() {
			while (index < resources.size()) {
				Enumeration e = (Enumeration)resources.elementAt(index);
				if (e.hasMoreElements()) return (URL)e.nextElement();
				index++;
			}
			throw new NoSuchElementException();
		}
	};
}

/**
 * Answers a stream on a resource found by looking up
 * resName using the class loader's resource lookup
 * algorithm. The default behavior is just to return null.
 *
 * @return		InputStream
 *					a stream on the resource or null.
 * @param		resName	String
 *					the name of the resource to find.
 *
 * @see			Class#getResourceAsStream
 */
public InputStream getResourceAsStream (String resName) {
	URL url = getResource(resName);
	try {
		if (url != null) return url.openStream();
	} catch (IOException e){}
	return null;
}

static void completeInitialization() {
	Class voidClass = Void.TYPE;

	/* Process reflect.cache=boot, other options are processed earlier in initializeClassLoaders() */
	String propValue = System.getProperty("reflect.cache");
	if (propValue != null) propValue = propValue.toLowerCase();
	/* Do not enable reflect cache if -Dreflect.cache=false is in commandline */
	if (!"false".equals(propValue)) {
		if (J9VMInternals.isXaggressiveImpl()) {
			reflectCacheAppOnly = false;
		}

		if (propValue != null) {
			java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(propValue, ",");
			while (tokenizer.hasMoreTokens()) {
				String value = tokenizer.nextToken();
				if ("boot".equals(value)) reflectCacheAppOnly = false;
				if ("app".equals(value)) reflectCacheAppOnly = true;
			}
		}
	}

	initSystemClassLoader = true;
}

/**
 * Convenience operation to obtain a reference to the system class loader.
 * The system class loader is the parent of any new <code>ClassLoader</code>
 * objects created in the course of an application and will normally be the
 * same <code>ClassLoader</code> as that used to launch an application.
 *
 * @return java.lang.ClassLoader the system classLoader.
 * @exception SecurityException
 *                if a security manager exists and it does not permit the
 *                caller to access the system class loader.
 */
@sun.reflect.CallerSensitive
public static ClassLoader getSystemClassLoader () {
	if (initSystemClassLoader) {
		Class classLoaderClass = ClassLoader.class;
		synchronized(classLoaderClass) {
			if (initSystemClassLoader) {
				initSystemClassLoader = false;

					String userLoader = System.getProperty("java.system.class.loader");
					if (userLoader != null) {
						try {
							Class loaderClass = Class.forName(userLoader, true, applicationClassLoader);
							Constructor constructor = loaderClass.getConstructor(new Class[]{classLoaderClass});
							applicationClassLoader = (ClassLoader)constructor.newInstance(new Object[]{applicationClassLoader});
						} catch (Throwable e) {
							throw new Error(e);
						}
					}
			}
		}
	}
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callersClassLoader = callerClassLoader();
		if (callersClassLoader != null &&
			callersClassLoader != applicationClassLoader &&
			!callersClassLoader.isAncestorOf(applicationClassLoader))
				security.checkPermission(
					RuntimePermission.permissionToGetClassLoader);
	}

	return applicationClassLoader;
}

/**
 * Answers an URL specifing a resource which can be found by
 * looking up resName using the system class loader's resource
 * lookup algorithm.
 *
 * @return		URL
 *					a URL specifying a system resource or null.
 * @param		resName String
 *					the name of the resource to find.
 *
 * @see			Class#getResource
 */
public static URL getSystemResource(String resName) {
	return getSystemClassLoader().getResource(resName);
}

/**
 * Answers an Emuneration of URL containing all resources which can be
 * found by looking up resName using the system class loader's resource
 * lookup algorithm.
 *
 * @param		resName String
 *					the name of the resource to find.
 *
 * @return		Enumeration
 *					an Enumeration of URL containing the system resources
 *
 * @throws IOException when an error occurs
 */
public static Enumeration<URL> getSystemResources(String resName) throws IOException {
	return getSystemClassLoader().getResources(resName);
}

/**
 * Answers a stream on a resource found by looking up
 * resName using the system class loader's resource lookup
 * algorithm. Basically, the contents of the java.class.path
 * are searched in order, looking for a path which matches
 * the specified resource.
 *
 * @return		a stream on the resource or null.
 * @param		resName		the name of the resource to find.
 *
 * @see			Class#getResourceAsStream
 */
public static InputStream getSystemResourceAsStream(String resName) {
	return getSystemClassLoader().getResourceAsStream(resName);
}

/**
 * Invoked by the Virtual Machine when resolving class references.
 * Equivalent to loadClass(className, false);
 *
 * @return 		java.lang.Class
 *					the Class object.
 * @param 		className String
 *					the name of the class to search for.
 * @exception	ClassNotFoundException
 *					If the class could not be found.
 */
public Class<?> loadClass (String className) throws ClassNotFoundException {
	return loadClass(className, false);
}

/**
 * Attempts to load the type <code>className</code> in the running VM,
 * optionally linking the type after a successful load.
 *
 * @return 		java.lang.Class
 *					the Class object.
 * @param 		className String
 *					the name of the class to search for.
 * @param 		resolveClass boolean
 *					indicates if class should be resolved after loading.
 * @exception	ClassNotFoundException
 *					If the class could not be found.
 */
protected Class<?> loadClass(final String className, boolean resolveClass) throws ClassNotFoundException {
	if (isParallelCapable) {
		Class	ret;
		try {
			synchronized(getClassLoadingLock(className)) {
				ret = loadClassHelper(className, resolveClass);
			}
		} finally {
			classNameBasedLock.remove(className);
		}
		return ret;

	} else {
		synchronized(this) {
			return loadClassHelper(className, resolveClass);
		}
	}
}

private Class<?> loadClassHelper(String className, boolean resolveClass) throws ClassNotFoundException {
	// Ask the VM to look in its cache.
	Class loadedClass = findLoadedClass(className);

	// search in parent if not found
	if (loadedClass == null) {
		try {
			if (parent == null) {
				loadedClass = bootstrapClassLoader.loadClass(className);
			} else
				loadedClass = parent.loadClass(className, resolveClass);
		} catch (ClassNotFoundException e) {
			// don't do anything.  Catching this exception is the normal protocol for
			// parent classloaders telling use they couldn't find a class.
		}

		// not findLoadedClass or by parent.loadClass, try locally
		if (loadedClass == null) {
			loadedClass = findClass(className);
		}
	}

	// resolve if required
	if (resolveClass) resolveClass(loadedClass);
	return loadedClass;
}

/**
 * Attempts to register the  the ClassLoader as being capable of
 * parallel class loading.  This requires that all superclasses must
 * also be parallel capable.
 *
 * @return		True if the ClassLoader successfully registers as
 * 				parallel capable, false otherwise.
 *
 * @see			java.lang.ClassLoader
 */
protected static boolean registerAsParallelCapable() {
	Class	callerCls = System.getCallerClass();
	Class	superCls = callerCls.getSuperclass();

	if (parallelCapableCollection.containsKey(callerCls)) {
		return	true;
	} else if (superCls == ClassLoader.class || parallelCapableCollection.containsKey(superCls)) {
		parallelCapableCollection.put(callerCls, null);
		return	true;
	}

	return false;
}

/**
 * Returns the lock object for class loading operations.
 * If this ClassLoader object has been registered as parallel capable,
 * a dedicated object associated with this specified class name is returned.
 * Otherwise, current ClassLoader object is returned.
 *
 * @param 		className String
 *					name of the to be loaded class
 *
 * @return		the lock for class loading operations
 *
 * @exception	NullPointerException
 *					if registered as parallel capable and className is null
 *
 * @see			java.lang.ClassLoader
 *
 */
protected Object getClassLoadingLock(final String className) {
	if (isParallelCapable)	{
		if (classNameBasedLock == null) {
			synchronized(lazyInitLock) {
				if (classNameBasedLock == null) {
						classNameBasedLock = new Hashtable();
				}
			}
		}

		Object lock;
		synchronized(classNameBasedLock) {
			//	Hashtable.get() does null pointer check
			lock = classNameBasedLock.get(className);
			if (lock == null) {
					lock = new ClassNameBasedLock();
					classNameBasedLock.put(className, lock);
			}
		}
		return	lock;
	} else {
		return this;
	}
}

/**
 * Forces a class to be linked (initialized).  If the class has
 * already been linked this operation has no effect.
 *
 * @param		clazz Class
 *					the Class to link.
 * @exception	NullPointerException
 *					if clazz is null.
 *
 * @see			Class#getResource
 */
protected final void resolveClass(Class<?> clazz) {
	if (clazz == null)
		throw new NullPointerException();
}

/**
 * Forces the parent of a classloader instance to be newParent
 *
 * @param		newParent ClassLoader
 *					the ClassLoader to make the parent.
 */
private void setParent(ClassLoader newParent) {
	parent = newParent;
}

/**
 * Answers true if the receiver is a system class loader.
 * <p>
 * Note that this method has package visibility only. It is
 * defined here to avoid the security manager check in
 * getSystemClassLoader, which would be required to implement
 * this method anywhere else.
 *
 * @return		boolean
 *					true if the receiver is a system class loader
 *
 * @see Class#getClassLoaderImpl()
 */
final boolean isASystemClassLoader() {
	if (this == bootstrapClassLoader) return true;
	ClassLoader cl = applicationClassLoader;
	while (cl != null) {
		if (this == cl) return true;
		cl = cl.parent;
	}
	return false;
}

/**
 * Answers true if the receiver is ancestor of another class loader.
 * <p>
 * Note that this method has package visibility only. It is
 * defined here to avoid the security manager check in
 * getParent, which would be required to implement
 * this method anywhere else.
 *
 * @param		child	ClassLoader, a child candidate
 *
 * @return		boolean
 *					true if the receiver is ancestor of the parameter
 */
final boolean isAncestorOf (ClassLoader child) {
	if (child == null) return false;
	if (this == bootstrapClassLoader) return true;
	ClassLoader cl = child.parent;
	while (cl != null) {
		if (this == cl) return true;
		cl = cl.parent;
	}
	return false;
}

/**
 * Answers an URL which can be used to access the resource
 * described by resName, using the class loader's resource lookup
 * algorithm. The default behavior is just to return null.
 * This should be implemented by a ClassLoader.
 *
 * @return		URL
 *					the location of the resource.
 * @param		resName String
 *					the name of the resource to find.
 */
protected URL findResource (String resName) {
	return null;
}

/**
 * Answers an Enumeration of URL which can be used to access the resources
 * described by resName, using the class loader's resource lookup
 * algorithm. The default behavior is just to return an empty Enumeration.
 *
 * @param		resName String
 *					the name of the resource to find.
 * @return		Enumeration
 *					the locations of the resources.
 *
 * @throws IOException when an error occurs
 */
protected Enumeration<URL> findResources (String resName) throws IOException {
	return new Vector<URL>().elements();
}

/**
 * Answers the absolute path of the file containing the library
 * associated with the given name, or null. If null is answered,
 * the system searches the directories specified by the system
 * property "java.library.path".
 *
 * @return		String
 *					the library file name or null.
 * @param		libName	String
 *					the name of the library to find.
 */
protected String findLibrary(String libName) {
	return null;
}

/**
 * Attempt to locate the requested package. If no package information
 * can be located, null is returned.
 *
 * @param		name		The name of the package to find
 * @return		The package requested, or null
 */
protected Package getPackage(String name) {
	boolean packagesNotNull = true;
	if (packages == null) {
		synchronized(lazyInitLock) {
			if (packages == null)
				packagesNotNull = false;
		}
	}
	if (packagesNotNull) {
		Package result = (Package) packages.get(name);
		if (result != null) {
			return result;
		}
	}
	if (this != bootstrapClassLoader) {
		ClassLoader parent = this.parent;
		if (parent == null)
			parent = bootstrapClassLoader;
		return parent.getPackage(name);
	}
	return null;
}

/**
 * Return all the packages known to this class loader.
 *
 * @return		All the packages known to this classloader
 */
protected Package[] getPackages() {

	Package[] ancestorsPackages = null;
	if (parent == null) {
		if (this != bootstrapClassLoader)
			ancestorsPackages = bootstrapClassLoader.getPackages();
	} else
		ancestorsPackages = parent.getPackages();

	Hashtable localPackages;
	synchronized(lazyInitLock) {
		localPackages = packages;
	}
	int resultSize = localPackages == null ? 0 : localPackages.size();
	if (ancestorsPackages != null)
		resultSize += ancestorsPackages.length;
	Package[] result = new Package[resultSize];
	int i = 0;
	if (ancestorsPackages != null) {
		for (; i < ancestorsPackages.length; i++) {
			result[i] = ancestorsPackages[i];
		}
	}
	if (localPackages != null) {
		Enumeration myPkgs = localPackages.elements();
		while (myPkgs.hasMoreElements()) {
			result[i++] = (Package) myPkgs.nextElement();
		}
	}
	return result;
}

/**
 * Define a new Package using the specified information.
 *
 * @param		name		The name of the package
 * @param		specTitle	The title of the specification for the Package
 * @param		specVersion	The version of the specification for the Package
 * @param		specVendor	The vendor of the specification for the Package
 * @param		implTitle	The implementation title of the Package
 * @param		implVersion	The implementation version of the Package
 * @param		implVendor	The specification vendor of the Package
 * @param		sealBase	The URL used to seal the Package, if null the Package is not sealed
 *
 * @return		The Package created
 *
 * @exception	IllegalArgumentException if the Package already exists
 */
protected Package definePackage(
	final String name, final String specTitle,
	final String specVersion, final String specVendor,
	final String implTitle, final String implVersion,
	final String implVendor, final URL sealBase)
	throws IllegalArgumentException
{

	if (packages == null) {
		synchronized(lazyInitLock) {
			if (packages == null) {
				packages = new Hashtable();
			}
		}
	}
	synchronized(packages) {
		if (getPackage(name) == null) {
			Package newPackage = new Package(name,
				specTitle, specVersion, specVendor,
				implTitle, implVersion, implVendor,
				sealBase, this);
			packages.put(name, newPackage);
			return newPackage;
		// K0053 = Package {0} already defined.
		} else throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0053", name));
	}
}

/**
 * Gets the signers of a class.
 *
 * @param		c		The Class object
 * @return		signers	The signers for the class
 */
final Object[] getSigners(Class c) {
	if (classSigners == null)	{
		synchronized(lazyInitLock) {
			if (classSigners == null) return null;
		}
	}
	try {
		Object result = classSigners.get(c);
		if (result != null)
			return (Object[]) result.clone();
	} catch (CloneNotSupportedException e) {}
	return null;
}

/**
 * Sets the signers of a class.
 *
 * @param		c		The Class object
 * @param		signers	The signers for the class
 */
protected final void setSigners(Class<?> c, Object[] signers) {
	if (c.getClassLoaderImpl() == this) {
		if (signers == null) {
			if (classSigners == null) {
				synchronized(lazyInitLock) {
					if (classSigners == null) {
						return;
					}
				}
			}
			classSigners.remove(c);
		} else {
				if (classSigners == null) {
					synchronized(lazyInitLock) {
						if (classSigners == null) {
							classSigners = new Hashtable();
						}
					}
				}
				classSigners.put(c, signers);
		}
	} else c.getClassLoaderImpl().setSigners(c, signers);

}

@sun.reflect.CallerSensitive
static ClassLoader getCallerClassLoader() {
	ClassLoader loader = getStackClassLoader(2);
	if (loader == bootstrapClassLoader) return null;
	return loader;
}

/**
 * Returns the ClassLoader of the method (including natives) at the
 * specified depth on the stack of the calling thread. Frames representing
 * the VM implementation of java.lang.reflect are not included in the list.
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
 * @see com.ibm.oti.vm.VM#getStackClassLoader
 */
@sun.reflect.CallerSensitive
static final native ClassLoader getStackClassLoader(int depth);

/**
 * Returns the ClassLoader of the method that called the caller.
 * i.e. A.x() calls B.y() calls callerClassLoader(),
 * A's ClassLoader will be returned. Returns null for the
 * bootstrap ClassLoader.
 *
 * @return 		a ClassLoader or null for the bootstrap ClassLoader
 */
@sun.reflect.CallerSensitive
static ClassLoader callerClassLoader() {
	ClassLoader loader = getStackClassLoader(2);
	if (loader == bootstrapClassLoader) return null;
	return loader;
}

/**
 * Loads and links the library specified by the argument.
 *
 * @param		libName		the name of the library to load
 * @param		loader		the classloader in which to load the library
 *
 * @exception	UnsatisfiedLinkError
 *							if the library could not be loaded
 * @exception	SecurityException
 *							if the library was not allowed to be loaded
 */
static synchronized void loadLibraryWithClassLoader(String libName, ClassLoader loader) {
	SecurityManager smngr = System.getSecurityManager();
	if (smngr != null)
		smngr.checkLink(libName);
	if (loader != null) {
		String realLibName = loader.findLibrary(libName);

		if (realLibName != null) {
			loadLibraryWithPath(realLibName, loader, null);
			return;
		}
	}
	loadLibraryWithPath(
		libName,
		loader,
		System.internalGetProperties().getProperty(
			loader == null
				? "com.ibm.oti.vm.bootstrap.library.path"
				: "java.library.path"
		)
	);
}

/**
 * Loads and links the library specified by the argument.
 * No security check is done.
 *
 * @param		libName			the name of the library to load
 * @param		loader			the classloader in which to load the library
 * @param		libraryPath		the library path to search, or null
 *
 * @exception	UnsatisfiedLinkError
 *							if the library could not be loaded
 */
static void loadLibraryWithPath(String libName, ClassLoader loader, String libraryPath) {
	if (File.separatorChar == '\\'){
		if (libName.startsWith("/") && com.ibm.oti.util.Util.startsWithDriveLetter(libName.substring(1))){
			libName = libName.substring(1);
		}
	}
	byte[] message = ClassLoader.loadLibraryWithPath(com.ibm.oti.util.Util.getBytes(libName), loader, libraryPath == null ? null : com.ibm.oti.util.Util.getBytes(libraryPath));
	if (message != null) {
		String error;
		try {
			error = com.ibm.oti.util.Util.convertFromUTF8(message, 0, message.length);
		} catch (java.io.IOException e) {
			error = com.ibm.oti.util.Util.toString(message);
		}
		throw new UnsatisfiedLinkError(libName + " (" + error + ")");
	}
}

private static native byte[] loadLibraryWithPath(byte[] libName, ClassLoader loader, byte[] libraryPath);

static void loadLibrary(Class caller, String name, boolean fullPath) {
	if (fullPath)
		loadLibraryWithPath(name, caller.getClassLoaderImpl(), null);
	else
		loadLibraryWithClassLoader(name, caller.getClassLoaderImpl());
}

/**
 * Sets the assertion status of a class.
 *
 * @param		cname		Class name
 * @param		enable		Enable or disable assertion
 *
 * @since 1.4
 */
public void setClassAssertionStatus(String cname, boolean enable) {
	if (!isParallelCapable) {
		synchronized(this) {
			setClassAssertionStatusHelper(cname, enable);
		}
	} else {
		synchronized(assertionLock) {
			setClassAssertionStatusHelper(cname, enable);
		}
	}
}
private void setClassAssertionStatusHelper(String cname, boolean enable) {
		if (classAssertionStatus == null ) {
			classAssertionStatus = new HashMap();
		}
		classAssertionStatus.put(cname, Boolean.valueOf(enable));
}

/**
 * Sets the assertion status of a package.
 *
 * @param		pname		Package name
 * @param		enable		Enable or disable assertion
 *
 * @since 1.4
 */
public void setPackageAssertionStatus(String pname, boolean enable) {
	if (!isParallelCapable) {
		synchronized(this) {
			setPackageAssertionStatusHelper(pname, enable);
		}
	} else {
		synchronized(assertionLock) {
			setPackageAssertionStatusHelper(pname, enable);
		}
	}
}
private void setPackageAssertionStatusHelper(String pname, boolean enable) {
	if (packageAssertionStatus == null ) {
		packageAssertionStatus = new HashMap();
	}
	packageAssertionStatus.put(pname, Boolean.valueOf(enable));
}

 /**
 * Sets the default assertion status of a classloader
 *
 * @param		enable		Enable or disable assertion
 *
 * @since 1.4
 */
public void setDefaultAssertionStatus(boolean enable){
	if (!isParallelCapable) {
		synchronized(this) {
			defaultAssertionStatus = enable;
		}
	} else {
		synchronized(assertionLock) {
			defaultAssertionStatus = enable;
		}
	}
}

/**
 * Clears the default, package and class assertion status of a classloader
 *
 * @since 1.4
 */
public void clearAssertionStatus(){
	if (!isParallelCapable) {
		synchronized(this) {
			defaultAssertionStatus = false;
			classAssertionStatus = null;
			packageAssertionStatus = null;
		}
	} else {
		synchronized(assertionLock) {
			defaultAssertionStatus = false;
			classAssertionStatus = null;
			packageAssertionStatus = null;
		}
	}
}

/**
 * Answers the assertion status of the named class
 *
 * Returns the assertion status of the class or nested class if it has
 * been set. Otherwise returns the assertion status of its package or
 * superpackage if that has been set. Otherwise returns the default assertion
 * status.
 * Returns 1 for enabled and 0 for disabled.
 *
 * @param		cname	String
 *					the name of class.
 *
 * @return		int
 *					the assertion status.
 *
 * @since 1.4
 */
boolean getClassAssertionStatus(String cname) {
	if (!isParallelCapable) {
		synchronized(this) {
			return getClassAssertionStatusHelper(cname);
		}
	} else {
		synchronized(assertionLock) {
			return getClassAssertionStatusHelper(cname);
		}
	}
}
private boolean getClassAssertionStatusHelper(String cname) {
	int dlrIndex = -1;

	if (classAssertionStatus != null) {
		Boolean b = (Boolean) classAssertionStatus.get(cname);
		if (b != null) {
			return b.booleanValue();
		} else if ((dlrIndex = cname.indexOf('$'))>0) {
			b = (Boolean) classAssertionStatus.get(cname.substring(0, dlrIndex));
			if (b !=null)
				return b.booleanValue();
		}
	}
	if ((dlrIndex = cname.lastIndexOf('.'))>0) {
		return getPackageAssertionStatus(cname.substring(0, dlrIndex));
	}
	return getDefaultAssertionStatus();
}

/**
 * Answers the assertion status of the named package
 *
 * Returns the assertion status of the named package or superpackage if
 * that has been set. Otherwise returns the default assertion status.
 * Returns 1 for enabled and 0 for disabled.
 *
 * @param		pname	String
 *					the name of package.
 *
 * @return		int
 *					the assertion status.
 *
 * @since 1.4
 */
boolean getPackageAssertionStatus(String pname) {
	if (!isParallelCapable) {
		synchronized(this) {
			return getPackageAssertionStatusHelper(pname);
		}
	} else {
		synchronized(assertionLock) {
			return getPackageAssertionStatusHelper(pname);
		}
	}
}
private boolean getPackageAssertionStatusHelper(String pname) {
	int prdIndex = -1;

	if (packageAssertionStatus != null) {
		Boolean b = (Boolean) packageAssertionStatus.get(pname);
		if (b != null) {
			return b.booleanValue();
		} else if ((prdIndex = pname.lastIndexOf('.'))>0) {
			return getPackageAssertionStatus(pname.substring(0, prdIndex));
		}
	}
	return getDefaultAssertionStatus();
}

/**
 * Answers the default assertion status
 *
 * @return		boolean
 *					the default assertion status.
 *
 * @since 1.4
 */
boolean getDefaultAssertionStatus() {
	if (!isParallelCapable) {
		synchronized(this) {
			return defaultAssertionStatus;
		}
	} else {
		synchronized(assertionLock) {
			return defaultAssertionStatus;
		}
	}
}

/**
 * This setsup the assertion status based on the commandline args to VM
 *
 * @since 1.4
 */
private void initializeClassLoaderAssertStatus() {
	boolean bootLoader = bootstrapClassLoader == null;

	if (!bootLoader && !checkAssertionOptions) {
		// if the bootLoader didn't find any assertion options, other
		// classloaders can skip the check for options
		return;
	}

	boolean foundAssertionOptions = false;
	String [] vmargs = com.ibm.oti.vm.VM.getVMArgs();
	for (int i=0; i<vmargs.length; i++) {
		if (!vmargs[i].startsWith("-e") && !vmargs[i].startsWith("-d")) {
			continue;
		}
		// splice around :
		int indexColon = vmargs[i].indexOf(':');
		String vmargOptions, vmargExtraInfo;
		if ( indexColon == -1 ) {
			vmargOptions = vmargs[i];
			vmargExtraInfo = null;
		} else {
			vmargOptions = vmargs[i].substring(0, indexColon);
			vmargExtraInfo = vmargs[i].substring(indexColon+1);
		}
		if ( vmargOptions.compareTo("-ea") == 0
			|| vmargOptions.compareTo("-enableassertions") == 0
			|| vmargOptions.compareTo("-da") == 0
			|| vmargOptions.compareTo("-disableassertions") == 0
			) {
				foundAssertionOptions = true;
				boolean def = vmargOptions.charAt(1) == 'e';
				if (vmargExtraInfo == null) {
					if (bootLoader) {
						continue;
					}
					setDefaultAssertionStatus(def);
				} else {
					String str = vmargExtraInfo;
					int len = str.length();
					if ( len > 3 && str.charAt(len-1) == '.'  &&
						str.charAt(len-2) == '.' && str.charAt(len-3) == '.') {
						str = str.substring(0,len-3);
						setPackageAssertionStatus(str, def);
					} else {
						setClassAssertionStatus(str, def);
					}
				}
		} else if ( vmargOptions.compareTo("-esa") == 0
					|| vmargOptions.compareTo("-enablesystemassertions") == 0
					|| vmargOptions.compareTo("-dsa") == 0
					|| vmargOptions.compareTo("-disablesystemassertions") == 0
		) {
			if (bootLoader) {
				boolean def = vmargOptions.charAt(1) == 'e';
				setDefaultAssertionStatus(def);
			}
		}

	}
	if (bootLoader && foundAssertionOptions) {
		// assertion options found, every classloader must check the options
		checkAssertionOptions = true;
	}
}

/**
 * Constructs a new class from an array of bytes containing a
 * class definition in class file format and assigns the new
 * class to the specified protection domain.
 *
 * @param 		name java.lang.String
 *					the name of the new class.
 * @param 		buffer
 *					a memory image of a class file.
 * @param 		domain
 *					the protection domain this class should
 *					belong to.
 *
 * @return	the newly defined Class
 *
 * @throws ClassFormatError when the bytes are invalid
 *
 * @since 1.5
 */
protected final Class<?> defineClass(String name, java.nio.ByteBuffer buffer, ProtectionDomain domain) throws ClassFormatError {
	if (buffer.hasArray())
		return defineClass(name, buffer.array(), buffer.position(), buffer.limit() - buffer.position(), domain);

	int size = buffer.limit() - buffer.position();
	byte[] bytes = new byte[size];
	buffer.get(bytes);
	return defineClass(name, bytes, 0, bytes.length, domain);
}

Hashtable getGenericRepository() {
	if (genericRepository == null)	{
		synchronized(lazyInitLock) {
			if (genericRepository == null)
				genericRepository = new Hashtable();
		}
	}
	return genericRepository;
}

Hashtable getAnnotationCache() {
	Hashtable cache = annotationCache;
	if (cache == null)	{
		synchronized(lazyInitLock) {
			cache = annotationCache;
			if (cache == null) {
				cache = new Hashtable();
				annotationCache = cache;
			}
		}
	}
	return cache;
}

private volatile Hashtable methodCache;
private volatile Hashtable fieldCache;
private volatile Hashtable constructorCache;

static boolean isReflectCacheEnabled() {
	return reflectCacheEnabled;
}

static boolean isReflectCacheAppOnly() {
	return reflectCacheAppOnly;
}

static boolean isReflectCacheDebug() {
	return reflectCacheDebug;
}

final Hashtable getMethodCache() {
	Hashtable cache = methodCache;
	if (cache == null) {
		synchronized(lazyInitLock) {
			cache = methodCache;
			if (cache == null) {
				cache = new Hashtable();
				methodCache = cache;
			}
		}
	}
	return cache;
}

final CacheTable getMethodCache(Class cl) {
	Hashtable cache = methodCache;
	if (cache == null) {
		synchronized(lazyInitLock) {
			cache = methodCache;
			if (cache == null) {
				cache = new Hashtable();
				methodCache = cache;
			}
		}
	}
	synchronized(cache) {
		CacheTable clCache = (CacheTable)cache.get(cl);
		if (clCache == null) {
			clCache = new CacheTable(cache, cl);
			cache.put(cl, clCache);
		}
		clCache.used();
		return clCache;
	}
}

final Hashtable getFieldCache() {
	Hashtable cache = fieldCache;
	if (cache == null) {
		synchronized(lazyInitLock) {
			cache = fieldCache;
			if (cache == null) {
				cache = new Hashtable();
				fieldCache = cache;
			}
		}
	}
	return cache;
}

final CacheTable getFieldCache(Class cl) {
	Hashtable cache = fieldCache;
	if (cache == null) {
		synchronized(lazyInitLock) {
			cache = fieldCache;
			if (cache == null) {
				cache = new Hashtable();
				fieldCache = cache;
			}
		}
	}
	synchronized(cache) {
		CacheTable clCache = (CacheTable)cache.get(cl);
		if (clCache == null) {
			clCache = new CacheTable(cache, cl);
			cache.put(cl, clCache);
		}
		clCache.used();
		return clCache;
	}
}

final Hashtable getConstructorCache() {
	Hashtable cache = constructorCache;
	if (cache == null) {
		synchronized(lazyInitLock) {
			cache = constructorCache;
			if (cache == null) {
				cache = new Hashtable();
				constructorCache = cache;
			}
		}
	}
	return cache;
}

final CacheTable getConstructorCache(Class cl) {
	Hashtable cache = constructorCache;
	if (cache == null) {
		synchronized(lazyInitLock) {
			cache = constructorCache;
			if (cache == null) {
				cache = new Hashtable();
				constructorCache = cache;
			}
		}
	}
	synchronized(cache) {
		CacheTable clCache = (CacheTable)cache.get(cl);
		if (clCache == null) {
			clCache = new CacheTable(cache, cl);
			cache.put(cl, clCache);
		}
		clCache.used();
		return clCache;
	}
}

static class CacheTable extends Hashtable {
	private static final long serialVersionUID = 7227948070918461051L;

	Hashtable parent;
	Object key;
	int useCount;
	public CacheTable(Hashtable parent, Object key) {
		super();
		this.parent = parent;
		this.key = key;
	}
	public Object get(Object key) {
		SoftReference sf = (SoftReference)super.get(key);
		return sf != null ? sf.get() : null;
	}
	public Object getRef(Object key) {
		return super.get(key);
	}
	public void removeEmpty() {
		synchronized(parent) {
			synchronized(this) {
				if (isEmpty() && useCount == 0) {
					if (isReflectCacheDebug()) {
						System.err.println("Removed reflect cache for: " + key);
					}
					parent.remove(key);
				} else {
					if (isReflectCacheDebug()) {
						System.err.println("Reflect cache size: " + size() + " and useCount: " + useCount + " for: " + key);
					}
				}
			}
		}
	}
	public synchronized void used() {
		useCount++;
	}
	public synchronized void free() {
		useCount--;
	}
}

/**
 * Check if all the certs in one array are present in the other arrary
 * @param	pcerts	java.security.cert.Certificate[]
 * @param	certs	java.security.cert.Certificate[]
 * @return	ture when all the certs in one array are present in the other arrary
 * 			false otherwise
 */
private boolean compareCerts(java.security.cert.Certificate[] pcerts,
		java.security.cert.Certificate[] certs) {
	if (pcerts == null && certs == null
			|| pcerts == certs
			|| pcerts == null && certs.length == 0
			|| certs == null && pcerts.length == 0 ) {
		return	true;
	} else if (pcerts == null || certs == null || pcerts.length != certs.length) {
		return false;
	}

	boolean foundMatch = true;
	test: for(int i=0; i<pcerts.length; i++) {
		if (pcerts[i] == certs[i])	continue;
		if (pcerts[i].equals(certs[i]))	continue;
		for(int j=0; j<certs.length; j++) {
			if (j == i) continue;
			if (pcerts[i] == certs[j])	continue test;
			if (pcerts[i].equals(certs[j]))	continue test;
		}
		foundMatch = false;
		break;
	}

	return	foundMatch;
}

//prevent subclasses from becoming Cloneable
protected Object clone() throws CloneNotSupportedException {
	throw new CloneNotSupportedException();
}

}
