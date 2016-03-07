
package java.security;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2014  All Rights Reserved
 */

/**
 * Checks access to system resources. Supports marking of code
 * as priveleged. Makes context snapshots to allow checking from
 * other contexts.
 *
 * @author		OTI
 * @version		initial
 */

public final class AccessController {
	static {
		// Initialize vm-internal caches
		initializeInternal();
	}

private static native void initializeInternal();
private static final SecurityPermission createAccessControlContext = new SecurityPermission("createAccessControlContext"); //$NON-NLS-1$

/* [PR CMVC 188787] Enabling -Djava.security.debug option within WAS keeps JVM busy */
static class DebugRecursionDetection {
	private static ThreadLocal<String> tlDebug = new ThreadLocal<String>();
	static ThreadLocal<String> getTlDebug() {
		return tlDebug;
	}
}

/**
 * Prevents this class from being instantiated.
 */
private AccessController() {
}

/**
 * Returns an array of ProtectionDomain from the classes on the stack,
 * from the specified depth up to the first privileged frame, or the
 * end of the stack if there is not a privileged frame. The array
 * may be larger than required, but must be null terminated.
 *
 * The first element of the result is the AccessControlContext,
 * which may be null, either from the privileged frame, or
 * from the current Thread if there is not a privileged frame.
 *
 * A privileged frame is any frame running one of the following methods:
 *
 * <code><ul>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
 * <li>java/security/AccessController.doPrivileged(Ljava/security/PrivilegedExceptionAction;Ljava/security/AccessControlContext;)Ljava/lang/Object;</li>
 * </ul></code>
 *
 * @param depth The stack depth at which to start. Depth 0 is the current
 * frame (the caller of this native).
 *
 * @return an Object[] where the first element is AccessControlContext,
 * and the other elements are ProtectionsDomain.
 */

private static native Object[] getProtectionDomains(int depth);

/**
 * provide debug info according to debug settings before throwing AccessControlException
 *
 * @param debug		overall debug flag returned from DebugRecursionDetection.getTlDebug()
 * @param perm 		the permission to check
 * @param pDomain	the pDomain to check
 * @param createACCdenied	if true, actual cause of this ACE was SecurityPermission("createAccessControlContext") denied
 * @exception	AccessControlException	always throw an AccessControlException
 */
private static void throwACE(boolean debug, Permission perm, ProtectionDomain pDomain, boolean createACCdenied) {
	if (debug && ((AccessControlContext.debugSetting() & AccessControlContext.DEBUG_ACCESS) != 0)) {
		DebugRecursionDetection.getTlDebug().set("");	//$NON-NLS-1$
		AccessControlContext.debugPrintAccess();
		if (createACCdenied) {
			System.err.println("access denied " + perm + " due to untrusted AccessControlContext since " + createAccessControlContext + " is denied.");	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			System.err.println("access denied " + perm);	//$NON-NLS-1$
		}
		DebugRecursionDetection.getTlDebug().remove();
	}
	if (debug && ((AccessControlContext.debugSetting() & AccessControlContext.DEBUG_ACCESS_FAILURE) != 0)) {
		DebugRecursionDetection.getTlDebug().set("");	//$NON-NLS-1$
		new Exception("Stack trace").printStackTrace();	//$NON-NLS-1$
		if (createACCdenied) {
			System.err.println("domain that failed " + createAccessControlContext + " check " + pDomain);	//$NON-NLS-1$ //$NON-NLS-2$
		} else {
			System.err.println("domain that failed " + pDomain);	//$NON-NLS-1$
		}
		DebugRecursionDetection.getTlDebug().remove();
	}
	// K002c = Access denied {0}
	throw new AccessControlException(com.ibm.oti.util.Msg.getString("K002c", perm), perm);	//$NON-NLS-1$
}

/**
 * Checks whether the running program is allowed to
 * access the resource being guarded by the given
 * Permission argument.
 *
 * @param		perm					the permission to check
 * @exception	AccessControlException	if access is not allowed.
 * 				NullPointerException if perm is null
 */
public static void checkPermission(Permission perm) throws AccessControlException {
	if (perm == null) throw new NullPointerException();
	boolean		debug = true;
	if (AccessControlContext.debugSetting() != 0) {
		if ((String)DebugRecursionDetection.getTlDebug().get() != null) {
			debug = false;
		}
	}
	if (debug && ((AccessControlContext.debugSetting() & AccessControlContext.DEBUG_ACCESS_STACK) != 0)) {
		DebugRecursionDetection.getTlDebug().set("");
		new Exception("Stack trace").printStackTrace();
		DebugRecursionDetection.getTlDebug().remove();
	}
	Object[] domains = getProtectionDomains(1);
	AccessControlContext acc = (AccessControlContext)domains[0];
	ProtectionDomain[] pDomains;
	if (acc != null && acc.domainCombiner != null) {
		if (debug && ((AccessControlContext.debugSetting() & AccessControlContext.DEBUG_ACCESS) != 0)) {
			DebugRecursionDetection.getTlDebug().set("");
			AccessControlContext.debugPrintAccess();
			System.err.println("AccessController invoking the Combiner");
			DebugRecursionDetection.getTlDebug().remove();
		}
		pDomains = acc.domainCombiner.combine(
			toArrayOfProtectionDomains(domains, null),
			acc.context);
	} else {
		pDomains = toArrayOfProtectionDomains(domains, acc);
	}
	if (debug && ((AccessControlContext.debugSetting() & AccessControlContext.DEBUG_ACCESS_DOMAIN) != 0)) {
		DebugRecursionDetection.getTlDebug().set("");
		AccessControlContext.debugPrintAccess();
		if (pDomains == null || pDomains.length == 0) {
			System.err.println("domain (context is null)");
		} else {
			for (int i=0; i<pDomains.length; i++) {
				System.err.println("domain " + i + " " + pDomains[i]);
			}
		}
		DebugRecursionDetection.getTlDebug().remove();
	}
	int length = pDomains == null ? 0 : pDomains.length;
	if (acc != null && acc.context != null && !acc.isAuthorized) {
		// only check SecurityPermission createAccessControlContext when acc.context is not null and not authorized
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			//	all PDs returned from getProtectionDomains excluding those within acc.context have to have
			//	SecurityPermission createAccessControlContext to allow further security check proceed
			for (int i = 0; i < (length - acc.context.length) ; i++) {
				if (pDomains[i].implies(createAccessControlContext)) {
					continue;
				} else {
					//	new behavior introduced by this fix
					//	an ACE is thrown if there is a untrusted PD but without SecurityPermission createAccessControlContext
					throwACE(debug, perm, pDomains[i], true);
				}
			}
		}
	}
	for (int i = 0; i < length ; i++) {
		//	invoke PD within acc.context first
		if (!pDomains[length - i - 1].implies(perm)) {
			throwACE(debug, perm, pDomains[length - i - 1], false);
		}
	}
	if (debug && ((AccessControlContext.debugSetting() & AccessControlContext.DEBUG_ACCESS) != 0)) {
		DebugRecursionDetection.getTlDebug().set("");
		AccessControlContext.debugPrintAccess();
		System.err.println("access allowed " + perm);
		DebugRecursionDetection.getTlDebug().remove();
	}
}

/*
 * Used to keep the context live during doPrivileged().
 *
 * @see 		#doPrivileged(PrivilegedAction, AccessControlContext)
 */
private static void keepalive(AccessControlContext context) {
}

/**
 * Answers the access controller context of the current thread,
 * including the inherited ones. It basically retrieves all the
 * protection domains from the calling stack and creates an
 * <code>AccessControlContext</code> with them.
 *
 * @return an AccessControlContext which captures the current state
 *
 * @see 		AccessControlContext
 */
public static AccessControlContext getContext() {
	Object[] domains = getProtectionDomains(1);
	AccessControlContext acc = (AccessControlContext)domains[0];

	if (acc != null && acc.domainCombiner != null) {
		ProtectionDomain[] pDomains;
		pDomains = acc.domainCombiner.combine(
			toArrayOfProtectionDomains(domains, null),
			acc.context);
		if (pDomains != null && pDomains.length == 0) pDomains = null;
		AccessControlContext result = new AccessControlContext(pDomains, false);
		result.domainCombiner = acc.domainCombiner;
		return result;
	}
	return new AccessControlContext(toArrayOfProtectionDomains(domains, acc), false);
}

private static ProtectionDomain[] toArrayOfProtectionDomains(Object[] domains, AccessControlContext acc) {
	int len = 0, size = domains.length - 1;
	int extra = 0;
	if (acc != null && acc.context != null) {
		extra = acc.context.length;
	}
	ProtectionDomain[] answer = new ProtectionDomain[size + extra];
	for (int i = 1; i <= size; i++) {
		boolean found = false;
		if ((answer[len] = (ProtectionDomain)domains[i]) == null)
			break;
		if (acc != null && acc.context != null) {
			for (int j=0; j<acc.context.length; j++) {
				if (answer[len] == acc.context[j]) {
					found = true;
					break;
				}
			}
		}
		if (!found) len++;
	}
	if (len == 0 && acc != null) return acc.context;
	else
	if (len + extra == 0) return null;
	if (len < size) {
		ProtectionDomain[] copy = new ProtectionDomain[len + extra];
		System.arraycopy(answer, 0, copy, 0, len);
		answer = copy;
	}
	if (acc != null && acc.context != null)
		System.arraycopy(acc.context, 0, answer, len, acc.context.length);
	return answer;
}

/**
 * Performs the privileged action specified by <code>action</code>.
 * <p>
 * When permission checks are made, if the permission has been granted by all
 * frames below and including the one representing the call to this method,
 * then the permission is granted. In otherwords, the check stops here.
 *
 * Any unchecked exception generated by this method will propagate up the chain.
 * @param <T>
 *
 * @param action The PrivilegedAction to performed
 *
 * @return the result of the PrivilegedAction
 *
 * @exception	NullPointerException if action is null
 *
 * @see 		#doPrivileged(PrivilegedAction)
 */
@sun.reflect.CallerSensitive
public static <T> T doPrivileged(PrivilegedAction<T> action) {
	return action.run();
}

/**
 * Performs the privileged action specified by <code>action</code>.
 * <p>
 * When permission checks are made, if the permission has been granted by all
 * frames below and including the one representing the call to this method,
 * then the permission is granted iff it is granted by the AccessControlContext
 * <code>context</code>. In otherwords, no more checking of the current stack
 * is performed. Instead, the passed in context is checked.
 *
 * Any unchecked exception generated by this method will propagate up the chain.
 * @param <T>
 *
 * @param action The PrivilegedAction to performed
 * @param context The AccessControlContext to check
 *
 * @return the result of the PrivilegedAction
 *
 * @exception	NullPointerException if action is null
 *
 * @see 		#doPrivileged(PrivilegedAction)
 */
@sun.reflect.CallerSensitive
public static <T> T doPrivileged(PrivilegedAction<T> action, AccessControlContext context) {
	T result = action.run();
	keepalive(context);
	return result;
}

/**
 * Performs the privileged action specified by <code>action</code>.
 * <p>
 * When permission checks are made, if the permission has been granted by all
 * frames below and including the one representing the call to this method,
 * then the permission is granted. In otherwords, the check stops here.
 *
 * Any unchecked exception generated by this method will propagate up the chain.
 * However, checked exceptions will be caught an re-thrown as PrivilegedActionExceptions
 * @param <T>
 *
 * @param action The PrivilegedExceptionAction to performed
 *
 * @return the result of the PrivilegedExceptionAction
 *
 * @throws PrivilegedActionException when a checked exception occurs when performing the action
 * 			NullPointerException if action is null
 *
 * @see 		#doPrivileged(PrivilegedAction)
 */
@sun.reflect.CallerSensitive
public static <T> T doPrivileged(PrivilegedExceptionAction<T> action)
	throws PrivilegedActionException
{
	try {
		return action.run();
	} catch (RuntimeException ex) {
		throw ex;
	} catch (Exception ex) {
		throw new PrivilegedActionException(ex);
	}
}

/**
 * Performs the privileged action specified by <code>action</code>.
 * <p>
 * When permission checks are made, if the permission has been granted by all
 * frames below and including the one representing the call to this method,
 * then the permission is granted iff it is granted by the AccessControlContext
 * <code>context</code>. In otherwords, no more checking of the current stack
 * is performed. Instead, the passed in context is checked.
 *
 * Any unchecked exception generated by this method will propagate up the chain.
 * However, checked exceptions will be caught an re-thrown as PrivilegedActionExceptions
 * @param <T>
 *
 * @param action The PrivilegedExceptionAction to performed
 * @param context The AccessControlContext to check
 *
 * @return the result of the PrivilegedExceptionAction
 *
 * @throws PrivilegedActionException when a checked exception occurs when performing the action
 * 			NullPointerException if action is null
 *
 * @see 		#doPrivileged(PrivilegedAction)
 */
@sun.reflect.CallerSensitive
public static <T> T doPrivileged (PrivilegedExceptionAction<T> action, AccessControlContext context)
	throws PrivilegedActionException
{
	try {
		T result = action.run();
		keepalive(context);
		return result;
	} catch (RuntimeException ex) {
		throw ex;
	} catch (Exception ex) {
		throw new PrivilegedActionException(ex);
	}
}

/**
 * Performs the privileged action specified by <code>action</code>, retaining
 * any current DomainCombiner.
 * <p>
 * When permission checks are made, if the permission has been granted by all
 * frames below and including the one representing the call to this method,
 * then the permission is granted. In otherwords, the check stops here.
 *
 * Any unchecked exception generated by this method will propagate up the chain.
 * @param <T>
 *
 * @param action The PrivilegedAction to performed
 *
 * @return the result of the PrivilegedAction
 *
 * @see 		#doPrivileged(PrivilegedAction)
 */
@sun.reflect.CallerSensitive
public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
	AccessControlContext accWithCombiner = null;
	Object[] domains = getProtectionDomains(1);
	AccessControlContext acc = (AccessControlContext)domains[0];

	if (acc != null) {
		ProtectionDomain[] pDomains = null;
		if (acc.domainCombiner == null) {
			pDomains = toArrayOfProtectionDomains(domains, acc);
		}
		accWithCombiner = new AccessControlContext(pDomains, false);
		accWithCombiner.domainCombiner = acc.domainCombiner;
	}
	return doPrivileged(action, accWithCombiner);
}

/**
 * Performs the privileged action specified by <code>action</code>, retaining
 * any current DomainCombiner.
 * <p>
 * When permission checks are made, if the permission has been granted by all
 * frames below and including the one representing the call to this method,
 * then the permission is granted. In otherwords, the check stops here.
 *
 * Any unchecked exception generated by this method will propagate up the chain.
 * However, checked exceptions will be caught an re-thrown as PrivilegedActionExceptions
 * @param <T>
 *
 * @param action The PrivilegedExceptionAction to performed
 *
 * @return the result of the PrivilegedExceptionAction
 *
 * @throws PrivilegedActionException when a checked exception occurs when performing the action
 *
 * @see 		#doPrivileged(PrivilegedAction)
 */
@sun.reflect.CallerSensitive
public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action)
	throws PrivilegedActionException
{
	AccessControlContext accWithCombiner = null;
	Object[] domains = getProtectionDomains(1);
	AccessControlContext acc = (AccessControlContext)domains[0];

	if (acc != null) {
		ProtectionDomain[] pDomains = null;
		if (acc.domainCombiner == null) {
			pDomains = toArrayOfProtectionDomains(domains, acc);
		}
		accWithCombiner = new AccessControlContext(pDomains, false);
		accWithCombiner.domainCombiner = acc.domainCombiner;
	}
	return doPrivileged(action, accWithCombiner);
}
}
