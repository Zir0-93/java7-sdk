
package java.security;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2014  All Rights Reserved
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.ibm.oti.security.CheckedAccessControlContext;
import com.ibm.oti.security.ContextCombine;

/**
 * An AccessControlContext encapsulates the information which is needed
 * by class AccessController to detect if a Permission would be granted
 * at a particular point in a programs execution.
 *
 * @author		OTI
 * @version		initial
 */

public final class AccessControlContext {

	static int debugSetting = -1;
	DomainCombiner domainCombiner;
	ProtectionDomain[] context;
	boolean isAuthorized = false;

	private static final SecurityPermission createAccessControlContext =
		new SecurityPermission("createAccessControlContext");
	private static final SecurityPermission getDomainCombiner =
		new SecurityPermission("getDomainCombiner");

	static final int DEBUG_ACCESS = 1;
	static final int DEBUG_ACCESS_STACK = 2;
	static final int DEBUG_ACCESS_DOMAIN = 4;
	static final int DEBUG_ACCESS_FAILURE = 8;
	static final int DEBUG_ACCESS_THREAD = 0x10;
	static final int DEBUG_ALL = 0xff;

	static {
		ContextCombine c = new ContextCombine() {
			public java.security.AccessControlContext combineDomains(AccessControlContext acc1, AccessControlContext acc2) {
				Set set;
				if (acc1.context != null) {
					set = new HashSet(Arrays.asList(acc1.context));
				} else {
					set = new HashSet();
				}
				if (acc2.context != null) {
					set.addAll(Arrays.asList(acc2.context));
				}
				ProtectionDomain[] myDomains = (ProtectionDomain[]) set.toArray(new ProtectionDomain[set.size()]);
				java.security.AccessControlContext myContext = new java.security.AccessControlContext(myDomains, false);
				return myContext;
			}
		};
		CheckedAccessControlContext.setCombiner(c);
	}

static int debugSetting() {
	if (debugSetting != -1) return debugSetting;
	debugSetting = 0;
	boolean access = false;
	String value = (String)AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
			return System.getProperty("java.security.debug");
		}});
	if (value == null) return debugSetting;
	int start = 0;
	int length = value.length();
	while (start < length) {
		int index = value.indexOf(',', start);
		if (index == -1) index = length;
		String keyword = value.substring(start, index);
		if (keyword.equals("all")) {
			debugSetting  = DEBUG_ALL;
			return debugSetting;
		} else if (keyword.startsWith("access")) {
			debugSetting |= DEBUG_ACCESS;
			if ((start + 6) < length && value.charAt(start + 6) == ':') {
				index = start + 6;
				access = true;
			}
		} else if (access && keyword.equals("stack")) {
			debugSetting |= DEBUG_ACCESS_STACK;
		} else if (access && keyword.equals("domain")) {
			debugSetting |= DEBUG_ACCESS_DOMAIN;
		} else if (access && keyword.equals("failure")) {
			debugSetting |= DEBUG_ACCESS_FAILURE;
		} else if (access && keyword.equals("thread")) {
			debugSetting |= DEBUG_ACCESS_THREAD;
		} else {
			access = false;
		}
		start = index + 1;
	}
	return debugSetting;
}

static void debugPrintAccess() {
	System.err.print("access: ");
	if ((debugSetting() & DEBUG_ACCESS_THREAD) == DEBUG_ACCESS_THREAD) {
		System.err.print("(" + Thread.currentThread() + ")");
	}
}

/**
 * Constructs a new instance of this class given an array of
 * protection domains.
 *
 * @param fromContext the array of ProtectionDomain
 *
 * @exception	NullPointerException if fromContext is null
 */
public AccessControlContext(ProtectionDomain[] fromContext) {
	int length = fromContext.length;
	if (length == 0) {
		context = null;
	} else {
		int domainIndex = 0;
		context = new ProtectionDomain[length];
		next : for (int i = 0; i < length; i++) {
			ProtectionDomain current = fromContext[i];
			if (current == null) continue;
			for (int j = 0; j < i; j++)
				if (current == context[j]) continue next;
			context[domainIndex++] = current;
		}
		if (domainIndex == 0) {
			context = null;
		} else if (domainIndex != length) {
			ProtectionDomain[] copy = new ProtectionDomain[domainIndex];
			System.arraycopy(context, 0, copy, 0, domainIndex);
			context = copy;
		}
	}
}

AccessControlContext(ProtectionDomain[] context, boolean ignored) {
	this.context = context;
	this.isAuthorized = true;
}

/**
 * Constructs a new instance of this class given a context
 * and a DomainCombiner
 *
 * @param acc the AccessControlContext
 * @param combiner the DomainCombiner
 *
 * @exception	java.security.AccessControlException thrown
 * 					when the caller doesn't have the  "createAccessControlContext" SecurityPermission
 * 				NullPointerException if the provided context is null.
 */
public AccessControlContext(AccessControlContext acc, DomainCombiner combiner) {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkPermission(createAccessControlContext);
	this.context = acc.context;
	this.isAuthorized = true;
	this.domainCombiner = combiner;
}

/**
 * Checks if the permission <code>perm</code> is allowed in this context.
 * All ProtectionDomains must grant the permission for it to be granted.
 *
 * @param		perm java.security.Permission
 *					the permission to check
 * @exception	java.security.AccessControlException
 *					thrown when perm is not granted.
 *				NullPointerException if perm is null
 */
public void checkPermission(Permission perm) throws AccessControlException {
	if (perm == null) throw new NullPointerException();
	if ((debugSetting() & DEBUG_ACCESS_DOMAIN) != 0) {
		debugPrintAccess();
		if (context == null || context.length == 0) {
			System.err.println("domain (context is null)");
		} else {
			for (int i=0; i<context.length; i++) {
				System.err.println("domain " + i + " " + context[i]);
			}
		}
	}
	int i = context == null ? 0 : context.length;
	while (--i>=0 && context[i].implies(perm)) ;
	if (i >= 0) {
		if ((debugSetting() & DEBUG_ACCESS) != 0) {
			debugPrintAccess();
			System.err.println("access denied " + perm);
		}
		if ((debugSetting() & DEBUG_ACCESS_FAILURE) != 0) {
			new Exception("Stack trace").printStackTrace();
			System.err.println("domain that failed " + context[i]);
		}
		// K002c = Access denied {0}
		throw new AccessControlException(com.ibm.oti.util.Msg.getString("K002c", perm), perm);
	}
	if ((debugSetting() & DEBUG_ACCESS) != 0) {
		debugPrintAccess();
		System.err.println("access allowed " + perm);
	}
}

/**
 * Compares the argument to the receiver, and answers true
 * if they represent the <em>same</em> object using a class
 * specific comparison. In this case, they must both be
 * AccessControlContexts and contain the same protection domains.
 *
 * @param		o		the object to compare with this object
 * @return		<code>true</code>
 *					if the object is the same as this object
 *				<code>false</code>
 *					if it is different from this object
 * @see			#hashCode
 */
public boolean equals(Object o) {
	if (this == o) return true;
	if (o == null || this.getClass() != o.getClass()) return false;
	AccessControlContext otherContext = (AccessControlContext) o;
	ProtectionDomain[] otherDomains = otherContext.context;
	int length = context == null ? 0 : context.length;
	int olength = otherDomains == null ? 0 : otherDomains.length;
	if (length != olength) return false;

	next : for (int i = 0; i < length; i++) {
		ProtectionDomain current = context[i];
		for (int j = 0; j < length; j++)
			if (current == otherDomains[j]) continue next;
		return false;
	}
	return true;
}

/**
 * Answers an integer hash code for the receiver. Any two
 * objects which answer <code>true</code> when passed to
 * <code>equals</code> must answer the same value for this
 * method.
 *
 * @return		the receiver's hash
 *
 * @see			#equals
 */
public int hashCode() {
	int result=0;
	int i = context == null ? 0 : context.length;
	while (--i>=0)
		result ^= context[i].hashCode();
	return result;
}

/**
 * Answers the DomainCombiner for the receiver.
 *
 * @return the DomainCombiner or null
 *
 * @exception	java.security.AccessControlException thrown
 * 					when the caller doesn't have the  "getDomainCombiner" SecurityPermission
 */
public DomainCombiner getDomainCombiner() {
	SecurityManager security = System.getSecurityManager();
	if (security != null)
		security.checkPermission(getDomainCombiner);
	return domainCombiner;
}

/*
 * Added to resolve: S6907662, CVE-2010-4465: System clipboard should ensure access restrictions
 * Used internally:
 *  	java.awt.AWTEvent
 *		java.awt.Component
 *		java.awt.EventQueue
 *		java.awt.MenuComponent
 *		java.awt.TrayIcon
 *		java.security.ProtectionDomain
 *		javax.swing.Timer
 *		javax.swing.TransferHandler
 */
ProtectionDomain[] getContext() {
	return context;
}

/*
 * Added to resolve: S6907662, CVE-2010-4465: System clipboard should ensure access restrictions
 * Basically a copy of AccessController.toArrayOfProtectionDomains().
 * Called internally from java.security.ProtectionDomain
 */
AccessControlContext(ProtectionDomain[] domains, AccessControlContext acc) {
	int len = 0, size = domains == null ? 0 : domains.length;
	int extra = 0;
	if (acc != null && acc.context != null) {
		extra = acc.context.length;
	}
	ProtectionDomain[] answer = new ProtectionDomain[size + extra];
	for (int i = 0; i < size; i++) {
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
	if (len == 0 && acc != null) context = acc.context;
	else
	if (len + extra == 0) {
		context = null;
	} else {
		if (len < size) {
			ProtectionDomain[] copy = new ProtectionDomain[len + extra];
			System.arraycopy(answer, 0, copy, 0, len);
			answer = copy;
		}
		if (acc != null && acc.context != null)
			System.arraycopy(acc.context, 0, answer, len, acc.context.length);
		context = answer;
	}
	isAuthorized = true;
}

/*
 * Added to resolve: S6907662, CVE-2010-4465: System clipboard should ensure access restrictions
 * Called internally from java.security.ProtectionDomain
 */
AccessControlContext optimize() {
	return this;
}

}
