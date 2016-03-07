/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

/*
 * Special handle that in the interpreter calls the equivalent's vmDispatchTarget.
 * Provides ability to have JIT specific subtypes without requiring individual
 * interpreter targets.
 * vmSlot will be zero
 */
abstract class PassThroughHandle extends MethodHandle {
	final MethodHandle equivalent;

	/**
	 * Create a new PassThroughHandle that will call 'equivalent' MethodHandle
	 * when invoked in the interpreter.
	 *
	 * @param equivalent the equivalent methodhandle, usually using the collect-operate-spread pattern
	 * @param thunkArg extra thunkArg used in computeThunks.
	 */
	PassThroughHandle(MethodHandle equivalent, Object thunkArg) {
		super(equivalent.type, equivalent.definingClass, equivalent.name, KIND_PASSTHROUGH, thunkArg);
		this.equivalent = equivalent;
		this.rawModifiers = equivalent.rawModifiers;
		this.defc = equivalent.defc;
		this.specialCaller = equivalent.specialCaller;
	}

	/**
	 * Helper constructor.  Calls {@link #PassThroughHandle(MethodHandle, Object)} with null thunkArg
	 */
	PassThroughHandle(MethodHandle equivalent) {
		this(equivalent, null);
	}

	/**
	 * Helper constructor for cloneWithNewType, copys relevant fields to the new object
	 */
	PassThroughHandle(PassThroughHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.equivalent = originalHandle.equivalent;
	}
}
