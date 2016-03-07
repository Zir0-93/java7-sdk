/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;

@VMCONSTANTPOOL_CLASS
final class FilterReturnHandle extends ConvertHandle {
	@VMCONSTANTPOOL_FIELD
	final MethodHandle filter;

	FilterReturnHandle(MethodHandle next, MethodHandle filter) {
		super(next, next.type.changeReturnType(filter.type.returnType), FilterReturnHandle.class, "FilterReturnHandle", KIND_FILTERRETURN, filter.type()); //$NON-NLS-1$
		this.filter = filter;
	}

	FilterReturnHandle(FilterReturnHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.filter = originalHandle.filter;
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected final ThunkTuple computeThunks(Object filterHandleType) {
		// We include the full type of filter in order to get the type cast right.
		String filterSignature = ((MethodType)filterHandleType).toMethodDescriptorString();
		return thunkTable().get(new ThunkKeyWithObject(ThunkKey.computeThunkableSignature(type().toMethodDescriptorString()), filterSignature));
	}

	// ILGen macros
	private static native int fixReturnType(int invokeExactCall);

	// Archetypes
	@Invisible
	private final void    invokeExact_thunkArchetype_V(int argPlaceholder) throws Throwable {        ILGenMacros.invokeExact_V(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final boolean invokeExact_thunkArchetype_Z(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_Z(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final byte    invokeExact_thunkArchetype_B(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_B(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final char    invokeExact_thunkArchetype_C(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_C(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final short   invokeExact_thunkArchetype_S(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_S(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final int     invokeExact_thunkArchetype_I(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_I(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final long    invokeExact_thunkArchetype_J(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_J(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final float   invokeExact_thunkArchetype_F(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_F(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final double  invokeExact_thunkArchetype_D(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_D(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	@Invisible
	private final Object  invokeExact_thunkArchetype_L(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_L(filter, fixReturnType(ILGenMacros.invokeExact_I(next, argPlaceholder))); }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new FilterReturnHandle(this, newType);
	}
}
