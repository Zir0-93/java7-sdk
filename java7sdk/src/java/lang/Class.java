package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2014  All Rights Reserved
 */

import java.io.InputStream;
import java.security.ProtectionDomain;
import java.security.AllPermission;
import java.security.Permissions;
import java.lang.reflect.*;
import java.net.URL;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.security.PrivilegedAction;
import java.lang.ref.*;
import java.util.Map;
import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.scope.ClassScope;
import sun.reflect.misc.ReflectUtil;
import sun.reflect.annotation.AnnotationType;

/**
 * An instance of class Class is the in-image representation
 * of a Java class. There are three basic types of Classes
 * <dl>
 * <dt><em>Classes representing object types (classes or interfaces)</em></dt>
 * <dd>These are Classes which represent the class of a
 *     simple instance as found in the class hierarchy.
 *     The name of one of these Classes is simply the
 *     fully qualified class name of the class or interface
 *     that it represents. Its <em>signature</em> is
 *     the letter "L", followed by its name, followed
 *     by a semi-colon (";").</dd>
 * <dt><em>Classes representing base types</em></dt>
 * <dd>These Classes represent the standard Java base types.
 *     Although it is not possible to create new instances
 *     of these Classes, they are still useful for providing
 *     reflection information, and as the component type
 *     of array classes. There is one of these Classes for
 *     each base type, and their signatures are:
 *     <ul>
 *     <li><code>B</code> representing the <code>byte</code> base type</li>
 *     <li><code>S</code> representing the <code>short</code> base type</li>
 *     <li><code>I</code> representing the <code>int</code> base type</li>
 *     <li><code>J</code> representing the <code>long</code> base type</li>
 *     <li><code>F</code> representing the <code>float</code> base type</li>
 *     <li><code>D</code> representing the <code>double</code> base type</li>
 *     <li><code>C</code> representing the <code>char</code> base type</li>
 *     <li><code>Z</code> representing the <code>boolean</code> base type</li>
 *     <li><code>V</code> representing void function return values</li>
 *     </ul>
 *     The name of a Class representing a base type
 *     is the keyword which is used to represent the
 *     type in Java source code (i.e. "int" for the
 *     <code>int</code> base type.</dd>
 * <dt><em>Classes representing array classes</em></dt>
 * <dd>These are Classes which represent the classes of
 *     Java arrays. There is one such Class for all array
 *     instances of a given arity (number of dimensions)
 *     and leaf component type. In this case, the name of the
 *     class is one or more left square brackets (one per
 *     dimension in the array) followed by the signature ofP
 *     the class representing the leaf component type, which
 *     can be either an object type or a base type. The
 *     signature of a Class representing an array type
 *     is the same as its name.</dd>
 * </dl>
 *
 * @author		OTI
 * @version		initial
 */
public final class Class<T> implements java.io.Serializable, GenericDeclaration, Type, AnnotatedElement {
	private static final long serialVersionUID = 3206093459760846163L;
	private static ProtectionDomain AllPermissionsPD;
	private static final int SYNTHETIC = 0x1000;
	private static final int ANNOTATION = 0x2000;
	private static final int ENUM = 0x4000;
	private static final int MEMBER_INVALID_TYPE = -1;

	private static final Class[] EmptyParameters = new Class[0];

	private long vmRef;
	private Object classLoader;

	private ProtectionDomain protectionDomain;
	private String classNameString;

	private AnnotationType annotationType;

	transient ClassValue.ClassValueMap classValueMap;

	private volatile Map<String, T> _cachedEnumConstantDirectory;

	private volatile T[] _cachedEnumConstants;

	J9VMInternals.ClassInitializationLock initializationLock;
/**
 * Prevents this class from being instantiated. Instances
 * created by the virtual machine only.
 */
private Class() {}

/**
 * Don't change the stack depth of the security.checkMemberAccess() call!
 * The default implementation requires that the code being checked be at
 * a stack depth of 4.
 *
 * @param		type			type of access, PUBLIC or DECLARED
 *
 */
private void checkMemberAccess(int type) {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(2);
		if (callerClassLoader != ClassLoader.bootstrapClassLoader) {
			if (type != MEMBER_INVALID_TYPE) {
				security.checkMemberAccess(this, type);
			}
			String packageName = this.getPackageName();
			ClassLoader loader = getClassLoaderImpl();
			if (sun.reflect.misc.ReflectUtil.needsPackageAccessCheck(callerClassLoader, loader)) {
				if (Proxy.isProxyClass(this)) {
					sun.reflect.misc.ReflectUtil.checkProxyPackageAccess(callerClassLoader, this.getInterfaces());
				} else {
					if (packageName != "") {	//$NON-NLS-1$
						security.checkPackageAccess(packageName);
					}
				}
			}
		}
	}
}

/**
 * Don't change the stack depth of the security.checkMemberAccess() call!
 * The default implementation requires that the code being checked be at
 * a stack depth of 4.
 *
 * This helper method is only called by getClasses, and skip security.checkPackageAccess()
 * when the class is a ProxyClass and the package name is sun.proxy.
 *
 * @param		type			type of access, PUBLIC or DECLARED
 *
 */
private void checkNonSunProxyMemberAccess(int type) {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		ClassLoader callerClassLoader = ClassLoader.getStackClassLoader(2);
		if (callerClassLoader != ClassLoader.bootstrapClassLoader) {
			security.checkMemberAccess(this, type);
			String packageName = this.getPackageName();
			ClassLoader loader = getClassLoaderImpl();
			if (!(Proxy.isProxyClass(this) && packageName.equals(sun.reflect.misc.ReflectUtil.PROXY_PACKAGE)) &&
					packageName != "" && sun.reflect.misc.ReflectUtil.needsPackageAccessCheck(callerClassLoader, loader))	//$NON-NLS-1$
			{
					security.checkPackageAccess(packageName);
			}
		}
	}
}

/**
 * Answers a Class object which represents the class
 * named by the argument. The name should be the name
 * of a class as described in the class definition of
 * java.lang.Class, however Classes representing base
 * types can not be found using this method.
 *
 * @param		className	The name of the non-base type class to find
 * @return		the named Class
 * @throws		ClassNotFoundException If the class could not be found
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public static Class<?> forName(String className) throws ClassNotFoundException {
	ClassLoader defaultClassLoader = ClassLoader.callerClassLoader();
	return forNameImpl(className, true, defaultClassLoader);
}

AnnotationType getAnnotationType() {return annotationType;}
void setAnnotationType(AnnotationType t) {annotationType = t;}

/**
 * Answers a Class object which represents the class
 * named by the argument. The name should be the name
 * of a class as described in the class definition of
 * java.lang.Class, however Classes representing base
 * types can not be found using this method.
 * Security rules will be obeyed.
 *
 * @param		className			The name of the non-base type class to find
 * @param		initializeBoolean	A boolean indicating whether the class should be
 *									initialized
 * @param		classLoader			The classloader to use to load the class
 * @return		the named class.
 * @throws		ClassNotFoundException If the class could not be found
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public static Class<?> forName(String className, boolean initializeBoolean, ClassLoader classLoader)
	throws ClassNotFoundException
{
	/* perform security checks */
	if (null == classLoader) {
		SecurityManager security = System.getSecurityManager();
		if (null != security) {
			ClassLoader callerClassLoader = ClassLoader.callerClassLoader();
			if (callerClassLoader != null) {
				/* only allowed if caller has RuntimePermission("getClassLoader") permission */
				security.checkPermission(RuntimePermission.permissionToGetClassLoader);
			}
		}
	}
	return forNameImpl(className, initializeBoolean, classLoader);
}

/**
 * Answers a Class object which represents the class
 * named by the argument. The name should be the name
 * of a class as described in the class definition of
 * java.lang.Class, however Classes representing base
 * types can not be found using this method.
 *
 * @param		className			The name of the non-base type class to find
 * @param		initializeBoolean	A boolean indicating whether the class should be
 *									initialized
 * @param		classLoader			The classloader to use to load the class
 * @return		the named class.
 * @throws		ClassNotFoundException If the class could not be found
 *
 * @see			java.lang.Class
 */
private static native Class forNameImpl(String className,
                            boolean initializeBoolean,
                            ClassLoader classLoader)
	throws ClassNotFoundException;

/**
 * Answers an array containing all public class members
 * of the class which the receiver represents and its
 * superclasses and interfaces
 *
 * @return		the class' public class members
 * @throws		SecurityException If member access is not allowed
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public Class<?>[] getClasses() {
	checkNonSunProxyMemberAccess(Member.PUBLIC);

	java.util.Vector publicClasses = new java.util.Vector();
	Class current = this;
	Class[] classes;
	while(current != null) {
		classes = current.getDeclaredClassesImpl();
		for (int i = 0; i < classes.length; i++)
			if (Modifier.isPublic(classes[i].getModifiers()))
				publicClasses.addElement(classes[i]);
		current = current.getSuperclass();
	}
	classes = new Class[publicClasses.size()];
	publicClasses.copyInto(classes);
	return classes;
}

/**
 * Answers the classloader which was used to load the
 * class represented by the receiver. Answer null if the
 * class was loaded by the system class loader
 *
 * @return		the receiver's class loader or nil
 *
 * @see			java.lang.ClassLoader
 */
@sun.reflect.CallerSensitive
public ClassLoader getClassLoader() {
	ClassLoader loader = getClassLoaderImpl();
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
		// No check for bootstrapClassLoader, contrary to
		// spec but testing reveals this is correct behavior.
		if (loader == ClassLoader.bootstrapClassLoader) return null;
		ClassLoader callersClassLoader = ClassLoader.callerClassLoader();
		if (callersClassLoader != null && callersClassLoader != loader
			&& !callersClassLoader.isAncestorOf(loader))
				security.checkPermission(
					RuntimePermission.permissionToGetClassLoader);
	}
	if (loader == ClassLoader.bootstrapClassLoader) return null;
	return loader;
}

/**
 * Answers the ClassLoader which was used to load the
 * class represented by the receiver. Answer null if the
 * class was loaded by the system class loader
 *
 * @return		the receiver's class loader or nil
 *
 * @see			java.lang.ClassLoader
 */
ClassLoader getClassLoader0() {
	ClassLoader loader = getClassLoaderImpl();
	return loader;
}

/**
 * Return the ClassLoader for this Class without doing any security
 * checks. The bootstrap ClassLoader is returned, unlike getClassLoader()
 * which returns null in place of the bootstrap ClassLoader.
 *
 * @return the ClassLoader
 *
 * @see ClassLoader#isASystemClassLoader()
 */
ClassLoader getClassLoaderImpl()
{
	return J9VMInternals.getClassLoader(this);
}

/**
 * Answers a Class object which represents the receiver's
 * component type if the receiver represents an array type.
 * Otherwise answers nil. The component type of an array
 * type is the type of the elements of the array.
 *
 * @return		the component type of the receiver.
 *
 * @see			java.lang.Class
 */
public native Class<?> getComponentType();

private void throwNoSuchMethodException(String name, Class[] types) throws NoSuchMethodException {
	StringBuffer error = new StringBuffer();
	error.append(getName()).append('.').append(name).append('(');
	if (types.length > 0) {
		error.append(types[0] == null ? null : types[0].getName());
		for (int i=1; i<types.length; i++) {
			error.append(", ").append(types[i] == null ? null : types[i].getName()); //$NON-NLS-1$
		}
	}
	error.append(')');
	throw new NoSuchMethodException(error.toString());
}

/**
 * Answers a public Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @return		the constructor described by the arguments.
 * @throws		NoSuchMethodException if the constructor could not be found.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getConstructors
 */
@sun.reflect.CallerSensitive
public Constructor<T> getConstructor(Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	checkMemberAccess(Member.PUBLIC);

	Constructor cachedConstructor = lookupCachedConstructor(parameterTypes == null ? EmptyParameters : parameterTypes);
	if (cachedConstructor != null && Modifier.isPublic(cachedConstructor.getModifiers())) {
		return cachedConstructor;
	}

	J9VMInternals.prepare(this);

	// Handle the default constructor case upfront
	if(parameterTypes == null || parameterTypes.length == 0) {
		Constructor rc = getConstructorImpl(EmptyParameters, "()V"); //$NON-NLS-1$
		if (rc == null) throwNoSuchMethodException("<init>", EmptyParameters); //$NON-NLS-1$
		return cacheConstructor(rc);
	}

	// Build a signature for the requested method.
	int total = 3;
	String[] sigs = new String[parameterTypes.length];
	for(int i = 0; i < parameterTypes.length; i++) {
		if (parameterTypes[i] != null) {
			sigs[i] = parameterTypes[i].getSignature();
			total += sigs[i].length();
		} else throwNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
	}
	StringBuffer signature = new StringBuffer(total);
	signature.append('(');
	for(int i = 0; i < parameterTypes.length; i++)
		signature.append(sigs[i]);
	signature.append(")V"); //$NON-NLS-1$

	Constructor rc = getConstructorImpl((Class[])parameterTypes.clone(), signature.toString());
	if (rc != null)
		rc = checkParameterTypes(rc, parameterTypes);
	if (rc == null) throwNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
	return cacheConstructor(rc);
}

/**
 * Answers a public Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @param		signature		the signature of the method.
 * @return		the constructor described by the arguments.
 *
 * @see			#getConstructors
 */
private native Constructor getConstructorImpl(Class parameterTypes[], String signature);

/**
 * Answers an array containing Constructor objects describing
 * all constructors which are visible from the current execution
 * context.
 *
 * @return		all visible constructors starting from the receiver.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Constructor<?>[] getConstructors() throws SecurityException {
	checkMemberAccess(Member.PUBLIC);
	Constructor[] cachedConstructors = lookupCachedConstructors(PublicKey);
	if (cachedConstructors != null) {
		return cachedConstructors;
	}

	J9VMInternals.prepare(this);

	return cacheConstructors(getConstructorsImpl(), PublicKey);
}

/**
 * Answers an array containing Constructor objects describing
 * all constructors which are visible from the current execution
 * context.
 *
 * @return		all visible constructors starting from the receiver.
 *
 * @see			#getMethods
 */
private native Constructor[] getConstructorsImpl();

/**
 * Answers an array containing all class members of the class
 * which the receiver represents. Note that some of the fields
 * which are returned may not be visible in the current
 * execution context.
 *
 * @return		the class' class members
 * @throws		SecurityException if member access is not allowed
 *
 * @see			java.lang.Class
 */
@sun.reflect.CallerSensitive
public Class<?>[] getDeclaredClasses() throws SecurityException {
	checkNonSunProxyMemberAccess(Member.DECLARED);
	return getDeclaredClassesImpl();
}

/**
 * Answers an array containing all class members of the class
 * which the receiver represents. Note that some of the fields
 * which are returned may not be visible in the current
 * execution context.
 *
 * @return		the class' class members
 *
 * @see			java.lang.Class
 */
private native Class[] getDeclaredClassesImpl();

/**
 * Answers a Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @return		the constructor described by the arguments.
 * @throws		NoSuchMethodException if the constructor could not be found.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getConstructors
 */
@sun.reflect.CallerSensitive
public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	checkMemberAccess(Member.DECLARED);

	Constructor cachedConstructor = lookupCachedConstructor(parameterTypes == null ? EmptyParameters : parameterTypes);
	if (cachedConstructor != null) {
		return cachedConstructor;
	}

	J9VMInternals.prepare(this);

	// Handle the default constructor case upfront
	if(parameterTypes == null || parameterTypes.length == 0) {
		Constructor rc = getDeclaredConstructorImpl(EmptyParameters, "()V"); //$NON-NLS-1$
		if (rc == null) throwNoSuchMethodException("<init>", EmptyParameters); //$NON-NLS-1$
		return cacheConstructor(rc);
	}

	// Build a signature for the requested method.
	int total = 3;
	String[] sigs = new String[parameterTypes.length];
	for(int i = 0; i < parameterTypes.length; i++) {
		if (parameterTypes[i] != null) {
			sigs[i] = parameterTypes[i].getSignature();
			total += sigs[i].length();
		} else throwNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
	}
	StringBuffer signature = new StringBuffer(total);
	signature.append('(');
	for(int i = 0; i < parameterTypes.length; i++)
		signature.append(sigs[i]);
	signature.append(")V"); //$NON-NLS-1$

	Constructor rc = getDeclaredConstructorImpl((Class[])parameterTypes.clone(), signature.toString());
	if (rc != null)
		rc = checkParameterTypes(rc, parameterTypes);
	if (rc == null) throwNoSuchMethodException("<init>", parameterTypes); //$NON-NLS-1$
	return cacheConstructor(rc);
}

/**
 * Answers a Constructor object which represents the
 * constructor described by the arguments.
 *
 * @param		parameterTypes	the types of the arguments.
 * @param		signature		the signature of the method.
 * @return		the constructor described by the arguments.
 *
 * @see			#getConstructors
 */
private native Constructor getDeclaredConstructorImpl(Class parameterTypes[], String signature);

/**
 * Answers an array containing Constructor objects describing
 * all constructor which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's constructors.
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
	checkMemberAccess(Member.DECLARED);
	Constructor[] cachedConstructors = lookupCachedConstructors(DeclaredKey);
	if (cachedConstructors != null) {
		return cachedConstructors;
	}

	J9VMInternals.prepare(this);

	return cacheConstructors(getDeclaredConstructorsImpl(), DeclaredKey);
}

/**
 * Answers an array containing Constructor objects describing
 * all constructor which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's constructors.
 *
 * @see			#getMethods
 */
private native Constructor[] getDeclaredConstructorsImpl();

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument. Note that the Constructor may not be
 * visible from the current execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException if the requested field could not be found
 * @throws		SecurityException if member access is not allowed
 *
 * @see			#getDeclaredFields
 */
@sun.reflect.CallerSensitive
public Field getDeclaredField(String name) throws NoSuchFieldException, SecurityException {
	checkMemberAccess(Member.DECLARED);
	Field cachedField = lookupCachedField(name);
	if (cachedField != null && cachedField.getDeclaringClass() == this) {
		return cachedField;
	}

	J9VMInternals.prepare(this);

	return cacheField(getDeclaredFieldImpl(name));
}

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument. Note that the Constructor may not be
 * visible from the current execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException If the given field does not exist
 *
 * @see			#getDeclaredFields
 */
private native Field getDeclaredFieldImpl(String name) throws NoSuchFieldException;

/**
 * Answers an array containing Field objects describing
 * all fields which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's fields.
 * @throws		SecurityException If member access is not allowed
 *
 * @see			#getFields
 */
@sun.reflect.CallerSensitive
public Field[] getDeclaredFields() throws SecurityException {
	checkMemberAccess(Member.DECLARED);
	Field[] cachedFields = lookupCachedFields(DeclaredKey);
	if (cachedFields != null) {
		return cachedFields;
	}

	J9VMInternals.prepare(this);

	return cacheFields(getDeclaredFieldsImpl(), DeclaredKey);
}

/**
 * Answers an array containing Field objects describing
 * all fields which are defined by the receiver. Note that
 * some of the fields which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's fields.
 *
 * @see			#getFields
 */
private native Field[] getDeclaredFieldsImpl();

/**
 * Answers a Method object which represents the method
 * described by the arguments. Note that the associated
 * method may not be visible from the current execution
 * context.
 *
 * @param		name			the name of the method
 * @param		parameterTypes	the types of the arguments.
 * @return		the method described by the arguments.
 * @throws		NoSuchMethodException if the method could not be found.
 * @throws		SecurityException If member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	checkMemberAccess(Member.DECLARED);
	Method result, bestCandidate;
	int maxDepth;
	String strSig;
	Class[] params;

	Method cachedMethod = lookupCachedMethod(name, parameterTypes == null ? EmptyParameters : parameterTypes);
	if (cachedMethod != null && cachedMethod.getDeclaringClass() == this) {
		return cachedMethod;
	}

	if ((this == sun.misc.Unsafe.class) && (name.equals("getUnsafe"))) { //$NON-NLS-1$
		throwNoSuchMethodException(name, parameterTypes);
	}

	J9VMInternals.prepare(this);

	// Handle the no parameter case upfront
	if(name == null || parameterTypes == null || parameterTypes.length == 0) {
		strSig = "()"; //$NON-NLS-1$
		params = EmptyParameters;
	} else {
		// Build a signature for the requested method.
		strSig = getParameterTypesSignature(name, parameterTypes);
		params = (Class[])parameterTypes.clone();

	}

	result = getDeclaredMethodImpl(name, params, strSig, null);
	if (result == null) throwNoSuchMethodException(name, params);

	if (params.length > 0) {
		// the result.getDeclaringClass() is always the same as this class for getDeclaredMethod()
		ClassLoader loader = getClassLoaderImpl();
		for (int i=0; i<params.length; i++) {
			Class parameterType = params[i];
			if (!parameterType.isPrimitive()) {
				try {
					if (Class.forName(parameterType.getName(), false, loader) != parameterType) {
						throwNoSuchMethodException(name, parameterTypes);
					}
				} catch(ClassNotFoundException e) {
					throwNoSuchMethodException(name, parameterTypes);
				}
			}
		}
	}

	/* [PR 113003] The native is called repeatedly until it returns null,
	 * as each call returns another match if one exists.
	 * If more than one match is found, the code below selects the
	 * candidate method whose return type has the largest depth. The latter
	 * case is expected to occur only in certain JCK tests, as most Java
	 * compilers will refuse to produce a class file with multiple methods
	 * of the same name differing only in return type.
	 *
	 * Selecting by largest depth is one possible algorithm that satisfies the
	 * spec.
	 */
	bestCandidate = result;
	maxDepth = result.getReturnType().getClassDepth();
	while( true ) {
		result = getDeclaredMethodImpl( name, params, strSig, result );
		if( result == null ) {
			break;
		}
		int resultDepth = result.getReturnType().getClassDepth();
		if( resultDepth > maxDepth ) {
			bestCandidate = result;
			maxDepth = resultDepth;
		}
	}
	return cacheMethod(bestCandidate);
}

/**
 * This native iterates over methods matching the provided name and signature
 * in the receiver class. The startingPoint parameter is passed the last
 * method returned (or null on the first use), and the native returns the next
 * matching method or null if there are no more matches.
 * Note that the associated method may not be visible from the
 * current execution context.
 *
 * @param		name				the name of the method
 * @param		parameterTypes		the types of the arguments.
 * @param		partialSignature	the signature of the method, without return type.
 * @param		startingPoint		the method to start searching after, or null to start at the beginning
 * @return		the next Method described by the arguments
 *
 * @see			#getMethods
 */
private native Method getDeclaredMethodImpl(String name, Class parameterTypes[], String partialSignature, Method startingPoint);

/**
 * Answers an array containing Method objects describing
 * all methods which are defined by the receiver. Note that
 * some of the methods which are returned may not be visible
 * in the current execution context.
 *
 * @throws		SecurityException	if member access is not allowed
 * @return		the receiver's methods.
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Method[] getDeclaredMethods() throws SecurityException {
	checkMemberAccess(Member.DECLARED);
	Method[] methods;

	Method[] cachedMethods = lookupCachedMethods(DeclaredKey);
	if (cachedMethods != null) {
		return cachedMethods;
	}

	J9VMInternals.prepare(this);

	methods = getDeclaredMethodsImpl();

	if (this == sun.misc.Unsafe.class) {
		methods = filterUnsafe(methods);
	}

	return cacheMethods(methods, DeclaredKey);
}

/**
 * Answers an array containing Method objects describing
 * all methods which are defined by the receiver. Note that
 * some of the methods which are returned may not be visible
 * in the current execution context.
 *
 * @return		the receiver's methods.
 *
 * @see			#getMethods
 */
private native Method[] getDeclaredMethodsImpl();

/**
 * Answers the class which declared the class represented
 * by the receiver. This will return null if the receiver
 * is a member of another class.
 *
 * @return		the declaring class of the receiver.
 */
public Class<?> getDeclaringClass() {
	Class declaringClass = getDeclaringClassImpl();
	if (declaringClass == null) {
		return declaringClass;
	}
	if (declaringClass.isClassADeclaredClass(this)) {
		declaringClass.checkMemberAccess(MEMBER_INVALID_TYPE);
		return declaringClass;
	}

	// K0555 = incompatible InnerClasses attribute between "{0}" and "{1}"
	throw new IncompatibleClassChangeError(
			com.ibm.oti.util.Msg.getString("K0555", this.getName(),	declaringClass.getName()));  //$NON-NLS-1$
}
/**
 * Returns true if the class passed in to the method is a declared class of
 * this class.
 *
 * @param		aClass		The class to validate
 * @return		true if aClass a declared class of this class
 * 				false otherwise.
 *
 */
private native boolean isClassADeclaredClass(Class<?> aClass);

/**
 * Answers the class which declared the class represented
 * by the receiver. This will return null if the receiver
 * is a member of another class.
 *
 * @return		the declaring class of the receiver.
 */
private native Class getDeclaringClassImpl();

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument which must be visible from the current
 * execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException If the given field does not exist
 * @throws		SecurityException If access is denied
 *
 * @see			#getDeclaredFields
 */
@sun.reflect.CallerSensitive
public Field getField(String name) throws NoSuchFieldException, SecurityException {
	checkMemberAccess(Member.PUBLIC);
	Field cachedField = lookupCachedField(name);
	if (cachedField != null && Modifier.isPublic(cachedField.getModifiers())) {
		return cachedField;
	}

	J9VMInternals.prepare(this);

	return cacheField(getFieldImpl(name));
}

/**
 * Answers a Field object describing the field in the receiver
 * named by the argument which must be visible from the current
 * execution context.
 *
 * @param		name		The name of the field to look for.
 * @return		the field in the receiver named by the argument.
 * @throws		NoSuchFieldException If the given field does not exist
 *
 * @see			#getDeclaredFields
 */
private native Field getFieldImpl(String name) throws NoSuchFieldException;

/**
 * Answers an array containing Field objects describing
 * all fields which are visible from the current execution
 * context.
 *
 * @return		all visible fields starting from the receiver.
 * @throws		SecurityException If member access is not allowed
 *
 * @see			#getDeclaredFields
 */
@sun.reflect.CallerSensitive
public Field[] getFields() throws SecurityException {
	checkMemberAccess(Member.PUBLIC);
	Field[] cachedFields = lookupCachedFields(PublicKey);
	if (cachedFields != null) {
		return cachedFields;
	}

	J9VMInternals.prepare(this);

	return cacheFields(getFieldsImpl(), PublicKey);
}

/**
 * Answers an array containing Field objects describing
 * all fields which are visible from the current execution
 * context.
 *
 * @return		all visible fields starting from the receiver.
 *
 * @see			#getDeclaredFields
 */
private native Field[] getFieldsImpl();

/**
 * Answers an array of Class objects which match the interfaces
 * specified in the receiver classes <code>implements</code>
 * declaration
 *
 * @return		Class[]
 *					the interfaces the receiver claims to implement.
 */
public Class<?>[] getInterfaces()
{
	return J9VMInternals.getInterfaces(this);
}

/**
 * Answers a Method object which represents the method
 * described by the arguments.
 *
 * @param		name String
 *					the name of the method
 * @param		parameterTypes Class[]
 *					the types of the arguments.
 * @return		Method
 *					the method described by the arguments.
 * @throws	NoSuchMethodException
 *					if the method could not be found.
 * @throws	SecurityException
 *					if member access is not allowed
 *
 * @see			#getMethods
 */
@sun.reflect.CallerSensitive
public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
	checkMemberAccess(Member.PUBLIC);
	Method result, bestCandidate;
	int maxDepth;
	String strSig;
	Class[] params;

	Method cachedMethod = lookupCachedMethod(name, parameterTypes == null ? EmptyParameters : parameterTypes);
	if (cachedMethod != null && Modifier.isPublic(cachedMethod.getModifiers())) {
		return cachedMethod;
	}

	if ((this == sun.misc.Unsafe.class) && (name.equals("getUnsafe"))) { //$NON-NLS-1$
		throwNoSuchMethodException(name, parameterTypes);
	}

	J9VMInternals.prepare(this);

	// Handle the no parameter case upfront
	if(name == null || parameterTypes == null || parameterTypes.length == 0) {
		strSig = "()"; //$NON-NLS-1$
		params = EmptyParameters;
	} else {
		// Build a signature for the requested method.
		strSig = getParameterTypesSignature(name, parameterTypes);
		params = (Class[])parameterTypes.clone();

	}
	result = getMethodImpl(name, params, strSig);

	if (result == null) throwNoSuchMethodException(name, params);

	if (params.length > 0) {
		ClassLoader loader = result.getDeclaringClass().getClassLoaderImpl();
		for (int i=0; i<params.length; i++) {
			Class parameterType = params[i];
			if (!parameterType.isPrimitive()) {
				try {
					if (Class.forName(parameterType.getName(), false, loader) != parameterType) {
						throwNoSuchMethodException(name, parameterTypes);
					}
				} catch(ClassNotFoundException e) {
					throwNoSuchMethodException(name, parameterTypes);
				}
			}
		}
	}

	/* [PR 113003] The native is called repeatedly until it returns null,
	 * as each call returns another match if one exists. The first call uses
	 * getMethodImpl which searches across superclasses and interfaces, but
	 * since the spec requires that we only weigh multiple matches against
	 * each other if they are in the same class, on subsequent calls we call
	 * getDeclaredMethodImpl on the declaring class of the first hit.
	 * If more than one match is found, the code below selects the
	 * candidate method whose return type has the largest depth. This case
	 * case is expected to occur only in certain JCK tests, as most Java
	 * compilers will refuse to produce a class file with multiple methods
	 * of the same name differing only in return type.
	 *
	 * Selecting by largest depth is one possible algorithm that satisfies the
	 * spec.
	 */
	bestCandidate = result;
	maxDepth = result.getReturnType().getClassDepth();
	Class declaringClass = result.getDeclaringClass();
	while( true ) {
		result = declaringClass.getDeclaredMethodImpl( name, params, strSig, result );
		if( result == null ) {
			break;
		}
		if( (result.getModifiers() & Modifier.PUBLIC) != 0 ) {
			int resultDepth = result.getReturnType().getClassDepth();
			if( resultDepth > maxDepth ) {
				bestCandidate = result;
				maxDepth = resultDepth;
			}
		}
	}

	return cacheMethod(bestCandidate);
}

/**
 * Answers a Method object which represents the first method found matching
 * the arguments.
 *
 * @param		name String
 *					the name of the method
 * @param		parameterTypes Class[]
 *					the types of the arguments.
 * @param		partialSignature String
 *					the signature of the method, without return type.
 * @return		Object
 *					the first Method found matching the arguments
 *
 * @see			#getMethods
 */
private native Method getMethodImpl(String name, Class parameterTypes[], String partialSignature);

/**
 * Answers an array containing Method objects describing
 * all methods which are visible from the current execution
 * context.
 *
 * @return		Method[]
 *					all visible methods starting from the receiver.
 * @throws	SecurityException
 *					if member access is not allowed
 *
 * @see			#getDeclaredMethods
 */
@sun.reflect.CallerSensitive
public Method[] getMethods() throws SecurityException {
	checkMemberAccess(Member.PUBLIC);
	Method[] methods;

	methods = lookupCachedMethods(PublicKey);
	if (methods != null) {
		return methods;
	}

	if(isPrimitive()) return new Method[0];

	J9VMInternals.prepare(this);

	if(isInterface())
	{
		methods = getInterfaceMethodsImpl();
		return cacheMethods(methods, PublicKey);
	}
	else
	{
		int vCount = 0;
		int sCount = 0;

		do {
			vCount = getVirtualMethodCountImpl();
			sCount = getStaticMethodCountImpl();
			methods = (Method[])Method.class.allocateAndFillArray(vCount + sCount);
			if ((true == getVirtualMethodsImpl(methods, 0, vCount)) && (true == getStaticMethodsImpl(methods, vCount, sCount))) {
				break;
			}
		} while (true);

		for (int index = 0; index < vCount; index++) {
			if (methods[index] != null && methods[index].getDeclaringClass().isInterface()) {
				// there is an abstract methods
				Method[] interfaceMethods = getClassInterfaceMethodsImpl();
				int count = interfaceMethods.length;
				for (int i = 0; i < interfaceMethods.length; i++) {
					if (interfaceMethods[i] != null) {
						for (int j = 0; j < vCount; j++) {
							if (methodsEqual(interfaceMethods[i], methods[j])) {
								Class declaringClass = methods[j].getDeclaringClass();
								if (declaringClass.isInterface()) {
									if (!declaringClass.equals(interfaceMethods[i].getDeclaringClass())) {
										//	this is an extra interface method not returned by getVirtualMethodsImpl()
										//	it will be added to methodResult
										continue;
									}
								}

								interfaceMethods[i] = null;
								count--;
								break;
							}
						}
					}
				}

				int methodsLength = methods.length;
				Method[] methodResult = new Method[methodsLength + count];
				System.arraycopy(methods, 0, methodResult, 0, methodsLength);
				int appendIndex = 0;
				for(int k = 0; k < interfaceMethods.length; k++) {
					if (interfaceMethods[k] != null) {
						methodResult[methodsLength + appendIndex] = interfaceMethods[k];
						appendIndex++;
					}
				}

				return cacheMethods(methodResult, PublicKey);
			}
		}

		if (this == sun.misc.Unsafe.class) {
			methods = filterUnsafe(methods);
		}

		return cacheMethods(methods, PublicKey);
	}
}

private boolean methodsEqual(Method m1, Method m2) {
	Class[] m1Parms, m2Parms;

	if(!m1.getName().equals(m2.getName())) {
		return false;
	}
	if(!m1.getReturnType().equals(m2.getReturnType())) {
		return false;
	}
	m1Parms = m1.getParameterTypes();
	m2Parms = m2.getParameterTypes();
	if(m1Parms.length != m2Parms.length) {
		return false;
	}
	for(int i = 0; i < m1Parms.length; i++) {
		if(m1Parms[i] != m2Parms[i]) {
			return false;
		}
	}
	return true;
}

private int getInterfaceMethodCountImpl()
{
	Class[] parents;
	int count;

	count = getDeclaredMethods().length;
	parents = getInterfaces();
	for(int i = 0; i < parents.length; i++) {
		count += parents[i].getInterfaceMethodCountImpl();
	}
	return count;
}

private Method[] getInterfaceMethodsImpl()
{
	Class[] parents;
	/**
	 * %local% is a local methods array returned from getDeclaredMethods()
	 * %scratch% is a temporary array with a pessimal amount of space,
	 * 	abstract local methods, non-null interface methods inherited from super-interfaces are copied to scratch sequentially,
	 * 	and eventually all non-null methods entries in scratch are copied to unique array which has exactly number of these non-null entries.
	 * %unique% array is returned as result of this getInterfaceMethodsImpl()
	 */
	Method[] scratch, unique, local;
	/**
	 * %index% points to the entry after latest non-null method entry added to scratch array
	 * 	or the number of non-null method entries already added to scratch array
	 * %localIndex% points to the entry after last local abstract method entry copied to scratch array
	 * 	or the number of local abstract method entries added to scratch array
	 * %indexPrevBlock% points to the entry after last interface method entry added to scratch which was returned by a call to getInterfaceMethodsImpl(),
	 * 	and following interface methods to be added will be returned by another call to getInterfaceMethodsImpl() for other super interfaces.
	 */
	int index, localIndex, indexPrevBlock;

	/* Get a pessimal amount of scratch space */
	scratch = new Method[getInterfaceMethodCountImpl()];

	/* Obtain the local methods. These are guaranteed to be returned */
	local = getDeclaredMethods();
	index = 0;
	for(int i = 0; i < local.length; i++) {
		/* <clinit> is never abstract */
		if(Modifier.isAbstract(local[i].getModifiers())) {
			scratch[index++] = local[i];
		}
	}
	localIndex = index;
	indexPrevBlock = index;

	/* Walk each superinterface */
	parents = getInterfaces();
	for(int i = 0; i < parents.length; i++) {
		/* Get the superinterface's (swept) methods */
		Method[] parentMethods = parents[i].getInterfaceMethodsImpl();
		for (int j = 0; j < parentMethods.length; j++) {
			if (parentMethods[j] != null) {
				/* Sweep out any local overrides */
				boolean	redundant = false;
				for (int k = 0; k < localIndex; k++) {
					if(methodsEqual(scratch[k], parentMethods[j])) {
						//	found local override
						redundant = true;
						break;
					}
				}
				if (!redundant) {
					for (int k = localIndex; k < indexPrevBlock; k++) {
						if (scratch[k].equals(parentMethods[j])) {
							//	found a dup
							redundant = true;
							break;
						}
					}
				}
				if (!redundant) {
					//	non-null interface methods, no override, no dup
					scratch[index] = parentMethods[j];
					index++;
				}
			}
		}
		indexPrevBlock = index;
	}

	/* Copy into a correctly sized array and return */
	unique = new Method[index];
	System.arraycopy(scratch, 0, unique, 0, index);

	return unique;
}

/* return the number of interface methods inherited from parent classes & interfaces */
private int getClassInterfaceMethodsCountImpl() {
	Class parent;
	Class[] interfaces;
	int count = 0;

	parent = this.getSuperclass();
	if (parent != null && !parent.equals(Object.class)) {
		count += parent.getClassInterfaceMethodsCountImpl();
	}

	interfaces = getInterfaces();
	for(int i = 0; i < interfaces.length; i++) {
		count += interfaces[i].getInterfaceMethodCountImpl();
	}
	return count;
}

/* return an array of interface methods inherited from parent classes & interfaces */
private Method[] getClassInterfaceMethodsImpl()
{
	Method[] unique, scratch, parentsMethods;
	Class parent;
	Class[] interfaces;
	int index = 0, indexPrevBlock = 0;

	scratch = new Method[getClassInterfaceMethodsCountImpl()];
	parent = this.getSuperclass();
	if (parent != null && !parent.equals(Object.class)) {
		parentsMethods = parent.getClassInterfaceMethodsImpl();
		System.arraycopy(parentsMethods, 0, scratch, 0, parentsMethods.length);
		index = parentsMethods.length;
		indexPrevBlock = index;
	}

	/* Walk each superinterface */
	interfaces = getInterfaces();
	for(int i = 0; i < interfaces.length; i++) {
		Method[] interfaceMethods = interfaces[i].getInterfaceMethodsImpl();

		if (index == 0) {
			System.arraycopy(interfaceMethods, 0, scratch, 0, interfaceMethods.length);
			index = interfaceMethods.length;
			indexPrevBlock = index;
		} else {
			/* 	Not check override here, interface methods overrided will be removed within getMethods()
			 *  when comparing interface methods returned from this methods with virtual methods returned
			 *  from getVirtualMethodsImpl()
			 */
			/* Sweep out any duplicates */
			for(int j = 0; j < interfaceMethods.length; j++) {
				//	compare with interface methods inherited from parent classes and
				//	other interfaces already added to scratch
				if (interfaceMethods[j] != null) {
					boolean redundant = false;
					for(int k = 0; k < indexPrevBlock; k++) {
						if((scratch[k] != null) && interfaceMethods[j].equals(scratch[k])) {
							//	found a dup
							redundant = true;
							break;
						}
					}
					if (!redundant) {
						// add to scratch
						scratch[index] = interfaceMethods[j];
						index++;
					}
				}
			}
			indexPrevBlock = index;
		}
	}

	unique = new Method[index];
	System.arraycopy(scratch, 0, unique, 0, index);

	return unique;
}

private native int getVirtualMethodCountImpl();
private native boolean getVirtualMethodsImpl(Method[] array, int start, int count);
private native int getStaticMethodCountImpl();
private native boolean getStaticMethodsImpl(Method[] array, int start, int count);
private native Object[] allocateAndFillArray(int size);

/**
 * Answers an integer which which is the receiver's modifiers.
 * Note that the constants which describe the bits which are
 * returned are implemented in class java.lang.reflect.Modifier
 * which may not be available on the target.
 *
 * @return		the receiver's modifiers
 */
public int getModifiers() {
	int rawModifiers = getModifiersImpl();
	if (isArray()) {
		rawModifiers &= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
				Modifier.ABSTRACT | Modifier.FINAL;
	} else {
		rawModifiers &= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
					Modifier.STATIC | Modifier.FINAL | Modifier.INTERFACE |
					Modifier.ABSTRACT | SYNTHETIC | ENUM | ANNOTATION;
	}
	return rawModifiers;
}

private native int getModifiersImpl();

/**
 * Answers the name of the class which the receiver represents.
 * For a description of the format which is used, see the class
 * definition of java.lang.Class.
 *
 * @return		the receiver's name.
 *
 * @see			java.lang.Class
 */
public String getName() {
	String name = classNameString;
	if (name != null){
		return name;
	}
	//must have been null to set it
	name = getNameImpl().intern();
	classNameString = name;
	return name;
}

native String getNameImpl();

/**
 * Answers the ProtectionDomain of the receiver.
 * <p>
 * Note: In order to conserve space in embedded targets, we allow this
 * method to answer null for classes in the system protection domain
 * (i.e. for system classes). System classes are always given full
 * permissions (i.e. AllPermission). This is not changeable via the
 * java.security.Policy.
 *
 * @return		ProtectionDomain
 *					the receiver's ProtectionDomain.
 *
 * @see			java.lang.Class
 */
public ProtectionDomain getProtectionDomain() {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkPermission(
			RuntimePermission.permissionToGetProtectionDomain);

	ProtectionDomain result = getPDImpl();
	if (result != null) return result;

	if (AllPermissionsPD == null) {
			Permissions collection = new Permissions();
			collection.add(new AllPermission());
			AllPermissionsPD = new ProtectionDomain(null, collection);
	}
	return AllPermissionsPD;
}

/**
 * Answers the ProtectionDomain of the receiver.
 * <p>
 * This method is for internal use only.
 *
 * @return		ProtectionDomain
 *					the receiver's ProtectionDomain.
 *
 * @see			java.lang.Class
 */
ProtectionDomain getPDImpl() {
	return protectionDomain;
}

/**
 * Answers the name of the package to which the receiver belongs.
 * For example, Object.class.getPackageName() returns "java.lang".
 *
 * @return		the receiver's package name.
 *
 * @see			#getPackage
 */
String getPackageName() {
	String name = getName();
	int index = name.lastIndexOf('.');
	if (index >= 0) return name.substring(0, index);
	return ""; //$NON-NLS-1$
}

/**
 * Answers a read-only stream on the contents of the
 * resource specified by resName. The mapping between
 * the resource name and the stream is managed by the
 * class' class loader.
 *
 * @param		resName 	the name of the resource.
 * @return		a stream on the resource.
 *
 * @see			java.lang.ClassLoader
 */
public URL getResource(String resName) {
	ClassLoader loader = this.getClassLoaderImpl();
	if (loader == ClassLoader.bootstrapClassLoader)
		return ClassLoader.getSystemResource(this.toResourceName(resName));
	else
		return loader.getResource(this.toResourceName(resName));
}

/**
 * Answers a read-only stream on the contents of the
 * resource specified by resName. The mapping between
 * the resource name and the stream is managed by the
 * class' class loader.
 *
 * @param		resName		the name of the resource.
 * @return		a stream on the resource.
 *
 * @see			java.lang.ClassLoader
 */
public InputStream getResourceAsStream(String resName) {
		ClassLoader loader = this.getClassLoaderImpl();
		if (loader == ClassLoader.bootstrapClassLoader)
			return ClassLoader.getSystemResourceAsStream(this.toResourceName(resName));
		else
			return loader.getResourceAsStream(this.toResourceName(resName));
}

/**
 * Answers a String object which represents the class's
 * signature, as described in the class definition of
 * java.lang.Class.
 *
 * @return		the signature of the class.
 *
 * @see			java.lang.Class
 */
private String getSignature() {
	if(isArray()) return getName(); // Array classes are named with their signature
	if(isPrimitive()) {
		// Special cases for each base type.
		if(this == void.class) return "V"; //$NON-NLS-1$
		if(this == boolean.class) return "Z"; //$NON-NLS-1$
		if(this == byte.class) return "B"; //$NON-NLS-1$
		if(this == char.class) return "C"; //$NON-NLS-1$
		if(this == short.class) return "S"; //$NON-NLS-1$
		if(this == int.class) return "I"; //$NON-NLS-1$
		if(this == long.class) return "J"; //$NON-NLS-1$
		if(this == float.class) return "F"; //$NON-NLS-1$
		if(this == double.class) return "D"; //$NON-NLS-1$
	}

	// General case.
	// Create a StringBuffer of the correct size
	String name = getName();
	return new StringBuffer(name.length() + 2).
		append('L').append(name).append(';').toString();
}

/**
 * Answers the signers for the class represented by the
 * receiver, or null if there are no signers.
 *
 * @return		the signers of the receiver.
 *
 * @see			#getMethods
 */
public Object[] getSigners() {
	 return getClassLoaderImpl().getSigners(this);
}

/**
 * Answers the Class which represents the receiver's
 * superclass. For Classes which represent base types,
 * interfaces, and for java.lang.Object the method
 * answers null.
 *
 * @return		the receiver's superclass.
 */
public Class<? super T> getSuperclass()
{
	return J9VMInternals.getSuperclass(this);
}

/**
 * Answers true if the receiver represents an array class.
 *
 * @return		<code>true</code>
 *					if the receiver represents an array class
 *              <code>false</code>
 *                  if it does not represent an array class
 */
public native boolean isArray();

/**
 * Answers true if the type represented by the argument
 * can be converted via an identity conversion or a widening
 * reference conversion (i.e. if either the receiver or the
 * argument represent primitive types, only the identity
 * conversion applies).
 *
 * @return		<code>true</code>
 *					the argument can be assigned into the receiver
 *              <code>false</code>
 *					the argument cannot be assigned into the receiver
 * @param		cls	Class
 *					the class to test
 * @throws	NullPointerException
 *					if the parameter is null
 *
 */
public native boolean isAssignableFrom(Class<?> cls);

/**
 * Answers true if the argument is non-null and can be
 * cast to the type of the receiver. This is the runtime
 * version of the <code>instanceof</code> operator.
 *
 * @return		<code>true</code>
 *					the argument can be cast to the type of the receiver
 *              <code>false</code>
 *					the argument is null or cannot be cast to the
 *					type of the receiver
 *
 * @param		object Object
 *					the object to test
 */
public native boolean isInstance(Object object);

/**
 * Answers true if the receiver represents an interface.
 *
 * @return		<code>true</code>
 *					if the receiver represents an interface
 *              <code>false</code>
 *                  if it does not represent an interface
 */
public boolean isInterface() {
	return !isArray() && (getModifiersImpl() & 512 /* AccInterface */) != 0;
}

/**
 * Answers true if the receiver represents a base type.
 *
 * @return		<code>true</code>
 *					if the receiver represents a base type
 *              <code>false</code>
 *                  if it does not represent a base type
 */
public native boolean isPrimitive();

/**
 * Answers a new instance of the class represented by the
 * receiver, created by invoking the default (i.e. zero-argument)
 * constructor. If there is no such constructor, or if the
 * creation fails (either because of a lack of available memory or
 * because an exception is thrown by the constructor), an
 * InstantiationException is thrown. If the default constructor
 * exists, but is not accessible from the context where this
 * message is sent, an IllegalAccessException is thrown.
 *
 * @return		a new instance of the class represented by the receiver.
 * @throws		IllegalAccessException if the constructor is not visible to the sender.
 * @throws		InstantiationException if the instance could not be created.
 */
@sun.reflect.CallerSensitive
public T newInstance() throws IllegalAccessException, InstantiationException {
	checkNonSunProxyMemberAccess(Member.PUBLIC);
	return (T)J9VMInternals.newInstanceImpl(this);
}

/**
 * Used as a prototype for the jit.
 *
 * @param 		callerClass
 * @return		the object
 * @throws 		InstantiationException
 */
private Object newInstancePrototype(Class callerClass) throws InstantiationException {
	throw new InstantiationException(this);
}

/**
 * Answers a string describing a path to the receiver's appropriate
 * package specific subdirectory, with the argument appended if the
 * argument did not begin with a slash. If it did, answer just the
 * argument with the leading slash removed.
 *
 * @return		String
 *					the path to the resource.
 * @param		resName	String
 *					the name of the resource.
 *
 * @see			#getResource
 * @see			#getResourceAsStream
 */
private String toResourceName(String resName) {
	// Turn package name into a directory path
	if (resName.length() > 0 && resName.charAt(0) == '/')
		return resName.substring(1);

	String qualifiedClassName = getName();
	int classIndex = qualifiedClassName.lastIndexOf('.');
	if (classIndex == -1) return resName; // from a default package
	return qualifiedClassName.substring(0, classIndex + 1).replace('.', '/') + resName;
}

/**
 * Answers a string containing a concise, human-readable
 * description of the receiver.
 *
 * @return		a printable representation for the receiver.
 */
public String toString() {
	// Note change from 1.1.7 to 1.2: For primitive types,
	// return just the type name.
	if (isPrimitive()) return getName();
	return (isInterface() ? "interface " : "class ") + getName(); //$NON-NLS-1$ //$NON-NLS-2$
}

/**
 * Returns the Package of which this class is a member.
 * A class has a Package iff it was loaded from a SecureClassLoader
 *
 * @return		Package the Package of which this class is a member or null
 *
 */
public Package getPackage() {
	return getClassLoaderImpl().getPackage(getPackageName());
}

static Class getPrimitiveClass(String name) {
	if (name.equals("float")) //$NON-NLS-1$
		return new float[0].getClass().getComponentType();
	if (name.equals("double")) //$NON-NLS-1$
		return new double[0].getClass().getComponentType();
	if (name.equals("int")) //$NON-NLS-1$
		return new int[0].getClass().getComponentType();
	if (name.equals("long")) //$NON-NLS-1$
		return new long[0].getClass().getComponentType();
	if (name.equals("char")) //$NON-NLS-1$
		return new char[0].getClass().getComponentType();
	if (name.equals("byte")) //$NON-NLS-1$
		return new byte[0].getClass().getComponentType();
	if (name.equals("boolean")) //$NON-NLS-1$
		return new boolean[0].getClass().getComponentType();
	if (name.equals("short")) //$NON-NLS-1$
		return new short[0].getClass().getComponentType();
	if (name.equals("void")) { //$NON-NLS-1$
		try {
			java.lang.reflect.Method method = Runnable.class.getMethod("run", new Class[0]); //$NON-NLS-1$
			return method.getReturnType();
		} catch (Exception e) {
			com.ibm.oti.vm.VM.dumpString("Cannot initialize Void.TYPE\n");
		}
	}
	throw new Error("Unknown primitive type: " + name);
}

/**
 * Returns the assertion status for this class.
 * Assertion is enabled/disabled based on
 * classloader default, package or class default at runtime
 *
 * @since 1.4
 *
 * @return		the assertion status for this class
 */
public boolean desiredAssertionStatus() {
	ClassLoader cldr = getClassLoaderImpl();
	if (cldr != null) {
		return cldr.getClassAssertionStatus(getName());
	}
	return false;
}

/**
 * Answer the class at depth.
 *
 * Notes:
 * 	 1) This method operates on the defining classes of methods on stack.
 *		NOT the classes of receivers.
 *
 *	 2) The item at index zero describes the caller of this method.
 *
 * @param 		depth
 * @return		the class at the given depth
 */
@sun.reflect.CallerSensitive
static final native Class getStackClass(int depth);

/**
 * Walk the stack and answer an array containing the maxDepth
 * most recent classes on the stack of the calling thread.
 *
 * Starting with the caller of the caller of getStackClasses(), return an
 * array of not more than maxDepth Classes representing the classes of
 * running methods on the stack (including native methods).  Frames
 * representing the VM implementation of java.lang.reflect are not included
 * in the list.  If stopAtPrivileged is true, the walk will terminate at any
 * frame running one of the following methods:
 *
 * <code><ul>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
 * </ul></code>
 *
 * If one of the doPrivileged methods is found, the walk terminate and that frame is NOT included in the returned array.
 *
 * Notes: <ul>
 * 	 <li> This method operates on the defining classes of methods on stack.
 *		NOT the classes of receivers. </li>
 *
 *	 <li> The item at index zero in the result array describes the caller of
 *		the caller of this method. </li>
 *</ul>
 *
 * @param 		maxDepth			maximum depth to walk the stack, -1 for the entire stack
 * @param 		stopAtPrivileged	stop at priviledged classes
 * @return		the array of the most recent classes on the stack
 */
@sun.reflect.CallerSensitive
static final native Class[] getStackClasses(int maxDepth, boolean stopAtPrivileged);

/**
 * Called from JVM_ClassDepth.
 * Answers the index in the stack of the first method which
 * is contained in a class called <code>name</code>. If no
 * methods from this class are in the stack, return -1.
 *
 * @param		name String
 *					the name of the class to look for.
 * @return		int
 *					the depth in the stack of a the first
 *					method found.
 */
static int classDepth (String name) {
	Class[] classes = getStackClasses(-1, false);
	for (int i=1; i<classes.length; i++)
		if (classes[i].getName().equals(name))
			return i - 1;
	return -1;
}

/**
 * Called from JVM_ClassLoaderDepth.
 * Answers the index in the stack of thee first class
 * whose class loader is not a system class loader.
 *
 * @return		the frame index of the first method whose class was loaded by a non-system class loader.
 */
static int classLoaderDepth() {
	// Now, check if there are any non-system class loaders in
	// the stack up to the first privileged method (or the end
	// of the stack.
	Class[] classes = getStackClasses(-1, true);
	for (int i=1; i<classes.length; i++) {
		ClassLoader cl = classes[i].getClassLoaderImpl();
		if (!cl.isASystemClassLoader()) return i - 1;
	}
	return -1;
}

/**
 * Called from JVM_CurrentClassLoader.
 * Answers the class loader of the first class in the stack
 * whose class loader is not a system class loader.
 *
 * @return		the most recent non-system class loader.
 */
@sun.reflect.CallerSensitive
static ClassLoader currentClassLoader() {
	// Now, check if there are any non-system class loaders in
	// the stack up to the first privileged method (or the end
	// of the stack.
	Class[] classes = getStackClasses(-1, true);
	for (int i=1; i<classes.length; i++) {
		ClassLoader cl = classes[i].getClassLoaderImpl();
		if (!cl.isASystemClassLoader()) return cl;
	}
	return null;
}

/**
 * Called from JVM_CurrentLoadedClass.
 * Answers the first class in the stack which was loaded
 * by a class loader which is not a system class loader.
 *
 * @return		the most recent class loaded by a non-system class loader.
 */
@sun.reflect.CallerSensitive
static Class currentLoadedClass() {
	// Now, check if there are any non-system class loaders in
	// the stack up to the first privileged method (or the end
	// of the stack.
	Class[] classes = getStackClasses(-1, true);
	for (int i=1; i<classes.length; i++) {
		ClassLoader cl = classes[i].getClassLoaderImpl();
		if (!cl.isASystemClassLoader()) return classes[i];
	}
	return null;
}

/**
 * Return the specified Annotation for this Class. Inherited Annotations
 * are searched.
 *
 * @param annotation the Annotation type
 * @return the specified Annotation or null
 *
 * @since 1.5
 */
public <A extends Annotation> A getAnnotation(Class<A> annotation) {
	if (annotation == null) throw new NullPointerException();
	Annotation[] ans = getAnnotations();
	for (int i = 0; i < ans.length; i++) {
		if (ans[i].annotationType() == annotation) {
			return (A)ans[i];
		}
	}
	return null;
}

/**
 * Return the Annotations for this Class, including the Annotations
 * inherited from superclasses.
 *
 * @return an array of Annotation
 *
 * @since 1.5
 */
public Annotation[] getAnnotations() {
	HashMap<String,Annotation> annotations = new HashMap<String,Annotation>();
	//Store annotations from the current class
	Annotation[] anns = getDeclaredAnnotations();
	for (int i = 0; i < anns.length; i++) {
		annotations.put(anns[i].annotationType().getName(), anns[i]);
	}
	Class c = getSuperclass();
	while (c != null) {
		Annotation[] ann = c.getDeclaredAnnotations();
		if (ann != null) {
			for (int i = 0; i < ann.length; i++) {
				// if we have an annotation of this type stored skip it
				if (!annotations.containsKey(ann[i].annotationType().getName())) {
					// if the annotation is Inherited store the annotation
					if (ann[i].annotationType().getAnnotation(Inherited.class) != null) {
						annotations.put(ann[i].annotationType().getName(), ann[i]);
					}
				}
			}
		}
		c = c.getSuperclass();
	}

	Annotation[] annArray = new Annotation[annotations.size()];
	annotations.values().toArray(annArray);

	return annArray;
}

/**
 * Return the Annotations only for this Class, not including Annotations
 * inherited from superclasses.
 *
 * @return an array of Annotation
 *
 * @since 1.5
 */
public Annotation[] getDeclaredAnnotations() {
	java.util.Hashtable collection = getClassLoaderImpl().getAnnotationCache();
	Annotation[] annotations = (Annotation[])collection.get(this);
	if (annotations == null) {
		byte[] annotationsData = getDeclaredAnnotationsData();
		if (annotationsData == null) {
			annotations = new Annotation[0];
		} else {
			Access access = new Access();
			annotations =
				sun.reflect.annotation.AnnotationParser.toArray(
						sun.reflect.annotation.AnnotationParser.parseAnnotations(
								annotationsData,
								access.getConstantPool(this),
								this));
		}
		collection.put(this, annotations);
	}
	return (Annotation[])annotations.clone();
}

private native byte[] getDeclaredAnnotationsData();

/**
 * Answer if this class is an Annotation.
 *
 * @return true if this class is an Annotation
 *
 * @since 1.5
 */
public boolean isAnnotation() {
	return !isArray() && (getModifiersImpl() & ANNOTATION) != 0;
}

/**
 * Answer if the specified Annotation exists for this Class. Inherited
 * Annotations are searched.
 *
 * @param annotation the Annotation type
 * @return true if the specified Annotation exists
 *
 * @since 1.5
 */
public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
	if (annotation == null) throw new NullPointerException();
	return getAnnotation(annotation) != null;
}

/**
 * Cast this Class to a subclass of the specified Class.
 *
 * @param cls the Class to cast to
 * @return this Class, cast to a subclass of the specified Class
 *
 * @throws ClassCastException if this Class is not the same or a subclass
 *		of the specified Class
 *
 * @since 1.5
 */
public <U> Class<? extends U> asSubclass(Class<U> cls) {
	if (!cls.isAssignableFrom(this))
		throw new ClassCastException(this.toString());
	return (Class<? extends U>)this;
}

/**
 * Cast the specified object to this Class.
 *
 * @param object the object to cast
 *
 * @return the specified object, cast to this Class
 *
 * @throws ClassCastException if the specified object cannot be cast
 *		to this Class
 *
 * @since 1.5
 */
public T cast(Object object) {
	if (object != null && !this.isAssignableFrom(object.getClass()))
		// K0336 = Cannot cast {0} to {1}
		throw new ClassCastException(com.ibm.oti.util.Msg.getString("K0336", object.getClass(), this)); //$NON-NLS-1$
	return (T)object;
}

/**
 * Answer if this Class is an enum.
 *
 * @return true if this Class is an enum
 *
 * @since 1.5
 */
public boolean isEnum() {
	return !isArray() && (getModifiersImpl() & ENUM) != 0 &&
		getSuperclass() == Enum.class;
}

/**
 *
 * @return Map keyed by enum name, of uncloned and cached enum constants in this class
 */
java.util.Map<String, T> enumConstantDirectory() {
	if (_cachedEnumConstantDirectory == null) {
		T[] enums = getEnumConstantsShared();
		if (enums == null) {
			/*
			 * Class#valueOf() is the caller of this method,
			 * according to the spec it throws IllegalArgumentException if the class is not an Enum.
			 */
			throw new IllegalArgumentException(getName() + " is not an Enum");
		}
		Map<String, T> map = new HashMap<String, T>(enums.length * 4 / 3);
		for (int i = 0; i < enums.length; i++) {
			map.put(((Enum) enums[i]).name(), enums[i]);
		}
		synchronized(getClassLoaderImpl().lazyInitLock){
			if (_cachedEnumConstantDirectory == null) {
				_cachedEnumConstantDirectory = map;
			}
		}
	}
	return _cachedEnumConstantDirectory;
}

/**
 * Answer the shared uncloned array of enum constants for this Class. Returns null if
 * this class is not an enum.
 *
 * @return the array of enum constants, or null
 *
 * @since 1.5
 */
T[] getEnumConstantsShared() {
	if (null == _cachedEnumConstants) {
		if (!isEnum()) return null;
		try {
			Method values = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
				public Method run() throws Exception {
					Method method = getMethod("values"); //$NON-NLS-1$
					// the enum class may not be visible
					method.setAccessible(true);
					return method;
				}
			});
			_cachedEnumConstants = (T[])values.invoke(this);

		} catch (Exception e) {
			return null;
		}
	}

	return _cachedEnumConstants;
}

/**
 * Answer the array of enum constants for this Class. Returns null if
 * this class is not an enum.
 *
 * @return the array of enum constants, or null
 *
 * @since 1.5
 */
public T[] getEnumConstants() {
	if (null != getEnumConstantsShared()) {
		return getEnumConstantsShared().clone();
	} else {
		return null;
	}
}

/**
 * Answer if this Class is synthetic. A synthetic Class is created by
 * the compiler.
 *
 * @return true if this Class is synthetic.
 *
 * @since 1.5
 */
public boolean isSynthetic() {
	return !isArray() && (getModifiersImpl() & SYNTHETIC) != 0;
}

private native String getGenericSignature();

private CoreReflectionFactory getFactory() {
	return CoreReflectionFactory.make(this, ClassScope.make(this));
}

private ClassRepository getClassRepository(String signature) {
	java.util.Hashtable collection = getClassLoaderImpl().getGenericRepository();
	ClassRepository classRepository = (ClassRepository)collection.get(this);
	if (classRepository == null) {
		classRepository = ClassRepository.make(signature, getFactory());
		collection.put(this, classRepository);
	}
	return classRepository;
}

/**
 * Answers an array of TypeVariable for the generic parameters declared
 * on this Class.
 *
 * @return		the TypeVariable[] for the generic parameters
 *
 * @since 1.5
 */
public TypeVariable<Class<T>>[] getTypeParameters() {
	String signature = getGenericSignature();
	if (signature == null) return new TypeVariable[0];
	ClassRepository repository = getClassRepository(signature);
	return repository.getTypeParameters();
}

/**
 * Answers an array of Type for the Class objects which match the
 * interfaces specified in the receiver classes <code>implements</code>
 * declaration.
 *
 * @return		Type[]
 *					the interfaces the receiver claims to implement.
 *
 * @since 1.5
 */
public Type[] getGenericInterfaces() {
	String signature = getGenericSignature();
	if (signature == null) return getInterfaces();
	ClassRepository repository = getClassRepository(signature);
	return repository.getSuperInterfaces();
}

/**
 * Answers the Type for the Class which represents the receiver's
 * superclass. For classes which represent base types,
 * interfaces, and for java.lang.Object the method
 * answers null.
 *
 * @return		the Type for the receiver's superclass.
 *
 * @since 1.5
 */
public Type getGenericSuperclass() {
	String signature = getGenericSignature();
	if (signature == null) return getSuperclass();
	if (isInterface()) return null;
	ClassRepository repository = getClassRepository(signature);
	return repository.getSuperclass();
}

private native Object getEnclosingObject();

/**
 * If this Class is defined inside a constructor, return the Constructor.
 *
 * @return the enclosing Constructor or null
 * @throws SecurityException if declared member access or package access is not allowed
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 */
@sun.reflect.CallerSensitive
public Constructor<?> getEnclosingConstructor() {
	Constructor<?> constructor = null;
	Object enclosing = getEnclosingObject();
	if (enclosing instanceof Constructor) {
		constructor = (Constructor)enclosing;
		Class<?> declaring = constructor.getDeclaringClass();
		declaring.checkMemberAccess(Member.DECLARED);
	}
	return constructor;
}

/**
 * If this Class is defined inside a method, return the Method.
 *
 * @return the enclosing Method or null
 * @throws SecurityException if declared member access or package access is not allowed
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 */
@sun.reflect.CallerSensitive
public Method getEnclosingMethod() {
	Method method = null;
	Object enclosing = getEnclosingObject();
	if (enclosing instanceof Method) {
		method = (Method)enclosing;
		Class<?> declaring = method.getDeclaringClass();
		declaring.checkMemberAccess(Member.DECLARED);
	}
	return method;
}

private native Class getEnclosingObjectClass();

/**
 * Return the enclosing Class of this Class. Unlike getDeclaringClass(),
 * this method works on any nested Class, not just classes nested directly
 * in other classes.
 *
 * @return the enclosing Class or null
 * @throws SecurityException if package access is not allowed
 *
 * @since 1.5
 *
 * @see #getDeclaringClass()
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 * @see #isMemberClass()
 */
public Class<?> getEnclosingClass() {
	Class<?> enclosingClass = getDeclaringClass();
	if (enclosingClass == null) {
		enclosingClass = getEnclosingObjectClass();
	}
	if (enclosingClass != null) {
		enclosingClass.checkMemberAccess(MEMBER_INVALID_TYPE);
	}

	return enclosingClass;
}

private native String getSimpleNameImpl();

/**
 * Return the simple name of this Class. The simple name does not include
 * the package or the name of the enclosing class. The simple name of an
 * anonymous class is "".
 *
 * @return the simple name
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 */
public String getSimpleName() {
	int arrayCount = 0;
	Class baseType = this;
	if (isArray()) {
		arrayCount = 1;
		while ((baseType = baseType.getComponentType()).isArray()) {
			arrayCount++;
		}
	}
	String simpleName = baseType.getSimpleNameImpl();
	if (simpleName == null) {
		// either a base class, or anonymous class
		if (baseType.getEnclosingObjectClass() != null) {
			simpleName = ""; //$NON-NLS-1$
		} else {
			// remove the package name
			simpleName = baseType.getName();
			int index = simpleName.lastIndexOf('.');
			if (index != -1) {
				simpleName = simpleName.substring(index+1);
			}
		}
	}
	if (arrayCount > 0) {
		StringBuffer result = new StringBuffer(simpleName);
		for (int i=0; i<arrayCount; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}
	return simpleName;
}

/**
 * Return the canonical name of this Class. The canonical name is null
 * for a local or anonymous class. The canonical name includes the package
 * and the name of the enclosing class.
 *
 * @return the canonical name or null
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 * @see #isLocalClass()
 */
public String getCanonicalName() {
	int arrayCount = 0;
	Class baseType = this;
	if (isArray()) {
		arrayCount = 1;
		while ((baseType = baseType.getComponentType()).isArray()) {
			arrayCount++;
		}
	}
	if (baseType.getEnclosingObjectClass() != null) {
		// local or anonymous class
		return null;
	}
	String canonicalName;
	Class declaringClass = baseType.getDeclaringClass();
	if (declaringClass == null) {
		canonicalName = baseType.getName();
	} else {
		String declaringClassCanonicalName = declaringClass.getCanonicalName();
		if (declaringClassCanonicalName == null) return null;
		// remove the enclosingClass from the name, including the $
		String simpleName = baseType.getName().substring(declaringClass.getName().length() + 1);
		canonicalName = declaringClassCanonicalName + '.' + simpleName;
	}

	if (arrayCount > 0) {
		StringBuffer result = new StringBuffer(canonicalName);
		for (int i=0; i<arrayCount; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}
	return canonicalName;
}

/**
 * Answer if this Class is anonymous. An unnamed Class defined
 * inside a method.
 *
 * @return true if this Class is anonymous.
 *
 * @since 1.5
 *
 * @see #isLocalClass()
 */
public boolean isAnonymousClass() {
	return getSimpleNameImpl() == null && getEnclosingObjectClass() != null;
}

/**
 * Answer if this Class is local. A named Class defined inside
 * a method.
 *
 * @return true if this Class is local.
 *
 * @since 1.5
 *
 * @see #isAnonymousClass()
 */
public boolean isLocalClass() {
	return getEnclosingObjectClass() != null && getSimpleNameImpl() != null;
}

/**
 * Answer if this Class is a member Class. A Class defined inside another
 * Class.
 *
 * @return true if this Class is local.
 *
 * @since 1.5
 *
 * @see #isLocalClass()
 */
public boolean isMemberClass() {
	return getEnclosingObjectClass() == null && getDeclaringClass() != null;
}

/**
 * Return the depth in the class hierarchy of the receiver.
 * Base type classes and Object return 0.
 *
 * @return receiver's class depth
 *
 * @see #getDeclaredMethod
 * @see #getMethod
 */
private native int getClassDepth();

/**
 * Compute the signature for get*Method()
 *
 * @param		name			the name of the method
 * @param		parameterTypes	the types of the arguments
 * @return 		the signature string
 * @throws		NoSuchMethodException if one of the parameter types cannot be found in the local class loader
 *
 * @see #getDeclaredMethod
 * @see #getMethod
 */
private String getParameterTypesSignature(String name, Class parameterTypes[]) throws NoSuchMethodException {
	int total = 2;
	String[] sigs = new String[parameterTypes.length];
	for(int i = 0; i < parameterTypes.length; i++) {
		Class parameterType = parameterTypes[i];
		if (parameterType != null) {
			sigs[i] = parameterType.getSignature();
			total += sigs[i].length();
		} else throwNoSuchMethodException(name, parameterTypes);
	}
	StringBuffer signature = new StringBuffer(total);
	signature.append('(');
	for(int i = 0; i < parameterTypes.length; i++)
		signature.append(sigs[i]);
	signature.append(')');
	return signature.toString();
}

private static Method copyMethod, copyField, copyConstructor;
private static Field methodParameterTypesField;
private static Field constructorParameterTypesField;
private static final Object[] NoArgs = new Object[0];
private static final CacheKey PublicKey = new CacheKey(".m", EmptyParameters, null); //$NON-NLS-1$
private static final CacheKey DeclaredKey = new CacheKey(".d", EmptyParameters, null); //$NON-NLS-1$

static void initCacheIds() {
	AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
			Class mClass = Method.class;
			try {
				methodParameterTypesField = mClass.getDeclaredField("parameterTypes"); //$NON-NLS-1$
				methodParameterTypesField.setAccessible(true);
			} catch (NoSuchFieldException e) {}
			try {
				copyMethod = mClass.getDeclaredMethod("copy", new Class[0]); //$NON-NLS-1$
				copyMethod.setAccessible(true);
			} catch (NoSuchMethodException e) {}
			Class fClass = Field.class;
			try {
				copyField = fClass.getDeclaredMethod("copy", new Class[0]); //$NON-NLS-1$
				copyField.setAccessible(true);
			} catch (NoSuchMethodException e) {}
			Class cClass = Constructor.class;
			try {
				constructorParameterTypesField = cClass.getDeclaredField("parameterTypes"); //$NON-NLS-1$
				constructorParameterTypesField.setAccessible(true);
			} catch (NoSuchFieldException e) {}
			try {
				copyConstructor = cClass.getDeclaredMethod("copy", new Class[0]); //$NON-NLS-1$
				copyConstructor.setAccessible(true);
			} catch (NoSuchMethodException e) {}
			return null;
		}
	});
}

static ReferenceQueue queue = new ReferenceQueue();
static final class ReflectRef extends SoftReference implements Runnable {
	CacheKey key;
	ClassLoader.CacheTable clCache;
	public ReflectRef(Object referent, CacheKey keyValue, ClassLoader.CacheTable clCacheValue) {
		super(referent, queue);
		key = keyValue;
		clCache = clCacheValue;
	}
	public void run() {
		synchronized(clCache) {
			if (clCache.getRef(key) == this) {
				clCache.remove(key);
			}
		}
		// cannot call this inside the synchronized block, or deadlock may occur
		clCache.removeEmpty();
	}
}

static final class CacheKey {
	static final int PRIME = 31;
	String name;
	Class[] parameterTypes;
	Class returnType;
	int hashCode;
	public CacheKey(String name, Class[] params, Class returnType) {
		this.name = name;
		this.parameterTypes = params;
		this.returnType = returnType;
	}
	public int hashCode() {
		if (hashCode == 0) {
			int result = name.hashCode();
			if (parameterTypes.length > 0) {
				int arrayHash = 1;
				for (int i=0; i<parameterTypes.length; i++) {
					arrayHash = (PRIME * arrayHash) + (parameterTypes[i] == null ? 0 : parameterTypes[i].hashCode());
				}
				result = PRIME * result + arrayHash;
			}
			if (returnType != null) {
				result = PRIME * result + returnType.hashCode();
			}
			hashCode = result;
		}
		return hashCode;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		final CacheKey other = (CacheKey) obj;
		if (returnType != other.returnType)
			return false;
		if (parameterTypes.length != other.parameterTypes.length || !name.equals(other.name))
			return false;
		for (int i=0; i< parameterTypes.length; i++) {
			if (parameterTypes[i] != other.parameterTypes[i]) return false;
		}
		return true;
	}

}

private Method lookupCachedMethod(String methodName, Class[] parameters) {
	if (!ClassLoader.isReflectCacheEnabled()) return null;

	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("lookup Method: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(methodName);
		System.err.println(output);
	}
	Hashtable clCache = (Hashtable)getClassLoaderImpl().getMethodCache().get(this);
	if (clCache != null) {
		// use a null returnType to find the Method with the largest depth
		Method method = (Method)clCache.get(new CacheKey(methodName, parameters, null));
		if (method != null) {
			try {
				Class[] orgParams = (Class[])methodParameterTypesField.get(method);
				for (int i=0; i<orgParams.length; i++) {
					// ensure the parameter classes are identical
					if (parameters[i] != orgParams[i]) return null;
				}
				return (Method)copyMethod.invoke(method, NoArgs);
			} catch (IllegalAccessException e) {
				InternalError err = new InternalError(e.toString());
				err.initCause(e);
				throw err;
			} catch (InvocationTargetException e) {
				InternalError err = new InternalError(e.toString());
				err.initCause(e);
				throw err;
			}
		}
	}
	return null;
}

private Method cacheMethod(Method method) {
	if (!ClassLoader.isReflectCacheEnabled()) return method;
	if (ClassLoader.isReflectCacheAppOnly() && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return method;
	}
	if (copyMethod == null) return method;
	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("cache Method: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(method.getName());
		System.err.println(output);
	}
	try {
		CacheKey key = new CacheKey(method.getName(), (Class[])methodParameterTypesField.get(method), method.getReturnType());
		Class declaringClass = method.getDeclaringClass();
		if (declaringClass != this) {
			ClassLoader.CacheTable clCache = getClassLoaderImpl().getMethodCache(declaringClass);
			synchronized(clCache) {
				Method foundMethod = (Method)clCache.get(key);
				if (foundMethod == null) {
					clCache.put(key, new ReflectRef(method, key, clCache));
				} else {
					method = foundMethod;
				}
				clCache.free();
			}
		}
		ClassLoader.CacheTable clCache = getClassLoaderImpl().getMethodCache(this);
		synchronized(clCache) {
			if (declaringClass == this) {
				Method foundMethod = (Method)clCache.get(key);
				if (foundMethod == null) {
					clCache.put(key, new ReflectRef(method, key, clCache));
				} else {
					method = foundMethod;
				}
			}
			// cache the Method with the largest depth with a null returnType
			CacheKey lookupKey = new CacheKey(method.getName(), (Class[])methodParameterTypesField.get(method), null);
			clCache.put(lookupKey, new ReflectRef(method, lookupKey, clCache));
			clCache.free();
		}
		return (Method)copyMethod.invoke(method, NoArgs);
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	} catch (InvocationTargetException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
}

private Field lookupCachedField(String fieldName) {
	if (!ClassLoader.isReflectCacheEnabled()) return null;

	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("lookup Field: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(fieldName);
		System.err.println(output);
	}
	ClassLoader loader = getClassLoaderImpl();
	// called during Unsafe initialization, before the bootstrap ClassLoader is initialized
	if (loader == null) return null;
	Hashtable clCache = (Hashtable)loader.getFieldCache().get(this);
	if (clCache != null) {
		Field field = (Field)clCache.get(new CacheKey(fieldName, EmptyParameters, null));
		if (field != null) {
			try {
				return (Field)copyField.invoke(field, NoArgs);
			} catch (IllegalAccessException e) {
				InternalError err = new InternalError(e.toString());
				err.initCause(e);
				throw err;
			} catch (InvocationTargetException e) {
				InternalError err = new InternalError(e.toString());
				err.initCause(e);
				throw err;
			}
		}
	}
	return null;
}

private Field cacheField(Field field) {
	if (!ClassLoader.isReflectCacheEnabled()) return field;
	if (ClassLoader.isReflectCacheAppOnly() && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return field;
	}
	if (copyField == null) return field;
	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("cache Field: "); //$NON-NLS-1$
		output.append(getName());
		output.append('.');
		output.append(field.getName());
		System.err.println(output);
	}
	CacheKey key = new CacheKey(field.getName(), EmptyParameters, field.getType());
	Class declaringClass = field.getDeclaringClass();
	if (declaringClass != this) {
		ClassLoader.CacheTable clCache = getClassLoaderImpl().getFieldCache(declaringClass);
		synchronized(clCache) {
			Field foundField = (Field)clCache.get(key);
			if (foundField == null) {
				clCache.put(key, new ReflectRef(field, key, clCache));
			} else {
				field = foundField;
			}
			clCache.free();
		}
	}
	ClassLoader.CacheTable clCache = getClassLoaderImpl().getFieldCache(this);
	synchronized(clCache) {
		if (declaringClass == this) {
			Field foundField = (Field)clCache.get(key);
			if (foundField == null) {
				clCache.put(key, new ReflectRef(field, key, clCache));
			} else {
				field = foundField;
			}
		}
		// cache the Field returned from getField() with a null returnType
		CacheKey lookupKey = new CacheKey(field.getName(), EmptyParameters, null);
		clCache.put(lookupKey, new ReflectRef(field, lookupKey, clCache));
		clCache.free();
	}
	try {
		return (Field)copyField.invoke(field, NoArgs);
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	} catch (InvocationTargetException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
}

private Constructor lookupCachedConstructor(Class[] parameters) {
	if (!ClassLoader.isReflectCacheEnabled()) return null;

	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("lookup Constructor: "); //$NON-NLS-1$
		output.append(getName()).append('(');
		for (int i=0; i<parameters.length; i++) {
			if (i != 0) output.append(", "); //$NON-NLS-1$
			output.append(parameters[i].getName());
		}
		output.append(')');
		System.err.println(output);
	}
	Hashtable clCache = (Hashtable)getClassLoaderImpl().getConstructorCache().get(this);
	if (clCache != null) {
		Constructor constructor = (Constructor)clCache.get(new CacheKey(getName(), parameters, null));
		if (constructor != null) {
			try {
				Class[] orgParams = (Class[])constructorParameterTypesField.get(constructor);
				for (int i=0; i<orgParams.length; i++) {
					// ensure the parameter classes are identical
					if (parameters[i] != orgParams[i]) return null;
				}
				return (Constructor)copyConstructor.invoke(constructor, NoArgs);
			} catch (IllegalAccessException e) {
				InternalError err = new InternalError(e.toString());
				err.initCause(e);
				throw err;
			} catch (InvocationTargetException e) {
				InternalError err = new InternalError(e.toString());
				err.initCause(e);
				throw err;
			}
		}
	}
	return null;
}

private Constructor cacheConstructor(Constructor constructor) {
	if (!ClassLoader.isReflectCacheEnabled()) return constructor;
	if (ClassLoader.isReflectCacheAppOnly() && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return constructor;
	}
	if (copyConstructor == null) return constructor;
	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("cache Constructor: "); //$NON-NLS-1$
		output.append(getName()).append('(');
		Class[] params = constructor.getParameterTypes();
		for (int i=0; i<params.length; i++) {
			if (i != 0) output.append(", "); //$NON-NLS-1$
			output.append(params[i].getName());
		}
		output.append(')');
		System.err.println(output);
	}
	ClassLoader.CacheTable clCache = getClassLoaderImpl().getConstructorCache(this);
	try {
		CacheKey key = new CacheKey(getName(), (Class[])constructorParameterTypesField.get(constructor), null);
		ReflectRef ref = new ReflectRef(constructor, key, clCache);
		synchronized(clCache) {
			clCache.put(key, ref);
			clCache.free();
		}
		return (Constructor)copyConstructor.invoke(constructor, NoArgs);
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	} catch (InvocationTargetException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
}

private Method[] copyMethods(Method[] methods) {
	Method[] result = new Method[methods.length];
	try {
		for (int i=0; i<methods.length; i++) {
			result[i] = (Method)copyMethod.invoke(methods[i], NoArgs);
		}
		return result;
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	} catch (InvocationTargetException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
}

private Method[] lookupCachedMethods(CacheKey cacheKey) {
	if (!ClassLoader.isReflectCacheEnabled()) return null;

	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("lookup Methods in: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	Hashtable clCache = (Hashtable)getClassLoaderImpl().getMethodCache().get(this);
	if (clCache != null) {
		Method[] methods = (Method[])clCache.get(cacheKey);
		if (methods != null) {
			return copyMethods(methods);
		}
	}
	return null;
}

private Method[] cacheMethods(Method[] methods, CacheKey cacheKey) {
	if (!ClassLoader.isReflectCacheEnabled()) return methods;
	if (ClassLoader.isReflectCacheAppOnly() && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return methods;
	}
	if (copyMethod == null) return methods;
	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("cache Methods: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	try {
		Class lastDeclaringClass = null;
		ClassLoader.CacheTable declaringCache = null;
		ClassLoader loader = getClassLoaderImpl();
		ClassLoader.CacheTable clCache = loader.getMethodCache(this);
		for (int i=0; i<methods.length; i++) {
			CacheKey key = new CacheKey(methods[i].getName(), (Class[])methodParameterTypesField.get(methods[i]), methods[i].getReturnType());
			Class declaringClass = methods[i].getDeclaringClass();
			if (declaringClass != this) {
				if (declaringClass != lastDeclaringClass) {
					if (declaringCache != null) declaringCache.free();
					declaringCache = loader.getMethodCache(declaringClass);
					lastDeclaringClass = declaringClass;
				}
				synchronized(declaringCache) {
					Method method = (Method)declaringCache.get(key);
					if (method == null) {
						declaringCache.put(key, new ReflectRef(methods[i], key, declaringCache));
					} else {
						methods[i] = method;
					}
				}
			} else {
				synchronized(clCache) {
					Method method = (Method)clCache.get(key);
					if (method == null) {
						clCache.put(key, new ReflectRef(methods[i], key, clCache));
					} else {
						methods[i] = method;
					}
				}
			}
		}
		if (declaringCache != null) declaringCache.free();
		ReflectRef ref = new ReflectRef(methods, cacheKey, clCache);
		synchronized(clCache) {
			clCache.put(cacheKey, ref);
			clCache.free();
		}
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
	return copyMethods(methods);
}

private Field[] copyFields(Field[] fields) {
	Field[] result = new Field[fields.length];
	try {
		for (int i=0; i<fields.length; i++) {
			result[i] = (Field)copyField.invoke(fields[i], NoArgs);
		}
		return result;
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	} catch (InvocationTargetException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
}

private Field[] lookupCachedFields(CacheKey cacheKey) {
	if (!ClassLoader.isReflectCacheEnabled()) return null;

	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("lookup Fields in: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	Hashtable clCache = (Hashtable)getClassLoaderImpl().getFieldCache().get(this);
	if (clCache != null) {
		Field[] fields = (Field[])clCache.get(cacheKey);
		if (fields != null) {
			return copyFields(fields);
		}
	}
	return null;
}

private Field[] cacheFields(Field[] fields, CacheKey cacheKey) {
	if (!ClassLoader.isReflectCacheEnabled()) return fields;
	if (ClassLoader.isReflectCacheAppOnly() && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return fields;
	}
	if (copyField == null) return fields;
	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("cache Fields: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	Class lastDeclaringClass = null;
	ClassLoader loader = getClassLoaderImpl();
	ClassLoader.CacheTable clCache = loader.getFieldCache(this);
	ClassLoader.CacheTable declaringCache = null;
	for (int i=0; i<fields.length; i++) {
		CacheKey key = new CacheKey(fields[i].getName(), EmptyParameters, fields[i].getType());
		Class declaringClass = fields[i].getDeclaringClass();
		if (declaringClass != this) {
			if (declaringClass != lastDeclaringClass) {
				if (declaringCache != null) declaringCache.free();
				declaringCache = loader.getFieldCache(declaringClass);
				lastDeclaringClass = declaringClass;
			}
			synchronized(declaringCache) {
				Field field = (Field)declaringCache.get(key);
				if (field == null) {
					declaringCache.put(key, new ReflectRef(fields[i], key, declaringCache));
				} else {
					fields[i] = field;
				}
			}
		} else {
			synchronized(clCache) {
				Field field = (Field)clCache.get(key);
				if (field == null) {
					clCache.put(key, new ReflectRef(fields[i], key, clCache));
				} else {
					fields[i] = field;
				}
			}
		}
	}
	if (declaringCache != null) declaringCache.free();
	ReflectRef ref = new ReflectRef(fields, cacheKey, clCache);
	synchronized(clCache) {
		clCache.put(cacheKey, ref);
		clCache.free();
	}
	return copyFields(fields);
}

private Constructor[] copyConstructors(Constructor[] constructors) {
	Constructor[] result = new Constructor[constructors.length];
	try {
		for (int i=0; i<constructors.length; i++) {
			result[i] = (Constructor)copyConstructor.invoke(constructors[i], NoArgs);
		}
		return result;
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	} catch (InvocationTargetException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
}

private Constructor[] lookupCachedConstructors(CacheKey cacheKey) {
	if (!ClassLoader.isReflectCacheEnabled()) return null;

	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("lookup Constructors in: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	Hashtable clCache = (Hashtable)getClassLoaderImpl().getConstructorCache().get(this);
	if (clCache != null) {
		Constructor[] constructors = (Constructor[])clCache.get(cacheKey);
		if (constructors != null) {
			return copyConstructors(constructors);
		}
	}
	return null;
}

private Constructor[] cacheConstructors(Constructor[] constructors, CacheKey cacheKey) {
	if (!ClassLoader.isReflectCacheEnabled()) return constructors;
	if (ClassLoader.isReflectCacheAppOnly() && ClassLoader.getStackClassLoader(2) == ClassLoader.bootstrapClassLoader) {
		return constructors;
	}
	if (copyConstructor == null) return constructors;
	if (ClassLoader.isReflectCacheDebug()) {
		StringBuffer output = new StringBuffer(200);
		output.append("cache Constructors: "); //$NON-NLS-1$
		output.append(getName());
		System.err.println(output);
	}
	ClassLoader.CacheTable clCache = getClassLoaderImpl().getConstructorCache(this);
	try {
		for (int i=0; i<constructors.length; i++) {
			CacheKey key = new CacheKey(getName(), (Class[])constructorParameterTypesField.get(constructors[i]), null);
			synchronized(clCache) {
				Constructor constructor = (Constructor)clCache.get(key);
				if (constructor == null) {
					clCache.put(key, new ReflectRef(constructors[i], key, clCache));
				} else {
					constructors[i] = constructor;
				}
			}
		}
		ReflectRef ref = new ReflectRef(constructors, cacheKey, clCache);
		synchronized(clCache) {
			clCache.put(cacheKey, ref);
			clCache.free();
		}
	} catch (IllegalAccessException e) {
		InternalError err = new InternalError(e.toString());
		err.initCause(e);
		throw err;
	}
	return copyConstructors(constructors);
}

private static Constructor checkParameterTypes(Constructor constructor, Class[] parameterTypes) {
	Class[] constructorParameterTypes;
	if (constructorParameterTypesField != null) {
		try {
			constructorParameterTypes = (Class[])constructorParameterTypesField.get(constructor);
		} catch (IllegalArgumentException e) {
			throw new InternalError(e.toString());
		} catch (IllegalAccessException e) {
			throw new InternalError(e.toString());
		}
	} else
		constructorParameterTypes = constructor.getParameterTypes();
	for (int i = 0; i < parameterTypes.length; i++) {
		if (parameterTypes[i] != constructorParameterTypes[i]) return null;
	}
	return constructor;
}

private static Method[] filterUnsafe(Method[] methods) {
	int length = methods.length;
	for (int i = 0; i < length; ++i) {
		if (methods[i].getName().equals("getUnsafe")) { //$NON-NLS-1$
			Method[] filtered = new Method[length - 1];
			System.arraycopy(methods, 0, filtered, 0, i);
			System.arraycopy(methods, i + 1, filtered, i, length - 1 - i);
			return filtered;
		}
	}
	return methods;
}

}
