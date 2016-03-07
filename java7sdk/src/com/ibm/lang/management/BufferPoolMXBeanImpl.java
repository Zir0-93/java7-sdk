/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

/**
 * Runtime type for {@link java.nio.BufferPoolMXBean}.
 * 
 * @author jinglv
 * @since 1.7
 */
package com.ibm.lang.management;

import java.lang.management.BufferPoolMXBean;
import java.util.LinkedList;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import sun.misc.JavaNioAccess.BufferPool;

/**
 * The implementation MXBean for buffer pool
 * 
 */
public class BufferPoolMXBeanImpl implements BufferPoolMXBean{

	protected ObjectName objectName;
	
	// we have two types of buffer pool for now
    private	static BufferPool directpool = sun.misc.SharedSecrets.getJavaNioAccess().getDirectBufferPool();
    
    private static BufferPool mappedpool = sun.nio.ch.FileChannelImpl.getMappedBufferPool();
    
    private BufferPool pool;
    
    private static List<BufferPoolMXBean> list = new LinkedList<BufferPoolMXBean>();
    
    static {
    	// return a list in management factory
    	try {
			list.add(new BufferPoolMXBeanImpl(directpool, new ObjectName("java.nio:type=BufferPool,name=direct")));
			list.add(new BufferPoolMXBeanImpl(mappedpool, new ObjectName("java.nio:type=BufferPool,name=mapped")));
		} catch (MalformedObjectNameException e) {
			// ignore
		}
    }
    
    private BufferPoolMXBeanImpl(BufferPool pool, ObjectName name) {
    	this.pool = pool;
    	this.objectName = name;
	}
    
    public static List<BufferPoolMXBean> getBufferPoolMXBeans(){
    	return list;
    }
    
	@Override
	public ObjectName getObjectName() {
		return objectName;
	}

	@Override
	public String getName() {
		return pool.getName();
	}

	@Override
	public long getCount() {
		return pool.getCount();
	}

	@Override
	public long getTotalCapacity() {
		return pool.getTotalCapacity();
	}

	@Override
	public long getMemoryUsed() {
		return pool.getMemoryUsed();
	}
}
