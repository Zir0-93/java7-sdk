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
 * Copyright (c) 2002, 2005, Oracle and/or its affiliates. All rights reserved.
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
package java.net;
import java.io.IOException;
import com.ibm.jvm.MemorySafetyService;                                         //IBM-T6_wrt_bringup

/*
 * Package private implementation of InetAddressImpl for IPv4.
 *
 * @since 1.4
 */
class Inet4AddressImpl implements InetAddressImpl {
    public native String getLocalHostName() throws UnknownHostException;
    public native InetAddress[]
        lookupAllHostAddr(String hostname) throws UnknownHostException;
    public native String getHostByAddr(byte[] addr) throws UnknownHostException;
    private native boolean isReachable0(byte[] addr, int timeout, byte[] ifaddr, int ttl) throws IOException;

    public synchronized InetAddress anyLocalAddress() {
        if (anyLocalAddress == null) {
        //    anyLocalAddress = new Inet4Address(); // {0x00,0x00,0x00,0x00} //IBM-T6_wrt_bringup
          //  anyLocalAddress.holder().hostName = "0.0.0.0";                 //IBM-T6_wrt_bringup
           long oldMemory = -1;                                                //IBM-T6_wrt_bringup
            try                                                     //IBM-T6_wrt_bringup
            {                                                       //IBM-T6_wrt_bringup
                               oldMemory = MemorySafetyService.enterSafeMemoryArea(); //IBM-T6_wrt_bringup
                               anyLocalAddress = new Inet4Address(); // {0x00,0x00,0x00,0x00} //IBM-T6_wrt_bringup
                               anyLocalAddress.holder().hostName = "0.0.0.0";           //IBM-T6_wrt_bringup
                                                                                //IBM-T6_wrt_bringup
              }                                                       //IBM-T6_wrt_bringup
           finally                                                 //IBM-T6_wrt_bringup
                {                                                       //IBM-T6_wrt_bringup
                         MemorySafetyService.exitLastMemoryArea(oldMemory); //IBM-T6_wrt_bringup
             }                                                       //IBM-T6_wrt_bringup                                                                     

        }
        return anyLocalAddress;
    }

    public synchronized InetAddress loopbackAddress() {
        if (loopbackAddress == null) {
            byte[] loopback = {0x7f,0x00,0x00,0x01};
            loopbackAddress = new Inet4Address("localhost", loopback);
        }
        return loopbackAddress;
    }

  public boolean isReachable(InetAddress addr, int timeout, NetworkInterface netif, int ttl) throws IOException {
      byte[] ifaddr = null;
      if (netif != null) {
          /*
           * Let's make sure we use an address of the proper family
           */
          java.util.Enumeration it = netif.getInetAddresses();
          InetAddress inetaddr = null;
          while (!(inetaddr instanceof Inet4Address) &&
                 it.hasMoreElements())
              inetaddr = (InetAddress) it.nextElement();
          if (inetaddr instanceof Inet4Address)
              ifaddr = inetaddr.getAddress();
      }
      return isReachable0(addr.getAddress(), timeout, ifaddr, ttl);
  }
    private InetAddress      anyLocalAddress;
    private InetAddress      loopbackAddress;
}
//IBM-T6_wrt_bringup
