/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Modifier;

/* InvokeExactHandle is a MethodHandle subclass used to MethodHande.invokeExact
 * with a specific signature on a MethodHandle.
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 * <p>
 * Can be thought of as a special case of VirtualHandle.
 */
final class InvokeExactHandle extends MethodHandle {
	/* MethodType that the first argument MethodHandle must match */
	final MethodType nextType;

	InvokeExactHandle(MethodType type) {
		super(invokeExactMethodType(type), MethodHandle.class, "invokeExact", KIND_INVOKEEXACT, null); //$NON-NLS-1$
		nextType = type;
		this.vmSlot = 0;
		this.rawModifiers = Modifier.PUBLIC;
		this.defc = MethodHandle.class;
	}

	InvokeExactHandle(InvokeExactHandle originalHandle, MethodType newType) {
			super(originalHandle, newType);
			this.nextType = originalHandle.nextType;
	}

	/*
	 * Insert MethodHandle as first argument to existing type.
	 * (LMethodHandle;otherargs)returntype
	 */
	private static final MethodType invokeExactMethodType(MethodType type){
		if (type == null) {
			throw new IllegalArgumentException();
		}
		return type.insertParameterTypes(0, MethodHandle.class);
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	protected ThunkTuple computeThunks(Object arg) {
		String signature = type().toMethodDescriptorString();

		// The first argument is always a MethodHandle.
		// We don't upcast that to Object to avoid a downcast in the thunks.
		//
		int afterMethodHandleArgument = signature.indexOf(';')+1;

		return thunkTable().get(new ThunkKey(ThunkKey.computeThunkableSignature(signature, 0, afterMethodHandleArgument)));
	}

	// Archetypes
	@Invisible
	private final void   invokeExact_thunkArchetype_V(MethodHandle next, int argPlaceholder) throws Throwable { ILGenMacros.typeCheck(next, nextType);        ILGenMacros.invokeExact_V(next, argPlaceholder); }
	@Invisible
	private final int    invokeExact_thunkArchetype_I(MethodHandle next, int argPlaceholder) throws Throwable { ILGenMacros.typeCheck(next, nextType); return ILGenMacros.invokeExact_I(next, argPlaceholder); }
	@Invisible
	private final long   invokeExact_thunkArchetype_J(MethodHandle next, int argPlaceholder) throws Throwable { ILGenMacros.typeCheck(next, nextType); return ILGenMacros.invokeExact_J(next, argPlaceholder); }
	@Invisible
	private final float  invokeExact_thunkArchetype_F(MethodHandle next, int argPlaceholder) throws Throwable { ILGenMacros.typeCheck(next, nextType); return ILGenMacros.invokeExact_F(next, argPlaceholder); }
	@Invisible
	private final double invokeExact_thunkArchetype_D(MethodHandle next, int argPlaceholder) throws Throwable { ILGenMacros.typeCheck(next, nextType); return ILGenMacros.invokeExact_D(next, argPlaceholder); }
	@Invisible
	private final Object invokeExact_thunkArchetype_L(MethodHandle next, int argPlaceholder) throws Throwable { ILGenMacros.typeCheck(next, nextType); return ILGenMacros.invokeExact_L(next, argPlaceholder); }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new InvokeExactHandle(this, newType);
	}
}
