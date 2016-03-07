package java.lang.ref;

/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2003, 2014  All Rights Reserved
 */

class FinalReference<T> extends Reference<T> {

	public FinalReference(T referent, ReferenceQueue<? super T> q) {
		initReference(referent, q);
	}

}
