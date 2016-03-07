/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v7
 * (C) Copyright IBM Corp. 2014, 2014. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 *  2010-10-14: this file was modified by International Business Machines Corporation.
 *  Modifications Copyright 2010 IBM Corporation.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package java.util.concurrent.atomic;

/**
 * An <tt>AtomicStampedReference</tt> maintains an object reference
 * along with an integer "stamp", that can be updated atomically.  
 *
 * <p> Implementation note. This implementation maintains stamped
 * references by creating internal objects representing "boxed"
 * [reference, integer] pairs.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The type of object referred to by this reference
 */
public class AtomicStampedReference<V>  {

    private static class ReferenceIntegerPair<T> {
        private final int padRef;
        private final int integer;
        private final T reference;
        ReferenceIntegerPair(T r, int i) {
            reference = r; integer = i; padRef = 0;
        }
    }

    private static final ReferenceIntegerPair staticRef = new ReferenceIntegerPair(null, 0);

    private final AtomicReference<ReferenceIntegerPair<V>>  atomicRef;

    private static boolean doubleWordCASSupported(ReferenceIntegerPair p) { return false; }

    private boolean doubleWordCAS(ReferenceIntegerPair p, V newReference, V oldReference, int newInteger, int oldInteger)
       {
       return false;
       }

    private static boolean doubleWordSetSupported(ReferenceIntegerPair p) { return false; }

    private void doubleWordSet(ReferenceIntegerPair p, V newReference, int newInteger) 
       {
	 //return false;
       }


    /**
     * Creates a new <tt>AtomicStampedReference</tt> with the given
     * initial values.
     *
     * @param initialRef the initial reference
     * @param initialStamp the initial stamp
     */
    public AtomicStampedReference(V initialRef, int initialStamp) {
        atomicRef = new AtomicReference<ReferenceIntegerPair<V>>
            (new ReferenceIntegerPair<V>(initialRef, initialStamp));
    }

    /**
     * Returns the current value of the reference.
     *
     * @return the current value of the reference
     */
    public V getReference() {
        return atomicRef.get().reference;
    }

    /**
     * Returns the current value of the stamp.
     *
     * @return the current value of the stamp
     */
    public int getStamp() {
        return atomicRef.get().integer;
    }

    /**
     * Returns the current values of both the reference and the stamp.
     * Typical usage is <tt>int[1] holder; ref = v.get(holder); </tt>.
     *
     * @param stampHolder an array of size of at least one.  On return,
     * <tt>stampholder[0]</tt> will hold the value of the stamp.
     * @return the current value of the reference
     */
    public V get(int[] stampHolder) {
        ReferenceIntegerPair<V> p = atomicRef.get();
        stampHolder[0] = p.integer;
        return p.reference;
    }

    /**
     * Atomically sets the value of both the reference and stamp
     * to the given update values if the
     * current reference is <tt>==</tt> to the expected reference
     * and the current stamp is equal to the expected stamp.  Any given
     * invocation of this operation may fail (return
     * <tt>false</tt>) spuriously, but repeated invocation when
     * the current value holds the expected value and no other thread
     * is also attempting to set the value will eventually succeed.
     *
     * @param expectedReference the expected value of the reference
     * @param newReference the new value for the reference
     * @param expectedStamp the expected value of the stamp
     * @param newStamp the new value for the stamp
     * @return true if successful
     */
    public boolean weakCompareAndSet(V      expectedReference,
                                     V      newReference,
                                     int    expectedStamp,
                                     int    newStamp) {
        ReferenceIntegerPair<V> current = atomicRef.get();

        if (doubleWordCASSupported(current)) 
	   return doubleWordCAS(current, newReference, expectedReference, newStamp, expectedStamp);
        else
           return  expectedReference == current.reference &&
            expectedStamp == current.integer &&
            ((newReference == current.reference &&
              newStamp == current.integer) ||
             atomicRef.weakCompareAndSet(current,
                                     new ReferenceIntegerPair<V>(newReference,
                                                              newStamp)));
    }

    /**
     * Atomically sets the value of both the reference and stamp
     * to the given update values if the
     * current reference is <tt>==</tt> to the expected reference
     * and the current stamp is equal to the expected stamp. 
     *
     * @param expectedReference the expected value of the reference
     * @param newReference the new value for the reference
     * @param expectedStamp the expected value of the stamp
     * @param newStamp the new value for the stamp
     * @return true if successful
     */
    public boolean compareAndSet(V      expectedReference,
                                 V      newReference,
                                 int    expectedStamp,
                                 int    newStamp) {
        ReferenceIntegerPair<V> current = atomicRef.get();

        if (doubleWordCASSupported(current)) 
	   return doubleWordCAS(current, newReference, expectedReference, newStamp, expectedStamp);
        else
           return  expectedReference == current.reference &&
            expectedStamp == current.integer &&
            ((newReference == current.reference &&
              newStamp == current.integer) ||
             atomicRef.compareAndSet(current,
                                     new ReferenceIntegerPair<V>(newReference,
                                                              newStamp)));
    }


    /**
     * Unconditionally sets the value of both the reference and stamp.
     *
     * @param newReference the new value for the reference
     * @param newStamp the new value for the stamp
     */
    public void set(V newReference, int newStamp) {
        ReferenceIntegerPair<V> current = atomicRef.get();

        if (doubleWordSetSupported(current))  
           doubleWordSet(current, newReference, newStamp);
        else if (newReference != current.reference || newStamp != current.integer)
            atomicRef.set(new ReferenceIntegerPair<V>(newReference, newStamp));
    }

    /**
     * Atomically sets the value of the stamp to the given update value
     * if the current reference is <tt>==</tt> to the expected
     * reference.  Any given invocation of this operation may fail
     * (return <tt>false</tt>) spuriously, but repeated invocation
     * when the current value holds the expected value and no other
     * thread is also attempting to set the value will eventually
     * succeed.
     *
     * @param expectedReference the expected value of the reference
     * @param newStamp the new value for the stamp
     * @return true if successful
     */
    public boolean attemptStamp(V expectedReference, int newStamp) {
        ReferenceIntegerPair<V> current = atomicRef.get();
        if (doubleWordCASSupported(current)) 
	   return doubleWordCAS(current, expectedReference, expectedReference, newStamp, current.integer);
        else
            return  expectedReference == current.reference &&
            (newStamp == current.integer ||
             atomicRef.compareAndSet(current,
                                     new ReferenceIntegerPair<V>(expectedReference,
                                                              newStamp)));
    }
}








