/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2009, 2014  All Rights Reserved
 */
package java.lang.invoke;

import java.lang.invoke.MethodHandle.Invisible;
import java.lang.reflect.Constructor;

/* ConstructorHandle is a MethodHandle subclass used to call <init> methods.  This
 * class is similar to DirectHandle in that the method to call has already
 * been resolved down to an exact method address.
 * <p>
 * The constructor must be called with the type of the <init> method. This means
 * it must have a void return type.
 * <p>
 * This is the equivalent of calling newInstance except with a known constructor.
 * <p>
 * The vmSlot will hold a J9Method address of the <init> method.
 */
final class ConstructorHandle extends MethodHandle {

	public ConstructorHandle(Class definingClass, MethodType type) throws NoSuchMethodException, IllegalAccessException {
		super(type, definingClass, "<init>", KIND_CONSTRUCTOR, null); //$NON-NLS-1$
		/* Pass definingClass as SpecialToken as KIND_SPECIAL & KIND_CONSTRUCTOR share lookup code */
		this.defc = finishMethodInitialization(definingClass, type);
	}

	public ConstructorHandle(Constructor ctor) throws IllegalAccessException {
		super(MethodType.methodType(ctor.getDeclaringClass(), ctor.getParameterTypes()), ctor.getDeclaringClass(), "<init>", KIND_CONSTRUCTOR, ctor.getDeclaringClass()); //$NON-NLS-1$

		boolean succeed = setVMSlotAndRawModifiersFromConstructor(this, ctor);
		if (!succeed) {
			throw new IllegalAccessException();
		}
	}

	ConstructorHandle(ConstructorHandle originalHandle, MethodType newType) {
		super(originalHandle, newType);
	}

	/*
	 * Constructors have type (args of passed in type)definingClass.
	 */
	MethodType computeHandleType(MethodType type) {
		return type.changeReturnType(definingClass);
	}

	// {{{ JIT support
	private static final ThunkTable _thunkTable = new ThunkTable();
	protected final ThunkTable thunkTable(){ return _thunkTable; }

	@Invisible
	private final Object invokeExact_thunkArchetype_L(int argPlaceholder) {
		ComputedCalls.dispatchJ9Method_V(vmSlot, ILGenMacros.push(ILGenMacros.rawNew(definingClass)), argPlaceholder);
		return ILGenMacros.pop_L();
	}
	// }}} JIT support

	@Override
	MethodHandle cloneWithNewType(MethodType newType) {
		return new ConstructorHandle(this, newType);
	}

}
