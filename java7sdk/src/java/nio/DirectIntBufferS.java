/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

// -- This file was mechanically generated: Do not edit! -- //

package java.nio;

import java.io.FileDescriptor;
import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.misc.VM;
import sun.nio.ch.DirectBuffer;
import com.ibm.jvm.util.RCMSupport;


class DirectIntBufferS

    extends IntBuffer



    implements DirectBuffer
{



    // Cached unsafe-access object
    protected static final Unsafe unsafe = Bits.unsafe();

    // Cached array base offset
    private static final long arrayBaseOffset = (long)unsafe.arrayBaseOffset(int[].class);

    // Cached unaligned-access capability
    protected static final boolean unaligned = Bits.unaligned();

    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
    //    protected long address;

    // An object attached to this buffer. If this buffer is a view of another
    // buffer then we use this field to keep a reference to that buffer to
    // ensure that its memory isn't freed before we are done with it.
    private final Object att;

    public Object attachment() {
        return att;
    }








































    public Cleaner cleaner() { return null; }

    FileDescriptor fd = null;
















































































    // For duplicates and slices
    //
    DirectIntBufferS(DirectBuffer db,         // package-private
                               int mark, int pos, int lim, int cap,
                               int off)
    {

        super(mark, pos, lim, cap);
        address = db.address() + off;



        if (db instanceof MappedByteBuffer) {
            fd = ((MappedByteBuffer)db).getFD();
        }

        att = db;




    }

    public IntBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 2);
        assert (off >= 0);
        return new DirectIntBufferS(this, -1, 0, rem, rem, off);
    }

    public IntBuffer duplicate() {
        return new DirectIntBufferS(this,
                                              this.markValue(),
                                              this.position(),
                                              this.limit(),
                                              this.capacity(),
                                              0);
    }

    public IntBuffer asReadOnlyBuffer() {

        return new DirectIntBufferRS(this,
                                           this.markValue(),
                                           this.position(),
                                           this.limit(),
                                           this.capacity(),
                                           0);



    }



    public long address() {
        return address;
    }

    private long ix(int i) {
        return address + (i << 2);
    }

    public int get() {
        if (RCMSupport.isRCMSupported() && fd != null) {
            return (Bits.swap(unsafe.rcmGetInt(ix(nextGetIndex()))));
        }
        return (Bits.swap(unsafe.getInt(ix(nextGetIndex()))));
    }

    public int get(int i) {
        if (RCMSupport.isRCMSupported() && fd != null) {
            return (Bits.swap(unsafe.rcmGetInt(ix(nextGetIndex()))));
        }
        return (Bits.swap(unsafe.getInt(ix(checkIndex(i)))));
    }

    public IntBuffer get(int[] dst, int offset, int length) {

        if ((length << 2) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferUnderflowException();


            if (order() != ByteOrder.nativeOrder())
                // add rcm code, should be tracked as read
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyToIntArray(ix(pos), dst,
                                              offset << 2,
                                              length << 2);
                } else {
                    Bits.copyToIntArray(ix(pos), dst,
                                              offset << 2,
                                              length << 2);
                }
            else

                // add rcm code, should be tracked as read
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyToArray(ix(pos), dst, arrayBaseOffset,
                                     offset << 2,
                                     length << 2);
                } else {
                    Bits.copyToArray(ix(pos), dst, arrayBaseOffset,
                                     offset << 2,
                                     length << 2);
                }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;



    }



    public IntBuffer put(int x) {

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutInt(ix(nextPutIndex()), Bits.swap((x)));
        } else {
            unsafe.putInt(ix(nextPutIndex()), Bits.swap((x)));
        }
        Bits.keepAlive(this);
        return this;



    }

    public IntBuffer put(int i, int x) {

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutInt(ix(checkIndex(i)), Bits.swap((x)));
        } else {
            unsafe.putInt(ix(checkIndex(i)), Bits.swap((x)));
        }
        Bits.keepAlive(this);
        return this;



    }

    public IntBuffer put(IntBuffer src) {

        if (src instanceof DirectIntBufferS) {
            if (src == this)
                throw new IllegalArgumentException();
            DirectIntBufferS sb = (DirectIntBufferS)src;

            int spos = sb.position();
            int slim = sb.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);

            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);

            if (srem > rem)
                throw new BufferOverflowException();
            // add rcm code, should be tracked as both read and write
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmCopyMemory(sb.ix(spos), ix(pos), 
                                  srem << 2);
            } else {
                unsafe.copyMemory(sb.ix(spos), ix(pos), 
                                  srem << 2);
            }
            Bits.keepAlive(this);
            sb.position(spos + srem);
            position(pos + srem);
        } else if (src.hb != null) {

            int spos = src.position();
            int slim = src.limit();
            assert (spos <= slim);
            int srem = (spos <= slim ? slim - spos : 0);

            put(src.hb, src.offset + spos, srem);
            src.position(spos + srem);

        } else {
            super.put(src);
        }
        return this;



    }

    public IntBuffer put(int[] src, int offset, int length) {

        if ((length << 2) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferOverflowException();


            if (order() != ByteOrder.nativeOrder())
                // add rcm code, should be tracked as write
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyFromIntArray(src, 
                            offset << 2,
                            ix(pos), length << 2);
                } else {
                    Bits.copyFromIntArray(src, 
                            offset << 2,
                            ix(pos), length << 2);
                }
            else

                // add rcm code, should be tracked as write
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyFromArray(src, arrayBaseOffset, 
                            offset << 2,
                            ix(pos), length << 2);
                } else {
                    Bits.copyFromArray(src, arrayBaseOffset, 
                            offset << 2,
                            ix(pos), length << 2);
                }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;



    }

    public IntBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        unsafe.copyMemory(ix(pos), ix(0), rem << 2);
        Bits.keepAlive(this);
        position(rem);
        limit(capacity());
        discardMark();
        return this;



    }

    public boolean isDirect() {
        return true;
    }

    public boolean isReadOnly() {
        return false;
    }















































    public ByteOrder order() {

        return ((ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);





    }





































}
