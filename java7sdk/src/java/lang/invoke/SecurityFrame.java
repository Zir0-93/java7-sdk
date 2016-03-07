/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2012, 2014  All Rights Reserved
 */
package java.lang.invoke;

/*
 * A simple class that will be injected in each class loader, as
 * required, to act as a "trampoline" to ensure that methods which
 * are sensitive to their caller (ie: use getCallerClass())
 * can find a class in the correct ClassLoader and ProtectionDomain
 * when invoked by MethodHandle invocation.
 */
class SecurityFrame {

	private final MethodHandle target;

	public SecurityFrame(MethodHandle target) {
		this.target = target.asFixedArity();
	}

	public Object invoke(Object... args) throws Throwable {
		return target.invokeWithArguments(args);
	}
}
