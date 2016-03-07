/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */

package java.lang.invoke;
import sun.misc.Unsafe;

abstract class IndirectHandle extends MethodHandle {
	IndirectHandle(MethodType type, Class definingClass, String name, int kind)
		{ super(type, definingClass, name, kind, null); }

	IndirectHandle(IndirectHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	// {{{ JIT support
	protected abstract int vtableIndex(Object receiver);
	protected final long vtableIndexArgument(Object receiver){ return - VTABLE_ENTRY_SIZE*vtableIndex(receiver); }

	protected long jittedMethodAddress(Object receiver) {
		long receiverClass = getJ9ClassFromClass(receiver.getClass());
		long result;
		if (VTABLE_ENTRY_SIZE == 4) {
			result = getUnsafe().getInt(receiverClass - VTABLE_ENTRY_SIZE*vtableIndex(receiver));
		} else {
			result = getUnsafe().getLong(receiverClass - VTABLE_ENTRY_SIZE*vtableIndex(receiver));
		}
		return result;
	}

	// }}} JIT support

}
