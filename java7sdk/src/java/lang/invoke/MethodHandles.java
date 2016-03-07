/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2010, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.ConvertHandle.FilterHelpers;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.ibm.oti.util.Msg;
import com.ibm.oti.lang.ArgumentHelper;
import com.ibm.oti.vm.VM;
import com.ibm.oti.vm.VMLangAccess;

/**
 * Factory class for creating and adapting MethodHandles.
 *
 * @since 1.7
 */
public class MethodHandles {
	static final native Class getStackClass(int depth);

	MethodHandles() {
	}

	/**
	 * A factory for creating MethodHandles that require access-checking on creation.
	 * <p>
	 * Unlike Reflection, MethodHandle only requires access-checking when the MethodHandle
	 * is created, not when it is invoked.
	 * <p>
	 * This class provides the lookup authentication necessary when creating MethodHandles.  Any
	 * number of MethodHandles can be lookup using this token, and the token can be shared to provide
	 * others with the the "owner's" authentication level.
	 * <p>
	 * Sharing {@link Lookup} objects should be done with care, as they may allow access to private
	 * methods.
	 *
	 */
	static final public class Lookup {

		/**
		 * Bit flag 0x1 representing <i>public</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PUBLIC = Modifier.PUBLIC;

		/**
		 * Bit flag 0x2 representing <i>private</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PRIVATE = Modifier.PRIVATE;

		/**
		 * Bit flag 0x4 representing <i>protected</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PROTECTED = Modifier.PROTECTED;

		/**
		 * Bit flag 0x8 representing <i>package</i> access.  See {@link #lookupModes()}.
		 */
		public static final int PACKAGE = 0x8;

		private static final int INTERNAL_PRIVILEGED = 0x10;

		private static final int FULL_ACCESS_MASK = PUBLIC | PRIVATE | PROTECTED | PACKAGE;
		private static final int NO_ACCESS = 0;

		private static final String INVOKE_EXACT = "invokeExact"; //$NON-NLS-1$
		private static final String INVOKE = "invoke"; //$NON-NLS-1$
		static final int VARARGS = 0x80;

		/* single cached value of public Lookup object */
		static Lookup publicLookup = new Lookup(Object.class, Lookup.PUBLIC);

		/* single cached internal privileged lookup */
		static Lookup internalPrivilegedLookup = new Lookup(MethodHandle.class, Lookup.INTERNAL_PRIVILEGED);

		/* Access token used in lookups - Object for public lookup */
		final Class accessClass;
		final int accessMode;
		private boolean performSecurityCheck = true;

		Lookup(Class lookupClass, int lookupMode) {
			accessClass = lookupClass;
			accessMode = lookupMode;
		}

		Lookup(Class lookupClass) {
			this(lookupClass, FULL_ACCESS_MASK);
		}

		Lookup(Class lookupClass, boolean performSecurityCheck) {
			 this(lookupClass, FULL_ACCESS_MASK);
			 this.performSecurityCheck = performSecurityCheck;
		}

		/**
		 * A query to determine the lookup capabilities held by this instance.
		 *
		 * @return the lookup mode bit mask for this Lookup object
		 */
		public int lookupModes() {
			return accessMode;
		}

		/*
		 * Is the varargs bit set?
		 */
		static boolean isVarargs(int modifiers) {
			return (modifiers & VARARGS) != 0;
		}

		/* If the varargs bit is set, wrap the MethodHandle with an
		 * asVarargsCollector adapter.
		 * Last class type will be Object[] if not otherwise appropriate.
		 */
		private static MethodHandle convertToVarargsIfRequired(MethodHandle handle) {
			if (isVarargs(handle.rawModifiers)) {
				Class<?> lastClass = handle.type.parameterType(handle.type.parameterCount() - 1);
				return handle.asVarargsCollector(lastClass);
			}
			return handle;
		}

		/**
		 * Return an early-bound method handle to a non-static method.  The receiver must
		 * have a Class in its hierarchy that provides the virtual method named methodName.
		 * <p>
		 * The final MethodType of the MethodHandle will be identical to that of the method.
		 * The receiver will be inserted prior to the call and therefore does not need to be
		 * included in the MethodType.
		 *
		 * @param receiver - The Object to insert as a receiver.  Must implement the methodName
		 * @param methodName - The name of the method
		 * @param type - The MethodType describing the method
		 * @return a MethodHandle to the required method.
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchMethodException if the method doesn't exist
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle bind(Object receiver, String methodName, MethodType type) throws IllegalAccessException, NoSuchMethodException{
			nullCheck(receiver, methodName, type);
			MethodHandle handle = handleForMHInvokeMethods(receiver.getClass(), methodName, type);
			if (handle == null) {
				initCheck(methodName);
				handle = new DirectHandle(receiver.getClass(), methodName, type, MethodHandle.KIND_SPECIAL, receiver.getClass());
				handle = convertToVarargsIfRequired(new ReceiverBoundHandle(handle, receiver));
			}
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, receiver.getClass(), handle.rawModifiers, callingContext);

			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle, methodName, callingContext);

			return handle;
		}

		/* Get the Class to use as a calling context.
		 * If the Lookup object has private access (aka full trust), then
		 * use the accessClass.  Otherwise, find the calling class and use
		 * it.
		 *
		 * [X] getStackClass (frameless INL)
		 * [0] getCallingContextDepth3
		 * [1] find* api
		 * [2] calling context.
		 */
		private Class<?> getCallingContextDepth3() {
			Class<?> callerClass = accessClass;
			if (!Modifier.isPrivate(accessMode)) {
				callerClass = getStackClass(2);
			}
			return callerClass;
		}

		private void nullCheck(Object a, Object b) {
			if ((null == a) || (null == b)) {
				throw new NullPointerException();
			}
		}

		private void nullCheck(Object a, Object b, Object c) {
			if ((null == a) || (null == b) || (null == c)) {
				throw new NullPointerException();
			}
		}

		private void nullCheck(Object a, Object b, Object c, Object d) {
			if ((null == a) || (null == b) || (null == c) || (null == d)) {
				throw new NullPointerException();
			}
		}

		private void initCheck(String methodName) throws NoSuchMethodException {
			if ("<init>".equals(methodName)) { //$NON-NLS-1$
				throw new NoSuchMethodException("Invalid method name: \"<init>\"");
			} else if  ("<clinit>".equals(methodName)) { //$NON-NLS-1$
				throw new NoSuchMethodException("Invalid method name: \"<clinit>\"");
			}
		}

		/* We use this because VM.javalangVMaccess is not set when this class is loaded.
		 * Delay grabbing it until we need it, which by then the value will be set. */
		private static final class VMLangAccessGetter {
			public static final VMLangAccess vma = VM.getVMLangAccess();
		}

		static final VMLangAccess getVMLangAccess() {
			return VMLangAccessGetter.vma;
		}

		/* Verify two classes share the same package in a way to avoid Class.getPackage()
		 * and the security checks that go with it.
		 */
		private static boolean isSamePackage(Class<?> a, Class<?> b){
			// Two of the same class share a package
			if (a == b){
				return true;
			}

			VMLangAccess vma = getVMLangAccess();

			// If the string value is different, they're definitely not related
			if(!vma.getPackageName(a).equals(vma.getPackageName(b))) {
				return false;
			}

			ClassLoader cla = vma.getClassloader(a);
			ClassLoader clb = vma.getClassloader(b);

			// If both share the same classloader, then they are the same package
			if (cla == clb) {
				return true;
			}

			// If one is an ancestor of the other, they are also the same package
			if (vma.isAncestor(cla, clb) || vma.isAncestor(clb, cla)) {
				return true;
			}

			return false;
		}

		/* Equivalent of visible.c checkVisibility(); */
		void checkAccess(MethodHandle handle, boolean forVirtualHandle) throws IllegalAccessException {
			if (accessMode == INTERNAL_PRIVILEGED) {
				// Full access for use by MH implementation.
				return;
			}

			int handleModifiers = handle.rawModifiers;
			if (accessMode == NO_ACCESS) {
				throw new IllegalAccessException();
			}

			if (Modifier.isPublic(handleModifiers) && Modifier.isPublic(handle.defc.getModifiers())) {
				/* Public */
				if (Modifier.isPublic(accessMode)) {
					return;
				}
			} else if (Modifier.isPrivate(handleModifiers)) {
				/* Private */
				if (handle.defc == accessClass && Modifier.isPrivate(accessMode)) {
					return;
				}
			} else if (Modifier.isProtected(handleModifiers)) {
				/* Protected */
				if (accessMode != PUBLIC) {
					if (handle.definingClass.isArray()) {
						/* The only methods array classes have are defined on Object and thus accessible */
						return;
					}
					if (isSamePackage(accessClass, handle.defc)) {
						/* isSamePackage handles accessClass == handle.defc */
						return;
					}
					if (Modifier.isProtected(accessMode) && handle.defc.isAssignableFrom(accessClass)) {
						if ((handle.kind == MethodHandle.KIND_CONSTRUCTOR)
						|| ((handle.kind == MethodHandle.KIND_SPECIAL) && !forVirtualHandle)
						) {
							Class<?> targetClass = handle.definingClass;
							if (handle.kind == MethodHandle.KIND_SPECIAL) {
								targetClass = handle.specialCaller;
							}
							if (accessClass.isAssignableFrom(targetClass)) {
								/* success */
								return;
							}

						} else {
							/* MethodHandle.KIND_GETFIELD, MethodHandle.KIND_PUTFIELD & MethodHandle.KIND_VIRTUAL
							 * restrict the receiver to be a subclass under the current class and thus don't need
							 * additional access checks.
							 */
							return;
						}
					}
				}
			} else {
				/* default (package access) */
				if (((accessMode & PACKAGE) == PACKAGE) && isSamePackage(accessClass, handle.defc)){
					return;
				}
			}
			throw new IllegalAccessException();
		}

		private void checkSpecialAccess(Class<?> callerClass) throws IllegalAccessException {
			if (isWeakenedLookup() || accessClass != callerClass) {
				// K0585 = Private access required
				throw new IllegalAccessException(com.ibm.oti.util.Msg.getString("K0585")); //$NON-NLS-1$
			}
		}

		/**
		 * Return a MethodHandle bound to a specific-implementation of a virtual method, as if created by an invokespecial bytecode
		 * using the class specialToken.  The method and all Classes in its MethodType must be accessible to
		 * the caller.
		 * <p>
		 * The receiver class will be added to the MethodType of the handle, but dispatch will not occur based
		 * on the receiver.
		 *
		 * @param clazz - the class or interface from which defines the method
		 * @param methodName - the method name
		 * @param type - the MethodType of the method
		 * @param specialToken - the calling class for the invokespecial
		 * @return a MethodHandle
		 * @throws IllegalAccessException - if the method is static or access checking fails
		 * @throws NullPointerException - if clazz, methodName, type or specialToken is null
		 * @throws NoSuchMethodException - if clazz has no instance method named methodName with signature matching type
		 * @throws SecurityException - if any installed SecurityManager denies access to the method
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findSpecial(Class<?> clazz, String methodName, MethodType type, Class<?> specialToken) throws IllegalAccessException, NoSuchMethodException, SecurityException, NullPointerException {
			nullCheck(clazz, methodName, type, specialToken);
			checkSpecialAccess(specialToken);	/* Must happen before method resolution */
			initCheck(methodName);
			MethodHandle handle = new DirectHandle(clazz, methodName, type, MethodHandle.KIND_SPECIAL, specialToken);
			handle = restrictReceiver(handle);
			handle = convertToVarargsIfRequired(handle);
			Class<?> callingContext = getCallingContextDepth3();
			if ((handle.defc != accessClass) && !handle.defc.isAssignableFrom(accessClass)) {
				// K0586 = Lookup class ({0}) must be the same as or subclass of the current class ({1})
				throw new IllegalAccessException(com.ibm.oti.util.Msg.getString("K0586", accessClass, handle.defc)); //$NON-NLS-1$
			}
			checkAccess(handle, false);
			checkSecurity(handle.defc, clazz, handle.rawModifiers, callingContext);
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle, methodName, callingContext);
			return handle;
		}

		/**
		 * Return a MethodHandle to a static method.  The MethodHandle will have the same type as the
		 * method.  The method and all classes in its type must be accessible to the caller.
		 *
		 * @param clazz - the class defining the method
		 * @param methodName - the name of the method
		 * @param type - the MethodType of the method
		 * @return A MethodHandle able to invoke the requested static method
		 * @throws IllegalAccessException - if the method is not static or access checking fails
		 * @throws NullPointerException - if clazz, methodName or type is null
		 * @throws NoSuchMethodException - if clazz has no static method named methodName with signature matching type
		 * @throws SecurityException - if any installed SecurityManager denies access to the method
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findStatic(Class<?> clazz, String methodName, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(clazz, methodName, type);
			initCheck(methodName);
			MethodHandle handle = new DirectHandle(clazz, methodName, type, MethodHandle.KIND_STATIC, null);
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, clazz, handle.rawModifiers,callingContext);
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle, methodName, callingContext);

			/* Static check is performed by native code */
			return convertToVarargsIfRequired(handle);
		}

		/**
		 * Return a MethodHandle to a virtual method.  The method will be looked up in the first argument
		 * (aka receiver) prior to dispatch.  The type of the MethodHandle will be that of the method
		 * with the receiver type prepended.
		 *
		 * @param clazz - the class defining the method
		 * @param methodName - the name of the method
		 * @param type - the type of the method
		 * @return a MethodHandle that will do virtual dispatch on the first argument
		 * @throws IllegalAccessException - if method is static or access is refused
		 * @throws NullPointerException - if clazz, methodName or type is null
		 * @throws NoSuchMethodException - if clazz has no virtual method named methodName with signature matching type
		 * @throws SecurityException - if any installed SecurityManager denies access to the method
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findVirtual(Class<?> clazz, String methodName, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(clazz, methodName, type);
			MethodHandle handle = handleForMHInvokeMethods(clazz, methodName, type);
			boolean adaptInterfaceHandle = false;
			if (handle == null) {
				initCheck(methodName); // TODO compare with RI - spec does not require this check

				if (clazz.isInterface()) {
					handle = new InterfaceHandle(clazz, methodName, type);
					if (Modifier.isStatic(handle.rawModifiers)) {
						throw new IllegalAccessException();
					}
					/* Must do the adaption after the security checks as AsTypeHandle
					 * has a different 'defc' than the original handle would have.
					 */
					adaptInterfaceHandle = true;
				} else {
					handle = new DirectHandle(clazz, methodName, type, MethodHandle.KIND_SPECIAL, clazz);
					/* Static check is performed by native code */
					if (!Modifier.isPrivate(handle.rawModifiers) && !Modifier.isFinal(handle.rawModifiers)) {
						handle = new VirtualHandle((DirectHandle) handle);
					}
				}
				handle = restrictReceiver(handle);
				handle = convertToVarargsIfRequired(handle);
			}
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, true);
			checkSecurity(handle.defc, clazz, handle.rawModifiers, callingContext);
			if (adaptInterfaceHandle) {
				handle = adaptInterfaceLookupsOfObjectMethodsIfRequired(handle, clazz, methodName, type);
			}
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle, methodName, callingContext);
			return handle;
		}

		/**
		 * Restrict the receiver as indicated in the JVMS for invokespecial and invokevirtual.
		 * <blockquote>
		 * Finally, if the resolved method is protected (4.6), and it is a member of a superclass of the current class, and
		 * the method is not declared in the same runtime package (5.3) as the current class, then the class of objectref
		 * must be either the current class or a subclass of the current class.
		 * </blockquote>
		 */
		private MethodHandle restrictReceiver(MethodHandle handle) {
			if (!Modifier.isStatic(handle.rawModifiers)
			&& Modifier.isProtected(handle.rawModifiers)
			&& (handle.defc != accessClass)
			&& (!isSamePackage(handle.defc, accessClass))
			) {
				handle = handle.cloneWithNewType(handle.type.changeParameterType(0, accessClass));
			}
			return handle;
		}

		/**
		 * Adapt InterfaceHandles on public Object methods if the method is not redeclared in the interface class.
		 * Public methods of Object are implicitly members of interfaces and do not receive iTable indexes.
		 * If the declaring class is Object, create a VirtualHandle and asType it to the interface class.
		 * @param handle An InterfaceHandle
		 * @param clazz The lookup class
		 * @param methodName The lookup name
		 * @param type The lookup type
		 * @return Either the original handle or an adapted one for Object methods.
		 * @throws NoSuchMethodException
		 * @throws IllegalAccessException
		 */
		static MethodHandle adaptInterfaceLookupsOfObjectMethodsIfRequired(MethodHandle handle, Class<?> clazz, String methodName, MethodType type) throws NoSuchMethodException, IllegalAccessException {
			assert handle instanceof InterfaceHandle;
			/* Object methods need to be treated specially if the interface hasn't declared them itself */
			if (Object.class == handle.defc) {
				if (!Modifier.isPublic(handle.rawModifiers)) {
					/* Interfaces only inherit *public* methods from Object */
					throw new NoSuchMethodException(clazz + "." + methodName + type); //$NON-NLS-1$
				}
				handle = new VirtualHandle(new DirectHandle(Object.class, methodName, type, MethodHandle.KIND_SPECIAL, Object.class));
				handle = handle.asType(handle.type.changeParameterType(0, clazz));
			}
			return handle;
		}

		/*
		 * Check for methods MethodHandle.invokeExact or MethodHandle.invoke.
		 * Access checks are not required as these methods are public and therefore
		 * accessible to everyone.
		 */
		static MethodHandle handleForMHInvokeMethods(Class clazz, String methodName, MethodType type) {
			if (MethodHandle.class.isAssignableFrom(clazz)) {
				if (INVOKE_EXACT.equals(methodName)) {
					return new InvokeExactHandle(type);
				} else if (INVOKE.equals(methodName))  {
					return new InvokeGenericHandle(type);
				}
			}
			return null;
		}

		/**
		 * Return a MethodHandle that provides read access to a field.
		 * The MethodHandle will have a MethodType taking a single
		 * argument with type <code>clazz</code> and returning something of
		 * type <code>fieldType</code>.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to return the value of the field
		 * @throws IllegalAccessException if access is denied or the field is static
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findGetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			FieldHandle handle = new FieldGetterHandle(clazz, fieldName, fieldType, accessClass);
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, clazz, handle.rawModifiers, callingContext);
			return handle;
		}

		/**
		 * Return a MethodHandle that provides read access to a field.
		 * The MethodHandle will have a MethodType taking no arguments
		 * and returning something of type <code>fieldType</code>.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to return the value of the field
		 * @throws IllegalAccessException if access is denied or the field is not static
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findStaticGetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			FieldHandle handle = new StaticFieldGetterHandle(clazz, fieldName, fieldType, accessClass);
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, clazz, handle.rawModifiers, callingContext);
			return handle;
		}

		/**
		 * Return a MethodHandle that provides write access to a field.
		 * The MethodHandle will have a MethodType taking a two
		 * arguments, the first with type <code>clazz</code> and the second with
		 * type <code>fieldType</code>, and returning void.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to set the value of the field
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findSetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			if (fieldType == void.class) {
				throw new NoSuchFieldException();
			}
			FieldHandle handle = new FieldSetterHandle(clazz, fieldName, fieldType, accessClass);
			if (Modifier.isFinal(handle.rawModifiers)) {
				throw new IllegalAccessException("illegal setter on final field");
			}
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, clazz, handle.rawModifiers, callingContext);
			return handle;
		}

		/**
		 * Return a MethodHandle that provides write access to a field.
		 * The MethodHandle will have a MethodType taking one argument
		 * of type <code>fieldType</code> and returning void.
		 *
		 * @param clazz - the class defining the field
		 * @param fieldName - the name of the field
		 * @param fieldType - the type of the field
		 * @return a MethodHandle able to set the value of the field
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchFieldException if no field is found with given name and type in clazz
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findStaticSetter(Class<?> clazz, String fieldName, Class<?> fieldType) throws IllegalAccessException, NoSuchFieldException, SecurityException, NullPointerException {
			nullCheck(clazz, fieldName, fieldType);
			if (fieldType == void.class) {
				throw new NoSuchFieldException();
			}
			FieldHandle handle = new StaticFieldSetterHandle(clazz, fieldName, fieldType, accessClass);

			if (Modifier.isFinal(handle.rawModifiers)) {
				throw new IllegalAccessException("illegal setter on final field");
			}
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, clazz, handle.rawModifiers, callingContext);
			return handle;
		}

		/**
		 * Create a lookup on the request class.  The resulting lookup will have no more
		 * access privileges than the original.
		 *
		 * @param lookupClass - the class to create the lookup on
		 * @return a new MethodHandles.Lookup object
		 */
		public MethodHandles.Lookup in(Class<?> lookupClass){
			if (lookupClass == null) {
				throw new NullPointerException();
			}

			// If it's the same class as ourselves, return this
			if (lookupClass == accessClass) {
				return this;
			}

			int newAccessMode = accessMode & ~PROTECTED;

			if (!isSamePackage(accessClass,lookupClass)) {
				newAccessMode &= ~(PACKAGE | PROTECTED);
			}

			if ((newAccessMode & PRIVATE) == PRIVATE){
				Class a = getUltimateEnclosingClassOrSelf(accessClass);
				Class l = getUltimateEnclosingClassOrSelf(lookupClass);
				if (a != l) {
					newAccessMode &= ~PRIVATE;
				}
			}

			if(!Modifier.isPublic(lookupClass.getModifiers())){
				if(isSamePackage(accessClass, lookupClass)) {
					if ((accessMode & PACKAGE) == 0) {
						newAccessMode = NO_ACCESS;
					}
				} else {
					newAccessMode = NO_ACCESS;
				}
			} else {
				VMLangAccess vma = getVMLangAccess();
				if (vma.getClassloader(accessClass) != vma.getClassloader(lookupClass)) {
					newAccessMode &= ~(PACKAGE | PRIVATE | PROTECTED);
				}
			}

			return new Lookup(lookupClass, newAccessMode);
		}

		/*
		 * Get the top level class for a given class or return itself.
		 */
		private static Class getUltimateEnclosingClassOrSelf(Class c) {
			Class<?> enclosing = c.getEnclosingClass();
			Class<?> previous = c;

			while (enclosing != null) {
				previous = enclosing;
				enclosing = enclosing.getEnclosingClass();
			}
			return previous;
		}

		/*
		 * Determine if 'currentClassLoader' is the same or a child of the requestedLoader.  Necessary
		 * for access checking.
		 */
		private static boolean doesClassLoaderDescendFrom(ClassLoader currentLoader, ClassLoader requestedLoader) {
			if (requestedLoader == null) {
				/* Bootstrap loader is parent of everyone */
				return true;
			}
			if (currentLoader != requestedLoader) {
				while (currentLoader != null) {
					if (currentLoader == requestedLoader) {
						return true;
					}
					currentLoader = currentLoader.getParent();
				}
				return false;
			}
			return true;
		}

		/**
		 * The class being used for visibility checks and access permissions.
		 *
		 * @return The class used in by this Lookup object for access checking
		 */
		public Class<?> lookupClass() {
			return accessClass;
		}

		/**
		 * Make a MethodHandle to the Reflect method.  If the method is non-static, the receiver argument
		 * is treated as the intial argument in the MethodType.
		 * <p>
		 * If m is a virtual method, normal virtual dispatch is used on each invocation.
		 * <p>
		 * If the <code>accessible</code> flag is not set on the Reflect method, then access checking
		 * is performed using the lookup class.
		 *
		 * @param method - the reflect method
		 * @return A MethodHandle able to invoke the reflect method
		 * @throws IllegalAccessException - if access is denied
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle unreflect(Method method) throws IllegalAccessException{
			MethodHandle handle;
			int methodModifiers = method.getModifiers();

			boolean forVirtualHandle = false;
			if (Modifier.isStatic(methodModifiers)) {
				handle = new DirectHandle(method, MethodHandle.KIND_STATIC, null);
			} else if (method.getDeclaringClass().isInterface()){
				handle = new InterfaceHandle(method);
				/* Note, it is not required to call adaptInterfaceLookupsOfObjectMethodsIfRequired() here
				 * as Reflection will not return a j.l.r.Method for a public Object method with an interface
				 * as the declaringClass *unless* that the method is defined in the interface or superinterface.
				 */
			} else {

				/* Static check is performed by native code */
				if (!Modifier.isPrivate(methodModifiers) && !Modifier.isFinal(methodModifiers)) {
					handle = new VirtualHandle(method);
				} else {
					handle = new DirectHandle(method, MethodHandle.KIND_SPECIAL, method.getDeclaringClass());
				}
				forVirtualHandle = true;
				handle = restrictReceiver(handle);
			}

			if (!method.isAccessible()) {
				checkAccess(handle, forVirtualHandle);
			}
			Class<?> callingContext = getCallingContextDepth3();
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle, method.getName(), callingContext);

			return convertToVarargsIfRequired(handle);
		}

		/**
		 * Return a MethodHandle for the reflect constructor. The MethodType has a return type
		 * of the declared class, and the arguments of the constructor.  The MehtodHnadle
		 * creates a new object as through by newInstance.
		 * <p>
		 * If the <code>accessible</code> flag is not set, then access checking
		 * is performed using the lookup class.
		 *
		 * @param method - the Reflect constructor
		 * @return a Methodhandle that creates new instances using the requested constructor
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectConstructor(Constructor method) throws IllegalAccessException {
			MethodHandle handle = new ConstructorHandle(method);

			if (!method.isAccessible()) {
				checkAccess(handle, false);
			}

			return convertToVarargsIfRequired(handle);
		}

		/**
		 * Return a MethodHandle that will create an object of the required class and initialize it using
		 * the constructor method with signature <i>type</i>.  The MethodHandle will have a MethodType
		 * with the same parameters as the constructor method, but will have a return type of the
		 * <i>declaringClass</i> instead of void.
		 *
		 * @param declaringClass - Class that declares the constructor
		 * @param type - the MethodType of the constructor.  Return type must be void.
		 * @return a MethodHandle able to construct and intialize the required class
		 * @throws IllegalAccessException if access is denied
		 * @throws NoSuchMethodException if the method doesn't exist
		 * @throws SecurityException if the SecurityManager prevents access
		 * @throws NullPointerException if any of the arguments are null
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle findConstructor(Class<?> declaringClass, MethodType type) throws IllegalAccessException, NoSuchMethodException {
			nullCheck(declaringClass, type);
			if (type.returnType() != void.class) {
				throw new NoSuchMethodException();
			}
			MethodHandle handle = new ConstructorHandle(declaringClass, type);
			Class<?> callingContext = getCallingContextDepth3();
			checkAccess(handle, false);
			checkSecurity(handle.defc, declaringClass, handle.rawModifiers, callingContext);
			return convertToVarargsIfRequired(handle);
		}

		/**
		 * Return a MethodHandle for the Reflect method, that will directly call the requested method
		 * as through from the class <code>specialToken</code>.  The MethodType of the method handle
		 * will be that of the method with the receiver argument prepended.
		 *
		 * @param method - a Reflect method
		 * @param specialToken - the class the call is from
		 * @return a MethodHandle that directly dispatches the requested method
		 * @throws IllegalAccessException - if access is denied
		 */
		@sun.reflect.CallerSensitive
		public MethodHandle unreflectSpecial(Method method, Class<?> specialToken) throws IllegalAccessException {
			nullCheck(method, specialToken);
			checkSpecialAccess(specialToken);	/* Must happen before method resolution */
			int modifiers = method.getModifiers();

			if (Modifier.isStatic(modifiers)) {
				throw new IllegalAccessException();
			}
			/* Does not require 'restrictReceiver()'as DirectHandle(KIND_SPECIAL) sets receiver type to specialToken */
			MethodHandle handle = convertToVarargsIfRequired(new DirectHandle(method, MethodHandle.KIND_SPECIAL, specialToken));

			if (!method.isAccessible()) {
				checkAccess(handle, false);
			}
			Class<?> callingContext = getCallingContextDepth3();
			handle = SecurityFrameInjector.wrapHandleWithInjectedSecurityFrameIfRequired(this, handle, method.getName(), callingContext);

			return handle;
		}

		/**
		 * Create a MethodHandle that returns the value of the Reflect field.  There are two cases:
		 * <ol>
		 * <li>a static field - which has the MethodType with only a return type of the field</li>
		 * <li>an instance field - which has the MethodType with a return type of the field and a
		 * single argument of the object that contains the field</li>
		 * </ol>
		 * <p>
		 * If the <code>accessible</code> flag is not set, then access checking
		 * is performed using the lookup class.
		 *
		 * @param field - a Reflect field
		 * @return a MethodHandle able to return the field value
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectGetter(Field field) throws IllegalAccessException {
			int modifiers = field.getModifiers();

			MethodHandle handle;
			if (Modifier.isStatic(modifiers)) {
				handle = new StaticFieldGetterHandle(field);
			} else {
				handle = new FieldGetterHandle(field);
			}

			if (!field.isAccessible()) {
				checkAccess(handle, false);
			}
			return handle;
		}

		/**
		 * Create a MethodHandle that sets the value of the Reflect field.  All MethodHandles created
		 * here have a return type of void.  For the arguments, there are two cases:
		 * <ol>
		 * <li>a static field - which takes a single argument the same as the field</li>
		 * <li>an instance field - which takes two arguments, the object that contains the field, and the type of the field</li>
		 * </ol>
		 * <p>
		 * If the <code>accessible</code> flag is not set, then access checking
		 * is performed using the lookup class.
		 *
		 * @param field - a Reflect field
		 * @return a MethodHandle able to set the field value
		 * @throws IllegalAccessException - if access is denied
		 */
		public MethodHandle unreflectSetter(Field field) throws IllegalAccessException {
			MethodHandle handle;
			int modifiers = field.getModifiers();

			if (Modifier.isFinal(modifiers)) {
				throw new IllegalAccessException("illegal setter on final field");
			}

			if (Modifier.isStatic(modifiers)) {
				handle = new StaticFieldSetterHandle(field);
			} else {
				handle = new FieldSetterHandle(field);
			}

			if (!field.isAccessible()) {
				checkAccess(handle, false);
			}

			return handle;
		}

		@Override
		public String toString() {
			String toString = accessClass.getName();
			switch(accessMode) {
			case NO_ACCESS:
				toString += "/noaccess"; //$NON-NLS-1$
				break;
			case PUBLIC:
				toString += "/public"; //$NON-NLS-1$
				break;
			case PUBLIC | PACKAGE:
				toString += "/package"; //$NON-NLS-1$
				break;
			case PUBLIC | PACKAGE | PRIVATE:
				toString += "/private"; //$NON-NLS-1$
				break;
			}
			return toString;
		}

		/*
		 * Determine if this lookup has been weakened.
		 * A lookup is weakened when it doesn't have private access.
		 *
		 * return true if the lookup has been weakened.
		 */
		boolean isWeakenedLookup() {
			return PRIVATE != (accessMode & PRIVATE);
		}

		/*
		 * If there is a security manager, this performs 1 to 4 different security checks:
		 *
		 * 1) secmgr.checkMemberAccess(refc, Member.PUBLIC) for all members
		 * 2) secmgr.checkPackageAccess(refcPkg), if classloader of access class is not same or ancestor of classloader of reference class
		 * 3) secmgr.checkMemberAccess(defc, Member.DECLARED), if retrieved member is not public
		 * 4) secmgr.checkPackageAccess(defcPkg), if retrieved member is not public, and if defining class and reference class are in different classloaders,
		 *    and if classloader of access class is not same or ancestor of classloader of defining class
		 */
		void checkSecurity(Class<?> definingClass, Class<?> referenceClass, int modifiers, Class<?> callingContext) throws IllegalAccessException {
			if (accessMode == INTERNAL_PRIVILEGED) {
				// Full access for use by MH implementation.
				return;
			}
			if (null == definingClass) {
				throw new IllegalAccessException();
			}

			if (performSecurityCheck) {
				SecurityManager secmgr = System.getSecurityManager();
				if (secmgr != null) {
					/* Use leaf-class in the case of arrays for security check */
					while (definingClass.isArray()) {
						definingClass = definingClass.getComponentType();
					}
					while (referenceClass.isArray()) {
						referenceClass = referenceClass.getComponentType();
					}
					try {
						setAccessClassForSecuritCheck(accessClass);
						VMLangAccess vma = getVMLangAccess();
						/* first check */
						secmgr.checkMemberAccess(referenceClass, Member.PUBLIC);

						boolean checkCallerClass = accessClass != callingContext;
						ClassLoader referenceClassLoader = referenceClass.getClassLoader();
						if (!doesClassLoaderDescendFrom(referenceClassLoader, accessClass.getClassLoader())
						|| (checkCallerClass && !doesClassLoaderDescendFrom(referenceClassLoader, callingContext.getClassLoader()))
						) {
							String packageName = vma.getPackageName(referenceClass);
							secmgr.checkPackageAccess(packageName);
						}

						/* third check */
						if (!Modifier.isPublic(modifiers)) {
							secmgr.checkMemberAccess(definingClass, Member.DECLARED);

							/* fourth check */
							if (definingClass.getClassLoader() != referenceClass.getClassLoader()) {
								if (!doesClassLoaderDescendFrom(definingClass.getClassLoader(), accessClass.getClassLoader())) {
									secmgr.checkPackageAccess(vma.getPackageName(definingClass));
								}
							}
						}
					} finally {
						setAccessClassForSecuritCheck(null);
					}
				}
			}
		}

		private static native void setAccessClassForSecuritCheck(Class<?> accessClass);
	}

	/**
	 * Return a MethodHandles.Lookup object for the caller.
	 *
	 * @return a MethodHandles.Lookup object
	 */
	@sun.reflect.CallerSensitive
	public static MethodHandles.Lookup lookup() {
		Class<?> c = getStackClass(1);
		return new Lookup(c);
	}

	/**
	 * Return a MethodHandles.Lookup object that is only able to access <code>public</code> members.
	 *
	 * @return a MethodHandles.Lookup object
	 */
	public static MethodHandles.Lookup publicLookup() {
		return Lookup.publicLookup;
	}

	/**
	 * Return a MethodHandle that is the equivalent of calling
	 * MethodHandles.lookup().findVirtual(MethodHandle.class, "invokeExact", type).
	 * <p>
	 * The MethodHandle has a method type that is the same as type except that an additional
	 * argument of MethodHandle will be added as the first parameter.
	 * <p>
	 * This method is not subject to the same security checks as a findVirtual call.
	 *
	 * @param type - the type of the invokeExact call to lookup
	 * @return a MethodHandle equivalent to calling invokeExact on the first argument.
	 */
	public static MethodHandle exactInvoker(MethodType type) {
		if (type == null) {
			throw new NullPointerException();
		}
		return new InvokeExactHandle(type);
	}

	/**
	 * Return a MethodHandle that is the equivalent of calling
	 * MethodHandles.lookup().findVirtual(MethodHandle.class, "invoke", type).
	 * <p>
	 * The MethodHandle has a method type that is the same as type except that an additional
	 * argument of MethodHandle will be added as the first parameter.
	 * <p>
	 * This method is not subject to the same security checks as a findVirtual call.
	 *
	 * @param type - the type of the invoke call to lookup
	 * @return a MethodHandle equivalent to calling invoke on the first argument.
	 */
	public static MethodHandle invoker(MethodType type) {
		if (type == null) {
			throw new NullPointerException();
		}
		return new InvokeGenericHandle(type);
	}

	/**
	 * Return a MethodHandle that is able to invoke a MethodHandle of <i>type</i> as though by
	 * invoke after spreading the final Object[] parameter.
	 *
	 * @param type - the type of the invoke method to look up
	 * @param fixedArgCount - the number of fixed arguments in the methodtype
	 * @return a MethodHandle that invokes its first argument after spreading the Object array
	 * @throws IllegalArgumentException if the fixedArgCount is less than 0 or greater than type.ParameterCount()
	 * @throws NullPointerException if the type is null
	 */
	public static MethodHandle spreadInvoker(MethodType type, int fixedArgCount) throws IllegalArgumentException, NullPointerException {
		int typeParameterCount = type.parameterCount();
		if ((fixedArgCount < 0) || (fixedArgCount > typeParameterCount)) {
			// K039c = Invalid parameters
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
		}
		MethodHandle invoker = invoker(type);
		int spreadArgCount = typeParameterCount - fixedArgCount;
		return invoker.asSpreader(Object[].class, spreadArgCount);
	}

	/**
	 * Produce a MethodHandle that implements an if-else block.
	 *
	 * This MethodHandle is composed from three handles:
	 * <ul>
	 * <li>guard - a boolean returning handle that takes a subset of the arguments passed to the true and false targets</li>
	 * <li>trueTarget - the handle to call if the guard returns true</li>
	 * <li>falseTarget - the handle to call if the guard returns false</li>
	 * </ul>
	 *
	 * @param guard - method handle returning boolean to determine which target to call
	 * @param trueTarget - target method handle to call if guard is true
	 * @param falseTarget - target method handle to call if guard is false
	 * @return A MethodHandle that implements an if-else block.
	 *
	 * @throws NullPointerException - if any of the three method handles are null
	 * @throws IllegalArgumentException - if any of the following conditions are true:
	 * 				1) trueTarget and falseTarget have different MethodTypes
	 * 				2) the guard handle doesn't have a boolean return value
	 * 				3) the guard handle doesn't take a subset of the target handle's arguments
	 */
	public static MethodHandle guardWithTest(MethodHandle guard, MethodHandle trueTarget, MethodHandle falseTarget) throws NullPointerException, IllegalArgumentException{
		MethodType guardType = guard.type;
		MethodType trueType = trueTarget.type;
		MethodType falseType = falseTarget.type;
		if (!trueType.equals(falseType)) {
			throw new IllegalArgumentException();
		}
		int testArgCount = guardType.parameterCount();
		if ((guardType.returnType != boolean.class) || (testArgCount > trueType.parameterCount())) {
			throw new IllegalArgumentException();
		}
		if (!Arrays.equals(guardType.arguments, Arrays.copyOfRange(trueType.arguments, 0, testArgCount))) {
			throw new IllegalArgumentException();
		}

		return buildTransformHandle(new GuardWithTestHelper(guard, trueTarget, falseTarget), trueType);
	}

	/**
	 * Produce a MethodHandle that implements a try-catch block.
	 *
	 * This adapter acts as though the <i>tryHandle</i> where run inside a try block.  If <i>tryHandle</i>
	 * throws an exception of type <i>throwableClass</i>, the <i>catchHandle</i> is invoked with the
	 * exception instance and the original arguments.
	 * <p>
	 * The catchHandle may take a subset of the original arguments rather than the full set.  Its first
	 * argument will be the exception instance.
	 * <p>
	 * Both the catchHandle and the tryHandle must have the same return type.
	 *
	 * @param tryHandle - the method handle to wrap with the try block
	 * @param throwableClass - the class of exception to be caught and handled by catchHandle
	 * @param catchHandle - the method handle to call if an exception of type throwableClass is thrown by tryHandle
	 * @return a method handle that will call catchHandle if tryHandle throws an exception of type throwableClass
	 *
	 * @throws NullPointerException - if any of the parameters are null
	 * @throws IllegalArgumentException - if tryHandle and catchHandle have different return types,
	 * or the catchHandle doesn't take a throwableClass as its first argument,
	 * of if catchHandle arguments[1-N] differ from tryHandle arguments[0-(N-1)]
	 */
	public static MethodHandle catchException(MethodHandle tryHandle, Class<? extends Throwable> throwableClass, MethodHandle catchHandle) throws NullPointerException, IllegalArgumentException{
		if ((tryHandle == null) || (throwableClass == null) || (catchHandle == null)) {
			throw new NullPointerException();
		}
		MethodType tryType = tryHandle.type;
		MethodType catchType = catchHandle.type;
		if (tryType.returnType != catchType.returnType) {
			throw new IllegalArgumentException();
		}
		if (catchType.parameterType(0) != throwableClass) {
			throw new IllegalArgumentException();
		}
		int catchArgCount =  catchType.parameterCount();
		if ((catchArgCount - 1) > tryType.parameterCount()) {
			throw new IllegalArgumentException();
		}
		Class<?>[] tryParams = tryType.arguments;
		Class<?>[] catchParams = catchType.arguments;
		for (int i = 1; i < catchArgCount; i++) {
			if (!catchParams[i].equals(tryParams[i - 1])) {
				throw new IllegalArgumentException();
			}
		}

		return buildTransformHandle(new CatchHelper(tryHandle, catchHandle, throwableClass), tryType);
	}

	/**
	 * Produce a MethodHandle that acts as an identity function.  It will accept a single
	 * argument and return it.
	 *
	 * @param classType - the type to use for the return and parameter types
	 * @return an identity MethodHandle that returns its argument
	 * @throws NullPointerException - if the classType is null
	 * @throws IllegalArgumentException - if the the classType is void.
	 */
	public static MethodHandle identity(Class<?> classType) throws NullPointerException, IllegalArgumentException {
		if (classType == void.class) {
			throw new IllegalArgumentException();
		}
		try {
			MethodType methodType = MethodType.methodType(classType, classType);
			if (classType.isPrimitive()){
				return lookup().findStatic(MethodHandles.class, "identity", methodType); //$NON-NLS-1$
			}

			MethodHandle handle = lookup().findStatic(MethodHandles.class, "identity", MethodType.methodType(Object.class, Object.class)); //$NON-NLS-1$
			//TODO: optimize this here to just change the type (can't be done in asType b/c not safe in general)
			return handle.asType(methodType);
		} catch(IllegalAccessException e) {
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	@SuppressWarnings("unused")
	private static boolean identity(boolean x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static byte identity(byte x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static short identity(short x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static char identity(char x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static int identity(int x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static float identity(float x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static double identity(double x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static long identity(long x) {
		return x;
	}
	@SuppressWarnings("unused")
	private static Object identity(Object x) {
		return x;
	}

	/**
	 * Create a MethodHandle that returns the <i>constantValue</i> on each invocation.
	 * <p>
	 * Conversions of the <i>constantValue</i> to the <i>returnType</i> occur if possible, otherwise
	 * a ClassCastException is thrown.  For primitive <i>returnType</i>, widening primitive conversions are
	 * attempted.  Otherwise, reference conversions are attempted.
	 *
	 * @param returnType - the return type of the MethodHandle.
	 * @param constantValue - the value to return from the MethodHandle when invoked
	 * @return a MethodHandle that always returns the <i>constantValue</i>
	 * @throws NullPointerException - if the returnType is null
	 * @throws ClassCastException - if the constantValue cannot be converted to returnType
	 * @throws IllegalArgumentException - if the returnType is void
	 */
	public static MethodHandle constant(Class<?> returnType, Object constantValue) throws NullPointerException, ClassCastException, IllegalArgumentException {
		if (returnType == null) {
			throw new NullPointerException();
		}
		if (returnType == void.class) {
			throw new IllegalArgumentException();
		}
		if (returnType.isPrimitive()) {
			if (constantValue == null) {
				throw new IllegalArgumentException();
			}
			Class<?> unwrapped = MethodType.unwrapPrimitive(constantValue.getClass());
			if ((returnType != unwrapped) && !FilterHelpers.checkIfWideningPrimitiveConversion(unwrapped, returnType)) {
				throw new ClassCastException();
			}
		} else {
			returnType.cast(constantValue);
		}
		MethodHandle boundObjectMH = identity(Object.class).bindTo(constantValue);
		return boundObjectMH.asType(MethodType.methodType(returnType));
	}

	/**
	 * Return a MethodHandle able to read from the array.  The MethodHandle's return type will be the same as
	 * the elements of the array.  The MethodHandle will also accept two arguments - the first being the array, typed correctly,
	 * and the second will be the the <code>int</code> index into the array.
	 *
	 * @param arrayType - the type of the array
	 * @return a MethodHandle able to return values from the array
	 * @throws IllegalArgumentException - if arrayType is not actually an array
	 */
	public static MethodHandle arrayElementGetter(Class<?> arrayType) throws IllegalArgumentException {
		if (!arrayType.isArray()) {
			throw new IllegalArgumentException();
		}

		try {
			Class<?> componentType = arrayType.getComponentType();
			if (componentType.isPrimitive()) {
				// Directly lookup the appropriate helper method
				String name = componentType.getCanonicalName();
				MethodType type = MethodType.methodType(componentType, arrayType, int.class);
				return lookup().findStatic(MethodHandles.class, name + "ArrayGetter", type); //$NON-NLS-1$
			}

			// Lookup the "Object[]" case and use some asTypes() to get the right MT and return type.
			MethodType type = MethodType.methodType(Object.class, Object[].class, int.class);
			MethodHandle mh = lookup().findStatic(MethodHandles.class, "objectArrayGetter", type); //$NON-NLS-1$
			MethodType realType = MethodType.methodType(componentType, arrayType, int.class);

			return mh.asType(realType);
		} catch(IllegalAccessException e) {
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Return a MethodHandle able to write to the array.  The MethodHandle will have a void return type and take three
	 * arguments: the first being the array, typed correctly, the second will be the the <code>int</code> index into the array,
	 * and the third will be the item to write into the array
	 *
	 * @param arrayType - the type of the array
	 * @return a MehtodHandle able to write into the array
	 * @throws IllegalArgumentException - if arrayType is not actually an array
	 */
	public static MethodHandle arrayElementSetter(Class<?> arrayType) throws IllegalArgumentException {
		if (!arrayType.isArray()) {
			throw new IllegalArgumentException();
		}

		try {
			Class<?> componentType = arrayType.getComponentType();
			if (componentType.isPrimitive()) {
				// Directly lookup the appropriate helper method
				String name = componentType.getCanonicalName();
				MethodType type = MethodType.methodType(void.class, arrayType, int.class, componentType);
				return lookup().findStatic(MethodHandles.class, name + "ArraySetter", type); //$NON-NLS-1$
			}

			// Lookup the "Object[]" case and use some asTypes() to get the right MT and return type.
			MethodType type = MethodType.methodType(void.class, Object[].class, int.class, Object.class);
			MethodHandle mh = lookup().findStatic(MethodHandles.class, "objectArraySetter", type); //$NON-NLS-1$
			MethodType realType = MethodType.methodType(void.class, arrayType, int.class, componentType);

			return mh.asType(realType);
		} catch(IllegalAccessException e) {
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Return a MethodHandle that will throw the passed in Exception object.  The return type is largely
	 * irrelevant as the method never completes normally.  Any return type that is convenient can be
	 * used.
	 *
	 * @param returnType - The return type for the method
	 * @param exception - the type of Throwable to accept as an argument
	 * @return a MethodHandle that throws the passed in exception object
	 */
	public static MethodHandle throwException(Class<?> returnType, Class<? extends Throwable> exception) {
		MethodType realType = MethodType.methodType(returnType, exception);
		MethodHandle handle;

		try {
			if (returnType.isPrimitive() || returnType.equals(void.class)) {
				// Directly lookup the appropriate helper method
				MethodType type = MethodType.methodType(returnType, Throwable.class);
				String name = returnType.getCanonicalName();
				handle = lookup().findStatic(MethodHandles.class, name + "ExceptionThrower", type); //$NON-NLS-1$
			} else {
				MethodType type = MethodType.methodType(Object.class, Throwable.class);
				handle = lookup().findStatic(MethodHandles.class, "objectExceptionThrower", type); //$NON-NLS-1$
			}
			return handle.asType(realType);
		} catch(IllegalAccessException e) {
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/**
	 * Return a MethodHandle that will adapt the return value of <i>handle</i> by running the <i>filter</i>
	 * on it and returning the result of the filter.
	 * <p>
	 * If <i>handle</i> has a void return, <i>filter</i> must not take any parameters.
	 *
	 * @param handle - the MethodHandle that will have its return value adapted
	 * @param filter - the MethodHandle that will do the return adaption.
	 * @return a MethodHandle that will run the filter handle on the result of handle.
	 * @throws NullPointerException - if handle or filter is null
	 * @throws IllegalArgumentException - if the return type of <i>handle</i> differs from the type of the only argument to <i>filter</i>
	 */
	public static MethodHandle filterReturnValue(MethodHandle handle, MethodHandle filter) throws NullPointerException, IllegalArgumentException {
		MethodType filterType = filter.type;
		int filterArgCount = filterType.parameterCount();
		Class<?> handleReturnType = handle.type.returnType;

		if ((handleReturnType == void.class) && (filterArgCount == 0)) {
			// filter handle must not take any parameters as handle doesn't return anything
			return new FilterReturnHandle(handle, filter);
		}
		if ((filterType.parameterCount() == 1) && (filterType.parameterType(0) == handle.type.returnType)) {
			// filter handle must accept single parameter of handle's returnType
			return new FilterReturnHandle(handle, filter);
		}
		throw new IllegalArgumentException();
	}

	@SuppressWarnings("unused")
	private static void voidExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static int intExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static char charExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static byte byteExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static boolean booleanExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static short shortExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static long longExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static double doubleExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static float floatExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static Object objectExceptionThrower(Throwable t) throws Throwable {
		throw t;
	}

	@SuppressWarnings("unused")
	private static  int intArrayGetter(int[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static char charArrayGetter(char[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static short shortArrayGetter(short[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static byte byteArrayGetter(byte[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static long longArrayGetter(long[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static double doubleArrayGetter(double[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static float floatArrayGetter(float[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static boolean booleanArrayGetter(boolean[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static Object objectArrayGetter(Object[] array, int index) {
		return array[index];
	}

	@SuppressWarnings("unused")
	private static void intArraySetter(int[] array, int index, int value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void charArraySetter(char[] array, int index, char value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void shortArraySetter(short[] array, int index, short value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void byteArraySetter(byte[] array, int index, byte value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void longArraySetter(long[] array, int index, long value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void doubleArraySetter(double[] array, int index, double value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void floatArraySetter(float[] array, int index, float value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void booleanArraySetter(boolean[] array, int index, boolean value) {
		array[index] = value;
	}

	@SuppressWarnings("unused")
	private static void objectArraySetter(Object[] array, int index, Object value) {
		array[index] = value;
	}

	/**
	 * Produce a MethodHandle that adapts its arguments using the filter methodhandles before calling the underlying handle.
	 * <p>
	 * The type of the adapter is the type of the original handle with the filter argument types replacing their corresponding
	 * arguments.  Each of the adapters must return the correct type for their corresponding argument.
	 * <p>
	 * If the filters array is empty or contains only null filters, the original handle will be returned.
	 *
	 * @param handle - the underlying methodhandle to call with the filtered arguments
	 * @param startPosition - the position to start applying the filters at
	 * @param filters - the array of adapter handles to apply to the arguments
	 * @return a MethodHandle that modifies the arguments by applying the filters before calling the underlying handle
	 * @throws NullPointerException - if handle or filters is null
	 * @throws IllegalArgumentException - if one of the filters is not applicable to the corresponding handle argument
	 * 				or there are more filters then arguments when starting at startPosition
	 * 				or startPosition is invalid
	 *
	 */
	public static MethodHandle filterArguments(MethodHandle handle, int startPosition, MethodHandle... filters) throws NullPointerException, IllegalArgumentException {
		if ((handle == null) || (filters == null)) {
			throw new NullPointerException();
		}
		MethodType handleType = handle.type;
		if ((startPosition < 0) || ((startPosition + filters.length) > handleType.parameterCount())) {
			throw new IllegalArgumentException();
		}
		if (filters.length == 0) {
			return handle;
		}
		// clone the filter array so it can't be modified after the filters have been validated
		filters = filters.clone();
		// clone the parameter array
		Class<?>[] newArgTypes = handleType.parameterArray();
		boolean containsNonNullFilters = false;
		for (int i = 0; i < filters.length; i++) {
			MethodHandle filter = filters[i];
			if (filter != null) {
				containsNonNullFilters = true;
				MethodType filterType = filter.type;
				if (!newArgTypes[startPosition + i].equals(filterType.returnType)) {
					throw new IllegalArgumentException();
				}
				if (filterType.parameterCount() != 1) {
					throw new IllegalArgumentException();
				}
				newArgTypes[startPosition + i] = filterType.arguments[0];
			}
		}
		if (!containsNonNullFilters) {
			return handle;
		}
		MethodType newType = MethodType.methodType(handleType.returnType, newArgTypes);

		return buildTransformHandle(new FilterHelper(handle, startPosition, filters), newType);
	}

	/**
	 * Produce a MethodHandle that preprocesses some of the arguments by calling the preprocessor handle.
	 *
	 * If the preprocessor handle has a return type, it must be the same as the first argument type of the <i>handle</i>.
	 * If the preprocessor returns void, it does not contribute the first argument to the <i>handle</i>.
	 * In all cases, the preprocessor handle accepts a subset of the arguments for the handle.
	 *
	 * @param handle - the handle to call after preprocessing
	 * @param preprocessor - a methodhandle that preprocesses some of the incoming arguments
	 * @return a MethodHandle that preprocesses some of the arguments to the handle before calling the next handle, possibly with an additional first argument
	 * @throws NullPointerException - if any of the arguments are null
	 * @throws IllegalArgumentException - if the preprocessor's return type is not void and it differs from the first argument type of the handle,
	 * 			or if the arguments taken by the preprocessor isn't a subset of the arguments to the handle
	 */
	public static MethodHandle foldArguments(MethodHandle handle, MethodHandle preprocessor) throws NullPointerException, IllegalArgumentException {
		if ((handle == null) || (preprocessor == null)) {
			throw new NullPointerException();
		}
		MethodType preprocessorType = preprocessor.type;
		Class<?> preprocessorReturnClass = preprocessorType.returnType;
		MethodType handleType = handle.type;

		if (preprocessorReturnClass == void.class) {
			// special case: a preprocessor handle that returns void doesn't provide an argument to the underlying handle
			if (handleType.parameterCount() < preprocessorType.parameterCount()) {
				throw new IllegalArgumentException();
			}
			if (preprocessorType.parameterCount() > 0) {
				if (!Arrays.equals(preprocessorType.arguments, Arrays.copyOfRange(handleType.arguments, 0, preprocessorType.parameterCount()))) {
					throw new IllegalArgumentException();
				}
			}
			return buildTransformHandle(new VoidFoldHelper(handle, preprocessor), handleType);
		}

		if (handleType.parameterCount() <= preprocessorType.parameterCount()) {
			throw new IllegalArgumentException();
		}
		if (!preprocessorReturnClass.equals(handleType.arguments[0])) {
			throw new IllegalArgumentException();
		}
		if (preprocessorType.parameterCount() > 0) {
			if (!Arrays.equals(preprocessorType.arguments, Arrays.copyOfRange(handleType.arguments, 1, preprocessorType.parameterCount() + 1))) {
				throw new IllegalArgumentException("Can't apply fold of type: " + preprocessorType + " to handle of type: " + handleType);
			}
		}
		MethodType newType = handleType.dropParameterTypes(0, 1);
		return buildTransformHandle(new FoldHelper(handle, preprocessor), newType);
	}

	/**
	 * Produce a MethodHandle that will permute the incoming arguments according to the
	 * permute array.  The new handle will have a type of permuteType.
	 * <p>
	 * The permutations can include duplicating or rearranging the arguments.  The permute
	 * array must have the same number of items as there are parameters in the
	 * handle's type.
	 * <p>
	 * Each argument type must exactly match - no conversions are applied.
	 *
	 * @param handle - the original handle to call after permuting the arguments
	 * @param permuteType - the new type of the adapter handle
	 * @param permute - the reordering from the permuteType to the handle type
	 * @return a MethodHandle that rearranges the arguments before calling the original handle
	 * @throws NullPointerException - if any of the arguments are null
	 * @throws IllegalArgumentException - if permute array is not the same length as handle.type().parameterCount() or
	 * 			if handle.type() and permuteType have different return types, or
	 * 			if the permute arguments don't match the handle.type()
	 */
	public static MethodHandle permuteArguments(MethodHandle handle, MethodType permuteType, int... permute) throws NullPointerException, IllegalArgumentException {
		if ((handle == null) || (permuteType == null) || (permute == null)) {
			throw new NullPointerException();
		}
		if (permute.length != handle.type.parameterCount()) {
			throw new IllegalArgumentException();
		}
		if (permuteType.returnType != handle.type.returnType) {
			throw new IllegalArgumentException();
		}
		permute = permute.clone();	// ensure the permute[] can't be modified during/after validation
		// validate permuted args are an exact match
		Class<?>[] permuteArgs = permuteType.arguments;
		Class<?>[] handleArgs = handle.type.arguments;
		for (int i = 0; i < permute.length; i++) {
			int permuteIndex = permute[i];
			if ((permuteIndex < 0) || (permuteIndex >= permuteArgs.length)){
				throw new IllegalArgumentException();
			}
			if (handleArgs[i] != permuteArgs[permuteIndex]) {
				throw new IllegalArgumentException();
			}
		}
		return buildTransformHandle(new PermuteHelper(handle, permute), permuteType);
	}

	/**
	 * This method returns a method handle that delegates to the original method handle,
	 * ignoring a particular range of arguments (starting at a given location and
	 * with given types).  The type of the returned method handle is the type of the original handle
	 * with the given types inserted in the parameter type list at the given location.
	 *
	 * @param originalHandle - the original method handle to be transformed
	 * @param location -  the location of the first argument to be removed
	 * @param valueTypes - an array of the argument types to be removed
	 * @return a MethodHandle - representing a transformed handle as described above
	 */
	public static MethodHandle dropArguments(MethodHandle originalHandle, int location, Class<?>... valueTypes) {
		if ((null == originalHandle) || (null == valueTypes)) {
			// K0337 = null type not allowed
			throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0337")); //$NON-NLS-1$
		}
		if ((location < 0) || (location > originalHandle.type.parameterCount())) {
			// K039c = Invalid parameters
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
		}

		/* check that arguments are not void.class or null*/
		for (Class<?> c : valueTypes) {
			if (null == c) {
				// K0337 = null type not allowed
				throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0337")); //$NON-NLS-1$
			}
			if (c.equals(void.class)) {
				// K039c = Invalid parameters
				throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
			}
		}

		MethodType mtype = originalHandle.type.insertParameterTypes(location, valueTypes);
		return buildTransformHandle(new DropHelper(location, valueTypes.length, originalHandle), mtype);
	}

	/**
	 * This method returns a method handle that delegates to the original method handle,
	 * ignoring a particular range of arguments (starting at a given location and
	 * with given types).  The type of the returned method handle is the type of the original handle
	 * with the given types inserted in the parameter type list at the given location.
	 *
	 * @param originalHandle - the original method handle to be transformed
	 * @param location -  the location of the first argument to be removed
	 * @param valueTypes - a List of the argument types to be removed
	 * @return a MethodHandle - representing a transformed handle as described above
	 */
	public static MethodHandle dropArguments(MethodHandle originalHandle, int location, List<Class<?>> valueTypes) {
		if ((null == originalHandle) || (null == valueTypes)) {
			// K0337 = null type not allowed
			throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0337")); //$NON-NLS-1$
		}
		if ((location < 0) || (location > originalHandle.type.parameterCount())) {
			// K039c = Invalid parameters
			throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
		}

		/* check that arguments are not void.class or null*/
		for (Class<?> c : valueTypes) {
			if (null == c) {
				// K0337 = null type not allowed
				throw new NullPointerException(com.ibm.oti.util.Msg.getString("K0337")); //$NON-NLS-1$
			}
			if (c.equals(void.class)) {
				// K039c = Invalid parameters
				throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K039c")); //$NON-NLS-1$
			}
		}

		MethodType mtype = originalHandle.type.insertParameterTypes(location, valueTypes);
		return buildTransformHandle(new DropHelper(location, valueTypes.size(), originalHandle), mtype);
	}

	/* A helper method to invoke argument transformation helpers */
	private static MethodHandle buildTransformHandle(ArgumentHelper helper, MethodType mtype){
		MethodType helperType = MethodType.methodType(Object.class, Object[].class);
		try {
			return lookup().bind(helper, "helper", helperType).asCollector(Object[].class, mtype.parameterCount()).asType(mtype); //$NON-NLS-1$
		} catch(IllegalAccessException e) {
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		}
	}

	/* A helper class for use by the dropArguments methods */
	private static final class DropHelper implements ArgumentHelper {

		DropHelper(int pos, int count, MethodHandle mh) {
			dropPos = pos;
			dropCount = count;
			nextMethodHandle = mh;
		}

		private final MethodHandle nextMethodHandle;
		private final int dropPos;
		private final int dropCount;

		public Object helper(Object[] arguments) throws Throwable {
			int length = arguments.length - dropCount;
			Object[] amendedArray = new Object[length];
			if (dropPos != 0) {
				System.arraycopy(arguments, 0, amendedArray, 0, dropPos);
			}
			System.arraycopy(arguments, dropPos + dropCount, amendedArray, dropPos, length - dropPos);
			return nextMethodHandle.invokeWithArguments(amendedArray);
		}

	}

	static final MethodHandle spreadHelper(Class<?> arrayClass, int spreadCount, MethodHandle handle, MethodType collectType) {
		return buildTransformHandle(new SpreadHelper(arrayClass, spreadCount, handle), collectType);
	}

	/**
	 * Produce an adapter that converts the incoming arguments from <i>type</i> to the underlying MethodHandle's type
	 * and converts the return value as required.
	 * <p>
	 * The following conversions, beyond those allowed by {@link MethodHandle#asType(MethodType)} are also allowed:
	 * <ul>
	 * <li>A conversion to an interface is done without a cast</li>
	 * <li>A boolean is treated as a single bit unsigned integer and may be converted to other primitive types</li>
	 * <li>A primitive can also be cast using Java casting conversion if asType would have allowed Java method invocation conversion</li>
	 * <li>An unboxing conversion, possibly followed by a widening primitive conversion</li>
	 * </ul>
	 * These additional rules match Java casting conversion and those of the bytecode verifier.
	 *
	 * @param handle - the MethodHandle to invoke after converting the arguments to its type
	 * @param type - the type to convert from
	 * @return a MethodHandle which does the required argument and return conversions, if possible
	 * @throws NullPointerException - if either of the arguments are null
	 * @throws WrongMethodTypeException - if an illegal conversion is requested
	 */
	public static MethodHandle explicitCastArguments(MethodHandle handle, MethodType type) throws NullPointerException, WrongMethodTypeException {
		if ((handle == null) || (type == null)){
			throw new NullPointerException();
		}
		MethodType handleType = handle.type;
		if (handleType.equals(type)) {
			return handle;
		}
		MethodHandle mh = handle;
		if (handleType.returnType != type.returnType) {
			MethodHandle filter = FilterHelpers.getReturnFilter(handleType.returnType, type.returnType, true);
			mh = new FilterReturnHandle(handle, filter);
			/* Exit early if only return types differ */
			if (mh.type == type) {
				return mh;
			}
		}
		return new ExplicitCastHandle(mh, type);
	}

	/* A helper class for use by the asSpreader(Class, int) method */
	private static final class SpreadHelper implements ArgumentHelper {
		private final MethodHandle nextMethodHandle;
		private final int spreadCount;
		private final Class<?> arrayClass;

		SpreadHelper(Class<?> arrayClass, int spreadCount, MethodHandle mh) {
			this.arrayClass = arrayClass;
			this.spreadCount = spreadCount;
			nextMethodHandle = mh;
		}

		public Object helper(Object[] arguments) throws Throwable {
			/* Validate the spreadArgument */
			int lastArgIndex = arguments.length - 1;
			Object spreadArg = arguments[lastArgIndex];
			arrayClass.cast(spreadArg);
			if (spreadArg == null) {
				if (spreadCount != 0) {
					throw new IllegalArgumentException("cannot have null spread argument unless spreadCount is 0");
				}
			} else if (spreadCount != Array.getLength(spreadArg)) {
				throw new ArrayIndexOutOfBoundsException("expected '" + spreadCount +"' sized array; encountered '" + Array.getLength(spreadArg) + "' sized array");
			}

			/* copy all but the last arg into the new array and deal with the last arg separately */
			Object[] amendedArray = new Object[nextMethodHandle.type.parameterCount()];
			System.arraycopy(arguments, 0, amendedArray, 0, lastArgIndex);
			if (spreadCount != 0) {
				Class<?> componentType = arrayClass.getComponentType();
				if (!componentType.isPrimitive()) {
					System.arraycopy(spreadArg, 0, amendedArray, lastArgIndex, spreadCount);
				} else {
					boxArraycopy(spreadArg, componentType, amendedArray, lastArgIndex, spreadCount);
				}
			}
			return nextMethodHandle.invokeWithArguments(amendedArray);
		}

		static void boxArraycopy(Object spreadArg, Class<?> primitiveType, Object[] destination, int startPos, int length) {
			if (primitiveType.equals(int.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Integer.valueOf(((int[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(long.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Long.valueOf(((long[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(double.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Double.valueOf(((double[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(byte.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Byte.valueOf(((byte[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(char.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Character.valueOf(((char[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(float.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Float.valueOf(((float[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(short.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Short.valueOf(((short[])spreadArg)[i]);
				}
			} else if (primitiveType.equals(boolean.class)) {
				for (int i = 0; i < length; i++) {
					destination[startPos + i] = Boolean.valueOf(((boolean[])spreadArg)[i]);
				}
			}
		}
	}

	/**
	 * This method returns a method handle that delegates to the original method handle,
	 * adding a particular range of arguments (starting at a given location and
	 * with given types).  The type of the returned method handle is the type of the original handle
	 * with the given types dropped from the parameter type list at the given location.
	 *
	 * @param originalHandle - the original method handle to be transformed
	 * @param location -  the location of the first argument to be inserted
	 * @param values - an array of the argument types to be inserted
	 * @return a MethodHandle - representing a transformed handle as described above
	 */
	public static MethodHandle insertArguments(MethodHandle originalHandle, int location, Object... values) {
		MethodType originalType = originalHandle.type; // expected NPE if originalHandle is null
		Class<?>[] arguments = originalType.parameterArray();

		boolean noValuesToInsert = values.length == 0;  // expected NPE.  Must be null checked before location is checked.

		if ((location < 0) || (location >= originalType.parameterCount())) {
			throw new IllegalArgumentException();
		}

		if (noValuesToInsert) {
			// No values to insert
			return originalHandle;
		}

		// clone the values[] so it can't be modified after validation occurs
		values = values.clone();

		/* This loop does two things:
		 * 1) Validates that the 'values' can be legitimately inserted in originalHandle
		 * 2) Builds up the parameter array for the asType operation
		 */
		for (int i = 0; i < values.length; i++) { // expected NPE if values is null
			Class<?> clazz = arguments[location + i];
			Object value = values[i];
			Class<?> valueClazz = clazz;
			if (value != null) {
				valueClazz = value.getClass();
			}
			if (clazz.isPrimitive()) {
				Objects.requireNonNull(value);
				Class<?> unwrapped = MethodType.unwrapPrimitive(valueClazz);
				if ((clazz != unwrapped) && !FilterHelpers.checkIfWideningPrimitiveConversion(unwrapped, clazz)) {
					clazz.cast(value);	// guaranteed to throw ClassCastException
				}
			} else {
				clazz.cast(value);
			}
			// overwrite the original argument with the new class from the values[]
			arguments[location + i] = valueClazz;
		}
		originalHandle = originalHandle.asType(MethodType.methodType(originalType.returnType, arguments));

		MethodType mtype = originalType.dropParameterTypes(location, location + values.length);
		MethodHandle result = buildTransformHandle(new InsertHelper(location, values, originalHandle), mtype);

		return result;
	}

	/* A helper class for use by the insertArguments methods */
	private static final class InsertHelper implements ArgumentHelper {

		InsertHelper(int pos, Object[] values, MethodHandle mh) {
			this.pos = pos;
			this.values = values.clone();
			nextMethodHandle = mh;
		}

		private final MethodHandle nextMethodHandle;
		private final int pos;
		private final Object[] values;

		public Object helper(Object[] arguments) throws Throwable {
			int length = arguments.length + values.length;
			Object[] amendedArray = new Object[length];
			if (pos != 0) {
				System.arraycopy(arguments, 0, amendedArray, 0, pos);
			}
			System.arraycopy(values, 0, amendedArray, pos, values.length);
			if (pos != arguments.length) {
				System.arraycopy(arguments, pos, amendedArray, pos + values.length, arguments.length - pos);
			}
			return nextMethodHandle.asFixedArity().invokeWithArguments(amendedArray);
		}
	}

	private static final class GuardWithTestHelper implements ArgumentHelper {
		private final MethodHandle guard;
		private final MethodHandle trueTarget;
		private final MethodHandle falseTarget;

		GuardWithTestHelper(MethodHandle guard, MethodHandle trueTarget, MethodHandle falseTarget) {
			this.guard = guard;
			this.trueTarget = trueTarget;
			this.falseTarget = falseTarget;
		}

		@SuppressWarnings("boxing")
		public Object helper(Object[] arguments) throws Throwable {
			boolean result = (Boolean) guard.invokeWithArguments(Arrays.copyOfRange(arguments, 0, guard.type.parameterCount()));
			if (result) {
				return trueTarget.invokeWithArguments(arguments);
			}
			return falseTarget.invokeWithArguments(arguments);
		}
	}

	private static final class CatchHelper implements ArgumentHelper {
		private final MethodHandle tryTarget;
		private final MethodHandle catchTarget;
		private final Class<? extends Throwable> exceptionClass;

		CatchHelper(MethodHandle tryTarget, MethodHandle catchTarget, Class<? extends Throwable> exceptionClass) {
			this.tryTarget = tryTarget;
			this.catchTarget = catchTarget;
			this.exceptionClass = exceptionClass;
		}

		public Object helper(Object[] arguments) throws Throwable {
			try {
				return tryTarget.invokeWithArguments(arguments);
			} catch(Throwable t) {
				if (exceptionClass.isInstance(t)) {
					int catchArgCount = catchTarget.type.parameterCount();
					Object[] amendedArgs = new Object[catchArgCount];
					amendedArgs[0] = t;
					System.arraycopy(arguments, 0, amendedArgs, 1, catchArgCount - 1);
					return catchTarget.invokeWithArguments(amendedArgs);
				}
				throw t;
			}
		}
	}

	private static final class FilterHelper implements ArgumentHelper {
		private final MethodHandle target;
		private final int startPos;
		private final MethodHandle[] filters;

		FilterHelper(MethodHandle target, int startPos, MethodHandle[] filters) {
			this.target = target;
			this.startPos = startPos;
			this.filters = filters;
		}

		public Object helper(Object[] arguments) throws Throwable {
			Object[] filterArg = new Object[1];
			for (int i = 0; i < filters.length; i++) {
				MethodHandle filter = filters[i];
				if (filter != null) {
					filterArg[0] = arguments[startPos + i];
					arguments[startPos + i] = filter.invokeWithArguments(filterArg);
				}
			}
			return target.invokeWithArguments(arguments);
		}
	}

	private static final class FoldHelper implements ArgumentHelper {
		private final MethodHandle handle;
		private final MethodHandle preprocessor;

		FoldHelper(MethodHandle handle, MethodHandle preprocessor) {
			this.handle = handle;
			this.preprocessor = preprocessor;
		}

		public Object helper(Object[] arguments) throws Throwable {
			int length = arguments.length;
			Object[] newArgs = new Object[length + 1];
			if (length > 0) {
				System.arraycopy(arguments, 0, newArgs, 1, length);
			}
			newArgs[0] = preprocessor.invokeWithArguments(Arrays.copyOfRange(arguments, 0, preprocessor.type.parameterCount()));
			return handle.invokeWithArguments(newArgs);
		}
	}

	/* For use by foldArguments when the fold handle returns void */
	private static final class VoidFoldHelper implements ArgumentHelper {
		private final MethodHandle handle;
		private final MethodHandle preprocessor;

		VoidFoldHelper(MethodHandle handle, MethodHandle preprocessor) {
			this.handle = handle;
			this.preprocessor = preprocessor;
		}

		public Object helper(Object[] arguments) throws Throwable {
			preprocessor.invokeWithArguments(Arrays.copyOfRange(arguments, 0, preprocessor.type.parameterCount()));
			return handle.invokeWithArguments(arguments);
		}
	}

	private static final class PermuteHelper implements ArgumentHelper {
		private final MethodHandle handle;
		private final int[] permute;

		PermuteHelper(MethodHandle handle, int... permute) {
			this.handle = handle;
			this.permute = permute;
		}

		public Object helper(Object[] arguments) throws Throwable {
			Object[] newArgs = new Object[permute.length];
			for (int i = 0; i < permute.length; i++) {
				newArgs[i] = arguments[permute[i]];
			}
			return handle.invokeWithArguments(newArgs);
		}
	}
}
