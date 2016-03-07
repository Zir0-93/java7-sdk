/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/*
 * VirtualHandle is a MethodHandle that does virtual dispatch
 * on the receiver.
 * <p>
 * The vmSlot holds the vtable index for the correct method.
 * The type is the same as the method's except with the receiver's class prepended
 */
final class VirtualHandle extends IndirectHandle {

	VirtualHandle(Method method) throws IllegalAccessException {
		super(virtualMethodType(method), method.getDeclaringClass(), method.getName(), KIND_VIRTUAL);
		if (Modifier.isStatic(method.getModifiers())) {
			throw new IllegalArgumentException();
		}

		// Set the vmSlot to an vtable index
		boolean succeed = setVMSlotAndRawModifiersFromMethod(this, definingClass, method, this.kind, null);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	VirtualHandle(DirectHandle nonVirtualHandle) throws IllegalAccessException {
		super(nonVirtualHandle.type(), nonVirtualHandle.definingClass, nonVirtualHandle.name, KIND_VIRTUAL);
		this.rawModifiers = nonVirtualHandle.rawModifiers;
		this.defc = nonVirtualHandle.defc;

		// Set the vmSlot to an vtable index
		boolean succeed = setVMSlotAndRawModifiersFromSpecialHandle(this, nonVirtualHandle);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	VirtualHandle(VirtualHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	private static final MethodType virtualMethodType(Method method){
		MethodType originalType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
		return virtualMethodType(originalType, method.getDeclaringClass());
	}

	private static final MethodType virtualMethodType(MethodType type, Class definingClass){
		return type.insertParameterTypes(0, definingClass);
	}

	/// {{{ JIT support
	protected final int vtableIndex(Object receiver){ return ((int)vmSlot - INTRP_VTABLE_OFFSET) / VTABLE_ENTRY_SIZE; }
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
		return new VirtualHandle(this, newType);
	}
}
