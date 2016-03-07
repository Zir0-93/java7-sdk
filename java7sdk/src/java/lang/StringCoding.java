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
 * Copyright (c) 2000, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;                                 //IBM-io_converter
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import sun.io.MalformedInputException;                                          //IBM-io_converter
import sun.io.ByteToCharConverter;                                              //IBM-io_converter
import sun.io.CharToByteConverter;                                              //IBM-io_converter
import sun.io.Converters;                                                       //IBM-io_converter
import java.util.Arrays;
import com.ibm.jvm.MemorySafetyService;
import com.ibm.jvm.util.GlobalMap;
import sun.misc.MessageUtils;
import sun.nio.cs.HistoricallyNamedCharset;
import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;
import com.ibm.misc.IOConverter;

/**
 * Utility class for string encoding and decoding.
 */

class StringCoding {

    private StringCoding() { }

    /* The cached coders for each thread
     */
    private static ThreadLocal decoder = new ThreadLocal();
    private static ThreadLocal encoder = new ThreadLocal();

        /*                                                                      //IBM-io_converter
         * Make a decoder or converter by name                                  //IBM-io_converter
         * ibm.realtime made this static so that it can be used by DecoderCache //IBM-wrt_bringup
         * and also as part of the realtime global map.                         //IBM-wrt_bringup
         */                                                                     //IBM-io_converter
        static Object makeDecoder(final String enc) {                           //IBM-wrt_bringup
            String encoding = enc;                                              //IBM-io_converter
            if (enc.equals("\uFFFC"))                                           //IBM-io_converter
                encoding = Converters.getDefaultEncodingName();                 //IBM-io_converter
            try {                                                               //IBM-io_converter
                return (Object)ByteToCharConverter.getConverter(encoding);      //IBM-io_converter
            } catch (UnsupportedEncodingException e) {                          //IBM-io_converter
            }                                                                   //IBM-io_converter
            Charset cs;                                                         //IBM-io_converter
            try {                                                               //IBM-io_converter
                cs = Charset.forName(encoding);                                 //IBM-io_converter
            } catch (UnsupportedCharsetException x) {                           //IBM-io_converter
                //ibm.55142, 55582 start                                        //IBM-io_converter
                Object ret = null;                                              //IBM-io_converter
                if (enc.equals("\uFFFC")) {                                     //IBM-io_converter
                    String oldenc = encoding;                                   //IBM-io_converter
                    while (ret == null) {                                       //IBM-io_converter
                        encoding = Converters.getFallbackEncoding(encoding);    //IBM-io_converter
                        if (encoding == null)                                   //IBM-io_converter
                            break;                                              //IBM-io_converter
                        ret = makeDecoder(encoding);                            //IBM-io_converter
                    }                                                           //IBM-io_converter
                    warnUnsupportedCharset(oldenc, ret);                        //IBM-io_converter
                }                                                               //IBM-io_converter
                return ret;                                                     //IBM-io_converter
                //ibm.55142, 55582  end                                         //IBM-io_converter
            }                                                                   //IBM-io_converter
            return (Object)(cs.newDecoder()                                     //IBM-io_converter
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE));  //ibm.55235 //IBM-io_converter
        }                                                                       //IBM-io_converter
                                                                                //IBM-io_converter
                                                                                //IBM-wrt_bringup
                                                                                //IBM-wrt_bringup
    /*                                                                          //IBM-wrt_bringup
     * Keep an n-way decoder cache per thread                                   //IBM-wrt_bringup
     */                                                                         //IBM-wrt_bringup
    private final static class DecoderCache {                                   //IBM-wrt_bringup
        private static final int CACHE_SIZE = 6;                                //IBM-wrt_bringup
        private int    max = 0;                                                 //IBM-wrt_bringup
        private int    reuse   = 0;                                             //IBM-wrt_bringup
        private int    current = 0;                                             //IBM-wrt_bringup
        private String encoding [] = new String[CACHE_SIZE];                    //IBM-wrt_bringup
        private Object decoders [] = new Object[CACHE_SIZE];                    //IBM-wrt_bringup
                                                                                //IBM-wrt_bringup
        /*                                                                      //IBM-io_converter
         * Get a decoder or converter by name                                   //IBM-io_converter
         */                                                                     //IBM-io_converter
        Object getDecoder(final String enc)                                     //IBM-io_converter
             throws UnsupportedEncodingException {                              //IBM-io_converter
            Object obj;                                                         //IBM-io_converter

            /* In the normal case we are using the same as previous */          //IBM-io_converter
            if (max > 0 && encoding[current].equals(enc)) {                     //IBM-io_converter
                return  decoders[current];                                      //IBM-io_converter
            }                                                                   //IBM-io_converter

            /* Try the others entries in the cache */                           //IBM-io_converter
            int i;                                                              //IBM-io_converter
            for (i=0; i<max; i++) {                                             //IBM-io_converter
                if (i!=current && encoding[i].equals(enc)) {                    //IBM-io_converter
                    current = i;                                                //IBM-io_converter
                    return decoders[i];                                         //IBM-io_converter
                }                                                               //IBM-io_converter
            }                                                                   //IBM-io_converter
            try {                                                               //IBM-io_converter
                obj = (java.security.AccessController.doPrivileged(             //IBM-io_converter
                    new java.security.PrivilegedExceptionAction() {             //IBM-io_converter
                        public Object run () {                                  //IBM-io_converter
                            return makeDecoder(enc);                            //IBM-io_converter
                        }                                                       //IBM-io_converter
                    }                                                           //IBM-io_converter
                ));                                                             //IBM-io_converter
            } catch (PrivilegedActionException e) {                             //IBM-io_converter
                throw (UnsupportedEncodingException)e.getException();           //IBM-io_converter
            } catch (java.nio.charset.IllegalCharsetNameException ee) { /*ibm@86874*/ //IBM-io_converter
                throw new UnsupportedEncodingException(enc);            /*ibm@86874*/ //IBM-io_converter
            }                                                           /*ibm@86874*/ //IBM-io_converter

            if (obj==null) {                                                    //IBM-io_converter
                throw new UnsupportedEncodingException(enc);                    //IBM-io_converter
            }                                                                   //IBM-io_converter

            /* Determine position to reuse and fill in cache */                 //IBM-io_converter
            if (max < CACHE_SIZE) {                                             //IBM-io_converter
                i = max++;                                                      //IBM-io_converter
            } else {                                                            //IBM-io_converter
                i = reuse++;             /* round robin replacement */          //IBM-io_converter
                if (reuse == CACHE_SIZE)                                        //IBM-io_converter
                    reuse = 0;                                                  //IBM-io_converter
            }                                                                   //IBM-io_converter
            decoders[i] = obj;                                                  //IBM-io_converter
            encoding[i] = enc;                                                  //IBM-io_converter
            current = i;                                                        //IBM-io_converter
            return obj;                                                         //IBM-io_converter
        }                                                                       //IBM-io_converter
    }

        /*                                                                      //IBM-io_converter
         * Make a decoder or converter by name                                  //IBM-io_converter
         * ibm.realtime made this static so that it can be used by DecoderCache //IBM-wrt_bringup
         * and also as part of the realtime global map.                         //IBM-wrt_bringup
         */                                                                     //IBM-io_converter
        static Object makeEncoder(final String enc) {                           //IBM-wrt_bringup
            String encoding = enc;                                              //IBM-io_converter
            if (enc.equals("\uFFFC"))                                           //IBM-io_converter
                encoding = Converters.getDefaultEncodingName();                 //IBM-io_converter
            try {                                                               //IBM-io_converter
                return (Object)CharToByteConverter.getConverter(encoding);      //IBM-io_converter
            } catch (UnsupportedEncodingException e) {}                         //IBM-io_converter
            Charset cs;                                                         //IBM-io_converter
            try {                                                               //IBM-io_converter
                cs = Charset.forName(encoding);                                 //IBM-io_converter
            } catch (UnsupportedCharsetException x) {                           //IBM-io_converter
                //ibm.55142, 55582  start                                       //IBM-io_converter
                Object ret = null;                                              //IBM-io_converter
                if (enc.equals("\uFFFC")) {                                     //IBM-io_converter
                    String oldenc = encoding;                                   //IBM-io_converter
                    while (ret == null) {                                       //IBM-io_converter
                        encoding = Converters.getFallbackEncoding(encoding);    //IBM-io_converter
                        if (encoding == null)                                   //IBM-io_converter
                            break;                                              //IBM-io_converter
                        ret = makeEncoder(encoding);                            //IBM-io_converter
                    }                                                           //IBM-io_converter
                    warnUnsupportedCharset(oldenc, ret);                        //IBM-io_converter
                }                                                               //IBM-io_converter
                return ret;                                                     //IBM-io_converter
                //ibm.55142, 55582  end                                         //IBM-io_converter
            }                                                                   //IBM-io_converter
            return cs.newEncoder()                                              //IBM-io_converter
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);   //ibm.55235 //IBM-io_converter
        }                                                                       //IBM-io_converter
                                                                                //IBM-io_converter
                                                                                //IBM-wrt_bringup
                                                                                //IBM-wrt_bringup
    /*                                                                          //IBM-wrt_bringup
     * Keep an n-way encoder cache per thread                                   //IBM-wrt_bringup
     */                                                                         //IBM-wrt_bringup
    private final static class EncoderCache {                                   //IBM-wrt_bringup
        private static final int CACHE_SIZE = 6;                                //IBM-wrt_bringup
        private int    max = 0;                                                 //IBM-wrt_bringup
        private int    reuse   = 0;                                             //IBM-wrt_bringup
        private int    current = 0;                                             //IBM-wrt_bringup
        private String encoding [] = new String[CACHE_SIZE];                    //IBM-wrt_bringup
        private Object encoders [] = new Object[CACHE_SIZE];                    //IBM-wrt_bringup
                                                                                //IBM-wrt_bringup
        /*                                                                      //IBM-io_converter
         * Get a decoder or converter by name                                   //IBM-io_converter
         */                                                                     //IBM-io_converter
        Object getEncoder(final String enc)                                     //IBM-io_converter
             throws UnsupportedEncodingException {                              //IBM-io_converter
            Object obj;                                                         //IBM-io_converter
                                                                                //IBM-io_converter
            /* In the normal case we are using the same as previous */          //IBM-io_converter
            if (max > 0 && encoding[current].equals(enc)) {                     //IBM-io_converter
                return  encoders[current];                                      //IBM-io_converter
            }                                                                   //IBM-io_converter
                                                                                //IBM-io_converter
            /* Try the others entries in the cache */                           //IBM-io_converter
            int i;                                                              //IBM-io_converter
            for (i=0; i<max; i++) {                                             //IBM-io_converter
                if (i!=current && encoding[i].equals(enc)) {                    //IBM-io_converter
                    current = i;                                                //IBM-io_converter
                    return encoders[i];                                         //IBM-io_converter
                }                                                               //IBM-io_converter
            }                                                                   //IBM-io_converter
            try {                                                               //IBM-io_converter
                obj = (java.security.AccessController.doPrivileged(             //IBM-io_converter
                    new java.security.PrivilegedExceptionAction() {             //IBM-io_converter
                        public Object run () {                                  //IBM-io_converter
                            return makeEncoder(enc);                            //IBM-io_converter
                        }                                                       //IBM-io_converter
                    }                                                           //IBM-io_converter
                ));                                                             //IBM-io_converter
            } catch (PrivilegedActionException e) {                             //IBM-io_converter
                throw (UnsupportedEncodingException)e.getException();           //IBM-io_converter
            } catch (java.nio.charset.IllegalCharsetNameException ee) { //ibm@64726.1 //IBM-io_converter
                throw new UnsupportedEncodingException(enc);            //ibm@64726.1 //IBM-io_converter
            }                                                           //ibm@64726.1 //IBM-io_converter
                                                                                //IBM-io_converter
            if (obj==null) {                                                    //IBM-io_converter
                throw new UnsupportedEncodingException(enc);                    //IBM-io_converter
            }                                                                   //IBM-io_converter

            /* Determine position to reuse and fill in cache */                 //IBM-io_converter
            if (max < CACHE_SIZE) {                                             //IBM-io_converter
                i = max++;                                                      //IBM-io_converter
            } else {                                                            //IBM-io_converter
                i = reuse++;             /* round robin replacement */          //IBM-io_converter
                if (reuse == CACHE_SIZE)                                        //IBM-io_converter
                    reuse = 0;                                                  //IBM-io_converter
            }                                                                   //IBM-io_converter
            encoders[i] = obj;                                                  //IBM-io_converter
            encoding[i] = enc;                                                  //IBM-io_converter
            current = i;                                                        //IBM-io_converter
            return obj;                                                         //IBM-io_converter
        }                                                                       //IBM-io_converter
    }

    /*                                                                          //IBM-wrt_bringup
     * IBM-rt  Start                                                            //IBM-wrt_bringup
     * Start of realtime modifications to getDecoder and getEncoder.  Note      //IBM-wrt_bringup
     * that the other modifications are the move of makeDecoder and makeEncoder //IBM-wrt_bringup
     * from the caches above.  Both are now static methods on this class which  //IBM-wrt_bringup
     * should have no effect on performance.                                    //IBM-wrt_bringup
     *                                                                          //IBM-wrt_bringup
     * The essence of these mods is to avoid using the thread local caches      //IBM-wrt_bringup
     * if the current thread is realtime.  In this case we use a static global  //IBM-wrt_bringup
     * map in Immortal.  Obviously there will be some performance penalty       //IBM-wrt_bringup
     * for this but it shouldn't be too high; in essence just a synchronized    //IBM-wrt_bringup
     * block.  It would be nice if we could use the same cache implementation   //IBM-wrt_bringup
     * for the global map but sadly that wouldn't work if people started        //IBM-wrt_bringup
     * using more than 6 (CACHE_SIZE) char sets from scoped threads - the       //IBM-wrt_bringup
     * cache would keep overflowing and Immortal would eventually fill with     //IBM-wrt_bringup
     * decoders (or encoders).  The net is that we build a really simple        //IBM-wrt_bringup
     * map in Immortal - a vector of Object in which element 0 is the next      //IBM-wrt_bringup
     * block, element n is a String giving the charset name and element n+1     //IBM-wrt_bringup
     * the decoder (or encoder).                                                //IBM-wrt_bringup
     *                                                                          //IBM-wrt_bringup
     * Worries remaining.                                                       //IBM-wrt_bringup
     * I am not convinced that it's ever worth having the thread local stuff    //IBM-wrt_bringup
     * and it certainly adds footprint for realtime.  Using the global map for  //IBM-wrt_bringup
     * everything sounded a good idea but I haven't managed to make it work     //IBM-wrt_bringup
     * yet - it looks like it causes early init of various Realtime classes     //IBM-wrt_bringup
     * that complicates the issue.                                              //IBM-wrt_bringup
     */                                                                         //IBM-wrt_bringup
    static GlobalMap<String,Object> decoderMap;                                 //IBM-wrt_bringup
    static GlobalMap<String,Object> encoderMap;                                 //IBM-wrt_bringup
    static {                                                                    //IBM-wrt_bringup
        if (MemorySafetyService.isSafeMode()) {                                 //IBM-wrt_bringup
            // by making sure that the GlobalCache is small we can build it     //IBM-wrt_bringup
            // in the static init and thus save a test at runtime.              //IBM-wrt_bringup
            decoderMap = new GlobalMap<String,Object>();                        //IBM-wrt_bringup
            encoderMap = new GlobalMap<String,Object>();                        //IBM-wrt_bringup
        }                                                                       //IBM-wrt_bringup
    }                                                                           //IBM-wrt_bringup
    // IBM-rt End                                                               //IBM-wrt_bringup

   /*                                                                           //IBM-io_converter
    * Returns an Decoder from the cacheFor realtime we access a global          //IBM-wrt_bringup
    * map rather than the thread local cache in the non-realtime case.          //IBM-wrt_bringup
    */                                                                          //IBM-io_converter
    private static Object getDecoder(final String encoding)                     //IBM-io_converter
                                  throws UnsupportedEncodingException {         //IBM-io_converter
                                                                                //IBM-wrt_bringup
        // IBM-rt Start                                                         //IBM-wrt_bringup
        if (MemorySafetyService.canCurrentThreadAccessNonHeapMemoryAreas()) {                                   //IBM-wrt_bringup
            final String enc = encoding.intern();                               //IBM-wrt_bringup
            Object ret = decoderMap.getItem(enc);                               //IBM-wrt_bringup
            if (ret == null) synchronized (decoderMap) {                        //IBM-wrt_bringup
                // recheck the search now we've sync'd                          //IBM-wrt_bringup
                ret = decoderMap.getItem(enc);                                  //IBM-wrt_bringup
                if (ret == null) {                                              //IBM-wrt_bringup
                    // We now build the new decoder in Immortal.  This may be too cheap //IBM-wrt_bringup
                    // but it avoids making realtime changes to the mass of stuff in makeDecoder. //IBM-wrt_bringup
                    // Tests show that we consume 1136 bytes for the Cp1252 decoder and 624 for UTF8. //IBM-wrt_bringup
                    // Hopefully very little of that is temporary garbage.      //IBM-wrt_bringup
                    try {                                                               /*ibm@106380*/ //IBM-wrt_bringup
                        long oldArea = -1;										//IBM-wrt_bringup
                        try {
                        	oldArea = MemorySafetyService.enterSafeMemoryArea();
                            ret = makeDecoder(enc);                 //IBM-wrt_bringup
                            decoderMap.putItem(enc,ret);            //IBM-wrt_bringup
                        }finally {
                        	MemorySafetyService.exitLastMemoryArea(oldArea);
                        }
                    } catch (java.nio.charset.IllegalCharsetNameException ee) {         /*ibm@106380...*/ //IBM-wrt_bringup
                        throw new UnsupportedEncodingException(enc);            //IBM-wrt_bringup
                    }                                                           //IBM-wrt_bringup
                    if (ret==null) {                                    //IBM-wrt_bringup
                        throw new UnsupportedEncodingException(enc);            //IBM-wrt_bringup
                    }                                                                   /*...ibm@106380*/ //IBM-wrt_bringup
                }                                                               //IBM-wrt_bringup
            }                                                                   //IBM-wrt_bringup
            return ret;                                                         //IBM-wrt_bringup
        }                                                                       //IBM-wrt_bringup
        // IBM-rt End                                                           //IBM-wrt_bringup
                                                                                //IBM-wrt_bringup
        /*                                                                      //IBM-io_converter
         * Put all object creation inside a doPriv so that it appears on        //IBM-io_converter
         * the middleware heap, and not as a cross heap reference               //IBM-io_converter
         */                                                                     //IBM-io_converter
        DecoderCache cache;                                                     //IBM-io_converter

        cache = (DecoderCache)decoder.get();                                    //IBM-io_converter
        if (cache == null) {                                                    //IBM-io_converter
            cache = (DecoderCache) (                                            //IBM-io_converter
            java.security.AccessController.doPrivileged(                        //IBM-io_converter
                new java.security.PrivilegedAction() {                          //IBM-io_converter
                public Object run () {                                          //IBM-io_converter
                    return new DecoderCache();                                  //IBM-io_converter
                }                                                               //IBM-io_converter
            }                                                                   //IBM-io_converter
            ));                                                                 //IBM-io_converter
            decoder.set(cache);                                                 //IBM-io_converter
        }                                                                       //IBM-io_converter
        return cache.getDecoder(encoding);                                      //IBM-io_converter
    }                                                                           //IBM-io_converter


   /*                                                                           //IBM-io_converter
    * Returns an Encoder from the cache.  For realtime we access a global       //IBM-wrt_bringup
    * map rather than the thread local cache in the non-realtime case.          //IBM-wrt_bringup
    */                                                                          //IBM-io_converter
    private static Object getEncoder(final String encoding)                     //IBM-io_converter
                                  throws UnsupportedEncodingException {         //IBM-io_converter
                                                                                //IBM-wrt_bringup
        // IBM-rt Start                                                         //IBM-wrt_bringup
        if (! MemorySafetyService.canCurrentThreadAccessHeap()) {               //IBM-wrt_bringup
            final String enc = encoding.intern();                               //IBM-wrt_bringup
            Object ret = encoderMap.getItem(enc);                               //IBM-wrt_bringup
            if (ret == null) synchronized (encoderMap) {                        //IBM-wrt_bringup
                // recheck the search now we've sync'd                          //IBM-wrt_bringup
                ret = encoderMap.getItem(enc);                                  //IBM-wrt_bringup
                if (ret == null) {                                              //IBM-wrt_bringup
                    // We now build the new encoder in Immortal.  This may be too cheap //IBM-wrt_bringup
                    // but it avoids making realtime changes to the mass of stuff in makeDecoder. //IBM-wrt_bringup
                    // Tests show that we consume 1024 bytes for the ASCII encoder. //IBM-wrt_bringup
                    try {                                                               /*ibm@106380*/ //IBM-wrt_bringup
                    	long oldArea = -1;
                    	try {
                    		oldArea = MemorySafetyService.enterSafeMemoryArea();
                    		ret = makeEncoder(enc);                 //IBM-wrt_bringup
                            encoderMap.putItem(enc,ret);            //IBM-wrt_bringup
                    	} finally {
                    		MemorySafetyService.exitLastMemoryArea(oldArea);                    		
                    	}
                    } catch (java.nio.charset.IllegalCharsetNameException ee) { //IBM-wrt_bringup
                        throw new UnsupportedEncodingException(enc);            //IBM-wrt_bringup
                    }                                                           //IBM-wrt_bringup
                    if (ret == null) {                                          //IBM-wrt_bringup
                        throw new UnsupportedEncodingException(enc);            //IBM-wrt_bringup
                    }                                                           //IBM-wrt_bringup
                }                                                               //IBM-wrt_bringup
            }                                                                   //IBM-wrt_bringup
            return ret;                                                         //IBM-wrt_bringup
        }                                                                       //IBM-wrt_bringup
        // IBM-rt End                                                           //IBM-wrt_bringup
                                                                                //IBM-wrt_bringup
        /*                                                                      //IBM-io_converter
         * Put all object creation inside a doPriv so that it appears on        //IBM-io_converter
         * the middleware heap, and not as a cross heap reference               //IBM-io_converter
         */                                                                     //IBM-io_converter
        EncoderCache cache;                                                     //IBM-io_converter
        long oldArea = -1;                                                      //IBM-wrt_bringup
        try {                                                                   //IBM-wrt_bringup
            oldArea = MemorySafetyService.enterHeapMemoryArea();                //IBM-wrt_bringup
            cache = (EncoderCache)encoder.get();                                    //IBM-io_converter
            if (cache == null) {                                                    //IBM-io_converter
                cache = (EncoderCache) (                                            //IBM-io_converter
                java.security.AccessController.doPrivileged(                        //IBM-io_converter
                    new java.security.PrivilegedAction() {                          //IBM-io_converter
                    public Object run () {                                          //IBM-io_converter
                        return new EncoderCache();                                  //IBM-io_converter
                    }                                                               //IBM-io_converter
                }                                                                   //IBM-io_converter
                ));                                                                 //IBM-io_converter
                encoder.set(cache);                                                 //IBM-io_converter
            }                                                                       //IBM-io_converter
            return cache.getEncoder(encoding);                                      //IBM-io_converter
         } finally {                                                                //IBM-wrt_bringup
                MemorySafetyService.exitLastMemoryArea(oldArea);                    //IBM-wrt_bringup                		
         }                                                                          //IBM-wrt_bringup

    }

                                                                                //IBM-io_converter
    /*                                                                          //IBM-io_converter
     * Trim the given byte array to the given length                            //IBM-io_converter
     */                                                                         //IBM-io_converter
    private static byte[] trim(byte[] ba, int len) {                            //IBM-io_converter
        if (len == ba.length)                                                   //IBM-io_converter
            return ba;                                                          //IBM-io_converter
        byte[] tba = new byte[len];                                             //IBM-io_converter
        System.arraycopy(ba, 0, tba, 0, len);                                   //IBM-io_converter
        return tba;                                                             //IBM-io_converter
    }

                                                                                //IBM-io_converter
    /*                                                                          //IBM-io_converter
     * Trim the given char array to the given length                            //IBM-io_converter
     */                                                                         //IBM-io_converter
    private static char[] trim(char[] ca, int len) {                            //IBM-io_converter
        if (len == ca.length)                                                   //IBM-io_converter
            return ca;                                                          //IBM-io_converter
        char[] tca = new char[len];                                             //IBM-io_converter
        System.arraycopy(ca, 0, tca, 0, len);                                   //IBM-io_converter
        return tca;                                                             //IBM-io_converter
    }

                                                                                //IBM-io_converter
    /*                                                                          //IBM-io_converter
     * Warn about unsupported charset                                           //IBM-io_converter
     */                                                                         //IBM-io_converter
    private static void warnUnsupportedCharset(String csn, Object coder) {      //IBM-io_converter
        if (coder == null) {                                                    //IBM-io_converter
            /* This is a fatal condition */                                     //IBM-io_converter
            MessageUtils.err("Error: The encoding ISO-8859-1 is not available."); //IBM-io_converter
            System.exit(1);                                                     //IBM-io_converter
        }                                                                       //IBM-io_converter
        String name = coder.toString();                                         //IBM-io_converter
        int ix = name.indexOf(": ");                                            //IBM-io_converter
        if (ix>=0)                                                              //IBM-io_converter
            name = name.substring(ix+2);                                        //IBM-io_converter
        Converters.setDefaultEncodingName(name);                                //IBM-io_converter
        MessageUtils.err("[ Warning: The encoding '" + csn +                    //IBM-io_converter
                         "' is not supported; using '" + name + "' instead. ]"); //IBM-io_converter
    }


    /*                                                                          //IBM-io_converter
     * Decode from a named charset                                              //IBM-io_converter
     */                                                                         //IBM-io_converter
    static char[] decode(String charsetName, byte[] ba, int off, int len)       //IBM-io_converter
        throws UnsupportedEncodingException {                                   //IBM-io_converter
        Object obj = getDecoder(charsetName);                                   //IBM-io_converter

        /*                                                                      //IBM-io_converter
         * ByteToCharConverter                                                  //IBM-io_converter
         */                                                                     //IBM-io_converter
        if (obj instanceof ByteToCharConverter) {                               //IBM-io_converter
            ByteToCharConverter b2c = (ByteToCharConverter)obj;                 //IBM-io_converter
            //MessageUtils.err ("IO decoder is :" + b2c);                       //IBM-io_converter
            int en = b2c.getMaxCharsPerByte() * len;                            //IBM-io_converter
            char[] ca = new char[en];                                           //IBM-io_converter
            if (len == 0)                                                       //IBM-io_converter
                return ca;                                                      //IBM-io_converter

            b2c.reset();                                                        //IBM-io_converter

            /*                                                                  //IBM-io_converter
             * Loop thru the buffer ingoring MalforedInput                      //IBM-io_converter
             */                                                                 //IBM-io_converter
            int outlen = 0;                                                     //IBM-io_converter
            int inend  = off+len;                                               //IBM-io_converter
            for (;;) {                                                          //IBM-io_converter
                try {                                                           //IBM-io_converter
                    outlen += IOConverter.convert(b2c, ba, off, inend, ca, outlen, en);
                    break;                                                      //IBM-io_converter
                } catch (MalformedInputException x) {                           //IBM-io_converter
                    int newoff = b2c.nextByteIndex() + b2c.getBadInputLength(); //IBM-io_converter
                    if (newoff <= off)   /* paranoid check for loop */          //IBM-io_converter
                        break;                                                  //IBM-io_converter
                    off = newoff;                                               //IBM-io_converter
                    outlen = b2c.nextCharIndex();       //ibm.52333             //IBM-io_converter
                } catch (Exception e) {                                         //IBM-io_converter
                    throw new Error(e);                                         //IBM-io_converter
                }                                                               //IBM-io_converter
            }                                                                   //IBM-io_converter

            /*                                                                  //IBM-io_converter
             * Fast return for single byte.  We can do this because neither     //IBM-io_converter
             * flush() nor trim is required.                                    //IBM-io_converter
             *                                                                  //IBM-io_converter
            if (en == outlen)                                                   //IBM-io_converter
                return ca;                                                      //IBM-io_converter

            /*                                                                  //IBM-io_converter
             * Flush the converter and trim the output                          //IBM-io_converter
             */                                                                 //IBM-io_converter
            try {                                                               //IBM-io_converter
                outlen += IOConverter.flush(b2c, ca, b2c.nextCharIndex(), en);               //IBM-io_converter
            } catch (MalformedInputException x) {                               //IBM-io_converter
            } catch (Exception e) {                                             //IBM-io_converter
                throw new Error(e);                                             //IBM-io_converter
            }                                                                   //IBM-io_converter
            return trim(ca, outlen);                                            //IBM-io_converter
        }                                                                       //IBM-io_converter

        /*                                                                      //IBM-io_converter
         * Charset Decoder                                                      //IBM-io_converter
         */                                                                     //IBM-io_converter
        else {                                                                  //IBM-io_converter
            CharsetDecoder cd = (CharsetDecoder)obj;                            //IBM-io_converter
            //MessageUtils.err ("NIO decoder is :" + cd);                       //IBM-io_converter
            int en = (int)(cd.maxCharsPerByte() * len);                         //IBM-io_converter
            char[] ca = new char[en];                                           //IBM-io_converter
            if (len == 0)                                                       //IBM-io_converter
                return ca;                                                      //IBM-io_converter
            cd.reset();                                                         //IBM-io_converter
            ByteBuffer bb = ByteBuffer.wrap(ba, off, len);                      //IBM-io_converter
            CharBuffer cb = CharBuffer.wrap(ca);                                //IBM-io_converter
            CoderResult cr = cd.decode(bb, cb, true);                           //IBM-io_converter
            cr = cd.flush(cb);                                                  //IBM-io_converter
            return trim(ca, cb.position());                                     //IBM-io_converter
        }                                                                       //IBM-io_converter
    }

                                                                                //IBM-io_converter
    /*                                                                          //IBM-io_converter
     * Decode from the default charset                                          //IBM-io_converter
     */                                                                         //IBM-io_converter
    static char[] decode(byte[] ba, int off, int len) {                         //IBM-io_converter
        try {                                                                   //IBM-io_converter
            return decode("\ufffc", ba, off, len);                              //IBM-io_converter
        } catch (UnsupportedEncodingException e) {                              //IBM-io_converter
            return null;                                                        //IBM-io_converter
        }                                                                       //IBM-io_converter
    }

    /*                                                                          //IBM-io_converter
     * Encode to a named charset                                                //IBM-io_converter
     */                                                                         //IBM-io_converter
    static byte[] encode(String charsetName, char[] ca, int off, int len)       //IBM-io_converter
        throws UnsupportedEncodingException {                                   //IBM-io_converter
        Object obj = getEncoder(charsetName);                                   //IBM-io_converter
                                                                                //IBM-io_converter
        /*                                                                      //IBM-io_converter
         * CharToByteConverter                                                  //IBM-io_converter
         */                                                                     //IBM-io_converter
        if (obj instanceof CharToByteConverter) {                               //IBM-io_converter
            CharToByteConverter c2b = (CharToByteConverter)obj;                 //IBM-io_converter
            int    en = c2b.getMaxBytesPerChar() * len;                         //IBM-io_converter
            byte[] ba = new byte[en];                                           //IBM-io_converter
            if (len == 0)                                                       //IBM-io_converter
                return ba;                                                      //IBM-io_converter
                                                                                //IBM-io_converter
            c2b.reset();                                                        //IBM-io_converter
            int outlen = 0;                                                     //IBM-io_converter
            int inend  = off+len;                                               //IBM-io_converter
            for (;;) {                                                          //IBM-io_converter
                try {                                                           //IBM-io_converter
                    outlen += IOConverter.convert(c2b, ca, off, inend, ba, outlen, en);      //IBM-io_converter
                    break;                                                      //IBM-io_converter
                } catch (MalformedInputException e) {                           //IBM-io_converter
                    /* Ignore malformed input.  Be paranoid about badInputLength */ //IBM-io_converter
                    int newoff = c2b.nextCharIndex() + c2b.getBadInputLength(); //IBM-io_converter
                    if (newoff <= off)                                          //IBM-io_converter
                        break;                                                  //IBM-io_converter
                    off = newoff;                                               //IBM-io_converter
                    outlen = c2b.nextByteIndex();     //ibm.52333               //IBM-io_converter
                } catch (Exception e) {                                         //IBM-io_converter
                    throw new Error(e);           /* Should never happen */     //IBM-io_converter
                }                                                               //IBM-io_converter
            }                                                                   //IBM-io_converter
            try {                                                               //IBM-io_converter
                outlen += IOConverter.flush(c2b, ba, c2b.nextByteIndex(), en);               //IBM-io_converter
            } catch (MalformedInputException x) {                               //IBM-io_converter
            } catch (Exception e) {                                             //IBM-io_converter
                throw new Error(e);                                             //IBM-io_converter
            }                                                                   //IBM-io_converter
            return trim(ba, outlen);                                            //IBM-io_converter
        }                                                                       //IBM-io_converter
                                                                                //IBM-io_converter
        /*                                                                      //IBM-io_converter
         * Charset Encoder                                                      //IBM-io_converter
         */                                                                     //IBM-io_converter
        else {                                                                  //IBM-io_converter
            CharsetEncoder ce = (CharsetEncoder)obj;                            //IBM-io_converter
            int en = (int)(ce.maxBytesPerChar() * len);                         //IBM-io_converter
            byte[] ba = new byte[en];                                           //IBM-io_converter
            if (len == 0)                                                       //IBM-io_converter
                return ba;                                                      //IBM-io_converter
                                                                                //IBM-io_converter
            ce.reset();                                                         //IBM-io_converter
            ByteBuffer bb = ByteBuffer.wrap(ba);                                //IBM-io_converter
            CharBuffer cb = CharBuffer.wrap(ca, off, len);                      //IBM-io_converter
            CoderResult cr = ce.encode(cb, bb, true);                           //IBM-io_converter
            cr = ce.flush(bb);                                                  //IBM-io_converter
            return trim(ba, bb.position());                                     //IBM-io_converter
        }                                                                       //IBM-io_converter
    }

                                                                                //IBM-io_converter
    /*                                                                          //IBM-io_converter
     * Encode to the default charset                                            //IBM-io_converter
     */                                                                         //IBM-io_converter
    static byte[] encode(char[] ca, int off, int len) {
        try {                                                                   //IBM-io_converter
            return encode("\ufffc", ca, off, len);                              //IBM-io_converter
        } catch (UnsupportedEncodingException e) {                              //IBM-io_converter
            return null;                                                        //IBM-io_converter
        }                                                                       //IBM-io_converter
    }                                                                           //IBM-io_converter
                                                                                //IBM-io_converter
    /* Required slow path code for calls that pass a charset.                   //IBM-io_converter
     * The method above of taking the Charset name and throwing                 //IBM-io_converter
     * away the Charset itself does not work if we are passed                   //IBM-io_converter
     * a non-standard Charset object that someone has written                   //IBM-io_converter
     * themselves.                                                              //IBM-io_converter
     * In the cases where we are passed a Charset object we                     //IBM-io_converter
     * need to make sure we use it or we risk failing if                        //IBM-io_converter
     * we can't find that Charset by name later.                                //IBM-io_converter
     * Sadly this means we have the slow path available still                   //IBM-io_converter
     * but as far as I know this is only used by new methods                    //IBM-io_converter
     * in java 1.6 specifically:                                                //IBM-io_converter
     * java.lang.String.String(byte[] bytes, Charset charset)                   //IBM-io_converter
     * java.lang.String.String(byte[] bytes, int offset, int length, Charset charset) //IBM-io_converter
     * java.lang.String.getBytes(Charset charset)                               //IBM-io_converter
     * so no existing users will notice the drop in performance.                //IBM-io_converter
     * The code below comes straight from Suns original                         //IBM-io_converter
     * implementation.                                                          //IBM-io_converter
     *                                                                          //IBM-io_converter
     * See CMVC 127158 or JSE-3044 for full details and a                       //IBM-io_converter
     * couple of testcases that demonstrate the problem.                        //IBM-io_converter
     */                                                                         //IBM-io_converter
                                                                                //IBM-io_converter
    // Trim the given byte array to the given length                            //IBM-io_converter
    //                                                                          //IBM-io_converter
    private static byte[] safeTrim(byte[] ba, int len, Charset cs) {            //IBM-io_converter
 	if (len == ba.length                                                   //IBM-io_converter
	    && (System.getSecurityManager() == null                             //IBM-io_converter
		|| cs.getClass().getClassLoaderImpl() == ClassLoader.getSystemClassLoader())) //IBM-io_converter
	    return ba;                                                          //IBM-io_converter
        else                                                                    //IBM-io_converter
            return Arrays.copyOf(ba, len);                                      //IBM-io_converter
    }                                                                           //IBM-io_converter
                                                                                //IBM-io_converter
    // Trim the given byte array to the given length
    //
    private static byte[] safeTrim(byte[] ba, int len, Charset cs, boolean isTrusted) {
        if (len == ba.length && (isTrusted || System.getSecurityManager() == null))
            return ba;
        else
            return Arrays.copyOf(ba, len);
    }

    // Trim the given char array to the given length
    //
    private static char[] safeTrim(char[] ca, int len,
                                   Charset cs, boolean isTrusted) {
        if (len == ca.length && (isTrusted || System.getSecurityManager() == null))
            return ca;
        else
            return Arrays.copyOf(ca, len);
    }

    // Trim the given char array to the given length                            //IBM-io_converter
    //                                                                          //IBM-io_converter
    private static char[] safeTrim(char[] ca, int len, Charset cs) {            //IBM-io_converter
 	if (len == ca.length                                                   //IBM-io_converter
	    && (System.getSecurityManager() == null                             //IBM-io_converter
		|| cs.getClass().getClassLoaderImpl() == ClassLoader.getSystemClassLoader())) //IBM-io_converter
	    return ca;                                                          //IBM-io_converter
        else                                                                    //IBM-io_converter
            return Arrays.copyOf(ca, len);                                      //IBM-io_converter
    }                                                                           //IBM-io_converter
                                                                                //IBM-io_converter
    private static int scale(int len, float expansionFactor) {                  //IBM-io_converter
	// We need to perform double, not float, arithmetic; otherwise          //IBM-io_converter
	// we lose low order bits when len is larger than 2**24.                //IBM-io_converter
	return (int)(len * (double)expansionFactor);                            //IBM-io_converter
    }                                                                           //IBM-io_converter
                                                                                //IBM-io_converter
    // -- Decoding --
    private static class StringDecoder {
        private final String requestedCharsetName;
        private final Charset cs;
        private final CharsetDecoder cd;
                                                                                //IBM-io_converter
        private StringDecoder(Charset cs, String rcn) {
            this.requestedCharsetName = rcn;
            this.cs = cs;
            this.cd = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        String charsetName() {
            if (cs instanceof HistoricallyNamedCharset)
                return ((HistoricallyNamedCharset)cs).historicalName();
            return cs.name();
        }

        final String requestedCharsetName() {
            return requestedCharsetName;
        }

        char[] decode(byte[] ba, int off, int len) {
            int en = scale(len, cd.maxCharsPerByte());
            char[] ca = new char[en];
            if (len == 0)
                return ca;
            cd.reset();
            ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
            CharBuffer cb = CharBuffer.wrap(ca);
            try {
                CoderResult cr = cd.decode(bb, cb, true);
                if (!cr.isUnderflow())
                    cr.throwException();
                    cr = cd.flush(cb);
                if (!cr.isUnderflow())
                    cr.throwException();
            } catch (CharacterCodingException x) {
                // Substitution is always enabled,
                // so this shouldn't happen
                throw new Error(x);
            }
	    return safeTrim(ca, cb.position(), cs);                             //IBM-io_converter
        }
    }

    static char[] decode(Charset cs, byte[] ba, int off, int len) {             //IBM-io_converter
        // (1)We never cache the "external" cs, the only benefit of creating
        // an additional StringDe/Encoder object to wrap it is to share the
        // de/encode() method. These SD/E objects are short-lifed, the young-gen
        // gc should be able to take care of them well. But the best approash
        // is still not to generate them if not really necessary.
        // (2)The defensive copy of the input byte/char[] has a big performance
        // impact, as well as the outgoing result byte/char[]. Need to do the
        // optimization check of (sm==null && classLoader0==null) for both.
        // (3)getClass().getClassLoader0() is expensive
        // (4)There might be a timing gap in isTrusted setting. getClassLoader0()
        // is only chcked (and then isTrusted gets set) when (SM==null). It is
        // possible that the SM==null for now but then SM is NOT null later
        // when safeTrim() is invoked...the "safe" way to do is to redundant
        // check (... && (isTrusted || SM == null || getClassLoader0())) in trim
        // but it then can be argued that the SM is null when the opertaion
        // is started...
        CharsetDecoder cd = cs.newDecoder();
        int en = scale(len, cd.maxCharsPerByte());
        char[] ca = new char[en];
        if (len == 0)
            return ca;
        boolean isTrusted = false;
        if (System.getSecurityManager() != null) {
            if (!(isTrusted = (cs.getClass().getClassLoader0() == null))) {
                ba =  Arrays.copyOfRange(ba, off, off + len);
                off = 0;
            }
        }
        cd.onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE)
          .reset();
        if (cd instanceof ArrayDecoder) {
            int clen = ((ArrayDecoder)cd).decode(ba, off, len, ca);
            return safeTrim(ca, clen, cs, isTrusted);
        } else {
            ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
            CharBuffer cb = CharBuffer.wrap(ca);
            try {
                CoderResult cr = cd.decode(bb, cb, true);
                if (!cr.isUnderflow())
                    cr.throwException();
                cr = cd.flush(cb);
                if (!cr.isUnderflow())
                    cr.throwException();
            } catch (CharacterCodingException x) {
                // Substitution is always enabled,
                // so this shouldn't happen
                throw new Error(x);
            }
            return safeTrim(ca, cb.position(), cs, isTrusted);
        }
    }                                                                           //IBM-io_converter

    // -- Encoding --
    private static class StringEncoder {
        private Charset cs;
        private CharsetEncoder ce;
        private final String requestedCharsetName;
        private final boolean isTrusted;

        private StringEncoder(Charset cs, String rcn) {
            this.requestedCharsetName = rcn;
            this.cs = cs;
            this.ce = cs.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.isTrusted = (cs.getClass().getClassLoader0() == null);
        }

        String charsetName() {
            if (cs instanceof HistoricallyNamedCharset)
                return ((HistoricallyNamedCharset)cs).historicalName();
            return cs.name();
        }

        final String requestedCharsetName() {
            return requestedCharsetName;
        }

        byte[] encode(char[] ca, int off, int len) {
            int en = scale(len, ce.maxBytesPerChar());
            byte[] ba = new byte[en];
            if (len == 0)
                return ba;
                                                                                //IBM-io_converter
            ce.reset();
            ByteBuffer bb = ByteBuffer.wrap(ba);
            CharBuffer cb = CharBuffer.wrap(ca, off, len);
            try {
                CoderResult cr = ce.encode(cb, bb, true);
                if (!cr.isUnderflow())
                    cr.throwException();
                cr = ce.flush(bb);
                if (!cr.isUnderflow())
                    cr.throwException();
            } catch (CharacterCodingException x) {
                // Substitution is always enabled,
                // so this shouldn't happen
                throw new Error(x);
            }
	    return safeTrim(ba, bb.position(), cs);                             //IBM-io_converter
        }
    }

    static byte[] encode(Charset cs, char[] ca, int off, int len) {             //IBM-io_converter
        CharsetEncoder ce = cs.newEncoder();
        int en = scale(len, ce.maxBytesPerChar());
        byte[] ba = new byte[en];
        if (len == 0)
            return ba;
        boolean isTrusted = false;
        if (System.getSecurityManager() != null) {
            if (!(isTrusted = (cs.getClass().getClassLoader0() == null))) {
                ca =  Arrays.copyOfRange(ca, off, off + len);
                off = 0;
            }
        }
        ce.onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE)
          .reset();
        if (ce instanceof ArrayEncoder) {
            int blen = ((ArrayEncoder)ce).encode(ca, off, len, ba);
            return safeTrim(ba, blen, cs, isTrusted);
        } else {
            ByteBuffer bb = ByteBuffer.wrap(ba);
            CharBuffer cb = CharBuffer.wrap(ca, off, len);
            try {
                CoderResult cr = ce.encode(cb, bb, true);
                if (!cr.isUnderflow())
                    cr.throwException();
                cr = ce.flush(bb);
                if (!cr.isUnderflow())
                    cr.throwException();
            } catch (CharacterCodingException x) {
                throw new Error(x);
            }
            return safeTrim(ba, bb.position(), cs, isTrusted);
        }
    }                                                                           //IBM-io_converter

}

