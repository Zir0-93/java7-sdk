/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Modifier;

/* ReceiverBoundHandle is a DirectHandle subclass used to call methods
 * that have an exact known address and a bound first parameter.
 * <b>
 * The bound first parameter will be inserted into the stack prior to
 * executing.  We have a "free" stack slot that contains either the MH
 * receiver or null and the receiver parameter will be used to hammer
 * that slot.
 * <p>
 * This is use-able by both static and special methods as all stack
 * shapes will have a free slot as their first slot.
 * <p>
 * It may be necessary to convert the "receiver" object into the right type.
 * If this is a call to a static method, it may be necessary to convert the
 * object to a primitive.
 * <p>
 * The vmSlot will hold a J9Method address.
 */
@VMCONSTANTPOOL_CLASS
final class ReceiverBoundHandle extends DirectHandle {
	final Object receiver;

	public ReceiverBoundHandle(MethodHandle toBind, Object receiver) {
		super(toBind, KIND_BOUND);
		if (toBind instanceof DirectHandle) {
			vmSlot = toBind.vmSlot;
			this.receiver = receiver;
			this.defc = toBind.defc;
		} else {
			throw new IllegalArgumentException();
		}
	}

	private ReceiverBoundHandle(ReceiverBoundHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.receiver = originalHandle.receiver;
	}

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new ReceiverBoundHandle(this, newType);
	}

	/*
	 * MethodType is same as incoming handle minus the first
	 * argument.
	 */
	@Override
	MethodType computeHandleType(MethodType type) {
		return type.dropParameterTypes(0, 1);
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	final void nullCheckReceiverIfNonStatic(){
		if ((receiver == null) && !Modifier.isStatic(rawModifiers)) {
			throw new NullPointerException();
		}
	}

	@Invisible
	private final void invokeExact_thunkArchetype_V(int argPlaceholder) {
		nullCheckReceiverIfNonStatic();
		ComputedCalls.dispatchJ9Method_V(vmSlot, receiver, argPlaceholder);
	}

	@Invisible
	private final int invokeExact_thunkArchetype_I(int argPlaceholder) {
		nullCheckReceiverIfNonStatic();
		return ComputedCalls.dispatchJ9Method_I(vmSlot, receiver, argPlaceholder);
	}

	@Invisible
	private final long invokeExact_thunkArchetype_J(int argPlaceholder) {
		nullCheckReceiverIfNonStatic();
		return ComputedCalls.dispatchJ9Method_J(vmSlot, receiver, argPlaceholder);
	}

	@Invisible
	private final float invokeExact_thunkArchetype_F(int argPlaceholder) {
		nullCheckReceiverIfNonStatic();
		return ComputedCalls.dispatchJ9Method_F(vmSlot, receiver, argPlaceholder);
	}

	@Invisible
	private final double invokeExact_thunkArchetype_D(int argPlaceholder) {
		nullCheckReceiverIfNonStatic();
		return ComputedCalls.dispatchJ9Method_D(vmSlot, receiver, argPlaceholder);
	}

	@Invisible
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) {
		nullCheckReceiverIfNonStatic();
		return ComputedCalls.dispatchJ9Method_L(vmSlot, receiver, argPlaceholder);
	}
	// }}} JIT support
}
