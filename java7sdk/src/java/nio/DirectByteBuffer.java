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


class DirectByteBuffer

    extends MappedByteBuffer



    implements DirectBuffer
{



    // Cached unsafe-access object
    protected static final Unsafe unsafe = Bits.unsafe();

    // Cached array base offset
    private static final long arrayBaseOffset = (long)unsafe.arrayBaseOffset(byte[].class);

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



    private static class Deallocator
        implements Runnable
    {

        private static Unsafe unsafe = Unsafe.getUnsafe();

        private long address;
        private long size;
        private int capacity;

        private Deallocator(long address, long size, int capacity) {
            assert (address != 0);
            this.address = address;
            this.size = size;
            this.capacity = capacity;
        }

        public void run() {
            if (address == 0) {
                // Paranoia
                return;
            }
            unsafe.freeDBBMemory(address);
            address = 0;
            Bits.unreserveMemory(size, capacity);
        }

    }

    private final Cleaner cleaner;

    public Cleaner cleaner() { return cleaner; }

    FileDescriptor fd = getFD();













    // Primary constructor
    //
    DirectByteBuffer(int cap) {                   // package-private

        super(-1, 0, cap, cap);
        boolean pa = VM.isDirectMemoryPageAligned();
        int ps = Bits.pageSize();
        long size = Math.max(1L, (long)cap + (pa ? ps : 0));
        Bits.reserveMemory(size, cap);

        long base = 0;
        try {
            base = unsafe.allocateDBBMemory(size);
        } catch (OutOfMemoryError x) {
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        unsafe.setMemory(base, size, (byte) 0);
        if (pa && (base % ps != 0)) {
            // Round up to page boundary
            address = base + ps - (base & (ps - 1));
        } else {
            address = base;
        }
        cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
        att = null;



    }



    // Invoked to construct a direct ByteBuffer referring to the block of
    // memory. A given arbitrary object may also be attached to the buffer.
    //
    DirectByteBuffer(long addr, int cap, Object ob) {
        super(-1, 0, cap, cap);
        address = addr;
        cleaner = null;
        att = ob;
    }


    // Invoked only by JNI: NewDirectByteBuffer(void*, long)
    //
    private DirectByteBuffer(long addr, int cap) {
        super(-1, 0, cap, cap);
        address = addr;
        cleaner = null;
        att = null;
    }



    // For memory-mapped buffers -- invoked by FileChannelImpl via reflection
    //
    protected DirectByteBuffer(int cap, long addr,
                                     FileDescriptor fd,
                                     Runnable unmapper)
    {

        super(-1, 0, cap, cap, fd);
        address = addr;
        cleaner = Cleaner.create(this, unmapper);
        att = null;



    }



    // For duplicates and slices
    //
    DirectByteBuffer(DirectBuffer db,         // package-private
                               int mark, int pos, int lim, int cap,
                               int off)
    {

        super(mark, pos, lim, cap);
        address = db.address() + off;

        cleaner = null;





        att = db;




    }

    public ByteBuffer slice() {
        int pos = this.position();
        int lim = this.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);
        int off = (pos << 0);
        assert (off >= 0);
        return new DirectByteBuffer(this, -1, 0, rem, rem, off);
    }

    public ByteBuffer duplicate() {
        return new DirectByteBuffer(this,
                                              this.markValue(),
                                              this.position(),
                                              this.limit(),
                                              this.capacity(),
                                              0);
    }

    public ByteBuffer asReadOnlyBuffer() {

        return new DirectByteBufferR(this,
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
        return address + (i << 0);
    }

    public byte get() {
        if (RCMSupport.isRCMSupported() && fd != null) {
            return ((unsafe.rcmGetByte(ix(nextGetIndex()))));
        }
        return ((unsafe.getByte(ix(nextGetIndex()))));
    }

    public byte get(int i) {
        if (RCMSupport.isRCMSupported() && fd != null) {
            return ((unsafe.rcmGetByte(ix(nextGetIndex()))));
        }
        return ((unsafe.getByte(ix(checkIndex(i)))));
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {

        if ((length << 0) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
            checkBounds(offset, length, dst.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferUnderflowException();















                // add rcm code, should be tracked as read
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyToArray(ix(pos), dst, arrayBaseOffset,
                                     offset << 0,
                                     length << 0);
                } else {
                    Bits.copyToArray(ix(pos), dst, arrayBaseOffset,
                                     offset << 0,
                                     length << 0);
                }
            position(pos + length);
        } else {
            super.get(dst, offset, length);
        }
        return this;



    }



    public ByteBuffer put(byte x) {

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutByte(ix(nextPutIndex()), ((x)));
        } else {
            unsafe.putByte(ix(nextPutIndex()), ((x)));
        }
        Bits.keepAlive(this);
        return this;



    }

    public ByteBuffer put(int i, byte x) {

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutByte(ix(checkIndex(i)), ((x)));
        } else {
            unsafe.putByte(ix(checkIndex(i)), ((x)));
        }
        Bits.keepAlive(this);
        return this;



    }

    public ByteBuffer put(ByteBuffer src) {

        if (src instanceof DirectByteBuffer) {
            if (src == this)
                throw new IllegalArgumentException();
            DirectByteBuffer sb = (DirectByteBuffer)src;

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
                                  srem << 0);
            } else {
                unsafe.copyMemory(sb.ix(spos), ix(pos), 
                                  srem << 0);
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

    public ByteBuffer put(byte[] src, int offset, int length) {

        if ((length << 0) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
            checkBounds(offset, length, src.length);
            int pos = position();
            int lim = limit();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            if (length > rem)
                throw new BufferOverflowException();















                // add rcm code, should be tracked as write
                if (RCMSupport.isRCMSupported() && fd != null) {
                    Bits.rcmCopyFromArray(src, arrayBaseOffset, 
                            offset << 0,
                            ix(pos), length << 0);
                } else {
                    Bits.copyFromArray(src, arrayBaseOffset, 
                            offset << 0,
                            ix(pos), length << 0);
                }
            position(pos + length);
        } else {
            super.put(src, offset, length);
        }
        return this;



    }

    public ByteBuffer compact() {

        int pos = position();
        int lim = limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        unsafe.copyMemory(ix(pos), ix(0), rem << 0);
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
































































    byte _get(int i) {                          // package-private
        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            return unsafe.rcmGetByte(address + i); 
        } else {
            return unsafe.getByte(address + i);
        }
    }

    void _put(int i, byte b) {                  // package-private

        // add rcm code
        if (RCMSupport.isRCMSupported() && fd != null) {
            unsafe.rcmPutByte(address + i, b);
        } else {
            unsafe.putByte(address + i, b);
        }
        Bits.keepAlive(this);



    }




    private char getChar(long a) {
        if (unaligned) {
            char x = (RCMSupport.isRCMSupported() && fd != null) ? unsafe.rcmGetChar(a) 
                                     : unsafe.getChar(a);
            char y = (nativeByteOrder ? x : Bits.swap(x));
            Bits.keepAlive(this);
            return y;
        }
        char y = (RCMSupport.isRCMSupported() && fd != null) ? Bits.rcmGetChar(a, bigEndian) 
                                : Bits.getChar(a, bigEndian);
        Bits.keepAlive(this);
        return y;
    }

    public char getChar() {
        char y = getChar(ix(nextGetIndex((1 << 1))));
        Bits.keepAlive(this);
        return y;
    }

    public char getChar(int i) {
        char y = getChar(ix(checkIndex(i, (1 << 1))));
        Bits.keepAlive(this);
        return y;
    }



    private ByteBuffer putChar(long a, char x) {

        if (unaligned) {
            char y = (x);
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmPutChar(a, (nativeByteOrder ? y : Bits.swap(y)));
            } else {
                unsafe.putChar(a, (nativeByteOrder ? y : Bits.swap(y)));
            }
            Bits.keepAlive(this);
        } else {
            if (RCMSupport.isRCMSupported() && fd != null) {
                Bits.rcmPutChar(a, x, bigEndian);
            } else {
                Bits.putChar(a, x, bigEndian);
            }
            Bits.keepAlive(this);
        }
        return this;



    }

    public ByteBuffer putChar(char x) {

        putChar(ix(nextPutIndex((1 << 1))), x);
        return this;



    }

    public ByteBuffer putChar(int i, char x) {

        putChar(ix(checkIndex(i, (1 << 1))), x);
        return this;



    }

    public CharBuffer asCharBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 1;
        if (!unaligned && ((address + off) % (1 << 1) != 0)) {
            return (bigEndian
                    ? (CharBuffer)(new ByteBufferAsCharBufferB(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off))
                    : (CharBuffer)(new ByteBufferAsCharBufferL(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off)));
        } else {
            return (nativeByteOrder
                    ? (CharBuffer)(new DirectCharBufferU(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off))
                    : (CharBuffer)(new DirectCharBufferS(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off)));
        }
    }




    private short getShort(long a) {
        if (unaligned) {
            short x = (RCMSupport.isRCMSupported() && fd != null) ? unsafe.rcmGetShort(a) 
                                     : unsafe.getShort(a);
            short y = (nativeByteOrder ? x : Bits.swap(x));
            Bits.keepAlive(this);
            return y;
        }
        short y = (RCMSupport.isRCMSupported() && fd != null) ? Bits.rcmGetShort(a, bigEndian) 
                                : Bits.getShort(a, bigEndian);
        Bits.keepAlive(this);
        return y;
    }

    public short getShort() {
        short y = getShort(ix(nextGetIndex((1 << 1))));
        Bits.keepAlive(this);
        return y;
    }

    public short getShort(int i) {
        short y = getShort(ix(checkIndex(i, (1 << 1))));
        Bits.keepAlive(this);
        return y;
    }



    private ByteBuffer putShort(long a, short x) {

        if (unaligned) {
            short y = (x);
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmPutShort(a, (nativeByteOrder ? y : Bits.swap(y)));
            } else {
                unsafe.putShort(a, (nativeByteOrder ? y : Bits.swap(y)));
            }
            Bits.keepAlive(this);
        } else {
            if (RCMSupport.isRCMSupported() && fd != null) {
                Bits.rcmPutShort(a, x, bigEndian);
            } else {
                Bits.putShort(a, x, bigEndian);
            }
            Bits.keepAlive(this);
        }
        return this;



    }

    public ByteBuffer putShort(short x) {

        putShort(ix(nextPutIndex((1 << 1))), x);
        return this;



    }

    public ByteBuffer putShort(int i, short x) {

        putShort(ix(checkIndex(i, (1 << 1))), x);
        return this;



    }

    public ShortBuffer asShortBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 1;
        if (!unaligned && ((address + off) % (1 << 1) != 0)) {
            return (bigEndian
                    ? (ShortBuffer)(new ByteBufferAsShortBufferB(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off))
                    : (ShortBuffer)(new ByteBufferAsShortBufferL(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off)));
        } else {
            return (nativeByteOrder
                    ? (ShortBuffer)(new DirectShortBufferU(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off))
                    : (ShortBuffer)(new DirectShortBufferS(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off)));
        }
    }




    private int getInt(long a) {
        if (unaligned) {
            int x = (RCMSupport.isRCMSupported() && fd != null) ? unsafe.rcmGetInt(a) 
                                     : unsafe.getInt(a);
            int y = (nativeByteOrder ? x : Bits.swap(x));
            Bits.keepAlive(this);
            return y;
        }
        int y = (RCMSupport.isRCMSupported() && fd != null) ? Bits.rcmGetInt(a, bigEndian) 
                                : Bits.getInt(a, bigEndian);
        Bits.keepAlive(this);
        return y;
    }

    public int getInt() {
        int y = getInt(ix(nextGetIndex((1 << 2))));
        Bits.keepAlive(this);
        return y;
    }

    public int getInt(int i) {
        int y = getInt(ix(checkIndex(i, (1 << 2))));
        Bits.keepAlive(this);
        return y;
    }



    private ByteBuffer putInt(long a, int x) {

        if (unaligned) {
            int y = (x);
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmPutInt(a, (nativeByteOrder ? y : Bits.swap(y)));
            } else {
                unsafe.putInt(a, (nativeByteOrder ? y : Bits.swap(y)));
            }
            Bits.keepAlive(this);
        } else {
            if (RCMSupport.isRCMSupported() && fd != null) {
                Bits.rcmPutInt(a, x, bigEndian);
            } else {
                Bits.putInt(a, x, bigEndian);
            }
            Bits.keepAlive(this);
        }
        return this;



    }

    public ByteBuffer putInt(int x) {

        putInt(ix(nextPutIndex((1 << 2))), x);
        return this;



    }

    public ByteBuffer putInt(int i, int x) {

        putInt(ix(checkIndex(i, (1 << 2))), x);
        return this;



    }

    public IntBuffer asIntBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 2;
        if (!unaligned && ((address + off) % (1 << 2) != 0)) {
            return (bigEndian
                    ? (IntBuffer)(new ByteBufferAsIntBufferB(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off))
                    : (IntBuffer)(new ByteBufferAsIntBufferL(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off)));
        } else {
            return (nativeByteOrder
                    ? (IntBuffer)(new DirectIntBufferU(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off))
                    : (IntBuffer)(new DirectIntBufferS(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off)));
        }
    }




    private long getLong(long a) {
        if (unaligned) {
            long x = (RCMSupport.isRCMSupported() && fd != null) ? unsafe.rcmGetLong(a) 
                                     : unsafe.getLong(a);
            long y = (nativeByteOrder ? x : Bits.swap(x));
            Bits.keepAlive(this);
            return y;
        }
        long y = (RCMSupport.isRCMSupported() && fd != null) ? Bits.rcmGetLong(a, bigEndian) 
                                : Bits.getLong(a, bigEndian);
        Bits.keepAlive(this);
        return y;
    }

    public long getLong() {
        long y = getLong(ix(nextGetIndex((1 << 3))));
        Bits.keepAlive(this);
        return y;
    }

    public long getLong(int i) {
        long y = getLong(ix(checkIndex(i, (1 << 3))));
        Bits.keepAlive(this);
        return y;
    }



    private ByteBuffer putLong(long a, long x) {

        if (unaligned) {
            long y = (x);
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmPutLong(a, (nativeByteOrder ? y : Bits.swap(y)));
            } else {
                unsafe.putLong(a, (nativeByteOrder ? y : Bits.swap(y)));
            }
            Bits.keepAlive(this);
        } else {
            if (RCMSupport.isRCMSupported() && fd != null) {
                Bits.rcmPutLong(a, x, bigEndian);
            } else {
                Bits.putLong(a, x, bigEndian);
            }
            Bits.keepAlive(this);
        }
        return this;



    }

    public ByteBuffer putLong(long x) {

        putLong(ix(nextPutIndex((1 << 3))), x);
        return this;



    }

    public ByteBuffer putLong(int i, long x) {

        putLong(ix(checkIndex(i, (1 << 3))), x);
        return this;



    }

    public LongBuffer asLongBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 3;
        if (!unaligned && ((address + off) % (1 << 3) != 0)) {
            return (bigEndian
                    ? (LongBuffer)(new ByteBufferAsLongBufferB(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off))
                    : (LongBuffer)(new ByteBufferAsLongBufferL(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off)));
        } else {
            return (nativeByteOrder
                    ? (LongBuffer)(new DirectLongBufferU(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off))
                    : (LongBuffer)(new DirectLongBufferS(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off)));
        }
    }




    private float getFloat(long a) {
        if (unaligned) {
            int x = (RCMSupport.isRCMSupported() && fd != null) ? unsafe.rcmGetInt(a) 
                                     : unsafe.getInt(a);
            float y = Float.intBitsToFloat(nativeByteOrder ? x : Bits.swap(x));
            Bits.keepAlive(this);
            return y;
        }
        float y = (RCMSupport.isRCMSupported() && fd != null) ? Bits.rcmGetFloat(a, bigEndian) 
                                : Bits.getFloat(a, bigEndian);
        Bits.keepAlive(this);
        return y;
    }

    public float getFloat() {
        float y = getFloat(ix(nextGetIndex((1 << 2))));
        Bits.keepAlive(this);
        return y;
    }

    public float getFloat(int i) {
        float y = getFloat(ix(checkIndex(i, (1 << 2))));
        Bits.keepAlive(this);
        return y;
    }



    private ByteBuffer putFloat(long a, float x) {

        if (unaligned) {
            int y = Float.floatToRawIntBits(x);
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmPutInt(a, (nativeByteOrder ? y : Bits.swap(y)));
            } else {
                unsafe.putInt(a, (nativeByteOrder ? y : Bits.swap(y)));
            }
            Bits.keepAlive(this);
        } else {
            if (RCMSupport.isRCMSupported() && fd != null) {
                Bits.rcmPutFloat(a, x, bigEndian);
            } else {
                Bits.putFloat(a, x, bigEndian);
            }
            Bits.keepAlive(this);
        }
        return this;



    }

    public ByteBuffer putFloat(float x) {

        putFloat(ix(nextPutIndex((1 << 2))), x);
        return this;



    }

    public ByteBuffer putFloat(int i, float x) {

        putFloat(ix(checkIndex(i, (1 << 2))), x);
        return this;



    }

    public FloatBuffer asFloatBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 2;
        if (!unaligned && ((address + off) % (1 << 2) != 0)) {
            return (bigEndian
                    ? (FloatBuffer)(new ByteBufferAsFloatBufferB(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off))
                    : (FloatBuffer)(new ByteBufferAsFloatBufferL(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off)));
        } else {
            return (nativeByteOrder
                    ? (FloatBuffer)(new DirectFloatBufferU(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off))
                    : (FloatBuffer)(new DirectFloatBufferS(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off)));
        }
    }




    private double getDouble(long a) {
        if (unaligned) {
            long x = (RCMSupport.isRCMSupported() && fd != null) ? unsafe.rcmGetLong(a) 
                                     : unsafe.getLong(a);
            double y = Double.longBitsToDouble(nativeByteOrder ? x : Bits.swap(x));
            Bits.keepAlive(this);
            return y;
        }
        double y = (RCMSupport.isRCMSupported() && fd != null) ? Bits.rcmGetDouble(a, bigEndian) 
                                : Bits.getDouble(a, bigEndian);
        Bits.keepAlive(this);
        return y;
    }

    public double getDouble() {
        double y = getDouble(ix(nextGetIndex((1 << 3))));
        Bits.keepAlive(this);
        return y;
    }

    public double getDouble(int i) {
        double y = getDouble(ix(checkIndex(i, (1 << 3))));
        Bits.keepAlive(this);
        return y;
    }



    private ByteBuffer putDouble(long a, double x) {

        if (unaligned) {
            long y = Double.doubleToRawLongBits(x);
            if (RCMSupport.isRCMSupported() && fd != null) {
                unsafe.rcmPutLong(a, (nativeByteOrder ? y : Bits.swap(y)));
            } else {
                unsafe.putLong(a, (nativeByteOrder ? y : Bits.swap(y)));
            }
            Bits.keepAlive(this);
        } else {
            if (RCMSupport.isRCMSupported() && fd != null) {
                Bits.rcmPutDouble(a, x, bigEndian);
            } else {
                Bits.putDouble(a, x, bigEndian);
            }
            Bits.keepAlive(this);
        }
        return this;



    }

    public ByteBuffer putDouble(double x) {

        putDouble(ix(nextPutIndex((1 << 3))), x);
        return this;



    }

    public ByteBuffer putDouble(int i, double x) {

        putDouble(ix(checkIndex(i, (1 << 3))), x);
        return this;



    }

    public DoubleBuffer asDoubleBuffer() {
        int off = this.position();
        int lim = this.limit();
        assert (off <= lim);
        int rem = (off <= lim ? lim - off : 0);

        int size = rem >> 3;
        if (!unaligned && ((address + off) % (1 << 3) != 0)) {
            return (bigEndian
                    ? (DoubleBuffer)(new ByteBufferAsDoubleBufferB(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off))
                    : (DoubleBuffer)(new ByteBufferAsDoubleBufferL(this,
                                                                       -1,
                                                                       0,
                                                                       size,
                                                                       size,
                                                                       off)));
        } else {
            return (nativeByteOrder
                    ? (DoubleBuffer)(new DirectDoubleBufferU(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off))
                    : (DoubleBuffer)(new DirectDoubleBufferS(this,
                                                                 -1,
                                                                 0,
                                                                 size,
                                                                 size,
                                                                 off)));
        }
    }

}
