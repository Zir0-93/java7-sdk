/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

// {{{ JIT support
import java.util.concurrent.ConcurrentHashMap;
import sun.misc.Unsafe;
// }}} JIT support

/**
 * A MethodHandle is a reference to a Java-level method.  It is typed according to the method's signature and can be
 * invoked in three ways:
 * <ol>
 * <li>invokeExact - using the exact signature match</li>
 * <li>invoke - using a signature with the same number of arguments</li>
 * <li>invokeWithArguments - using a Object array to hold the correct number of arguments</li>
 * </ol>
 * <p>
 * In the case of #invokeExact, if the arguments do not match, based on a check of the MethodHandle's {@link #type()},
 * a WrongMethodTypeException will be thrown.
 * <p>
 * In the case of #invoke, each of the arguments will be converted to the correct type, before the call is initiated.
 * If the conversion cannot occur, a WrongMethodTypeException will be thrown.
 * <p>
 * Similar to #invoke, #invokeWithArguments will convert each of the arguments and place them on the stack before
 * the call is initiated. If the conversion cannot occur, a WrongMethodTypeException will be thrown.
 * <p>
 * A MethodHandle can be created using the MethodHandles factory.
 *
 * @since 1.7
 */
@VMCONSTANTPOOL_CLASS
public abstract class MethodHandle {
	/* Ensure that these stay in sync with the builder constants: J9MemoryModel>>#_PRAGMA_J9VMMethodHandleConstants */
	/* Order matters here - static and special are direct pointers */
	static final int KIND_STATIC = 0;
	static final int KIND_SPECIAL = 1;
	static final int KIND_VIRTUAL = 2;
	static final int KIND_INTERFACE = 3;
	static final int KIND_BOUND = 4;
	static final int KIND_GETFIELD = 5;
	static final int KIND_GETSTATICFIELD = 6;
	static final int KIND_PUTFIELD = 7;
	static final int KIND_PUTSTATICFIELD = 8;
	static final int KIND_CONSTRUCTOR = 9;
	static final int KIND_COLLECT = 10;
	static final int KIND_INVOKEEXACT = 11;
	static final int KIND_INVOKEGENERIC = 12;
	static final int KIND_ASTYPE = 13;
	static final int KIND_DYNAMICINVOKER = 14;
	static final int KIND_FILTERRETURN = 15;
	static final int KIND_EXPLICITCAST = 16;
	static final int KIND_VARARGSCOLLECT = 17;
	static final int KIND_PASSTHROUGH = 18;
	static final private int NUMBER_OF_DISPATCH_TARGETS = 19;

	static final long vmDispatchTargets[];
	static {
		vmDispatchTargets = new long[NUMBER_OF_DISPATCH_TARGETS];
		if (!lookupVMDispatchTargets(vmDispatchTargets)) {
			throw new InternalError("MethodHandle.vmDispatchTargets unavailable"); //$NON-NLS-1$
		}
	}

	static Unsafe myUnsafe = null;

	static final Unsafe getUnsafe() {
		if (myUnsafe == null) {
			myUnsafe = Unsafe.getUnsafe();
		}
		return myUnsafe;
	}

	@VMCONSTANTPOOL_FIELD
	final MethodType type;		/* Type of the MethodHandle */
	@VMCONSTANTPOOL_FIELD
	long vmSlot;			/* Either the address of the method to be invoked or the {i,v}table index */
	@VMCONSTANTPOOL_FIELD
	final long vmDispatchTarget;	/* Address of handle-specific invoke code.  Can only be used when calling from interpreter */

	@VMCONSTANTPOOL_FIELD
	int rawModifiers;			/* Field/Method modifiers.  Currently only used by fields to determine if volatile */
	@VMCONSTANTPOOL_FIELD
	final int kind;				/* The kind (STATIC/SPECIAL/etc) of this MethodHandle */

	/* Used by staticFieldGetterDispatchTargets as the class passed to the readStatic
	 * object access barrier. */
	@VMCONSTANTPOOL_FIELD
	final Class definingClass;	/* Class defining the the method or field. */
	//TODO: rename?  the above class really refers to referenceClass

	@VMCONSTANTPOOL_FIELD
	Class defc;	/* Used by security check */

	// {{{ JIT support

	final ThunkTuple thunks;
	abstract ThunkTable thunkTable();
	ThunkTuple computeThunks(Object arg) {
		// Must be overridden for MethodHandle classes with different shareability criteria
		return thunkTable().get(new ThunkKey(ThunkKey.computeThunkableSignature(type().toMethodDescriptorString())));
	}

	final long invokeExactTargetAddress() {
		return thunks.invokeExactThunk();
	}

	// TODO markers
	final static int VTABLE_ENTRY_SIZE = addressSize();
	final static int J9CLASS_OFFSET = vmRefFieldOffset();
  	final static int INTRP_VTABLE_OFFSET = vtableOffset();
	final static int HEADER_SIZE = objectOffset();

	static {
		// TODO:JSR292: Get rid of these if possible
		ComputedCalls.load();
		ThunkKey.load();
		ThunkTuple.load();
		ILGenMacros.load();
	}

	static long getJ9ClassFromClass(Class c) {
		if (VTABLE_ENTRY_SIZE == 4) {
			return getUnsafe().getInt(c, J9CLASS_OFFSET); // it's in the low-address word, not the low-order word
		} else {
			return getUnsafe().getLong(c, J9CLASS_OFFSET);
		}
	}

	/**
	 * Creates a new MethodHandle of the same kind that
	 * contains all the same fields, except the type is
	 * different.  Different from asType because no checks
	 * are done, the caller is responsible for determining
	 * if the operation will succeed.  Allows us to skip asType
	 * handles for reference castings we always know will succeed
	 * (such as Type to Object conversions)
	 * @param newType the new method type for this handle
	 * @returns a new MethodHandle with the newType expressed
	 */
	abstract MethodHandle cloneWithNewType(MethodType newType);

	private static MethodHandle asType(MethodHandle mh, MethodType newType) { return mh.asType(newType); }

	// }}} JIT support

	final String name;			/* Name used to look up method */
	Class specialCaller; 		/* Class used as part of lookup for KIND_SPECIAL*/

	MethodHandle(MethodType type, Class definingClass, String name, int kind, Object thunkArg) {
		this.definingClass = definingClass;
		this.defc = definingClass;
		this.name = name;
		this.kind = kind;
		this.vmDispatchTarget = vmDispatchTargets[kind];
		/* Must be called last as it may use previously set fields to modify the MethodType */
		this.type = computeHandleType(type);
		enforceArityLimit(this.type);
		/* Must be called even laster as it uses the method type */
		this.thunks = computeThunks(thunkArg);
		/* Force this to be initialized here, rather than in an thunkArchetype frame */
		getUnsafe();
	}

	MethodHandle(MethodHandle original, MethodType newType) {
		this.definingClass = original.definingClass;
		this.defc = original.defc;
		this.name = original.name;
		this.kind = original.kind;
		this.vmDispatchTarget = original.vmDispatchTarget;
		this.type = newType;
		enforceArityLimit(newType);
		this.thunks = original.thunks;
		this.rawModifiers = original.rawModifiers;
		this.specialCaller = original.specialCaller;
		this.vmSlot = original.vmSlot;
		this.previousAsType = original.previousAsType;
	}

	/*
	 * Marker interface for javac to recognize the polymorphic signature of the annotated methods.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface PolymorphicSignature{};

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})
	@interface Invisible{};

	/**
	 * Invoke the receiver MethodHandle against the supplied arguments.  The types of the arguments
	 * must be an exact match for the MethodType of the MethodHandle.
	 *
	 * @return The return value of the method
	 * @throws Throwable - To ensure type safety, must be marked as throwing Throwable.
	 * @throws WrongMethodTypeException - If the resolved method type is not exactly equal to the MethodHandle's type
	 */
	public final native @PolymorphicSignature Object invokeExact(Object... args) throws Throwable, WrongMethodTypeException;

	/**
	 * Invoke the receiver MethodHandle against the supplied arguments.  If the types of the arguments
	 * are not an exact match for the MethodType of the MethodHandle, conversions will be applied as
	 * possible.  The signature and the MethodHandle's MethodType must have the same number of arguments.
	 *
	 * @return The return value of the method.  May be converted according to the conversion rules.
	 * @throws Throwable - To ensure type safety, must be marked as throwing Throwable.
	 * @throws WrongMethodTypeException  - If the resolved method type cannot be converted to the MethodHandle's type
	 * @throws ClassCastException - if a conversion fails
	 */
	public final native @PolymorphicSignature Object invoke(Object... args) throws Throwable, WrongMethodTypeException, ClassCastException;

	/*
	 * Wrapper on VM method lookup logic.
	 *
	 * 	definingClass - class that lookup is against
	 *  name - method name
	 *  signature - method signature
	 *  specialCaller - class that invokespecial is against or null
	 *
	 *  Sets the vmSlot to hold the correct value for the kind of MethodHandle:
	 *  	KIND_STATIC			-	J9Method address
	 *  	KIND_SPECIAL		-	J9Method address
	 *  	KIND_VIRTUAL		-	Vtable index
	 *  	KIND_INTERFACE		-	Itable index
	 *  	KIND_CONSTRUCTOR	-	J9Method address
	 */
	private native Class lookupMethod(Class definingClass, String name, String signature, int kind, Class specialCaller);

	/*
	 * Wrapper on VM field lookup logic.
	 *
	 *  definingClass - class that lookup is against
	 *  name - field name
	 *  signature - field signature
	 *  accessClass is the MethodHandles.Lookup().lookupClass().
	 *
	 *  Sets the vmSlot to hold the correct value for the kind of MethodHandle:
	 *  	KIND_GETFEILD		-	field offset
	 *  	KIND_GETSTATICFIELD	-	field address
	 *  	KIND_PUTFIELD		-	field offset
	 *  	KIND_PUTSTATICFIELD	-	field address
	 */
	final native Class lookupField(Class definingClass, String name, String signature, int kind, Class accessClass);

	/*
	 * Rip apart a Field object that points to a field and fill in the
	 * vmSlot of the MH.  The vmSlot for an instance field is the
	 * J9JNIFieldID->offset.  For a static field, its the
	 * J9JNIField->offset + declaring classes ramStatics start address.
	 */
	static native boolean setVMSlotAndRawModifiersFromField(MethodHandle handle, Field field);

	/*
	 * Rip apart a Method object and fill in the vmSlot of the MH.
	 * The vmSlot for a method depends on the kind of method:
	 *  - J9Method address for static and special
	 *  - vtable index for virtual
	 *  - itable index for interface.
	 */
	static native boolean setVMSlotAndRawModifiersFromMethod(MethodHandle handle, Class declaringClass, Method method, int kind, Class specialToken);

	/*
	 * Rip apart a Constructor object and fill in the vmSlot of the MH.
	 * The vmSlot for an <init> method is the address of the J9Method.
	 */
	static native boolean setVMSlotAndRawModifiersFromConstructor(MethodHandle handle, Constructor ctor);

	/*
	 * Lookup all the VMDispatchTargets and store them into 'targets' array.
	 * 'targets' must be large enough to hold all the dispatch targets.
	 *
	 */
	private static native boolean lookupVMDispatchTargets(long[] targets);

	/*
	 * Should be called by the constructor.  This method is used
	 * to convert the incoming MethodType into the correct
	 * MethodType for the MethodHandle.  Its simply a convention
	 * to allow seeing what the handle conversion will be.
	 */
	MethodType computeHandleType(MethodType incomingType) {
		return incomingType;
	}

	/*
	 * Set the VMSlot for the VirtualHandle from the DirectHandle.  DirectHandle must be of KIND_SPECIAL.
	 * Necessary to deal with MethodHandles#findVirtual() being allowed to look up private methods.
	 * Does not do access checking as the access check should already have occurred when creating the
	 * DirectHandle.
	 */
	static native boolean setVMSlotAndRawModifiersFromSpecialHandle(VirtualHandle handle, DirectHandle specialHandle);

	/*
	 * Finish initialization of the receiver and return the defining class of the method it represents.
	 */
	final Class finishMethodInitialization(Class specialToken, MethodType oldType) throws NoSuchMethodException, IllegalAccessException {
		try {
			String signature = oldType.toMethodDescriptorString();
			return lookupMethod(definingClass, name, signature, kind, specialToken);
		} catch (NoSuchMethodError e) {
			throw new NoSuchMethodException(e.getMessage());
		} catch (IncompatibleClassChangeError e) {
			throw new IllegalAccessException(e.getMessage());
		}
	}

	/**
     * The MethodType of the MethodHandle.  Invocation must match this MethodType.
     *
	 * @return the MethodType of the MethodHandle.
     */
	public MethodType type() {
		return type;
	}

	/**
	 * Produce a MethodHandle that has an array of type <i>arrayClass</i> as its last argument and replaces the
	 * array with <i>spreadCount</i> arguments from the array before calling the original MethodHandle.  The
	 * MethodType of new the methodhandle and the original methodhandle will differ only in the regards to the
	 * last arguments.
	 * <p>
	 * The array must contain exactly <i>spreadCount</i> arguments to be passed to the original methodhandle.  The array
	 * may be null in the case when <i>spreadCount</i> is zero.  Incorrect argument array size will cause the method to
	 * throw an <code>IllegalArgumentException</code> instead of invoking the target.
	 *
	 * @param arrayClass - the source array for the spread arguments
	 * @param spreadCount - how many arguments to spread from the arrayClass
	 * @return a MethodHandle able to replace to the last parameter with <i>spreadCount</i> number of arguments
	 * @throws IllegalArgumentException - if arrayClass is not an array, the methodhandle has too few or too many parameters to satisfy spreadCount
	 * @throws WrongMethodTypeException - if it cannot convert from one MethodType to the new type.
	 */
	public MethodHandle asSpreader(Class<?> arrayClass, int spreadCount) throws IllegalArgumentException, WrongMethodTypeException {
		if (!arrayClass.isArray()) {
			throw new IllegalArgumentException();
		}
		if ((spreadCount < 0) || (spreadCount > type.parameterCount())) {
			throwIllegalArgumentExceptionForMHArgCount();
		}
		MethodHandle adapted = this;
		MethodType collectType;
		if (spreadCount == 0){
			collectType = type.appendParameterTypes(arrayClass);
		} else {
			final int length = type.parameterCount();
			Class<?> componentType = arrayClass.getComponentType();

			collectType = type.changeParameterType(length - spreadCount, arrayClass);
			if (spreadCount > 1) {
				/* Drop the remaining parameters */
				collectType = collectType.dropParameterTypes(length + 1 - spreadCount, length);
			}

			Class<?>[] parameters = type.arguments.clone();
			Arrays.fill(parameters, length - spreadCount, length, componentType);
			adapted = asType(MethodType.methodType(type.returnType, parameters));
		}
		return MethodHandles.spreadHelper(arrayClass, spreadCount, adapted, collectType);
	}

	/**
	 * Returns a MethodHandle that collects the requested incoming arguments, which must match the
	 * types in MethodType incomingArgs, into an array of <i>arrayClass</i>, called T.
	 *
	 * This method can only be called on MethodHandles that have type() such that their last parameter
	 * can be assigned to from an instance of <i>arrayClass</i>.  An IllegalArgumentException will be
	 * thrown if this is not the case.
	 *
	 * This take a MH with type (Something, Something, K)R and presents a MethodType with the form
	 * (Something, Something, T, T, T)R. Where K is assignable to from an array of <i>arrayClass</i> T.
	 *
	 * @param arrayClass - the class of the collect array.  Usually matches the type of the last argument.
	 * @param collectCount - the number of arguments of type 'T' to collect
	 * @return a MethodHandle which will collect <i>collectCount</i> arguments and pass them as the final argument
	 *
	 * @throws IllegalArgumentException if arrayClass is not an array or is not assignable to the last parameter of the MethodHandle, or collectCount is an invalid array size (less than 0 or more than 254)
	 * @throws WrongMethodTypeException if an asType call would fail when converting the final parameter to arrayClass
	 * @throws NullPointerException if arrayClass is null
	 */
	public MethodHandle asCollector(Class<?> arrayClass, int collectCount) throws IllegalArgumentException, WrongMethodTypeException, NullPointerException {
		if ((type.parameterCount() == 0) || (collectCount < 0)) {
			throw new IllegalArgumentException();
		}
		if (arrayClass == null) {
			throw new NullPointerException();
		}
		int index = type.parameterCount() - 1;
		Class<?> lastClass = type.parameterType(index);
		if (!arrayClass.isArray() || !lastClass.isAssignableFrom(arrayClass)) {
			throw new IllegalArgumentException("Cannot assign '" + arrayClass + "' to methodtype '" + type +"'");
		}
		return new CollectHandle(asType(type.changeParameterType(index, arrayClass)), collectCount);
	}

	private MethodHandle previousAsType;

	/**
	 * Returns a MethodHandle that presents as being of MethodType newType.  It will
	 * convert the arguments used to match type().  If a conversion is invalid, a
	 * ClassCastException will be thrown.
	 *
	 * If newType == type(), then the original MethodHandle may be returned.
	 *
	 * TODO: Describe the type conversion rules here.
	 * If the return type T1 is void, any returned value is discarded
	 * If the return type T0 is void and T1 a reference, a null value is introduced.
	 * If the return type T0 is void and T1 a primitive, a zero value is introduced.
	 *
	 * @param newType the MethodType for invoking this method with
	 * @return A MethodHandle with MethodType newType
	 *
	 * @throws ClassCastException if any of the requested coercions are invalid.
	 */
	public MethodHandle asType(MethodType newType) throws ClassCastException {
		if (this.type.equals(newType)) {
			return this;
		}
		MethodHandle localPreviousAsType = previousAsType;
		if ((localPreviousAsType != null) && (localPreviousAsType.type == newType)) {
			return localPreviousAsType;
		}
		MethodHandle handle = this;
		Class<?> fromReturn = type.returnType;
		Class<?> toReturn = newType.returnType;
		if (fromReturn != toReturn) {
			MethodHandle filter = ConvertHandle.FilterHelpers.getReturnFilter(fromReturn, toReturn, false);
			handle = new FilterReturnHandle(this, filter);
		}
		if (handle.type != newType) {
			handle = new AsTypeHandle(handle, newType);
		}
		previousAsType = handle;
		return handle;
	}

	private static final native int vtableOffset();
	private static final native int objectOffset();
	private static final native int addressSize();
	private static final native int vmRefFieldOffset();

	/**
	 * Invoke the MethodHandle using an Object[] of arguments.  The array must contain at exactly type().parameterCount() arguments.
	 *
	 * Each of the arguments in the array with be coerced to the appropriate type, if possible, based on the MethodType.
	 *
	 * @param args An array of Arguments, with length at exactly type().parameterCount() to be used in the call.
	 * @return An Object
	 *
	 * @throws Throwable May throw anything depending on the receiver MethodHandle.
	 * @throws WrongMethodTypeException if the target cannot be adjusted to the number of Objects being passed
	 * @throws ClassCastException if an argument cannot be converted
	 */
	public Object invokeWithArguments(Object... args) throws Throwable, WrongMethodTypeException, ClassCastException {

		return invokeWithArgumentsHelper(asType(MethodType.genericMethodType(args.length)), args);
	}

	private static native Object invokeWithArgumentsHelper(MethodHandle mh, Object[] args);

	/**
	 * Helper method to call {@link #invokeWithArguments(Object[])}.
	 *
	 * @param args - An array of arguments, with length at exactly type().parameterCount() to be used in the call.
	 * @return An Object
	 * @throws Throwable May throw anything depending on the receiver MethodHandle.
	 * @throws WrongMethodTypeException if the target cannot be adjusted to the number of Objects being passed
	 * @throws ClassCastException if an argument cannot be converted
	 * @throws NullPointerException if the args list is null
	 */
	public Object invokeWithArguments(List<?> args) throws Throwable, WrongMethodTypeException, ClassCastException, NullPointerException {
		return invokeWithArguments(args.toArray());
	}

	/**
	 * Create an varargs collector adapter on this MethodHandle.
	 *
	 * For {@link #asVarargsCollector(Class)} MethodHandles, <i>invokeExact</i> requires that the arguments
	 * exactly match the underlying MethodType.
	 * <p>
	 * <i>invoke</i> acts as normally unless the arities differ.  In that case, the trailing
	 * arguments are converted as though by a call to {@link #asCollector(int)} before invoking the underlying
	 * methodhandle.
	 *
	 * @param arrayParameter - the type of the array to collect the arguments into
	 * @return a varargs-collector methodhandle.
	 * @throws IllegalArgumentException - if the arrayParameter is not an array class or cannot be assigned to the last parameter of the MethodType
	 */
	public MethodHandle	asVarargsCollector(Class<?> arrayParameter) throws IllegalArgumentException {
		if (!arrayParameter.isArray()) {
			throw new IllegalArgumentException();
		}
		int lastArgPos = type().parameterCount() - 1;
		Class lastArgType = type().parameterType(lastArgPos);
		if (!lastArgType.isAssignableFrom(arrayParameter)) {
			throw new IllegalArgumentException();
		}
		return new VarargsCollectorHandle(this, arrayParameter);
	}

	/**
	 * Determine whether this is an {@link #asVarargsCollector(Class)} MethodHandle.
	 *
	 * @return true if an {@link #asVarargsCollector(Class)} handle, false otherwise.
	 */
	public boolean isVarargsCollector() {
		return (this instanceof VarargsCollectorHandle);
	}

	/**
	 * Return a fixed arity version of the current MethodHandle.
	 *
	 * <p>
	 * This is identical to the current method handle if {@link #isVarargsCollector()} is false.
	 * <p>
	 * If the current method is a varargs collector, then the returned handle will be the same
	 * but without the varargs nature.
	 *
	 * @return a fixed arity version of the current method handle
	 */
	public MethodHandle asFixedArity() {
		return this;
	}

	public MethodHandle bindTo(Object value) throws IllegalArgumentException, ClassCastException {
		/*
		 * Check whether the first parameter has a reference type assignable from value. Note that MethodType.parameterType(0) will
		 * throw an IllegalArgumentException if type has no parameters.
		 */
		Class<?> firstParameterType = type().parameterType(0);
		if (firstParameterType.isPrimitive()) {
			throw new IllegalArgumentException();
		}

		/*
		 * Ensure type compatibility.
		 */
		value = firstParameterType.cast(value);

		/*
		 * A DirectHandle can be wrapped in a ReceiverBoundHandle, but a ReceiverBoundHandle cannot be wrapped in another ReceiverBoundHandle.
		 */
		if (getClass() == DirectHandle.class) {
			return new ReceiverBoundHandle(this, value);
		}

		/*
		 * Devirtualize virtual/interface handles.
		 */
		if ((value != null) && this instanceof IndirectHandle) {
			try {
				return new MethodHandles.Lookup(value.getClass(), false).bind(value, name, type().dropParameterTypes(0, 1));
			} catch(IllegalAccessException e) {
				throw new Error(e);
			} catch (NoSuchMethodException e) {
				throw new Error(e);
			}
		}

		/*
		 * Binding the first argument of any other kind of MethodHandle is equivalent to calling MethodHandles.insertArguments.
		 */
		return MethodHandles.insertArguments(this, 0, value);
	}

	/*
	 * Return the result of J9_CP_TYPE(J9Class->romClass->cpShapeDescription, index)
	 */
	private static final native int getCPTypeAt(Class clazz, int index);

	/*
	 * sun.reflect.ConstantPool doesn't have a getMethodTypeAt method.  This is the
	 * equivalent for MethodType.
	 */
	private static final native MethodType getCPMethodTypeAt(Class clazz, int index);

	/*
	 * sun.reflect.ConstantPool doesn't have a getMethodHandleAt method.  This is the
	 * equivalent for MethodHandle.
	 */
	private static final native MethodHandle getCPMethodHandleAt(Class clazz, int index);

	private static final int BSM_ARGUMENT_SIZE = Short.SIZE / Byte.SIZE;
	private static final int BSM_ARGUMENT_COUNT_OFFSET = BSM_ARGUMENT_SIZE;
	private static final int BSM_ARGUMENTS_OFFSET = BSM_ARGUMENT_SIZE * 2;
	private static final int BSM_LOOKUP_ARGUMENT_INDEX = 0;
	private static final int BSM_NAME_ARGUMENT_INDEX = 1;
	private static final int BSM_TYPE_ARGUMENT_INDEX = 2;
	private static final int BSM_OPTIONAL_ARGUMENTS_START_INDEX = 3;

	@SuppressWarnings("unused")
	private static MethodHandle resolveInvokeDynamic(Class clazz, String name, String methodDescriptor, long bsmData) {
		sun.misc.Unsafe unsafe = getUnsafe();
		MethodHandle result = null;
		MethodType type = null;

		try {
			type = MethodType.fromMethodDescriptorString(methodDescriptor, clazz.getClassLoader());
			int bsmIndex = unsafe.getShort(bsmData);
			int bsmArgCount = unsafe.getShort(bsmData + BSM_ARGUMENT_COUNT_OFFSET);
			long bsmArgs = bsmData + BSM_ARGUMENTS_OFFSET;
			MethodHandle bsm = getCPMethodHandleAt(clazz, bsmIndex);
			if (null == bsm) {
				throw new NullPointerException("unable to resolve 'bootstrap_method_ref' in '" + clazz + "' at index " + bsmIndex);
			}
			Object[] staticArgs = new Object[BSM_OPTIONAL_ARGUMENTS_START_INDEX + bsmArgCount];
			/* Mandatory arguments */
			staticArgs[BSM_LOOKUP_ARGUMENT_INDEX] = new MethodHandles.Lookup(clazz, false);
			staticArgs[BSM_NAME_ARGUMENT_INDEX] = name;
			staticArgs[BSM_TYPE_ARGUMENT_INDEX] = type;

			/* Static optional arguments */
			sun.reflect.ConstantPool cp = sun.misc.SharedSecrets.getJavaLangAccess().getConstantPool(clazz);

			/* Check if we need to treat the last parameter specially when handling primitives.
			 * The type of the varargs array will determine how primitive ints from the constantpool
			 * get boxed: {Boolean, Byte, Short, Character or Integer}.
			 */
			boolean treatLastArgAsVarargs = bsm.isVarargsCollector();
			Class<?> varargsComponentType = bsm.type.lastParameterType().getComponentType();
			int bsmTypeArgCount = bsm.type.parameterCount();

			for (int i = 0; i < bsmArgCount; i++) {
				int staticArgIndex = BSM_OPTIONAL_ARGUMENTS_START_INDEX + i;
				short index = unsafe.getShort(bsmArgs + (i * BSM_ARGUMENT_SIZE));
				int cpType = getCPTypeAt(clazz, index);
				Object cpEntry = null;
				switch (cpType) {
				case 1:
					cpEntry = cp.getClassAt(index);
					break;
				case 2:
					cpEntry = cp.getStringAt(index);
					break;
				case 3: {
					int cpValue = cp.getIntAt(index);
					Class<?> argClass;
					if (treatLastArgAsVarargs && (staticArgIndex >= (bsmTypeArgCount - 1))) {
						argClass = varargsComponentType;
					} else {
						argClass = bsm.type().parameterType(staticArgIndex);
					}
					if (argClass == Short.TYPE) {
						cpEntry = (short) cpValue;
					} else if (argClass == Boolean.TYPE) {
						cpEntry = cpValue == 0 ? Boolean.FALSE : Boolean.TRUE;
					} else if (argClass == Byte.TYPE) {
						cpEntry = (byte) cpValue;
					} else if (argClass == Character.TYPE) {
						cpEntry = (char) cpValue;
					} else {
						cpEntry = cpValue;
					}
					break;
				}
				case 4:
					cpEntry = cp.getFloatAt(index);
					break;
				case 5:
					cpEntry = cp.getLongAt(index);
					break;
				case 6:
					cpEntry = cp.getDoubleAt(index);
					break;
				case 13:
					cpEntry = getCPMethodTypeAt(clazz, index);
					break;
				case 14:
					cpEntry = getCPMethodHandleAt(clazz, index);
					break;
				default:
					// Do nothing. The null check below will throw the appropriate exception.
				}

				if (cpEntry == null) {
					throw new NullPointerException();
				}

				staticArgs[staticArgIndex] = cpEntry;
			}

			java.lang.invoke.CallSite cs = (java.lang.invoke.CallSite) bsm.invokeWithArguments(staticArgs);
			if (cs != null) {
				if (!cs.getTarget().type().equals(type)) {
					throw new WrongMethodTypeException();
				}
				result = cs.dynamicInvoker();
			}
		} catch(Throwable e) {
			if (type == null) {
				throw new BootstrapMethodError();
			}

			try {
				/* create an exceptionHandle with appropriate drop adapter and install that */
				MethodHandle thrower = MethodHandles.throwException(type.returnType(), BootstrapMethodError.class);
				MethodHandle constructor = MethodHandles.lookup().findConstructor(BootstrapMethodError.class, MethodType.methodType(void.class, Throwable.class));
				result = MethodHandles.foldArguments(thrower, constructor.bindTo(e));
				result = MethodHandles.dropArguments(result, 0, type.parameterList());
			} catch (IllegalAccessException iae) {
				throw new Error(iae);
			} catch (NoSuchMethodException nsme) {
				throw new Error(nsme);
			}
		}

		return result;
	}

	@Override
	public String toString() {
		return "MethodHandle" + type.toString(); //$NON-NLS-1$
	}

	/*
	 * Used to convert an invokehandlegeneric bytecode into an AsTypeHandle + invokeExact OR to
	 * convert an InvokeGenericHandle into an AsTypeHandle.
	 *
	 * Allows us to only have the conversion logic for the AsTypeHandle and not worry about any
	 * other similar conversions.
	 */
	@SuppressWarnings("unused")  /* Used by builder */
	@VMCONSTANTPOOL_METHOD
	private final MethodHandle forGenericInvoke(MethodType newType, boolean dropFirstArg){
		if (this.type.equals(newType)) {
			return this;
		}
		if (dropFirstArg) {
			return asType(newType.dropParameterTypes(0, 1));
		}
		return asType(newType);
	}

	@SuppressWarnings("unused")
	@VMCONSTANTPOOL_METHOD
	private static final MethodHandle sendResolveMethodHandle(
			int cpRefKind,
			Class currentClass,
			Class definingClass,
			String name,
			String typeDescriptor,
			ClassLoader loader) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException, NoSuchMethodException {
		MethodHandles.Lookup lookup = new MethodHandles.Lookup(currentClass, false);
		MethodType type = null;

		switch(cpRefKind){
		case 1: /* getField */
			return lookup.findGetter(definingClass, name, resolveFieldHandleHelper(typeDescriptor, loader));
		case 2: /* getStatic */
			return lookup.findStaticGetter(definingClass, name, resolveFieldHandleHelper(typeDescriptor, loader));
		case 3: /* putField */
			return lookup.findSetter(definingClass, name, resolveFieldHandleHelper(typeDescriptor, loader));
		case 4: /* putStatic */
			return lookup.findStaticSetter(definingClass, name, resolveFieldHandleHelper(typeDescriptor, loader));
		case 5: /* invokeVirtual */
			type = MethodType.fromMethodDescriptorString(typeDescriptor, loader);
			return lookup.findVirtual(definingClass, name, type);
		case 6: /* invokeStatic */
			type = MethodType.fromMethodDescriptorString(typeDescriptor, loader);
			return lookup.findStatic(definingClass, name, type);
		case 7: /* invokeSpecial */
			type = MethodType.fromMethodDescriptorString(typeDescriptor, loader);
			return lookup.findSpecial(definingClass, name, type, currentClass);
		case 8: /* newInvokeSpecial */
			type = MethodType.fromMethodDescriptorString(typeDescriptor, loader);
			return lookup.findConstructor(definingClass, type);
		case 9: /* invokeInterface */
			type = MethodType.fromMethodDescriptorString(typeDescriptor, loader);
			return lookup.findVirtual(definingClass, name, type);
		}
		/* Can never happen */
		throw new UnsupportedOperationException();
	}

	/* Convert the field typedescriptor into a MethodType so we can reuse the parsing logic in
	 * #fromMethodDescritorString().  The verifier checks to ensure that the typeDescriptor is
	 * a valid field descriptor so adding the "()V" around it is valid.
	 */
	private static final Class<?> resolveFieldHandleHelper(String typeDescriptor, ClassLoader loader) {
		MethodType mt = MethodType.fromMethodDescriptorString("(" + typeDescriptor + ")V", loader); //$NON-NLS-1$ //$NON-NLS-2$
		return mt.parameterType(0);
	}

	@SuppressWarnings("unused")
	@VMCONSTANTPOOL_METHOD
	private MethodHandle returnFilterPlaceHolder() {
		return this;
	}

	@SuppressWarnings("unused")
	@VMCONSTANTPOOL_METHOD
	private static Object constructorPlaceHolder(Object newObjectRef) {
		return newObjectRef;
	}

	static void enforceArityLimit(MethodType type) {
		if (type.argSlots > 254) {
			throwIllegalArgumentExceptionForMTArgCount(type.argSlots);
		}
	}

	static void throwIllegalArgumentExceptionForMTArgCount(int argSlots) {
		// K0578 = MethodHandle would consume more than 255 argument slots ("{0}")
		throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0578", argSlots + 1)); //$NON-NLS-1$
	}

	static void throwIllegalArgumentExceptionForMHArgCount() {
		// K0579 = The MethodHandle has too few or too many parameters
		throw new IllegalArgumentException(com.ibm.oti.util.Msg.getString("K0579")); //$NON-NLS-1$
	}
}

// {{{ JIT support
class ComputedCalls {
	// Calls to these methods will get a "Computed Method" symref, and will in general have different arguments from those shown below

	public static native void     dispatchDirect_V(long address, int argPlaceholder);
	public static native int      dispatchDirect_I(long address, int argPlaceholder);
	public static native long     dispatchDirect_J(long address, int argPlaceholder);
	public static native float    dispatchDirect_F(long address, int argPlaceholder);
	public static native double   dispatchDirect_D(long address, int argPlaceholder);
	public static native Object   dispatchDirect_L(long address, int argPlaceholder);

	public static native void     dispatchVirtual_V(long address, long vtableIndex, Object receiver, int argPlaceholder);
	public static native int      dispatchVirtual_I(long address, long vtableIndex, Object receiver, int argPlaceholder);
	public static native long     dispatchVirtual_J(long address, long vtableIndex, Object receiver, int argPlaceholder);
	public static native float    dispatchVirtual_F(long address, long vtableIndex, Object receiver, int argPlaceholder);
	public static native double   dispatchVirtual_D(long address, long vtableIndex, Object receiver, int argPlaceholder);
	public static native Object   dispatchVirtual_L(long address, long vtableIndex, Object receiver, int argPlaceholder);

	// These ones aren't computed calls, but are so similar to the computed calls, I thought I'd throw them in here.
	public static native void     dispatchJ9Method_V(long j9method, int argPlaceholder);
	public static native int      dispatchJ9Method_I(long j9method, int argPlaceholder);
	public static native long     dispatchJ9Method_J(long j9method, int argPlaceholder);
	public static native float    dispatchJ9Method_F(long j9method, int argPlaceholder);
	public static native double   dispatchJ9Method_D(long j9method, int argPlaceholder);
	public static native Object   dispatchJ9Method_L(long j9method, int argPlaceholder);
	public static native void     dispatchJ9Method_V(long j9method, Object objectArg, int argPlaceholder);
	public static native int      dispatchJ9Method_I(long j9method, Object objectArg, int argPlaceholder);
	public static native long     dispatchJ9Method_J(long j9method, Object objectArg, int argPlaceholder);
	public static native float    dispatchJ9Method_F(long j9method, Object objectArg, int argPlaceholder);
	public static native double   dispatchJ9Method_D(long j9method, Object objectArg, int argPlaceholder);
	public static native Object   dispatchJ9Method_L(long j9method, Object objectArg, int argPlaceholder);

	public static void load(){}
}

class ThunkKey {
	public static String computeThunkableSignature(String signature) // upcast the whole signature
		{ return computeThunkableSignature(signature, signature.length()); }

	public static String computeThunkableSignature(String signature, int stopIndex) // stop upcasting arguments at stopIndex, but upcast return type
		{ return computeThunkableSignature(signature, stopIndex, signature.indexOf(')')); }

	public static String computeThunkableSignature(String signature, int stopIndex, int restartIndex) { // don't upcast anything between stopIndex and restartIndex-1
		// "Upcast" here means:
		// - All object types -> Ljava/lang/Object;
		// - Small int types  -> I
		StringBuilder sb = new StringBuilder();
		int i;
		int signatureLength = signature.length();
		for (i = 0; i < signatureLength; i++) {
			if (i == stopIndex) { // Don't upcast this part of the signature
				sb.append(signature, i, restartIndex);
				i = restartIndex-1;
				continue;
			}
			char c = signature.charAt(i);
			switch (c) {
				case '[':
				case 'L':
					sb.append("Ljava/lang/Object;");
					// Eat the rest of the type name
					while (signature.charAt(i) == '[')
						i++;
					if (signature.charAt(i) == 'L')
						while (signature.charAt(i) != ';')
							i++;
					break;
				case 'Z':
				case 'B':
				case 'C':
				case 'S':
					sb.append('I');
					break;
				default:
					sb.append(c);
					break;
			}
		}
		sb.append(signature, i, signatureLength);
		return sb.toString();
	}

	public boolean equals(Object other) {
		// Note the double-dispatch pattern here.  We effectively consult both
		// ThunkKeys to give either of them a chance to declare themselves unequal.
		if (other instanceof ThunkKey)
			return ((ThunkKey)other).equalsThunkKey(this);
		else
			return false;
	}

	public int hashCode() {
		return thunkableSignature.hashCode();
	}

	protected boolean equalsThunkKey(ThunkKey other) {
		return thunkableSignature.equals(other.thunkableSignature);
	}

	protected final String thunkableSignature;
	public final String thunkableSignature(){ return thunkableSignature; }

	public ThunkKey(String thunkableSignature){ this.thunkableSignature = thunkableSignature; }

	public static void load(){}
}

class UnshareableThunkKey extends ThunkKey {

	public UnshareableThunkKey(String thunkableSignature){ super(thunkableSignature); }

	// Whether this ThunkKey is on the left or right side of the equals call,
	// we want it to return false.
	public    boolean equals(Object other) { return false; }
	protected boolean equalsThunkKey(ThunkKey other) { return false; }

}

class ThunkKeyWithInt extends ThunkKey {
	private final int extraInt;
	public ThunkKeyWithInt(String thunkableSignature, int extraInt){ super(thunkableSignature); this.extraInt = extraInt; }

	public boolean equals(Object other) {
		if (other instanceof ThunkKeyWithInt)
			return ((ThunkKeyWithInt)other).equalsThunkKeyWithInt(this);
		else
			return false;
	}

	protected final boolean equalsThunkKeyWithInt(ThunkKeyWithInt other) {
		return equalsThunkKey(other) && extraInt == other.extraInt;
	}

	public int hashCode() {
		return super.hashCode() ^ (int)extraInt;
	}
}

class ThunkKeyWithLong extends ThunkKey {
	private final long extraLong;
	public ThunkKeyWithLong(String thunkableSignature, long extraLong){ super(thunkableSignature); this.extraLong = extraLong; }

	public boolean equals(Object other) {
		if (other instanceof ThunkKeyWithLong)
			return ((ThunkKeyWithLong)other).equalsThunkKeyWithLong(this);
		else
			return false;
	}

	protected final boolean equalsThunkKeyWithLong(ThunkKeyWithLong other) {
		return equalsThunkKey(other) && extraLong == other.extraLong;
	}

	public int hashCode() {
		return super.hashCode() ^ (int)extraLong ^ (int)(extraLong >> 32);
	}
}

class ThunkKeyWithObject extends ThunkKey {
	private final Object extraObject;
	public ThunkKeyWithObject(String thunkableSignature, Object extraObject){ super(thunkableSignature); this.extraObject = extraObject; }

	public boolean equals(Object other) {
		if (other instanceof ThunkKeyWithObject)
			return ((ThunkKeyWithObject)other).equalsThunkKeyWithObject(this);
		else
			return false;
	}

	protected final boolean equalsThunkKeyWithObject(ThunkKeyWithObject other) {
		return equalsThunkKey(other) && extraObject.equals(other.extraObject);
	}

	public int hashCode() {
		return super.hashCode() ^ extraObject.hashCode();
	}
}

class ThunkTuple {
	private final String thunkableSignature;
	public final String thunkableSignature(){ return thunkableSignature; }

	static native void registerNatives();
	static {
		registerNatives();
	}

	private long invokeExactThunk;

	public final long invokeExactThunk(){ return invokeExactThunk; }

	static ThunkTuple newShareable(String thunkableSignature) {
		return new ThunkTuple(thunkableSignature, initialInvokeExactThunk());
	}

	static ThunkTuple newCustom(String thunkableSignature, long invokeExactThunk) {
		return new ThunkTuple(thunkableSignature, invokeExactThunk);
	}

	private ThunkTuple(String thunkableSignature, long invokeExactThunk){
		this.thunkableSignature = thunkableSignature;
		this.invokeExactThunk   = invokeExactThunk;
	}

	private static native long initialInvokeExactThunk();

	public static void load(){}

	private static native void finalizeImpl(long invokeExactThunk);

	@Override
	protected void finalize() throws Throwable {
		finalizeImpl(invokeExactThunk);
	}
}

class ThunkTable {

	private final ConcurrentHashMap<ThunkKey, ThunkTuple> tuples; // TODO:JSR292: use soft references

	private static final boolean SHARE_THUNKS = true;

	ThunkTable(){
		this.tuples = new ConcurrentHashMap<ThunkKey, ThunkTuple>();
	}

	public ThunkTuple get(ThunkKey key) {
		ThunkTuple result = tuples.get(key);
		if (result == null) {
			result = ThunkTuple.newShareable(key.thunkableSignature()); // Thunk tables only contain shareable thunks; no need to put custom thunks in a table
			if (SHARE_THUNKS) {
				ThunkTuple existingEntry = tuples.putIfAbsent(key, result);
				if (existingEntry != null)
					result = existingEntry;
			} else
				return result;
		}
		assert(result != null);
		assert(key.thunkableSignature() == result.thunkableSignature());
		return result;
	}

	public static void load(){}
}

/**
 * An ILGenMacro method is executed at ilgen time by TR_ByteCodeIlGenerator::runMacro.
 *
 * @param argPlaceholder can be passed to other archetype calls.
 */
class ILGenMacros {
	public static final native int    placeholder     (int argPlaceholder); // Calls to this will disappear and be replaced by their arguments
	public static final native int    numArguments    (int argPlaceholder);
	public static final native Object populateArray   (Object array, int argPlaceholder); // returns the array
	public static final native int    firstN          (int n, int argPlaceholder);
	public static final native int    lastN           (int n, int argPlaceholder);
	public static final native Object rawNew          (Class c);

	// Operations manipulating the cold end of the operand stack (making it the "operand dequeue")
	public static final native int    push(int    arg);
	public static final native long   push(long   arg);
	public static final native float  push(float  arg);
	public static final native double push(double arg);
	public static final native Object push(Object arg);
	public static final native int    pop_I();
	public static final native long   pop_J();
	public static final native float  pop_F();
	public static final native double pop_D();
	public static final native Object pop_L();

	// MethodHandle semantics
	protected static native void typeCheck(MethodHandle handle, MethodType desiredType) throws WrongMethodTypeException;

	// MethodHandle.invokeExact without type checks
	protected static native void    invokeExact_V(MethodHandle handle, int argPlaceholder);
	protected static native boolean invokeExact_Z(MethodHandle handle, int argPlaceholder);
	protected static native byte    invokeExact_B(MethodHandle handle, int argPlaceholder);
	protected static native short   invokeExact_S(MethodHandle handle, int argPlaceholder);
	protected static native char    invokeExact_C(MethodHandle handle, int argPlaceholder);
	protected static native int     invokeExact_I(MethodHandle handle, int argPlaceholder);
	protected static native long    invokeExact_J(MethodHandle handle, int argPlaceholder);
	protected static native float   invokeExact_F(MethodHandle handle, int argPlaceholder);
	protected static native double  invokeExact_D(MethodHandle handle, int argPlaceholder);
	protected static native Object  invokeExact_L(MethodHandle handle, int argPlaceholder);
	protected static native void    invokeExact_V(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native boolean invokeExact_Z(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native byte    invokeExact_B(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native short   invokeExact_S(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native char    invokeExact_C(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native int     invokeExact_I(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native long    invokeExact_J(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native float   invokeExact_F(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native double  invokeExact_D(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native Object  invokeExact_L(MethodHandle handle, Object anotherArg, int argPlaceholder);
	protected static native void    invokeExact_V(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native boolean invokeExact_Z(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native byte    invokeExact_B(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native short   invokeExact_S(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native char    invokeExact_C(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native int     invokeExact_I(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native long    invokeExact_J(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native float   invokeExact_F(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native double  invokeExact_D(MethodHandle handle, int argPlaceholder, Object anotherArg);
	protected static native Object  invokeExact_L(MethodHandle handle, int argPlaceholder, Object anotherArg);

	public static void load(){}
}

// }}} JIT support

