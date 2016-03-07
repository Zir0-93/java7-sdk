/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2011, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;

@VMCONSTANTPOOL_CLASS
final class DynamicInvokerHandle extends MethodHandle {
	@VMCONSTANTPOOL_FIELD
	final CallSite site;

	DynamicInvokerHandle(CallSite site) {
		super(site.type(), site.getClass(), "dynamicInvoker", MethodHandle.KIND_DYNAMICINVOKER, null); //$NON-NLS-1$
		this.site = site;
		this.vmSlot = 0;
	}

	DynamicInvokerHandle(DynamicInvokerHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.site = originalHandle.site;
	}

	// {{{ JIT support
	@Invisible
	private final void    invokeExact_thunkArchetype_V(int argPlaceholder) throws Throwable {        ILGenMacros.invokeExact_V(site.getTarget(), argPlaceholder); }
	@Invisible
	private final int     invokeExact_thunkArchetype_I(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_I(site.getTarget(), argPlaceholder); }
	@Invisible
	private final long    invokeExact_thunkArchetype_J(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_J(site.getTarget(), argPlaceholder); }
	@Invisible
	private final float   invokeExact_thunkArchetype_F(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_F(site.getTarget(), argPlaceholder); }
	@Invisible
	private final double  invokeExact_thunkArchetype_D(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_D(site.getTarget(), argPlaceholder); }
	@Invisible
	private final Object  invokeExact_thunkArchetype_L(int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_L(site.getTarget(), argPlaceholder); }

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new DynamicInvokerHandle(this, newType);
	}
}
