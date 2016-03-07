/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;

/*
 * VarargsCollectorHandle is a MethodHandle subclass used to implement
 * MethodHandle.asVarargsCollector(Class<?> arrayType)
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 *
 * Type of VarargsCollectorHandle and its 'next' handle will always match
 */
final class VarargsCollectorHandle extends MethodHandle {
	final MethodHandle next;
	final Class<?> arrayType;

	VarargsCollectorHandle(MethodHandle next, Class<?> arrayType) {
		super(varargsCollectorType(next.type, arrayType), null, null, KIND_VARARGSCOLLECT, null);
		this.next = next;
		if (arrayType == null) {
			throw new IllegalArgumentException();
		}
		this.arrayType = arrayType;
		this.rawModifiers = next.rawModifiers & ~MethodHandles.Lookup.VARARGS;
		this.defc = next.defc;
	}

	VarargsCollectorHandle(VarargsCollectorHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.next = originalHandle.next;
		this.arrayType = originalHandle.arrayType;
	}

	static MethodType varargsCollectorType(MethodType nextType, Class<?> arrayType) {
		return nextType.changeParameterType(nextType.parameterCount() - 1, arrayType);
	}

	@Override
	public MethodHandle asType(MethodType newType) throws ClassCastException {
		if (type.equals(newType))  {
			return this;
		}
		int parameterCount = type.parameterCount();
		int newTypeParameterCount = newType.parameterCount();
		if (parameterCount == newTypeParameterCount) {
			if (type.parameterType(parameterCount - 1).isAssignableFrom(newType.parameterType(parameterCount - 1))) {
				return next.asType(newType);
			}
		}
		int collectCount = newTypeParameterCount - parameterCount + 1;
		if (collectCount < 0) {
			throw new WrongMethodTypeException();
		}
		return next.asCollector(arrayType, collectCount).asType(newType);
	}

	@Override
	public MethodHandle	asVarargsCollector(Class<?> arrayParameter) throws IllegalArgumentException {
		if (!arrayType.isAssignableFrom(arrayParameter)) {
			throw new IllegalArgumentException("Cannot assign '" + arrayParameter + "' to methodtype '" + type +"'");
		}
		return next.asVarargsCollector(arrayParameter);
	}

	@Override
	public MethodHandle asFixedArity() {
		MethodHandle fixedArity = next;
		while (fixedArity.isVarargsCollector()) {
			// cover varargsCollector on a varargsCollector
			fixedArity = ((VarargsCollectorHandle)fixedArity).next;
		}
		// asType will return 'this' if type is the same
		return fixedArity.asType(type());
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	@Override
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	@Invisible
	private final void   invokeExact_thunkArchetype_V(int argPlaceholder) throws Throwable {        ILGenMacros.invokeExact_V(next, argPlaceholder); }
	@Invisible
	private final int    invokeExact_thunkArchetype_I(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_I(next, argPlaceholder); }
	@Invisible
	private final long   invokeExact_thunkArchetype_J(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_J(next, argPlaceholder); }
	@Invisible
	private final float  invokeExact_thunkArchetype_F(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_F(next, argPlaceholder); }
	@Invisible
	private final double invokeExact_thunkArchetype_D(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_D(next, argPlaceholder); }
	@Invisible
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_L(next, argPlaceholder); }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new VarargsCollectorHandle(this, newType);
	}
}
