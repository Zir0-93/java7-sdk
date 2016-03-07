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


class DirectDoubleBufferS

    extends DoubleBuffer



    implements DirectBuffer
{



    // Cached unsafe-access object
    protected static final Unsafe unsafe = Bits.unsafe();

    // Cached array base offset
    private static final long arrayBaseOffset = (long)unsafe.arrayBaseOffset(double[].class);

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
    DirectDoubleBufferS(DirectBuffer db,         // package-private
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

    public DoubleBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 3);
        assert (off >= 0);
        return new DirectDoubleBufferS(this, -1, 0, rem, rem, off);
    }

    public DoubleBuffer duplicate() {
        return new DirectDoubleBufferS(this,
                                              this.markValue(),
                                              this.position(),
                                              this.limit(),
                                              this.capacity(),
                                              0);
    }

    public DoubleBuffer asReadOnlyBuffer() {

        return new DirectDoubleBufferRS(this,
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
        return address + (i << 3);
    }

    public double get() {
        if (RCMSupport.isRCMSupported() && fd != null) {
            return Double.longBitsToDouble(Bits.swap(unsafe.rcmGetLong(ix(nextGetIndex()))));
        }
        return Double.longBitsToDouble(Bits.swap(unsafe.getLong(ix(nextGetIndex()))));
    }

    public double get(int i) {
        if (RCMSupport.isRCMSupported() && fd != null) {
            return Double.longBitsToDouble(Bits.swap(unsafe.rcmGetLong(ix(nextGetIndex()))));
        }
        return Double.longBitsToDouble(Bits.swap(unsafe.getLong(ix(checkIndex(i)))));
    }

    public DoubleBuffer get(double[] dst, int offset, int length) {

        if ((length << 3) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
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
                    Bits.rcmCopyToLongArray(ix(pos), dst,
                                              offset << 3,
                                              length << 3);
                } else {
                    Bits.copyToLongArray(ix(pos), dst,
                                              offset << 3,
                                              length << 3);
                }
            else

                // add rcm code, should be tracked as read
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyToArray(ix(pos), dst, arrayBaseOffset,
                                     offset << 3,
                                     length << 3);
                } else {
                    Bits.copyToArray(ix(pos), dst, arrayBaseOffset,
                                     offset << 3,
                                     length << 3);
                }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;



    }



    public DoubleBuffer put(double x) {

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutLong(ix(nextPutIndex()), Bits.swap(Double.doubleToRawLongBits(x)));
        } else {
            unsafe.putLong(ix(nextPutIndex()), Bits.swap(Double.doubleToRawLongBits(x)));
        }
        Bits.keepAlive(this);
        return this;



    }

    public DoubleBuffer put(int i, double x) {

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutLong(ix(checkIndex(i)), Bits.swap(Double.doubleToRawLongBits(x)));
        } else {
            unsafe.putLong(ix(checkIndex(i)), Bits.swap(Double.doubleToRawLongBits(x)));
        }
        Bits.keepAlive(this);
        return this;



    }

    public DoubleBuffer put(DoubleBuffer src) {

        if (src instanceof DirectDoubleBufferS) {
            if (src == this)
                throw new IllegalArgumentException();
            DirectDoubleBufferS sb = (DirectDoubleBufferS)src;

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
                                  srem << 3);
            } else {
                unsafe.copyMemory(sb.ix(spos), ix(pos), 
                                  srem << 3);
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

    public DoubleBuffer put(double[] src, int offset, int length) {

        if ((length << 3) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
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
                    Bits.rcmCopyFromLongArray(src, 
                            offset << 3,
                            ix(pos), length << 3);
                } else {
                    Bits.copyFromLongArray(src, 
                            offset << 3,
                            ix(pos), length << 3);
                }
            else

                // add rcm code, should be tracked as write
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyFromArray(src, arrayBaseOffset, 
                            offset << 3,
                            ix(pos), length << 3);
                } else {
                    Bits.copyFromArray(src, arrayBaseOffset, 
                            offset << 3,
                            ix(pos), length << 3);
                }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;



    }

    public DoubleBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        unsafe.copyMemory(ix(pos), ix(0), rem << 3);
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