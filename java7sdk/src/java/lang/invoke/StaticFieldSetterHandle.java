/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;

/*
 * MethodHandle subclass that is able to set the value of
 * a static field.
 * <p>
 * vmSlot will hold the Unsafe field offset  + low tag.
 *
 */
final class StaticFieldSetterHandle extends FieldHandle {

	StaticFieldSetterHandle(Class referenceClass, String fieldName, Class fieldClass, Class accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(fieldMethodType(fieldClass), referenceClass, fieldName, fieldClass, KIND_PUTSTATICFIELD, accessClass);
	}

	StaticFieldSetterHandle(Field field) throws IllegalAccessException {
		super(fieldMethodType(field.getType()), field, KIND_PUTSTATICFIELD, true);
	}

	StaticFieldSetterHandle(StaticFieldSetterHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/* Create the MethodType to be passed to the constructor */
	private final static MethodType fieldMethodType(Class fieldClass) {
		return MethodType.methodType(void.class, fieldClass);
	}

	// {{{ JIT support
	@Invisible
	private final void invokeExact_thunkArchetype_V(int    newValue, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			getUnsafe().putIntVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putInt        (defc, vmSlot, newValue);
	}

	@Invisible
	private final void invokeExact_thunkArchetype_V(long   newValue, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			getUnsafe().putLongVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putLong        (defc, vmSlot, newValue);
	}

	@Invisible
	private final void invokeExact_thunkArchetype_V(float  newValue, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			getUnsafe().putFloatVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putFloat        (defc, vmSlot, newValue);
	}

	@Invisible
	private final void invokeExact_thunkArchetype_V(double newValue, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			getUnsafe().putDoubleVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putDouble        (defc, vmSlot, newValue);
	}

	@Invisible
	private final void invokeExact_thunkArchetype_V(Object newValue, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			getUnsafe().putObjectVolatile(defc, vmSlot, newValue);
		else
			getUnsafe().putObject        (defc, vmSlot, newValue);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new StaticFieldSetterHandle(this, newType);
	}
}

