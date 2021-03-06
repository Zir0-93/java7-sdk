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
 * an instance field.
 * <p>
 * vmSlot will hold the field offset in the instance.
 *
 */
final class FieldGetterHandle extends FieldHandle {

	FieldGetterHandle(Class referenceClass, String fieldName, Class fieldClass, Class accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(fieldMethodType(fieldClass, referenceClass), referenceClass, fieldName, fieldClass, KIND_GETFIELD, accessClass);
	}

	FieldGetterHandle(Field field) throws IllegalAccessException {
		super(fieldMethodType(field.getType(), field.getDeclaringClass()), field, KIND_GETFIELD, false);
	}

	FieldGetterHandle(FieldGetterHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/* Create the MethodType to be passed to the constructor */
	private final static MethodType fieldMethodType(Class returnType, Class argument) {
		return MethodType.methodType(returnType, argument);
	}

	// {{{ JIT support
	@Invisible
	private final int    invokeExact_thunkArchetype_I(Object receiver, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getIntVolatile(receiver, vmSlot+HEADER_SIZE);
		else
			return getUnsafe().getInt        (receiver, vmSlot+HEADER_SIZE);
	}

	@Invisible
	private final long   invokeExact_thunkArchetype_J(Object receiver, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getLongVolatile(receiver, vmSlot+HEADER_SIZE);
		else
			return getUnsafe().getLong        (receiver, vmSlot+HEADER_SIZE);
	}

	@Invisible
	private final float  invokeExact_thunkArchetype_F(Object receiver, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getFloatVolatile(receiver, vmSlot+HEADER_SIZE);
		else
			return getUnsafe().getFloat        (receiver, vmSlot+HEADER_SIZE);
	}

	@Invisible
	private final double invokeExact_thunkArchetype_D(Object receiver, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getDoubleVolatile(receiver, vmSlot+HEADER_SIZE);
		else
			return getUnsafe().getDouble        (receiver, vmSlot+HEADER_SIZE);
	}

	@Invisible
	private final Object invokeExact_thunkArchetype_L(Object receiver, int argPlaceholder) {
		if (Modifier.isVolatile(rawModifiers))
			return getUnsafe().getObjectVolatile(receiver, vmSlot+HEADER_SIZE);
		else
			return getUnsafe().getObject        (receiver, vmSlot+HEADER_SIZE);
	}

	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new FieldGetterHandle(this, newType);
	}
}
