/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Modifier;

/* InvokeGenericHandle is a MethodHandle subclass used to MethodHande.invokeGeneric
 * with a specific signature on a MethodHandle.
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 * <p>
 * Can be thought of as a special case of VirtualHandle.
 */
final class InvokeGenericHandle extends MethodHandle {
 	/* MethodType that the first argument MethodHandle will be cast to using asType */
 	final MethodType castType;

	InvokeGenericHandle(MethodType type) {
		super(type, MethodHandle.class, "invoke", KIND_INVOKEGENERIC, null); //$NON-NLS-1$
		if (type == null) {
			throw new IllegalArgumentException();
		}
		this.vmSlot = 0;
		this.rawModifiers = Modifier.PUBLIC;
		this.defc = MethodHandle.class;
		this.castType = type;
	}

	InvokeGenericHandle(InvokeGenericHandle originalHandle,	MethodType newType) {
		super(originalHandle, newType);
		this.castType = originalHandle.castType;
	}

	/*
	 * Insert MethodHandle as first argument to existing type.
	 * (LMethodHandle;otherargs)returntype
	 */
	@Override
	MethodType computeHandleType(MethodType type) {
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

	@Invisible
 	private final void   invokeExact_thunkArchetype_V(MethodHandle next, int argPlaceholder) throws Throwable {        ILGenMacros.invokeExact_V(next.asType(castType), argPlaceholder); }
	@Invisible
 	private final int    invokeExact_thunkArchetype_I(MethodHandle next, int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_I(next.asType(castType), argPlaceholder); }
	@Invisible
 	private final long   invokeExact_thunkArchetype_J(MethodHandle next, int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_J(next.asType(castType), argPlaceholder); }
	@Invisible
 	private final float  invokeExact_thunkArchetype_F(MethodHandle next, int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_F(next.asType(castType), argPlaceholder); }
	@Invisible
 	private final double invokeExact_thunkArchetype_D(MethodHandle next, int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_D(next.asType(castType), argPlaceholder); }
	@Invisible
 	private final Object invokeExact_thunkArchetype_L(MethodHandle next, int argPlaceholder) throws Throwable { return ILGenMacros.invokeExact_L(next.asType(castType), argPlaceholder); }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new InvokeGenericHandle(this, newType);
	}
}
