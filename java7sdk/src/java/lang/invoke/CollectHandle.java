/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;

/* CollectHandle is a MethodHandle subclass used to call another MethodHandle.
 * It accepts the incoming arguments and collects the requested number
 * of them into an array of type 'T'.
 * <p>
 * The vmSlot will hold 0 as there is no actual method for it.
 * <p>
 * Return types can NOT be adapted by this handle.
 * <p>
 * Can't pre-allocate the collect array as its not thread-safe - same handle
 * can be used in multiple threads or collected args array can be modified
 * down the call chain.
 */
final class CollectHandle extends MethodHandle {
	@VMCONSTANTPOOL_FIELD
	final MethodHandle next;
	@VMCONSTANTPOOL_FIELD
	final int collectArraySize; /* Size of the collect array */

	CollectHandle(MethodHandle next, int collectArraySize) {
		super(collectMethodType(next.type(), collectArraySize), null, null, KIND_COLLECT, collectArraySize);
		this.collectArraySize = collectArraySize;
		this.next = next;
		this.vmSlot = 0;
	}

	CollectHandle(CollectHandle original, MethodType newType) {
		super(original, newType);
		this.collectArraySize = original.collectArraySize;
		this.next = original.next;
		this.vmSlot = original.vmSlot;
	}

	private static final MethodType collectMethodType(MethodType type, int collectArraySize){
		if (type.parameterCount() == 0) {
			throw new IllegalArgumentException("last argument of MethodType must be an array class");
		}
		// Ensure the last class is an array
		Class<?> arrayComponent = type.arguments[type.parameterCount() - 1].getComponentType();
		if (arrayComponent == null) {
			throw new IllegalArgumentException("last argument of MethodType must be an array class");
		}
		// Change the T[] into a 'T'
		MethodType newType = type.changeParameterType(type.parameterCount() -1 , arrayComponent);

		// Add necessary additional 'T' to the type
		if (collectArraySize == 0) {
			newType = newType.dropParameterTypes(type.parameterCount() - 1 , type.parameterCount());
		} else if (collectArraySize > 1){
			Class<?>[] classes = new Class[collectArraySize - 1];
			for (int j = 0; j < classes.length; j++) {
				classes[j] = arrayComponent;
			}
			newType = newType.insertParameterTypes(newType.parameterCount(), classes);
		}
		return newType;
	}

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new CollectHandle(this, newType);
	}

	// {{{ JIT support

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	protected final ThunkTuple computeThunks(Object arg) {
		int collectArraySize = (Integer)arg;
		return thunkTable().get(new ThunkKeyWithInt(ThunkKey.computeThunkableSignature(type().toMethodDescriptorString()), collectArraySize));
	}

	private final Object allocateArray() {
		return java.lang.reflect.Array.newInstance(
			next.type().parameterType(next.type().parameterCount()-1).getComponentType(),
			collectArraySize);
	}

	// ILGen Macros
	private static native int numArgsToPassThrough();
	private static native int numArgsToCollect();

	// Archetypes
	@Invisible
	private final void invokeExact_thunkArchetype_V(int argPlaceholder) throws Throwable {
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		ILGenMacros.invokeExact_V(
			next,
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.pop_L());
	}

	@Invisible
	private final int invokeExact_thunkArchetype_I(int argPlaceholder) throws Throwable {
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		return ILGenMacros.invokeExact_I(
			next,
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.pop_L());
	}

	@Invisible
	private final long invokeExact_thunkArchetype_J(int argPlaceholder) throws Throwable {
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		return ILGenMacros.invokeExact_J(
			next,
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.pop_L());
	}

	@Invisible
	private final float invokeExact_thunkArchetype_F(int argPlaceholder) throws Throwable {
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		return ILGenMacros.invokeExact_F(
			next,
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.pop_L());
	}

	@Invisible
	private final double invokeExact_thunkArchetype_D(int argPlaceholder) throws Throwable {
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		return ILGenMacros.invokeExact_D(
			next,
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.pop_L());
	}

	@Invisible
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) throws Throwable {
		ILGenMacros.populateArray(
			ILGenMacros.push(allocateArray()),
			ILGenMacros.lastN(numArgsToCollect(), argPlaceholder));
		return ILGenMacros.invokeExact_L(
			next,
			ILGenMacros.firstN(numArgsToPassThrough(), argPlaceholder),
			ILGenMacros.pop_L());
	}

	// }}} JIT support

}
