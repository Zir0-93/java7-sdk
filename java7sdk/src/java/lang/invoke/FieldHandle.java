/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */

package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

abstract class FieldHandle extends MethodHandle {
	final Class<?> fieldClass;

	FieldHandle(MethodType type, Class<?> referenceClass, String fieldName, Class fieldClass, int kind, Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		super(type, referenceClass, fieldName, kind, null);
		this.fieldClass = fieldClass;
		this.defc = finishFieldInitialization(accessClass);
	}

	FieldHandle(MethodType type, Field field, int kind, boolean isStatic) throws IllegalAccessException {
		super(type, field.getDeclaringClass(), field.getName(), kind, null);
		this.fieldClass = field.getType();
		assert(isStatic == Modifier.isStatic(field.getModifiers()));

		boolean succeed = setVMSlotAndRawModifiersFromField(this, field);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	FieldHandle(FieldHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
		this.fieldClass = originalHandle.fieldClass;
	}

	final Class finishFieldInitialization(Class<?> accessClass) throws IllegalAccessException, NoSuchFieldException {
		String signature = MethodType.getBytecodeStringName(fieldClass);
		try {
			return lookupField(definingClass, name, signature, kind, accessClass);
		} catch (NoSuchFieldError e) {
			throw new NoSuchFieldException(e.getMessage());
		} catch (IncompatibleClassChangeError e) {
			throw new IllegalAccessException(e.getMessage());
		}
	}

}
