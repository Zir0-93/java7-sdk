/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Method;

/*
 * InterfaceHandle is a MethodHandle that does interface dispatch
 * on the receiver.
 * <p>
 * The vmSlot holds the itable index for the correct method.
 * The type is the same as the method's except with the interface class prepended
 */
final class InterfaceHandle extends IndirectHandle {

	static native void registerNatives();
	static {
		registerNatives();
	}

	InterfaceHandle(Class definingClass, String methodName, MethodType type) throws NoSuchMethodException, IllegalAccessException {
		super(type, definingClass, methodName, KIND_INTERFACE);
		assert definingClass.isInterface();
		this.defc = finishMethodInitialization(null, type);
	}

	InterfaceHandle(Method method) throws IllegalAccessException {
		super(MethodType.methodType(method.getReturnType(), method.getParameterTypes()), method.getDeclaringClass(), method.getName(), KIND_INTERFACE);

		if (!definingClass.isInterface()) {
			throw new IllegalArgumentException();
		}

		boolean succeed = setVMSlotAndRawModifiersFromMethod(this, definingClass, method, this.kind, specialCaller);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	public InterfaceHandle(InterfaceHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/* Virtual MethodHandles have the receiver type inserted as
	 * first argument of the MH's type.
	 */
	@Override
	MethodType computeHandleType(MethodType type) {
		return type.insertParameterTypes(0, definingClass);
	}

	/// {{{ JIT support
	protected final int vtableIndex(Object receiver) {
		Class interfaceClass = defc;
		if (interfaceClass.isInstance(receiver)) {
			long interfaceJ9Class = getJ9ClassFromClass(interfaceClass);
			long receiverJ9Class  = getJ9ClassFromClass(receiver.getClass());
			return convertITableIndexToVTableIndex(interfaceJ9Class, (int)vmSlot, receiverJ9Class);
		} else {
			throw new Error(); // TODO:JSR292: Now what?
		}
	}

	protected static native int convertITableIndexToVTableIndex(long interfaceClass, int itableIndex, long receiverClass);

	// Thunks

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	// Copied from IndirectHandle so the jit can specialize
	@Invisible
	private final void   invokeExact_thunkArchetype_V(Object receiver, int argPlaceholder) {        ComputedCalls.dispatchVirtual_V(jittedMethodAddress(receiver), vtableIndexArgument(receiver), receiver, argPlaceholder); }
	@Invisible
	private final int    invokeExact_thunkArchetype_I(Object receiver, int argPlaceholder) { return ComputedCalls.dispatchVirtual_I(jittedMethodAddress(receiver), vtableIndexArgument(receiver), receiver, argPlaceholder); }
	@Invisible
	private final long   invokeExact_thunkArchetype_J(Object receiver, int argPlaceholder) { return ComputedCalls.dispatchVirtual_J(jittedMethodAddress(receiver), vtableIndexArgument(receiver), receiver, argPlaceholder); }
	@Invisible
	private final float  invokeExact_thunkArchetype_F(Object receiver, int argPlaceholder) { return ComputedCalls.dispatchVirtual_F(jittedMethodAddress(receiver), vtableIndexArgument(receiver), receiver, argPlaceholder); }
	@Invisible
	private final double invokeExact_thunkArchetype_D(Object receiver, int argPlaceholder) { return ComputedCalls.dispatchVirtual_D(jittedMethodAddress(receiver), vtableIndexArgument(receiver), receiver, argPlaceholder); }
	@Invisible
	private final Object invokeExact_thunkArchetype_L(Object receiver, int argPlaceholder) { return ComputedCalls.dispatchVirtual_L(jittedMethodAddress(receiver), vtableIndexArgument(receiver), receiver, argPlaceholder); }

	/// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new InterfaceHandle(this, newType);
	}
}
