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
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileDescriptor;

import sun.net.ConnectionResetException;
import sun.net.NetHooks;
import sun.net.ResourceManager;

import com.ibm.net.NetworkRecycledException;            /*JSE-2495*/            //IBM-zos_cinet
import java.security.AccessController;                                          //IBM-zos_cinet
import sun.security.action.GetPropertyAction;           /*JSE-2495*/            //IBM-zos_cinet
                                                                                //IBM-zos_cinet
/**
 * Default Socket Implementation. This implementation does
 * not implement any security checks.
 * Note this class should <b>NOT</b> be public.
 *
 * @author  Steven B. Byrne
 */
abstract class AbstractPlainSocketImpl extends SocketImpl
{
    /* instance variable for SO_TIMEOUT */
    int timeout;   // timeout in millisec
    // traffic class
    private int trafficClass;

    private boolean shut_rd = false;
    private boolean shut_wr = false;

    private SocketInputStream socketInputStream = null;

    /* number of threads using the FileDescriptor */
    protected int fdUseCount = 0;

    /* lock when increment/decrementing fdUseCount */
    protected final Object fdLock = new Object();

    /* indicates a close is pending on the file descriptor */
    protected boolean closePending = false;

    /* indicates connection reset state */
    private int CONNECTION_NOT_RESET = 0;
    private int CONNECTION_RESET_PENDING = 1;
    private int CONNECTION_RESET = 2;
    private int resetState;
    private final Object resetLock = new Object();

   /* whether this Socket is a stream (TCP) socket or not (UDP)
    */
    protected boolean stream;

    /* JSE-2495                                                                 //IBM-zos_cinet
     * Variables to record user specified backlog and port                      //IBM-zos_cinet
     * value setting it to the default value                                    //IBM-zos_cinet
     */                                                                         //IBM-zos_cinet
     private int backlogQ = 50;                                                 //IBM-zos_cinet
     private int sport = 0;                                                     //IBM-zos_cinet
     private static boolean zOS = false;                                        //IBM-zos_cinet
    /* JSE-2495 ends */                                                         //IBM-zos_cinet
                                                                                //IBM-zos_cinet
    /**
     * Load net library into runtime.
     */
    static {
        java.security.AccessController.doPrivileged(
                  new sun.security.action.LoadLibraryAction("net"));
                                                                                //IBM-zos_cinet
        GetPropertyAction propa = new GetPropertyAction("os.name"); /*JSE-2495*/ //IBM-zos_cinet
        String osName = (String)AccessController.doPrivileged(propa);           //IBM-zos_cinet
        if (osName.equals("z/OS")) {                                            //IBM-zos_cinet
                zOS = true;                                                     //IBM-zos_cinet
        }                                               /*JSE-2495 ends*/       //IBM-zos_cinet
    }

    /**
     * Creates a socket with a boolean that specifies whether this
     * is a stream socket (true) or an unconnected UDP socket (false).
     */
    protected synchronized void create(boolean stream) throws IOException {
        this.stream = stream;
        if (!stream) {
            ResourceManager.beforeUdpCreate();
            // only create the fd after we know we will be able to create the socket
            fd = new FileDescriptor();
            try {
                socketCreate(false);
            } catch (IOException ioe) {
                ResourceManager.afterUdpClose();
                fd = null;
                throw ioe;
            }
        } else {
            fd = new FileDescriptor();
            socketCreate(true);
        }
        if (socket != null)
            socket.setCreated();
        if (serverSocket != null)
            serverSocket.setCreated();
    }

    /**
     * Creates a socket and connects it to the specified port on
     * the specified host.
     * @param host the specified host
     * @param port the specified port
     */
    protected void connect(String host, int port)
        throws UnknownHostException, IOException
    {
        boolean connected = false;
        try {
            InetAddress address = InetAddress.getByName(host);
            this.port = port;
            this.address = address;

            connectToAddress(address, port, timeout);
            connected = true;
        } finally {
            if (!connected) {
                try {
                    close();
                } catch (IOException ioe) {
                    /* Do nothing. If connect threw an exception then
                       it will be passed up the call stack */
                }
            }
        }
    }

    /**
     * Creates a socket and connects it to the specified address on
     * the specified port.
     * @param address the address
     * @param port the specified port
     */
    protected void connect(InetAddress address, int port) throws IOException {
        this.port = port;
        this.address = address;

        try {
            connectToAddress(address, port, timeout);
            return;
        } catch (IOException e) {
            // everything failed
            close();
            throw e;
        }
    }

    /**
     * Creates a socket and connects it to the specified address on
     * the specified port.
     * @param address the address
     * @param timeout the timeout value in milliseconds, or zero for no timeout.
     * @throws IOException if connection fails
     * @throws  IllegalArgumentException if address is null or is a
     *          SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    protected void connect(SocketAddress address, int timeout)
            throws IOException {
        boolean connected = false;
        try {
            if (address == null || !(address instanceof InetSocketAddress))
                throw new IllegalArgumentException("unsupported address type");
            InetSocketAddress addr = (InetSocketAddress) address;
            if (addr.isUnresolved())
                throw new UnknownHostException(addr.getHostName());
            this.port = addr.getPort();
            this.address = addr.getAddress();

            connectToAddress(this.address, port, timeout);
            connected = true;
        } finally {
            if (!connected) {
                try {
                    close();
                } catch (IOException ioe) {
                    /* Do nothing. If connect threw an exception then
                       it will be passed up the call stack */
                }
            }
        }
    }

    private void connectToAddress(InetAddress address, int port, int timeout) throws IOException {
        if (address.isAnyLocalAddress()) {
            doConnect(InetAddress.getLocalHost(), port, timeout);
        } else {
            doConnect(address, port, timeout);
        }
    }

    public void setOption(int opt, Object val) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        boolean on = true;
        switch (opt) {
            /* check type safety b4 going native.  These should never
             * fail, since only java.Socket* has access to
             * PlainSocketImpl.setOption().
             */
        case SO_LINGER:
            if (val == null || (!(val instanceof Integer) && !(val instanceof Boolean)))
                throw new SocketException("Bad parameter for option");
            if (val instanceof Boolean) {
                /* true only if disabling - enabling should be Integer */
                on = false;
            }
            break;
        case SO_TIMEOUT:
            if (val == null || (!(val instanceof Integer)))
                throw new SocketException("Bad parameter for SO_TIMEOUT");
            int tmp = ((Integer) val).intValue();
            if (tmp < 0)
                throw new IllegalArgumentException("timeout < 0");
            timeout = tmp;
            break;
        case IP_TOS:
             if (val == null || !(val instanceof Integer)) {
                 throw new SocketException("bad argument for IP_TOS");
             }
             trafficClass = ((Integer)val).intValue();
             break;
        case SO_BINDADDR:
            throw new SocketException("Cannot re-bind socket");
        case TCP_NODELAY:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for TCP_NODELAY");
            on = ((Boolean)val).booleanValue();
            break;
        case SO_SNDBUF:
        case SO_RCVBUF:
            if (val == null || !(val instanceof Integer) ||
                !(((Integer)val).intValue() > 0)) {
                throw new SocketException("bad parameter for SO_SNDBUF " +
                                          "or SO_RCVBUF");
            }
            break;
        case SO_KEEPALIVE:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for SO_KEEPALIVE");
            on = ((Boolean)val).booleanValue();
            break;
        case SO_OOBINLINE:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for SO_OOBINLINE");
            on = ((Boolean)val).booleanValue();
            break;
        case SO_REUSEADDR:
            if (val == null || !(val instanceof Boolean))
                throw new SocketException("bad parameter for SO_REUSEADDR");
            on = ((Boolean)val).booleanValue();
            break;
        default:
            throw new SocketException("unrecognized TCP option: " + opt);
        }
        socketSetOption(opt, on, val);
    }
    public Object getOption(int opt) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        if (opt == SO_TIMEOUT) {
            return new Integer(timeout);
        }
        int ret = 0;
        /*
         * The native socketGetOption() knows about 3 options.
         * The 32 bit value it returns will be interpreted according
         * to what we're asking.  A return of -1 means it understands
         * the option but its turned off.  It will raise a SocketException
         * if "opt" isn't one it understands.
         */

        switch (opt) {
        case TCP_NODELAY:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        case SO_OOBINLINE:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        case SO_LINGER:
            ret = socketGetOption(opt, null);
            return (ret == -1) ? Boolean.FALSE: (Object)(new Integer(ret));
        case SO_REUSEADDR:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        case SO_BINDADDR:
            InetAddressContainer in = new InetAddressContainer();
            ret = socketGetOption(opt, in);
            return in.addr;
        case SO_SNDBUF:
        case SO_RCVBUF:
            ret = socketGetOption(opt, null);
            return new Integer(ret);
        case IP_TOS:
            ret = socketGetOption(opt, null);
            if (ret == -1) { // ipv6 tos
                return new Integer(trafficClass);
            } else {
                return new Integer(ret);
            }
        case SO_KEEPALIVE:
            ret = socketGetOption(opt, null);
            return Boolean.valueOf(ret != -1);
        // should never get here
        default:
            return null;
        }
    }

    /**
     * The workhorse of the connection operation.  Tries several times to
     * establish a connection to the given <host, port>.  If unsuccessful,
     * throws an IOException indicating what went wrong.
     */

    synchronized void doConnect(InetAddress address, int port, int timeout) throws IOException {
        synchronized (fdLock) {
            if (!closePending && (socket == null || !socket.isBound())) {
                NetHooks.beforeTcpConnect(fd, address, port);
            }
        }
        try {
            acquireFD();
            try {
                socketConnect(address, port, timeout);
                /* socket may have been closed during poll/select */
                synchronized (fdLock) {
                    if (closePending) {
                        throw new SocketException ("Socket closed");
                    }
                }
                // If we have a ref. to the Socket, then sets the flags
                // created, bound & connected to true.
                // This is normally done in Socket.connect() but some
                // subclasses of Socket may call impl.connect() directly!
                if (socket != null) {
                    socket.setBound();
                    socket.setConnected();
                }
            } finally {
                releaseFD();
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Binds the socket to the specified address of the specified local port.
     * @param address the address
     * @param port the port
     */
    protected synchronized void bind(InetAddress address, int lport)
        throws IOException
    {
       synchronized (fdLock) {
            if (!closePending && (socket == null || !socket.isBound())) {
                NetHooks.beforeTcpBind(fd, address, lport);
            }
        }
        socketBind(address, lport);
        if (zOS){                                               /*JSE-2495*/    //IBM-zos_cinet
                this.sport = lport;                                             //IBM-zos_cinet
        }                                                                       //IBM-zos_cinet
        if (socket != null)
            socket.setBound();
        if (serverSocket != null)
            serverSocket.setBound();
    }

    /**
     * Listens, for a specified amount of time, for connections.
     * @param count the amount of time to listen for connections
     */
    protected synchronized void listen(int count) throws IOException {
        if (zOS){                                               /*JSE-2495*/    //IBM-zos_cinet
                this.backlogQ = count;                                          //IBM-zos_cinet
        }                                                                       //IBM-zos_cinet
        socketListen(count);
    }

    /**
     * Accepts connections.
     * @param s the connection
     */
    protected void accept(SocketImpl s) throws IOException {
        acquireFD();
        /*JSE-2495 starts*/                                                     //IBM-zos_cinet
        boolean recoverProp;                                                    //IBM-zos_cinet
        int tempPort=0, tempBacklogQ=50;                                        //IBM-zos_cinet
        do {                                                                    //IBM-zos_cinet
                recoverProp=false;                                              //IBM-zos_cinet
        try {
            socketAccept(s);
        /* NetworkRecycledException is only thrown on z/OS when a Network Stack //IBM-zos_cinet
         * gets recycled. This can only happen in Multiple Stack scenario and   //IBM-zos_cinet
         * when Java application  was listening on an INADDR_ANY.               //IBM-zos_cinet
         * It is captured below and processed based on a system property.       //IBM-zos_cinet
         */                                                                     //IBM-zos_cinet
        } catch(NetworkRecycledException nre) {                                 //IBM-zos_cinet
                recoverProp = Boolean.getBoolean("ibm.serversocket.recover");   //IBM-zos_cinet
                        if(recoverProp) {                                       //IBM-zos_cinet
                                tempPort=this.sport;                            //IBM-zos_cinet
                                tempBacklogQ = this.backlogQ;                   //IBM-zos_cinet
                                try {                                           //IBM-zos_cinet
                                        processNRException(InetAddress.anyLocalAddress(), tempPort, tempBacklogQ); //IBM-zos_cinet
                                } catch(SecurityException e) {                  //IBM-zos_cinet
                                        recoverProp = false;    //Set to false, releaseFD() will get called //IBM-zos_cinet
                                        throw e;                                //IBM-zos_cinet
                                } catch(IOException e) {                        //IBM-zos_cinet
                                        recoverProp = false;    //Set to false, releaseFD() will get called //IBM-zos_cinet
                                        throw e;                                //IBM-zos_cinet
                                }                                               //IBM-zos_cinet
                        } else                                                  //IBM-zos_cinet
                                throw nre;                    /*JSE-2495 ends*/ //IBM-zos_cinet
        } finally {
            if(!(zOS) || !(recoverProp)) {                                      //IBM-zos_cinet
              /*Ignoring releaseFD when NetworkRecycledException is processed.*/ //IBM-zos_cinet
                    releaseFD();                                                //IBM-zos_cinet
            }                                                                   //IBM-zos_cinet
        }
      }while(recoverProp);                                      /*JSE-2495*/    //IBM-zos_cinet
    }

    /**
     * Gets an InputStream for this socket.
     */
    protected synchronized InputStream getInputStream() throws IOException {
        if (isClosedOrPending()) {
            throw new IOException("Socket Closed");
        }
        if (shut_rd) {
            throw new IOException("Socket input is shutdown");
        }
        if (socketInputStream == null) {
            socketInputStream = new SocketInputStream(this);
        }
        return socketInputStream;
    }

    void setInputStream(SocketInputStream in) {
        socketInputStream = in;
    }

    /**
     * Gets an OutputStream for this socket.
     */
    protected synchronized OutputStream getOutputStream() throws IOException {
        if (isClosedOrPending()) {
            throw new IOException("Socket Closed");
        }
        if (shut_wr) {
            throw new IOException("Socket output is shutdown");
        }
        return new SocketOutputStream(this);
    }

    void setFileDescriptor(FileDescriptor fd) {
        this.fd = fd;
    }

    void setAddress(InetAddress address) {
        this.address = address;
    }

    void setPort(int port) {
        this.port = port;
    }

    void setLocalPort(int localport) {
        this.localport = localport;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     */
    protected synchronized int available() throws IOException {
        if (isClosedOrPending()) {
            throw new IOException("Stream closed.");
        }

        /*
         * If connection has been reset then return 0 to indicate
         * there are no buffered bytes.
         */
        if (isConnectionReset()) {
            return 0;
        }

        /*
         * If no bytes available and we were previously notified
         * of a connection reset then we move to the reset state.
         *
         * If are notified of a connection reset then check
         * again if there are bytes buffered on the socket.
         */
        int n = 0;
        try {
            n = socketAvailable();
            if (n == 0 && isConnectionResetPending()) {
                setConnectionReset();
            }
        } catch (ConnectionResetException exc1) {
            setConnectionResetPending();
            try {
                n = socketAvailable();
                if (n == 0) {
                    setConnectionReset();
                }
            } catch (ConnectionResetException exc2) {
            }
        }
        return n;
    }

    /**
     * Closes the socket.
     */
    protected void close() throws IOException {
        synchronized(fdLock) {
            if (fd != null) {
                if (!stream) {
                    ResourceManager.afterUdpClose();
                }
                if (fdUseCount == 0) {
                    if (closePending) {
                        return;
                    }
                    closePending = true;
                    /*
                     * We close the FileDescriptor in two-steps - first the
                     * "pre-close" which closes the socket but doesn't
                     * release the underlying file descriptor. This operation
                     * may be lengthy due to untransmitted data and a long
                     * linger interval. Once the pre-close is done we do the
                     * actual socket to release the fd.
                     */
                    try {
                        socketPreClose();
                    } finally {
                        socketClose();
                    }
                    fd = null;
                    return;
                } else {
                    /*
                     * If a thread has acquired the fd and a close
                     * isn't pending then use a deferred close.
                     * Also decrement fdUseCount to signal the last
                     * thread that releases the fd to close it.
                     */
                    if (!closePending) {
                        closePending = true;
                        fdUseCount--;
                        socketPreClose();
                    }
                }
            }
        }
    }

    void reset() throws IOException {
        if (fd != null) {
            socketClose();
        }
        fd = null;
        super.reset();
    }


    /**
     * Shutdown read-half of the socket connection;
     */
    protected void shutdownInput() throws IOException {
      if (fd != null) {
          socketShutdown(SHUT_RD);
          if (socketInputStream != null) {
              socketInputStream.setEOF(true);
          }
          shut_rd = true;
      }
    }
    
    /**
     * @return true if read-half of the socket connection is shut
     * [RDMA_SUPPORT]
     */
    boolean inputShut() {                                  // RDMA_CAPABILITY
    	return shut_rd;
    }                                                      // RDMA_CAPABILITY
    
    /**
     * Shutdown write-half of the socket connection;
     */
    protected void shutdownOutput() throws IOException {
      if (fd != null) {
          socketShutdown(SHUT_WR);
          shut_wr = true;
      }
    }
    
    /**
     * @return true if write-half of the socket connection is shut
     * [RDMA_SUPPORT]
     */
    boolean outputShut() {                                 // RDMA_CAPABILITY
    	return shut_wr;
    }                                                      // RDMA_CAPABILITY

    protected boolean supportsUrgentData () {
        return true;
    }

    protected void sendUrgentData (int data) throws IOException {
        if (fd == null) {
            throw new IOException("Socket Closed");
        }
        socketSendUrgentData (data);
    }

    /**
     * Cleans up if the user forgets to close it.
     */
    protected void finalize() throws IOException {
        close();
    }

    /*
     * "Acquires" and returns the FileDescriptor for this impl
     *
     * A corresponding releaseFD is required to "release" the
     * FileDescriptor.
     */
    FileDescriptor acquireFD() {
        synchronized (fdLock) {
            fdUseCount++;
            return fd;
        }
    }

    /*
     * "Release" the FileDescriptor for this impl.
     *
     * If the use count goes to -1 then the socket is closed.
     */
    void releaseFD() {
        synchronized (fdLock) {
            fdUseCount--;
            if (fdUseCount == -1) {
                if (fd != null) {
                    try {
                        socketClose();
                    } catch (IOException e) {
                    } finally {
                        fd = null;
                    }
                }
            }
        }
    }

    public boolean isConnectionReset() {
        synchronized (resetLock) {
            return (resetState == CONNECTION_RESET);
        }
    }

    public boolean isConnectionResetPending() {
        synchronized (resetLock) {
            return (resetState == CONNECTION_RESET_PENDING);
        }
    }

    public void setConnectionReset() {
        synchronized (resetLock) {
            resetState = CONNECTION_RESET;
        }
    }

    public void setConnectionResetPending() {
        synchronized (resetLock) {
            if (resetState == CONNECTION_NOT_RESET) {
                resetState = CONNECTION_RESET_PENDING;
            }
        }

    }

    /*
     * Return true if already closed or close is pending
     */
    public boolean isClosedOrPending() {
        /*
         * Lock on fdLock to ensure that we wait if a
         * close is in progress.
         */
        synchronized (fdLock) {
            if (closePending || (fd == null)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /*
     * Return the current value of SO_TIMEOUT
     */
    public int getTimeout() {
        return timeout;
    }

    /*
     * "Pre-close" a socket by dup'ing the file descriptor - this enables
     * the socket to be closed without releasing the file descriptor.
     */
    private void socketPreClose() throws IOException {
        socketClose0(true);
    }

    /*
     * Close the socket (and release the file descriptor).
     */
    protected void socketClose() throws IOException {
        socketClose0(false);
    }

    abstract void processNRException(InetAddress address, int port, int backlogQ) throws IOException;    /*JSE-2495*/ //IBM-zos_cinet
    abstract void socketCreate(boolean isServer) throws IOException;
    abstract void socketConnect(InetAddress address, int port, int timeout)
        throws IOException;
    abstract void socketBind(InetAddress address, int port)
        throws IOException;
    abstract void socketListen(int count)
        throws IOException;
    abstract void socketAccept(SocketImpl s)
        throws IOException;
    abstract int socketAvailable()
        throws IOException;
    abstract void socketClose0(boolean useDeferredClose)
        throws IOException;
    abstract void socketShutdown(int howto)
        throws IOException;
    abstract void socketSetOption(int cmd, boolean on, Object value)
        throws SocketException;
    abstract int socketGetOption(int opt, Object iaContainerObj) throws SocketException;
    abstract void socketSendUrgentData(int data)
        throws IOException;

    public final static int SHUT_RD = 0;
    public final static int SHUT_WR = 1;
}

class InetAddressContainer {
    InetAddress addr;
}
//IBM-zos_cinet
