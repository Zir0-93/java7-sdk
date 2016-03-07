/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* DirectHandle is a MethodHandle subclass used to call methods that have already
 * been resolved down to an exact method address.
 * <p>
 * The exact method address is known in the following cases:
 * <ul>
 * <li> MethodHandles.lookup().findStatic </li>
 * <li> MethodHandles.lookup().findSpecial </li>
 * </ul>
 * <p>
 * The vmSlot will hold a J9Method address.
 */
class DirectHandle extends MethodHandle {

	DirectHandle(Class definingClass, String methodName, MethodType type, int kind, Class specialCaller) throws NoSuchMethodException, IllegalAccessException {
		super(directMethodType(type, kind, specialCaller), definingClass, methodName, kind, null);
		assert (kind != KIND_SPECIAL) || (specialCaller != null);
		this.specialCaller = specialCaller;
		this.defc = finishMethodInitialization(specialCaller, type);
	}

	public DirectHandle(Method method, int kind, Class specialCaller) throws IllegalAccessException {
		super(directMethodType(MethodType.methodType(method.getReturnType(), method.getParameterTypes()), kind, specialCaller), method.getDeclaringClass(), method.getName(), kind, null);
		assert (kind != KIND_SPECIAL) || (specialCaller != null);
		this.specialCaller = specialCaller;
		boolean succeed = setVMSlotAndRawModifiersFromMethod(this, definingClass, method, this.kind, specialCaller);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	/*
	 * Create a new DirectHandle from another DirectHandle.
	 * This is used by ReceiverBoundHandle
	 */
	DirectHandle(MethodHandle other, int kind) {
		super(other.type, other.definingClass, other.name, kind, null);
		if (!(other instanceof DirectHandle)) {
			throw new IllegalArgumentException();
		}
		this.specialCaller = other.specialCaller;
		this.vmSlot = other.vmSlot;
		this.rawModifiers = other.rawModifiers;
		this.defc = other.defc;
	}

	DirectHandle(DirectHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/*
	 * Determine the correct MethodType for the DirectHandle
	 * 		KIND_STATIC		- unmodified
	 * 		KIND_SPECIAL	- insert specialCaller as first parameter
	 */
	private static final MethodType directMethodType(MethodType existingType, int kind, Class specialCaller) {
		if (kind == KIND_STATIC) {
			return existingType;
		}
		return existingType.insertParameterTypes(0, specialCaller);
	}

	final void nullCheckIfRequired(Object receiver) throws NullPointerException {
		if ((receiver == null) && !Modifier.isStatic(rawModifiers)) {
			throw new NullPointerException();
		}
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected ThunkTable thunkTable(){ return _thunkTable; }

	@Invisible
	private final void invokeExact_thunkArchetype_V(int argPlaceholder) {
		ComputedCalls.dispatchJ9Method_V(vmSlot, argPlaceholder);
	}
	@Invisible
	private final void invokeExact_thunkArchetype_V(Object receiver, int argPlaceholder) {
		nullCheckIfRequired(receiver);
		ComputedCalls.dispatchJ9Method_V(vmSlot, receiver, argPlaceholder);
	}
	@Invisible
	private final int invokeExact_thunkArchetype_I(int argPlaceholder) {
		return ComputedCalls.dispatchJ9Method_I(vmSlot, argPlaceholder);
	}
	@Invisible
	private final int invokeExact_thunkArchetype_I(Object receiver, int argPlaceholder) {
		nullCheckIfRequired(receiver);
		return ComputedCalls.dispatchJ9Method_I(vmSlot, receiver, argPlaceholder);
	}
	@Invisible
	private final long invokeExact_thunkArchetype_J(int argPlaceholder) {
		return ComputedCalls.dispatchJ9Method_J(vmSlot, argPlaceholder);
	}
	@Invisible
	private final long invokeExact_thunkArchetype_J(Object receiver, int argPlaceholder) {
		nullCheckIfRequired(receiver);
		return ComputedCalls.dispatchJ9Method_J(vmSlot, receiver, argPlaceholder);
	}
	@Invisible
	private final float invokeExact_thunkArchetype_F(int argPlaceholder) {
		return ComputedCalls.dispatchJ9Method_F(vmSlot, argPlaceholder);
	}
	@Invisible
	private final float invokeExact_thunkArchetype_F(Object receiver, int argPlaceholder) {
		nullCheckIfRequired(receiver);
		return ComputedCalls.dispatchJ9Method_F(vmSlot, receiver, argPlaceholder);
	}
	@Invisible
	private final double invokeExact_thunkArchetype_D(int argPlaceholder) {
		return ComputedCalls.dispatchJ9Method_D(vmSlot, argPlaceholder);
	}
	@Invisible
	private final double invokeExact_thunkArchetype_D(Object receiver, int argPlaceholder) {
		nullCheckIfRequired(receiver);
		return ComputedCalls.dispatchJ9Method_D(vmSlot, receiver, argPlaceholder);
	}
	@Invisible
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) {
		return ComputedCalls.dispatchJ9Method_L(vmSlot, argPlaceholder);
	}
	@Invisible
	private final Object invokeExact_thunkArchetype_L(Object receiver, int argPlaceholder) {
		nullCheckIfRequired(receiver);
		return ComputedCalls.dispatchJ9Method_L(vmSlot, receiver, argPlaceholder);
	}
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new DirectHandle(this, newType);
	}
}

