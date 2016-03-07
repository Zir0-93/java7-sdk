/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/*
 * MethodHandle subclass that is able to return the value of
 * a static field.
 * <p>
 * vmSlot will hold the Unsafe field offset + low tag.
 *
 */
final class StaticFieldGetterHandle extends FieldHandle {

	StaticFieldGetterHandle(Class referenceClass, String fieldName, Class fieldClass, Class accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(fieldMethodType(fieldClass), referenceClass, fieldName, fieldClass, KIND_GETSTATICFIELD, accessClass);
	}

	StaticFieldGetterHandle(Field field) throws IllegalAccessException {
		super(fieldMethodType(field.getType()), field, KIND_GETSTATICFIELD, true);
	}

	StaticFieldGetterHandle(StaticFieldGetterHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/* Create the MethodType to be passed to the constructor */
	private final static MethodType fieldMethodType(Class fieldClass) {
		return MethodType.methodType(fieldClass);
	}

	// {{{ JIT support
	@Invisible
	private final int    invokeExact_thunkArchetype_I(int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getIntVolatile(defc, vmSlot);
		else
			return getUnsafe().getInt        (defc, vmSlot);
	}

	@Invisible
	private final long   invokeExact_thunkArchetype_J(int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getLongVolatile(defc, vmSlot);
		else
			return getUnsafe().getLong        (defc, vmSlot);
	}

	@Invisible
	private final float  invokeExact_thunkArchetype_F(int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getFloatVolatile(defc, vmSlot);
		else
			return getUnsafe().getFloat        (defc, vmSlot);
	}

	@Invisible
	private final double invokeExact_thunkArchetype_D(int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getDoubleVolatile(defc, vmSlot);
		else
			return getUnsafe().getDouble        (defc, vmSlot);
	}

	@Invisible
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getObjectVolatile(defc, vmSlot);
		else
			return getUnsafe().getObject        (defc, vmSlot);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new StaticFieldGetterHandle(this, newType);
	}
}
