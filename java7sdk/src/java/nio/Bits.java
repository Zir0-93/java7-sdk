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
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.nio;

import java.util.concurrent.atomic.AtomicLong;
import java.security.AccessController;
import java.security.PrivilegedAction;                                          //IBM-io_converter
import sun.misc.Unsafe;
import sun.misc.VM;

/**
 * Access to bits, native and otherwise.
 */

class Bits {                            // package-private

    private Bits() { }


    // -- Swapping --

    static short swap(short x) {
        return Short.reverseBytes(x);
    }

    static char swap(char x) {
        return Character.reverseBytes(x);
    }

    static int swap(int x) {
        return Integer.reverseBytes(x);
    }

    static long swap(long x) {
        return Long.reverseBytes(x);
    }


    // -- get/put char --

    static private char makeChar(byte b1, byte b0) {
        return (char)((b1 << 8) | (b0 & 0xff));
    }

    static char getCharL(ByteBuffer bb, int bi) {
        return makeChar(bb._get(bi + 1),
                        bb._get(bi    ));
    }

    static char getCharL(long a) {
        return makeChar(_get(a + 1),
                        _get(a    ));
    }

    static char rcmGetCharL(long a) {                                           //IBM-multitenancy_management
        return makeChar(_rcmGet(a + 1), _rcmGet(a));                            //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static char getCharB(ByteBuffer bb, int bi) {
        return makeChar(bb._get(bi    ),
                        bb._get(bi + 1));
    }

    static char getCharB(long a) {
        return makeChar(_get(a    ),
                        _get(a + 1));
    }

    static char rcmGetCharB(long a) {                                           //IBM-multitenancy_management
        return makeChar(_rcmGet(a), _rcmGet(a + 1));                            //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static char getChar(ByteBuffer bb, int bi, boolean bigEndian) {
        return bigEndian ? getCharB(bb, bi) : getCharL(bb, bi);
    }

    static char getChar(long a, boolean bigEndian) {
        return bigEndian ? getCharB(a) : getCharL(a);
    }

    static char rcmGetChar(long a, boolean bigEndian) {                         //IBM-multitenancy_management
        return bigEndian ? rcmGetCharB(a) : rcmGetCharL(a);                     //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    private static byte char1(char x) { return (byte)(x >> 8); }
    private static byte char0(char x) { return (byte)(x     ); }

    static void putCharL(ByteBuffer bb, int bi, char x) {
        bb._put(bi    , char0(x));
        bb._put(bi + 1, char1(x));
    }

    static void putCharL(long a, char x) {
        _put(a    , char0(x));
        _put(a + 1, char1(x));
    }

    static void rcmPutCharL(long a, char x) {                                   //IBM-multitenancy_management
        _rcmPut(a, char0(x));                                                   //IBM-multitenancy_management
        _rcmPut(a + 1, char1(x));                                               //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putCharB(ByteBuffer bb, int bi, char x) {
        bb._put(bi    , char1(x));
        bb._put(bi + 1, char0(x));
    }

    static void putCharB(long a, char x) {
        _put(a    , char1(x));
        _put(a + 1, char0(x));
    }

    static void rcmPutCharB(long a, char x) {                                   //IBM-multitenancy_management
        _rcmPut(a, char1(x));                                                   //IBM-multitenancy_management
        _rcmPut(a + 1, char0(x));                                               //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putChar(ByteBuffer bb, int bi, char x, boolean bigEndian) {
        if (bigEndian)
            putCharB(bb, bi, x);
        else
            putCharL(bb, bi, x);
    }

    static void putChar(long a, char x, boolean bigEndian) {
        if (bigEndian)
            putCharB(a, x);
        else
            putCharL(a, x);
    }

    static void rcmPutChar(long a, char x, boolean bigEndian) {                 //IBM-multitenancy_management
        if (bigEndian)                                                          //IBM-multitenancy_management
            rcmPutCharB(a, x);                                                  //IBM-multitenancy_management
        else                                                                    //IBM-multitenancy_management
            rcmPutCharL(a, x);                                                  //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management

                                                                                //IBM-multitenancy_management
    // -- get/put short --

    static private short makeShort(byte b1, byte b0) {
        return (short)((b1 << 8) | (b0 & 0xff));
    }

    static short getShortL(ByteBuffer bb, int bi) {
        return makeShort(bb._get(bi + 1),
                         bb._get(bi    ));
    }

    static short getShortL(long a) {
        return makeShort(_get(a + 1),
                         _get(a    ));
    }

    static short rcmGetShortL(long a) {                                         //IBM-multitenancy_management
        return makeShort(_rcmGet(a + 1), _rcmGet(a));                           //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static short getShortB(ByteBuffer bb, int bi) {
        return makeShort(bb._get(bi    ),
                         bb._get(bi + 1));
    }

    static short getShortB(long a) {
        return makeShort(_get(a    ),
                         _get(a + 1));
    }

    static short rcmGetShortB(long a) {                                         //IBM-multitenancy_management
        return makeShort(_rcmGet(a), _rcmGet(a + 1));                           //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static short getShort(ByteBuffer bb, int bi, boolean bigEndian) {
        return bigEndian ? getShortB(bb, bi) : getShortL(bb, bi);
    }

    static short getShort(long a, boolean bigEndian) {
        return bigEndian ? getShortB(a) : getShortL(a);
    }

    static short rcmGetShort(long a, boolean bigEndian) {                       //IBM-multitenancy_management
        return bigEndian ? rcmGetShortB(a) : rcmGetShortL(a);                   //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    private static byte short1(short x) { return (byte)(x >> 8); }
    private static byte short0(short x) { return (byte)(x     ); }

    static void putShortL(ByteBuffer bb, int bi, short x) {
        bb._put(bi    , short0(x));
        bb._put(bi + 1, short1(x));
    }

    static void putShortL(long a, short x) {
        _put(a    , short0(x));
        _put(a + 1, short1(x));
    }

    static void rcmPutShortL(long a, short x) {                                 //IBM-multitenancy_management
        _rcmPut(a, short0(x));                                                  //IBM-multitenancy_management
        _rcmPut(a + 1, short1(x));                                              //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putShortB(ByteBuffer bb, int bi, short x) {
        bb._put(bi    , short1(x));
        bb._put(bi + 1, short0(x));
    }

    static void putShortB(long a, short x) {
        _put(a    , short1(x));
        _put(a + 1, short0(x));
    }

    static void rcmPutShortB(long a, short x) {                                 //IBM-multitenancy_management
        _rcmPut(a, short1(x));                                                  //IBM-multitenancy_management
        _rcmPut(a + 1, short0(x));                                              //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putShort(ByteBuffer bb, int bi, short x, boolean bigEndian) {
        if (bigEndian)
            putShortB(bb, bi, x);
        else
            putShortL(bb, bi, x);
    }

    static void putShort(long a, short x, boolean bigEndian) {
        if (bigEndian)
            putShortB(a, x);
        else
            putShortL(a, x);
    }

    static void rcmPutShort(long a, short x, boolean bigEndian) {               //IBM-multitenancy_management
        if (bigEndian)                                                          //IBM-multitenancy_management
            rcmPutShortB(a, x);                                                 //IBM-multitenancy_management
        else                                                                    //IBM-multitenancy_management
            rcmPutShortL(a, x);                                                 //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management

                                                                                //IBM-multitenancy_management
    // -- get/put int --

    static private int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3       ) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }

    static int getIntL(ByteBuffer bb, int bi) {
        return makeInt(bb._get(bi + 3),
                       bb._get(bi + 2),
                       bb._get(bi + 1),
                       bb._get(bi    ));
    }

    static int getIntL(long a) {
        return makeInt(_get(a + 3),
                       _get(a + 2),
                       _get(a + 1),
                       _get(a    ));
    }

    static int rcmGetIntL(long a) {                                             //IBM-multitenancy_management
        return makeInt(_rcmGet(a + 3), _rcmGet(a + 2), _rcmGet(a + 1),          //IBM-multitenancy_management
                _rcmGet(a));                                                    //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static int getIntB(ByteBuffer bb, int bi) {
        return makeInt(bb._get(bi    ),
                       bb._get(bi + 1),
                       bb._get(bi + 2),
                       bb._get(bi + 3));
    }

    static int getIntB(long a) {
        return makeInt(_get(a    ),
                       _get(a + 1),
                       _get(a + 2),
                       _get(a + 3));
    }

    static int rcmGetIntB(long a) {                                             //IBM-multitenancy_management
        return makeInt(_rcmGet(a), _rcmGet(a + 1), _rcmGet(a + 2),              //IBM-multitenancy_management
                _rcmGet(a + 3));                                                //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static int getInt(ByteBuffer bb, int bi, boolean bigEndian) {
        return bigEndian ? getIntB(bb, bi) : getIntL(bb, bi) ;
    }

    static int getInt(long a, boolean bigEndian) {
        return bigEndian ? getIntB(a) : getIntL(a) ;
    }

    static int rcmGetInt(long a, boolean bigEndian) {                           //IBM-multitenancy_management
        return bigEndian ? rcmGetIntB(a) : rcmGetIntL(a) ;                      //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    private static byte int3(int x) { return (byte)(x >> 24); }
    private static byte int2(int x) { return (byte)(x >> 16); }
    private static byte int1(int x) { return (byte)(x >>  8); }
    private static byte int0(int x) { return (byte)(x      ); }

    static void putIntL(ByteBuffer bb, int bi, int x) {
        bb._put(bi + 3, int3(x));
        bb._put(bi + 2, int2(x));
        bb._put(bi + 1, int1(x));
        bb._put(bi    , int0(x));
    }

    static void putIntL(long a, int x) {
        _put(a + 3, int3(x));
        _put(a + 2, int2(x));
        _put(a + 1, int1(x));
        _put(a    , int0(x));
    }

    static void rcmPutIntL(long a, int x) {                                     //IBM-multitenancy_management
        _rcmPut(a + 3, int3(x));                                                //IBM-multitenancy_management
        _rcmPut(a + 2, int2(x));                                                //IBM-multitenancy_management
        _rcmPut(a + 1, int1(x));                                                //IBM-multitenancy_management
        _rcmPut(a    , int0(x));                                                //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putIntB(ByteBuffer bb, int bi, int x) {
        bb._put(bi    , int3(x));
        bb._put(bi + 1, int2(x));
        bb._put(bi + 2, int1(x));
        bb._put(bi + 3, int0(x));
    }

    static void putIntB(long a, int x) {
        _put(a    , int3(x));
        _put(a + 1, int2(x));
        _put(a + 2, int1(x));
        _put(a + 3, int0(x));
    }

    static void rcmPutIntB(long a, int x) {                                     //IBM-multitenancy_management
        _rcmPut(a    , int3(x));                                                //IBM-multitenancy_management
        _rcmPut(a + 1, int2(x));                                                //IBM-multitenancy_management
        _rcmPut(a + 2, int1(x));                                                //IBM-multitenancy_management
        _rcmPut(a + 3, int0(x));                                                //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putInt(ByteBuffer bb, int bi, int x, boolean bigEndian) {
        if (bigEndian)
            putIntB(bb, bi, x);
        else
            putIntL(bb, bi, x);
    }

    static void putInt(long a, int x, boolean bigEndian) {
        if (bigEndian)
            putIntB(a, x);
        else
            putIntL(a, x);
    }

    static void rcmPutInt(long a, int x, boolean bigEndian) {                   //IBM-multitenancy_management
        if (bigEndian)                                                          //IBM-multitenancy_management
            rcmPutIntB(a, x);                                                   //IBM-multitenancy_management
        else                                                                    //IBM-multitenancy_management
            rcmPutIntL(a, x);                                                   //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management

                                                                                //IBM-multitenancy_management
    // -- get/put long --

    static private long makeLong(byte b7, byte b6, byte b5, byte b4,
                                 byte b3, byte b2, byte b1, byte b0)
    {
        return ((((long)b7       ) << 56) |
                (((long)b6 & 0xff) << 48) |
                (((long)b5 & 0xff) << 40) |
                (((long)b4 & 0xff) << 32) |
                (((long)b3 & 0xff) << 24) |
                (((long)b2 & 0xff) << 16) |
                (((long)b1 & 0xff) <<  8) |
                (((long)b0 & 0xff)      ));
    }

    static long getLongL(ByteBuffer bb, int bi) {
        return makeLong(bb._get(bi + 7),
                        bb._get(bi + 6),
                        bb._get(bi + 5),
                        bb._get(bi + 4),
                        bb._get(bi + 3),
                        bb._get(bi + 2),
                        bb._get(bi + 1),
                        bb._get(bi    ));
    }

    static long getLongL(long a) {
        return makeLong(_get(a + 7),
                        _get(a + 6),
                        _get(a + 5),
                        _get(a + 4),
                        _get(a + 3),
                        _get(a + 2),
                        _get(a + 1),
                        _get(a    ));
    }

    static long rcmGetLongL(long a) {                                           //IBM-multitenancy_management
        return makeLong(_rcmGet(a + 7), _rcmGet(a + 6), _rcmGet(a + 5),         //IBM-multitenancy_management
                _rcmGet(a + 4), _rcmGet(a + 3), _rcmGet(a + 2), _rcmGet(a + 1), //IBM-multitenancy_management
                _rcmGet(a));                                                    //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static long getLongB(ByteBuffer bb, int bi) {
        return makeLong(bb._get(bi    ),
                        bb._get(bi + 1),
                        bb._get(bi + 2),
                        bb._get(bi + 3),
                        bb._get(bi + 4),
                        bb._get(bi + 5),
                        bb._get(bi + 6),
                        bb._get(bi + 7));
    }

    static long getLongB(long a) {
        return makeLong(_get(a    ),
                        _get(a + 1),
                        _get(a + 2),
                        _get(a + 3),
                        _get(a + 4),
                        _get(a + 5),
                        _get(a + 6),
                        _get(a + 7));
    }

    static long rcmGetLongB(long a) {                                           //IBM-multitenancy_management
        return makeLong(_rcmGet(a), _rcmGet(a + 1), _rcmGet(a + 2),             //IBM-multitenancy_management
                _rcmGet(a + 3), _rcmGet(a + 4), _rcmGet(a + 5), _rcmGet(a + 6), //IBM-multitenancy_management
                _rcmGet(a + 7));                                                //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static long getLong(ByteBuffer bb, int bi, boolean bigEndian) {
        return bigEndian ? getLongB(bb, bi) : getLongL(bb, bi);
    }

    static long getLong(long a, boolean bigEndian) {
        return bigEndian ? getLongB(a) : getLongL(a);
    }

    static long rcmGetLong(long a, boolean bigEndian) {                         //IBM-multitenancy_management
        return bigEndian ? rcmGetLongB(a) : rcmGetLongL(a);                     //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    private static byte long7(long x) { return (byte)(x >> 56); }
    private static byte long6(long x) { return (byte)(x >> 48); }
    private static byte long5(long x) { return (byte)(x >> 40); }
    private static byte long4(long x) { return (byte)(x >> 32); }
    private static byte long3(long x) { return (byte)(x >> 24); }
    private static byte long2(long x) { return (byte)(x >> 16); }
    private static byte long1(long x) { return (byte)(x >>  8); }
    private static byte long0(long x) { return (byte)(x      ); }

    static void putLongL(ByteBuffer bb, int bi, long x) {
        bb._put(bi + 7, long7(x));
        bb._put(bi + 6, long6(x));
        bb._put(bi + 5, long5(x));
        bb._put(bi + 4, long4(x));
        bb._put(bi + 3, long3(x));
        bb._put(bi + 2, long2(x));
        bb._put(bi + 1, long1(x));
        bb._put(bi    , long0(x));
    }

    static void putLongL(long a, long x) {
        _put(a + 7, long7(x));
        _put(a + 6, long6(x));
        _put(a + 5, long5(x));
        _put(a + 4, long4(x));
        _put(a + 3, long3(x));
        _put(a + 2, long2(x));
        _put(a + 1, long1(x));
        _put(a    , long0(x));
    }

    static void rcmPutLongL(long a, long x) {                                   //IBM-multitenancy_management
        _rcmPut(a + 7, long7(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 6, long6(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 5, long5(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 4, long4(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 3, long3(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 2, long2(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 1, long1(x));                                               //IBM-multitenancy_management
        _rcmPut(a, long0(x));                                                   //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putLongB(ByteBuffer bb, int bi, long x) {
        bb._put(bi    , long7(x));
        bb._put(bi + 1, long6(x));
        bb._put(bi + 2, long5(x));
        bb._put(bi + 3, long4(x));
        bb._put(bi + 4, long3(x));
        bb._put(bi + 5, long2(x));
        bb._put(bi + 6, long1(x));
        bb._put(bi + 7, long0(x));
    }

    static void putLongB(long a, long x) {
        _put(a    , long7(x));
        _put(a + 1, long6(x));
        _put(a + 2, long5(x));
        _put(a + 3, long4(x));
        _put(a + 4, long3(x));
        _put(a + 5, long2(x));
        _put(a + 6, long1(x));
        _put(a + 7, long0(x));
    }

    static void rcmPutLongB(long a, long x) {                                   //IBM-multitenancy_management
        _rcmPut(a, long7(x));                                                   //IBM-multitenancy_management
        _rcmPut(a + 1, long6(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 2, long5(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 3, long4(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 4, long3(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 5, long2(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 6, long1(x));                                               //IBM-multitenancy_management
        _rcmPut(a + 7, long0(x));                                               //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putLong(ByteBuffer bb, int bi, long x, boolean bigEndian) {
        if (bigEndian)
            putLongB(bb, bi, x);
        else
            putLongL(bb, bi, x);
    }

    static void putLong(long a, long x, boolean bigEndian) {
        if (bigEndian)
            putLongB(a, x);
        else
            putLongL(a, x);
    }

    static void rcmPutLong(long a, long x, boolean bigEndian) {                 //IBM-multitenancy_management
        if (bigEndian)                                                          //IBM-multitenancy_management
            rcmPutLongB(a, x);                                                  //IBM-multitenancy_management
        else                                                                    //IBM-multitenancy_management
            rcmPutLongL(a, x);                                                  //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management

                                                                                //IBM-multitenancy_management
    // -- get/put float --

    static float getFloatL(ByteBuffer bb, int bi) {
        return Float.intBitsToFloat(getIntL(bb, bi));
    }

    static float getFloatL(long a) {
        return Float.intBitsToFloat(getIntL(a));
    }

    static float rcmGetFloatL(long a) {                                         //IBM-multitenancy_management
        return Float.intBitsToFloat(rcmGetIntL(a));                             //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static float getFloatB(ByteBuffer bb, int bi) {
        return Float.intBitsToFloat(getIntB(bb, bi));
    }

    static float getFloatB(long a) {
        return Float.intBitsToFloat(getIntB(a));
    }

    static float rcmGetFloatB(long a) {                                         //IBM-multitenancy_management
        return Float.intBitsToFloat(rcmGetIntB(a));                             //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static float getFloat(ByteBuffer bb, int bi, boolean bigEndian) {
        return bigEndian ? getFloatB(bb, bi) : getFloatL(bb, bi);
    }

    static float getFloat(long a, boolean bigEndian) {
        return bigEndian ? getFloatB(a) : getFloatL(a);
    }

    static float rcmGetFloat(long a, boolean bigEndian) {                       //IBM-multitenancy_management
        return bigEndian ? rcmGetFloatB(a) : rcmGetFloatL(a);                   //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putFloatL(ByteBuffer bb, int bi, float x) {
        putIntL(bb, bi, Float.floatToRawIntBits(x));
    }

    static void putFloatL(long a, float x) {
        putIntL(a, Float.floatToRawIntBits(x));
    }

    static void rcmPutFloatL(long a, float x) {                                 //IBM-multitenancy_management
        rcmPutIntL(a, Float.floatToRawIntBits(x));                              //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putFloatB(ByteBuffer bb, int bi, float x) {
        putIntB(bb, bi, Float.floatToRawIntBits(x));
    }

    static void putFloatB(long a, float x) {
        putIntB(a, Float.floatToRawIntBits(x));
    }

    static void rcmPutFloatB(long a, float x) {                                 //IBM-multitenancy_management
        rcmPutIntB(a, Float.floatToRawIntBits(x));                              //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putFloat(ByteBuffer bb, int bi, float x, boolean bigEndian) {
        if (bigEndian)
            putFloatB(bb, bi, x);
        else
            putFloatL(bb, bi, x);
    }

    static void putFloat(long a, float x, boolean bigEndian) {
        if (bigEndian)
            putFloatB(a, x);
        else
            putFloatL(a, x);
    }

    static void rcmPutFloat(long a, float x, boolean bigEndian) {               //IBM-multitenancy_management
        if (bigEndian)                                                          //IBM-multitenancy_management
            rcmPutFloatB(a, x);                                                 //IBM-multitenancy_management
        else                                                                    //IBM-multitenancy_management
            rcmPutFloatL(a, x);                                                 //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management

                                                                                //IBM-multitenancy_management
    // -- get/put double --

    static double getDoubleL(ByteBuffer bb, int bi) {
        return Double.longBitsToDouble(getLongL(bb, bi));
    }

    static double getDoubleL(long a) {
        return Double.longBitsToDouble(getLongL(a));
    }

    static double rcmGetDoubleL(long a) {                                       //IBM-multitenancy_management
        return Double.longBitsToDouble(rcmGetLongL(a));                         //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static double getDoubleB(ByteBuffer bb, int bi) {
        return Double.longBitsToDouble(getLongB(bb, bi));
    }

    static double getDoubleB(long a) {
        return Double.longBitsToDouble(getLongB(a));
    }

    static double rmcGetDoubleB(long a) {                                       //IBM-multitenancy_management
        return Double.longBitsToDouble(rcmGetLongB(a));                         //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static double getDouble(ByteBuffer bb, int bi, boolean bigEndian) {
        return bigEndian ? getDoubleB(bb, bi) : getDoubleL(bb, bi);
    }

    static double getDouble(long a, boolean bigEndian) {
        return bigEndian ? getDoubleB(a) : getDoubleL(a);
    }

    static double rcmGetDouble(long a, boolean bigEndian) {                     //IBM-multitenancy_management
        return bigEndian ? rmcGetDoubleB(a) : rcmGetDoubleL(a);                 //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putDoubleL(ByteBuffer bb, int bi, double x) {
        putLongL(bb, bi, Double.doubleToRawLongBits(x));
    }

    static void putDoubleL(long a, double x) {
        putLongL(a, Double.doubleToRawLongBits(x));
    }

    static void rcmPutDoubleL(long a, double x) {                               //IBM-multitenancy_management
        rcmPutLongL(a, Double.doubleToRawLongBits(x));                          //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putDoubleB(ByteBuffer bb, int bi, double x) {
        putLongB(bb, bi, Double.doubleToRawLongBits(x));
    }

    static void putDoubleB(long a, double x) {
        putLongB(a, Double.doubleToRawLongBits(x));
    }

    static void rcmPutDoubleB(long a, double x) {                               //IBM-multitenancy_management
        rcmPutLongB(a, Double.doubleToRawLongBits(x));                          //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void putDouble(ByteBuffer bb, int bi, double x, boolean bigEndian) {
        if (bigEndian)
            putDoubleB(bb, bi, x);
        else
            putDoubleL(bb, bi, x);
    }

    static void putDouble(long a, double x, boolean bigEndian) {
        if (bigEndian)
            putDoubleB(a, x);
        else
            putDoubleL(a, x);
    }

    static void rcmPutDouble(long a, double x, boolean bigEndian) {             //IBM-multitenancy_management
        if (bigEndian)                                                          //IBM-multitenancy_management
            rcmPutDoubleB(a, x);                                                //IBM-multitenancy_management
        else                                                                    //IBM-multitenancy_management
            rcmPutDoubleL(a, x);                                                //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management

                                                                                //IBM-multitenancy_management
    // -- Unsafe access --

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private static byte _get(long a) {
        return unsafe.getByte(a);
    }

    private static byte _rcmGet(long a) {                                       //IBM-multitenancy_management
        return unsafe.rcmGetByte(a);                                            //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    private static void _put(long a, byte b) {
        unsafe.putByte(a, b);
    }

    private static void _rcmPut(long a, byte b) {                               //IBM-multitenancy_management
        unsafe.rcmPutByte(a, b);                                                //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static Unsafe unsafe() {
        return unsafe;
    }


    // -- Processor and memory-system properties --

    private static final ByteOrder byteOrder;

    static ByteOrder byteOrder() {
        if (byteOrder == null)
            throw new Error("Unknown byte order");
        return byteOrder;
    }

    static {
        long a = unsafe.allocateMemory(8);
        try {
            unsafe.putLong(a, 0x0102030405060708L);
            byte b = unsafe.getByte(a);
            switch (b) {
            case 0x01: byteOrder = ByteOrder.BIG_ENDIAN;     break;
            case 0x08: byteOrder = ByteOrder.LITTLE_ENDIAN;  break;
            default:
                assert false;
                byteOrder = null;
            }
        } finally {
            unsafe.freeMemory(a);
        }
    }


    private static int pageSize = -1;

    static int pageSize() {
        if (pageSize == -1)
            pageSize = unsafe().pageSize();
        return pageSize;
    }

    static int pageCount(long size) {
        return (int)(size + (long)pageSize() - 1L) / pageSize();
    }

    static void keepAlive(Object o) {                                           //IBM-nio_vad
	// Do nothing. This is just to ptovide a chance for JIT to optimize.    //IBM-nio_vad
    }                                                                           //IBM-nio_vad
                                                                                //IBM-nio_vad
    private static boolean unaligned;
    private static boolean unalignedKnown = false;

    static boolean unaligned() {
        if (unalignedKnown)
            return unaligned;
        if (VM.isBooted()) {                                                     //IBM-io_converter
            PrivilegedAction pa                                                  //IBM-io_converter
            = new sun.security.action.GetPropertyAction("os.arch");             //IBM-io_converter
            String arch = (String)AccessController.doPrivileged(pa);             //IBM-io_converter
            unaligned = arch.equals("i386") || arch.equals("x86")                //IBM-io_converter
                || arch.equals("amd64");                                         //IBM-io_converter
            unalignedKnown = true;                                               //IBM-io_converter
        }                                                                        //IBM-io_converter
        else {                                                                   //IBM-io_converter
            unaligned = false;                                                   //IBM-io_converter
        }                                                                       //IBM-io_converter
        return unaligned;
    }


    // -- Direct memory management --

    // A user-settable upper limit on the maximum amount of allocatable
    // direct buffer memory.
    private static final AtomicLong maxCapacity = new AtomicLong(VM.maxDirectMemory());
    private static final AtomicLong currentCapacity = new AtomicLong();
    private static final AtomicLong reservedMemory = new AtomicLong();
    private static final AtomicLong activeReservations = new AtomicLong();
    private static boolean isMaxCapacityGrowable = true;
    private static final Object initCapacityLock = (Bits.class.getName() + ".initCapacityLock");
    private static boolean isMaxCapacityInit = false;

    private static final long DEFAULT_MAX_CAPACITY = (64L * 1024L * 1024L);
    private static final long MIN_MAX_CAPACITY_INCREMENT = (32L * 1024L * 1024L);
    private static final long INITIAL_SLEEP_MILLIS = 100L;
    private static final long SLEEP_MILLIS_INCREMENT = 50L;
    private static final int SLEEP_RETRIES = 5;
    private static final long RESERVATION_SUCCESSFUL = -1L;

    private static void initMaxCapacity() {
        // VM.maxDirectMemory() value may change during VM initialization
        // if it is launched with "-XX:MaxDirectMemorySize=<size>".
        // So 'maxCapacity' needs (re)initialization (once) after
        // the VM has booted.
        synchronized (initCapacityLock) {
            if (!isMaxCapacityInit && VM.isBooted()) {
                final long max = VM.maxDirectMemory();
                synchronized (Bits.class) {
                    isMaxCapacityGrowable = (max == DEFAULT_MAX_CAPACITY);
                }
                maxCapacity.set(max);
                isMaxCapacityInit = true;
            }
        }
    }

    /**
     * Attempts to reserve given capacity.
     *
     * Reservation is limited by the maximum capacity rather than the actual
     * reserved memory used, as these values may differ when buffers are page
     * aligned.
     *
     * @param size Number of bytes to increment 'reservedMemory' if
     *                  capacity reservation is successful.
     * @param cap Number of bytes to increment 'currentCapacity' if
     *                  capacity reservation is successful.
     * @return {@link RESERVATION_SUCCESSFUL} if reservation is successful,
     *          otherwise returns the observed value of 'currentCapacity'.
     */
    private static long tryReserveMemory(long size, long cap) {
        long capacity = 0L;
        long newCapacity = 0L;
        do {
            capacity = currentCapacity.get();
            newCapacity = (capacity + cap);
            if (maxCapacity.get() > newCapacity) {
                // sufficient free capacity; try incrementing 'currentCapacity'
                if (currentCapacity.compareAndSet(capacity, newCapacity)) {
                    // increment successful; increment other counters and return
                    reservedMemory.addAndGet(size);
                    activeReservations.incrementAndGet();
                    return RESERVATION_SUCCESSFUL;
                }
                // currentCapacity changed whilst being compared; retry
                continue;
            } else {
                // insufficent free capacity; return observed capacity value
                return capacity;
            }
        } while (true);
    }

    private static synchronized void syncReserveMemory(long size, long cap) {
        long capacity = tryReserveMemory(size, cap);
        if (capacity == RESERVATION_SUCCESSFUL) {
            return;
        }

        // provoke System.gc and wait for evidence it has happened
        System.gc();
        long sleepMillis = INITIAL_SLEEP_MILLIS;
        for (int retries = SLEEP_RETRIES; retries > 0; retries--) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ie) {
            }
            if (currentCapacity.get() < capacity) {
                // there has been some reduction in currentCapacity,
                // so GC must have occurred.
                break;
            }
            sleepMillis += SLEEP_MILLIS_INCREMENT;
        }

        capacity = tryReserveMemory(size, cap);
        if (capacity == RESERVATION_SUCCESSFUL) {
            return;
        }

        // grow maxCapacity if allowable
        if (isMaxCapacityGrowable) {
            currentCapacity.addAndGet(cap);
            maxCapacity.addAndGet(Math.max(MIN_MAX_CAPACITY_INCREMENT, cap));
            reservedMemory.addAndGet(size);
            activeReservations.incrementAndGet();
            return;
        }

        // free capacity not available and growing max capacity not allowed
        throw new OutOfMemoryError("Direct buffer memory::Please use appropriate '<size>' via -XX:MaxDirectMemorySize=<size>");
    }

    // These methods should be called whenever direct memory is allocated or
    // freed.  They allow the user to control the amount of direct memory
    // which a process may access.  All sizes are specified in bytes.
    static void reserveMemory(long size, int cap) {
        initMaxCapacity();

        long capacity = tryReserveMemory(size, cap);
        if (capacity == RESERVATION_SUCCESSFUL) {
            return;
        }

        // may need to provoke System.gc; reserve memory under synchronization
        syncReserveMemory(size, cap);
    }

    static void unreserveMemory(long size, int cap) {
        long capacity = currentCapacity.addAndGet(-cap);
        long reserved = reservedMemory.addAndGet(-size);
        activeReservations.decrementAndGet();
        assert (capacity > -1);
        assert (reserved > -1);
    }

    // -- Monitoring of direct buffer usage --

    static {
        // setup access to this package in SharedSecrets
        sun.misc.SharedSecrets.setJavaNioAccess(
            new sun.misc.JavaNioAccess() {
                @Override
                public sun.misc.JavaNioAccess.BufferPool getDirectBufferPool() {
                    return new sun.misc.JavaNioAccess.BufferPool() {
                        @Override
                        public String getName() {
                            return "direct";
                        }
                        @Override
                        public long getCount() {
                            return Bits.activeReservations.get();
                        }
                        @Override
                        public long getTotalCapacity() {
                            return Bits.currentCapacity.get();
                        }
                        @Override
                        public long getMemoryUsed() {
                            return Bits.reservedMemory.get();
                        }
                    };
                }
                @Override
                public ByteBuffer newDirectByteBuffer(long addr, int cap, Object ob) {
                    return new DirectByteBuffer(addr, cap, ob);
                }
                @Override
                public void truncate(Buffer buf) {
                    buf.truncate();
                }
        });
    }

    // -- Bulk get/put acceleration --

    // These numbers represent the point at which we have empirically
    // determined that the average cost of a JNI call exceeds the expense
    // of an element by element copy.  These numbers may change over time.
    static final int JNI_COPY_TO_ARRAY_THRESHOLD   = 6;
    static final int JNI_COPY_FROM_ARRAY_THRESHOLD = 6;

    // This number limits the number of bytes to copy per call to Unsafe's
    // copyMemory method. A limit is imposed to allow for safepoint polling
    // during a large copy
    static final long UNSAFE_COPY_THRESHOLD = 1024L * 1024L;

    // These methods do no bounds checking.  Verification that the copy will not
    // result in memory corruption should be done prior to invocation.
    // All positions and lengths are specified in bytes.

    /**
     * Copy from given source array to destination address.
     *
     * @param   src
     *          source array
     * @param   srcBaseOffset
     *          offset of first element of storage in source array
     * @param   srcPos
     *          offset within source array of the first element to read
     * @param   dstAddr
     *          destination address
     * @param   length
     *          number of bytes to copy
     */
    static void copyFromArray(Object src, long srcBaseOffset, long srcPos,
                              long dstAddr, long length)
    {
        long offset = srcBaseOffset + srcPos;
        while (length > 0) {
            long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD : length;
            unsafe.copyMemory(src, offset, null, dstAddr, size);
            length -= size;
            offset += size;
            dstAddr += size;
        }
    }

    /**
     * Copy from source address into given destination array.
     *
     * @param   srcAddr
     *          source address
     * @param   dst
     *          destination array
     * @param   dstBaseOffset
     *          offset of first element of storage in destination array
     * @param   dstPos
     *          offset within destination array of the first element to write
     * @param   length
     *          number of bytes to copy
     */
    static void copyToArray(long srcAddr, Object dst, long dstBaseOffset, long dstPos,
                            long length)
    {
        long offset = dstBaseOffset + dstPos;
        while (length > 0) {
            long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD : length;
            unsafe.copyMemory(null, srcAddr, dst, offset, size);
            length -= size;
            srcAddr += size;
            offset += size;
        }
    }

    static void copyFromCharArray(Object src, long srcPos, long dstAddr,
                                  long length)
    {
        copyFromShortArray(src, srcPos, dstAddr, length);
    }

    static void copyToCharArray(long srcAddr, Object dst, long dstPos,
                                long length)
    {
        copyToShortArray(srcAddr, dst, dstPos, length);
    }

    static native void copyFromShortArray(Object src, long srcPos, long dstAddr,
                                          long length);
    static native void copyToShortArray(long srcAddr, Object dst, long dstPos,
                                        long length);

    static native void copyFromIntArray(Object src, long srcPos, long dstAddr,
                                        long length);
    static native void copyToIntArray(long srcAddr, Object dst, long dstPos,
                                      long length);

    static native void copyFromLongArray(Object src, long srcPos, long dstAddr,
                                         long length);
    static native void copyToLongArray(long srcAddr, Object dst, long dstPos,
                                       long length);

    static void rcmCopyFromArray(Object src, long srcBaseOffset, long srcPos,   //IBM-multitenancy_management
            long dstAddr, long length) {                                        //IBM-multitenancy_management
        long offset = srcBaseOffset + srcPos;                                   //IBM-multitenancy_management
        while (length > 0) {                                                    //IBM-multitenancy_management
            long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD //IBM-multitenancy_management
                    : length;                                                   //IBM-multitenancy_management
            unsafe.rcmCopyMemory(src, offset, null, dstAddr, size);             //IBM-multitenancy_management
            length -= size;                                                     //IBM-multitenancy_management
            offset += size;                                                     //IBM-multitenancy_management
            dstAddr += size;                                                    //IBM-multitenancy_management
        }                                                                       //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void rcmCopyToArray(long srcAddr, Object dst, long dstBaseOffset,    //IBM-multitenancy_management
            long dstPos, long length) {                                         //IBM-multitenancy_management
        long offset = dstBaseOffset + dstPos;                                   //IBM-multitenancy_management
        while (length > 0) {                                                    //IBM-multitenancy_management
            long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD //IBM-multitenancy_management
                    : length;                                                   //IBM-multitenancy_management
            unsafe.rcmCopyMemory(null, srcAddr, dst, offset, size);             //IBM-multitenancy_management
            length -= size;                                                     //IBM-multitenancy_management
            srcAddr += size;                                                    //IBM-multitenancy_management
            offset += size;                                                     //IBM-multitenancy_management
        }                                                                       //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void rcmCopyFromCharArray(Object src, long srcPos, long dstAddr,     //IBM-multitenancy_management
            long length) {                                                      //IBM-multitenancy_management
        rcmCopyFromShortArray(src, srcPos, dstAddr, length);                    //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    static void rcmCopyToCharArray(long srcAddr, Object dst, long dstPos,       //IBM-multitenancy_management
            long length) {                                                      //IBM-multitenancy_management
        rcmCopyToShortArray(srcAddr, dst, dstPos, length);                      //IBM-multitenancy_management
    }                                                                           //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    // add rcm code, should be tracked as write                                 //IBM-multitenancy_management
    static native void rcmCopyFromShortArray(Object src, long srcPos,           //IBM-multitenancy_management
            long dstAddr, long length);                                         //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    // add rcm code, should be tracked as read                                  //IBM-multitenancy_management
    static native void rcmCopyToShortArray(long srcAddr, Object dst,            //IBM-multitenancy_management
            long dstPos, long length);                                          //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    // add rcm code, should be tracked as write                                 //IBM-multitenancy_management
    static native void rcmCopyFromIntArray(Object src, long srcPos,             //IBM-multitenancy_management
            long dstAddr, long length);                                         //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    // add rcm code, should be tracked as read                                  //IBM-multitenancy_management
    static native void rcmCopyToIntArray(long srcAddr, Object dst, long dstPos, //IBM-multitenancy_management
            long length);                                                       //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    // add rcm code, should be tracked as write                                 //IBM-multitenancy_management
    static native void rcmCopyFromLongArray(Object src, long srcPos,            //IBM-multitenancy_management
            long dstAddr, long length);                                         //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
    // add rcm code, should be tracked as read                                  //IBM-multitenancy_management
    static native void rcmCopyToLongArray(long srcAddr, Object dst,             //IBM-multitenancy_management
            long dstPos, long length);                                          //IBM-multitenancy_management
                                                                                //IBM-multitenancy_management
}
//IBM-io_converter
//IBM-nio_vad
