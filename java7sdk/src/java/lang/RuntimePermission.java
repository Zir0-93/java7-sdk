package java.lang;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 1998, 2014  All Rights Reserved
 */

/**
 * RuntimePermission objects represent access to runtime
 * support.
 *
 * @author		OTI
 * @version		initial
 */
public final class RuntimePermission extends java.security.BasicPermission {
	private static final long serialVersionUID = 7399184964622342223L;

	/**
	 * Constants for runtime permissions used in this package.
	 */
	static final RuntimePermission permissionToSetSecurityManager =
		new RuntimePermission("setSecurityManager");
	static final RuntimePermission permissionToCreateSecurityManager =
		new RuntimePermission("createSecurityManager");
	static final RuntimePermission permissionToGetProtectionDomain =
		new RuntimePermission("getProtectionDomain");
	static final RuntimePermission permissionToGetClassLoader =
		new RuntimePermission("getClassLoader");
	static final RuntimePermission permissionToCreateClassLoader =
		new RuntimePermission("createClassLoader");
	static final RuntimePermission permissionToModifyThread =
		new RuntimePermission("modifyThread");
	static final RuntimePermission permissionToModifyThreadGroup =
		new RuntimePermission("modifyThreadGroup");
	static final RuntimePermission permissionToExitVM =
		new RuntimePermission("exitVM");
	static final RuntimePermission permissionToReadFileDescriptor =
		new RuntimePermission("readFileDescriptor");
	static final RuntimePermission permissionToWriteFileDescriptor =
		new RuntimePermission("writeFileDescriptor");
	static final RuntimePermission permissionToQueuePrintJob =
		new RuntimePermission("queuePrintJob");
	static final RuntimePermission permissionToSetFactory =
		new RuntimePermission("setFactory");
	static final RuntimePermission permissionToSetIO =
		new RuntimePermission("setIO");
	static final RuntimePermission permissionToStopThread =
		new RuntimePermission("stopThread");
	static final RuntimePermission permissionToSetContextClassLoader =
		new RuntimePermission("setContextClassLoader");

/**
 * Creates an instance of this class with the given name.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		permissionName String
 *					the name of the new permission.
 */
public RuntimePermission(java.lang.String permissionName)
{
	super(permissionName);
}

/**
 * Creates an instance of this class with the given name and
 * action list. The action list is ignored.
 *
 * @author		OTI
 * @version		initial
 *
 * @param		name String
 *					the name of the new permission.
 * @param		actions String
 *					ignored.
 */
public RuntimePermission(java.lang.String name, java.lang.String actions)
{
	super(name, actions);
}

}
