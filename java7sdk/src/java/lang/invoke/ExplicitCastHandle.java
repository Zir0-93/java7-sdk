/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;

/* ExplicitCastHandle is a MethodHandle subclass used to implement
 * MethodHandles.explicitCastArgument()
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 *
 */
final class ExplicitCastHandle extends ArgumentConversionHandle {

	ExplicitCastHandle(MethodHandle handle, MethodType type) {
		super(handle, type, null, null, KIND_EXPLICITCAST);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	//
	// ILGen Macros
	//
	protected static native int convertArgs(int argPlaceholder);

	//
	// Archetypes
	//
	@Invisible
	private final void    invokeExact_thunkArchetype_V(int argPlaceholder) throws Throwable {        ILGenMacros.invokeExact_V(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final boolean invokeExact_thunkArchetype_Z(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_Z(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final byte    invokeExact_thunkArchetype_B(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_B(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final short   invokeExact_thunkArchetype_S(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_S(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final char    invokeExact_thunkArchetype_C(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_C(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final int     invokeExact_thunkArchetype_I(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_I(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final long    invokeExact_thunkArchetype_J(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_J(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final float   invokeExact_thunkArchetype_F(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_F(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final double  invokeExact_thunkArchetype_D(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_D(next, convertArgs(argPlaceholder)); }
	@Invisible
	private final Object  invokeExact_thunkArchetype_L(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_L(next, convertArgs(argPlaceholder)); }

	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new ExplicitCastHandle(this, newType);
	}
}
