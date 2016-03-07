package java.lang.ref;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2002, 2014  All Rights Reserved
 */

class Finalizer {

// called by java.lang.Runtime.runFinalization0
static void runFinalization() {
	runFinalizationImpl();
}

private static native void runFinalizationImpl();

// called by java.lang.Shutdown.runAllFinalizers native
// invoked when Runtime.runFinalizersOnExit() was called with true
static void runAllFinalizers() {
	runAllFinalizersImpl();
}

private static native void runAllFinalizersImpl();
}
