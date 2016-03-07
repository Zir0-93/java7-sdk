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

/**
 * =======================================================================
 * Module Information:
 *
 * DESCRIPTION: This class is used to convert sockets from the TCP to
 * a type that is supported by the underlying network provider before
 * accept or connect happens.
 * Much of this code is derived from sun.net.sdp.SdpProvider class.
 * 
 * This package private final class provides RDMA_CAPABILITY.
 * =======================================================================
 */

package java.net;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Enumeration;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.io.PrintWriter;
import java.io.StringWriter;

import sun.security.action.GetPropertyAction;

final class NetworkSelector {
	// indicates if the RDMA network provider is enabled
	private static final boolean enabled;
	private static final String osName;
	private static final String osArch;
	// Address which user prefers to override during bind
	private static InetAddress preferredAddress;

	// rules when the RDMA protocol is used
	private static LinkedList<Rule> bindRules;
	private static LinkedList<Rule> connectRules;
	private static LinkedList<Rule> acceptRules;
	// network provider to name mapping
	private static HashMap<String, NetworkProvider> providerMap;
        
	// Avoid class instantiation
	private NetworkSelector() {
		// do nothing
	}
	
	/**
     * Helper class to parse the port spec:
     *   <port-spec> = ("*" | port)[ "-" ("*" | port) ]
     */
	private static class PortRange {
    	private final int portStart;
    	private final int portEnd;
    	private static final int MAX_PORT = 65535;
    	private static final int MIN_PORT = 0;
    	
    	/**
    	 * parse port spec and instantiate PortRange object
    	 */
    	public PortRange(String s) throws NumberFormatException {
		int start = MIN_PORT;
		int end = MAX_PORT;

    		// check for range indicator
    		int pos = s.indexOf('-');
    		if (pos < 0) {
    			// only starting port field?
    			boolean all = s.equals("*");
				if (!all) {
					int val = Integer.parseInt(s);
					if (val < MIN_PORT) {
						val = MIN_PORT;
					} else if (val > MAX_PORT) {
						val = MAX_PORT;
					}
					start = val;
					end = start;
				}
    			//portStart = all ? 0 : Integer.parseInt(s);
    			//portEnd = all ? MAX_PORT : portStart;
    		} else {
    			// both start and end port fields?
    			String low = s.substring(0, pos);
			if (low.length() != 0) {
				int val = Integer.parseInt(low);
				if (val < MIN_PORT) {
					val = MIN_PORT;
				} else if (val > MAX_PORT) {
					val = MAX_PORT;
				}
				start = val;
			}

    			String high = s.substring(pos + 1);
			if (high.length() != 0) {
				int val = Integer.parseInt(high);
				if (val < MIN_PORT) {
					val = MIN_PORT;
				} else if (val > MAX_PORT) {
					val = MAX_PORT;
				}
				end = val;
			}

			if (end < start) {
				end = start;
			}
    			//portStart = low.equals("*") ? 0 : Integer.parseInt(low);
    			//portEnd = high.equals("*") ? MAX_PORT : Integer.parseInt(high);
    		}
		portStart = start;
		portEnd = end;
    	}
    	
    	/**
    	 * check whether the specified port falls within the identified range
    	 */
    	public boolean match(int port) {
    		return (port >= this.portStart && port <= this.portEnd);
    	}

		/**
		 * convert this port spec to a string
		 */
		public String toString() {
			if (portStart == portEnd) {
				return new String(""+portStart);
			} else {
				return new String(""+portStart+"-"+portEnd);
			}
		}
    }
		
	/**
     * Helper class to parse the host spec:
     *   <host-spec> = (hostname | ipaddress ["/" prefix])
     */
    private static class Host {
    	// each host record
    	private final boolean anyHost;
    	private final byte[] addressAsBytes;
    	private final int prefixByteCount;
    	private final byte mask;
    	private final boolean loopbackOrLocal;
    	private final boolean ibLocal;
    	
    	/* list of various interface addresses available on the localhost
    	 * ibAddresses - list of InfiniBand addresses
    	 * ethAddresses - list of EtherNet addresses
    	 * othAddresses - list of Other addresses like loopback
    	 */
    	private static List<String> ibAddresses = new LinkedList<String>();
    	private static List<String> ethAddresses = new LinkedList<String>();
    	private static List<String> othAddresses = new LinkedList<String>();

    	/*
    	 * default return value for anyHost
    	 */
    	final static Host[] allHosts = new Host[] {new Host() {
    		public boolean match(SocketAction action, InetAddress address, boolean isRemote) {
    			return true;
    		}
    	}};
    	
    	/**
    	 * constructor for any host
    	 */
    	private Host() {
    		this.anyHost = true;
    		this.addressAsBytes = null;
    		this.prefixByteCount = 0;
    		this.mask = 0;
    		this.loopbackOrLocal = false;
    		this.ibLocal = false;
    	}
    	
    	/*
    	 * retrieve all the InfiniBand network interfaces on this host
    	 */
    	static {
            try {
                boolean preferIPv4Stack = Boolean.parseBoolean(java.security.AccessController.doPrivileged
                        (new GetPropertyAction("java.net.preferIPv4Stack")));

                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

                // walk through the network interfaces to obtain IP addresses			
                if (nis != null) {
                    while (nis.hasMoreElements()) {
                        NetworkInterface ni = nis.nextElement();
                        if (ni != null && ni.isUp()) {
                            boolean isIB = false;
                            boolean isEth = false;
            
                            // check whether it is IB or ETH interface
                            if (ni.getName().startsWith("ib")) {
                                isIB = true;
                            } else if (ni.getName().startsWith("eth")) {
                            	isEth = true;
                            }

                            Enumeration<InetAddress> inetAddrs = ni.getInetAddresses();
                            while (inetAddrs.hasMoreElements()) {
                                String addr = inetAddrs.nextElement().getHostAddress();

                                // skip IPv4 address portion if preferIPv6Address is true
                                if (InetAddress.preferIPv6Address && !preferIPv4Stack && addr.indexOf(":") < 0) {
                                        continue;
                                }

                                // skip IPv6 address portion if preferIPv6Address is not specified
                                if (!InetAddress.preferIPv6Address && addr.indexOf(":") >= 0) {
                                        continue;
                                }

                                if (isIB) {
                                        ibAddresses.add(addr);
                               	} else if (isEth) {
                                        ethAddresses.add(addr);
                               	} else {
                                        othAddresses.add(addr);
                                }
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                // Ignore...
            }
        }
    	
    	/**
    	 * check whether the IP address is loopback or local
    	 */
	   	static public boolean isLoopbackOrLocal(InetAddress addr) {
	   		// check for loopback or null address
			if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()) {
				return true;
			}
			// check for infiniband address
    		for (String address: ibAddresses) {
    			if (address.equals(addr.getHostAddress())) {
    				return true;
    			}
    		}
    		// check for ethernet address
    		for (String address: ethAddresses) {
    			if (address.equals(addr.getHostAddress())) {
    				return true;
    			}
    		}
    		// check for other addresses, if any
    		for (String address: othAddresses) {
    			if (address.equals(addr.getHostAddress())) {
    				return true;
    			}
    		}
    		return false;
    	}
	   	
	   	/**
	   	 * check whether the IP address is valid local IB address
	   	 */
	   	static public boolean isIBLocal(InetAddress addr) {
	   		for (String address: ibAddresses) {
	   			if (address.equals(addr.getHostAddress())) {
	   				return true;
	   			}
	   		}
	   		return false;
	   	}
       	
    	/**
    	 * constructor for a host with specific IP address and prefix
    	 */
    	private Host(InetAddress address, int prefix) {
    		this.anyHost = false;
    		this.addressAsBytes = address.getAddress();
    		this.prefixByteCount = prefix >> 3;
    		this.mask = (byte)(0xff << (8 - (prefix % 8)));
    		this.loopbackOrLocal = isLoopbackOrLocal(address);
    		this.ibLocal = isIBLocal(address);
    	}
    	
    	/**
    	 * check whether the specified IP address belongs to this host
    	 */
    	boolean match(SocketAction action, InetAddress address, boolean isRemote) {
    		// connections from any host can be accepted
    		if (action == SocketAction.ACCEPT && isRemote) {
    			if (anyHost) {
    				return true;
    			}
    		}
    		
			// retrieve the raw IP address
    		byte[] candidate = address.getAddress();
    		
    		// check address length
    		if (candidate.length != addressAsBytes.length) {
    			return false;
    		}
    		
    		// check address bytes
    		for (int i = 0; i < prefixByteCount; i++) {
    			if (candidate[i] != addressAsBytes[i]) {
    				return false;
    			}
    		}
    		
    		// check remaining bits
    		if ((prefixByteCount < addressAsBytes.length) &&
    				((candidate[prefixByteCount] & mask) != (addressAsBytes[prefixByteCount] & mask))) {
    			return false;
    		}
    		return true;
    	}
    	
    	/**
    	 * parse the specified host spec and return an arry of hosts
    	 */
    	static Host[] parse(SocketAction action, String hostString, boolean isRemote) {
    		// recognize the keywords: '*', 'any', and 'all' for accepting connection from any host
    		if (action == SocketAction.ACCEPT && isRemote) {
    			if (hostString.equals("*") || hostString.equals("any") || hostString.equals("all")) {
    				return allHosts;
    			}
    		}
    			
    		LinkedList<Host> hosts = new LinkedList<Host>();
    		int pos = hostString.indexOf('/');
    		try {
    			if (pos < 0) {
    				/* hostname or ipaddress (no prefix)
    				 * get all the ip addresses for the specified host
    				 */
    				InetAddress[] addresses = InetAddress.getAllByName(hostString);
    				for (InetAddress address: addresses) {
    					int prefix = (address instanceof Inet4Address) ? 32 : 128;
    					hosts.addLast(new Host(address, prefix));
    				}
    			} else {
    				/* ipaddress/prefix
    				 * get the ip address for the specified host
    				 */
    				InetAddress address = InetAddress.getByName(hostString.substring(0, pos));
    				int prefix = -1;
    				try {
    					prefix = Integer.parseInt(hostString.substring(pos + 1));
    					if (address instanceof Inet4Address) {
    						// must be 1-31
    						if (prefix < 0 || prefix > 32) prefix = -1;
    					} else {
    						// must be 1-128
    						if (prefix < 0 || prefix > 128) prefix = -1;
    					}
    				} catch (NumberFormatException e) {
    					// Ignore...
    				}
    				if (prefix > 0) {
    					hosts.addLast(new Host(address, prefix));
    				} else {
    					fail("Malformed prefix '%s'", hostString);
    					return null;
    				}
    			}
    		} catch (UnknownHostException uhe) {
    			fail("Unknown host or malformed IP address '%s'", hostString);
    			return null;
    		}
    		return hosts.toArray(new Host[hosts.size()]);
    	}
    	
    	/**
    	 * getter method for loopback flag
    	 */
    	public boolean getIsLoopBackOrLocal() {
    		return this.loopbackOrLocal;
    	}
    	
    	/**
    	 * getter method for ibLocal flag
    	 */
    	public boolean getIsIBLocal() {
    		return this.ibLocal;
    	}

		/**
		 * convert this host spec to a string
		 */
		public String toString() {
			String hostStr = null;
			try {
				if (anyHost) {
					hostStr = new String("*");
				} else {
					hostStr = new String(""
							+(InetAddress.getByAddress(addressAsBytes)).toString()
							+"/"+(prefixByteCount << 3));
				}
			} catch (Exception e) {
				// do nothing
			}
			return hostStr;
		}
    }
	
    /**
     * A rule for matching an accept/bind/connect request
     */
    private static interface Rule {
    	boolean match(SocketAction action, InetAddress localAddress, int localPort, InetAddress remoteAddress);
    	// getter methods for retrieving various rule related attributes
    	SocketAction getAction();
    	PortRange getPortRange();
    	Host getPrimaryHost();
    	Host getSecondaryHost();
    	NetworkProvider getNetworkProvider();
    }
	
    /**
     * Helper class to match host-port rule
     */
    private static class HostPortRule implements Rule {
    	private SocketAction action;
    	private PortRange range;
    	private Host host;
    	private Host remote;
    	private NetworkProvider provider;
    	
    	/**
    	 * constructor
      	 */
    	HostPortRule(SocketAction action, Host host, PortRange range, Host remote, NetworkProvider provider) {
    		this.action = action;
    		this.host = host;
    		this.range = range;
    		this.remote = remote;
    		this.provider = provider;
    	}
    	
    	/**
    	 * check whether rule attributes match that of specified ones
    	 */
    	public boolean match(SocketAction action, InetAddress localAddress, int localPort, InetAddress remoteAddr) {
    		if (!host.match(action, localAddress, false)) {
				if (!((action == SocketAction.ACCEPT || action == SocketAction.BIND) &&
						(preferredAddress != null) && (localAddress.equals(preferredAddress)))) {
    				return false;
				}
    		}

    		if (!range.match(localPort)) {
    			return false;
    		}
    	
    		// remoteAddr will be null in cases where the rule is for a bind/connect
			if (action == SocketAction.BIND || action == SocketAction.CONNECT) {
    			if (remoteAddr == null || remote == null) {
    				return true;
    			}
			}
    		return remote.match(action, remoteAddr, true);
    	}
    	
    	/**
    	 * getter method for socket action field
    	 */
    	public SocketAction getAction() {
    		return action;
    	}
    	
    	/**
    	 * getter method for port range field
    	 */
    	public PortRange getPortRange() {
    		return range;
    	}
    	
    	/**
    	 * getter method for primary host
    	 */
    	public Host getPrimaryHost() {
    		return host;
    	}
    	
    	/**
    	 * getter method for secondary host
    	 */
    	public Host getSecondaryHost() {
    		return remote;
    	}
    	
    	/**
    	 * getter method for network provider
    	 */
    	public NetworkProvider getNetworkProvider() {
    		return provider;
    	} 	

		/**
		 * convert this rule to a string
		 */
		public String toString() {
			return new String("action: "+SocketAction.toString(action)+" host: "+host+" port: "+range+" remote: "+remote);
		}
    }
    
    /**
     * Add rule to the appropriate list
     */
    private static void addRule(Rule rule) {
    	switch(rule.getAction()) {
    	case BIND:
    		bindRules.addLast(rule);
    		break;
    	case ACCEPT:
    		acceptRules.addLast(rule);
    		// for each accept rule also add a bind rule
    		bindRules.addLast(new HostPortRule(SocketAction.BIND, rule.getPrimaryHost(), rule.getPortRange(),
    				rule.getSecondaryHost(), rule.getNetworkProvider()));
    		break;
    	case CONNECT:
    		connectRules.addLast(rule);
    		break;
    	default:
    		// do nothing
    		break;
    	}
    }
      
    /**
     * Helper method to throw runtime exception in failing to parse
     * a rule field.
     */
    private static void fail(String msg, Object... args) {
    	Formatter f = new Formatter();
    	f.format(msg, args);
    	throw new RuntimeException(f.out().toString());
    }
    
    /**
     * Load rules from the specified configuration file.
     * 
     * Each non-blank or non-comment line must have the following format:
     *   <entry>         = <net-spec> <sp> <connect-entry> | <accept-entry> | <bind-entry>
     *   <net-spec>      = "rdma"
     *   <sp>            = 1*LWSP-char
     *   <connect-entry> = "connect" <host-spec> <sp> <port-spec>
     *   <host-spec>     = (hostname | ipaddress ["/" prefix])
     *   <port-spec>     = ("*" | port)[ "-" ("*" | port) ]
     *   <accept-entry>  = "accept" <host-spec> <sp> <port-spec> <sp> ("*"| "any" | "all" | <client-spec> [<sp> <client-spec>])
     *   <client-spec>   = (hostname | ipaddress ["/" prefix])
     *   <bind-entry>    = "bind" <host-spec> <sp> <port-spec>
     * Note:
     *   1) Comment lines should begin with '#' character.
     *   2) For connect entry, host and port specs should refer to the remote host.
     *   3) For accept entry, host and port specs should refer to the local host.
     *   4) For accept entry, all client specs should refer to remote hosts.
     *   5) For bind entry, host and port specs should refer to the local host.
     *   6) Each hostname or ipaddress specified should be a valid InfiniBand interface address.
     *   7) In case of bind, hostname or ipaddress could be either null or loopback.
     *   8) In case of accept, local hostname or ipaddress could be either null or loopback.
     *   9) Null or loopback addresses will be replaced by the preferred IB address during
     *      the actual bind.
     *  10) For null IP address use one of '0','0.0','0.0.0', or '0.0.0.0'.
     *  11) For loopback IP address use '127.0.0.1'.
     *  12) In case of connect, host and port spec should not be null or local.
     *  13) Server configuration file can have accept and bind entries.
     *  14) Each accept entry implicitly generates a bind entry as well.
     *  15) Client configuration file can have connect and bind entries.
     */
    private static void loadRulesFromFile(String file) throws IOException {
    	Scanner scanner = new Scanner(new File(file));
    	try {
    		while (scanner.hasNextLine()) {
    			String line = scanner.nextLine().trim();
    			
    			// skip blank lines and comments
    			if (line.length() == 0 || line.charAt(0) == '#') {
    				continue;
    			}
    			
    			// must have at least 4 fields
    			String[] s = line.split("\\s+");
    			if (s.length < 4) {
    				fail("Malformed line '%s'", line);
    				continue;
    			}
    			
    			// first field is the network identifier
    			NetworkProvider provider = loadProvider(s[0]);
    			if (provider == null) {
    				fail("Network provider for '%s' could not be loaded", s[0]);
    				continue;
    			}
    			
    			// second field is the action ("connect" or "accept" or "bind")
    			SocketAction action = SocketAction.parse(s[1]);
    			if (action == null) {
    				fail("SocketAction '%s' not recognized", s[1]);
    				continue;
    			}
    			
    			if (action == SocketAction.ACCEPT && s.length < 5) {
    				fail("Less than expected number of arguments '%s'", line);
    				continue;
    			}

				if ((action == SocketAction.BIND || action == SocketAction.CONNECT) && s.length > 4) {
					fail("Greater than expected number of arguments '%s'", line);
					continue;
				}
    			
    			// third field is the host spec
    			Host[] primaryHosts = Host.parse(action, s[2], false);
    			if (primaryHosts == null) {
    				continue;
    			}
    			
    			// fourth field is the port spec
    			PortRange range = null;
    			try {
    				range = new PortRange(s[3]);
    			} catch (NumberFormatException nfe) {
    				fail("Malformed port range '%s'", s[3]);
    				continue;
    			}
    			   			
    			/* fifth and above fields (if exist) contain client addresses
    			 * create one rule for each host-client combination
    			 */
    			if (action == SocketAction.ACCEPT) {
    				for (int i = 4; i < s.length; i++) {
    					Host[] remoteHost = Host.parse(action, s[i], true);
    					if (remoteHost != null) {
    						for (int p = 0; p < primaryHosts.length; p++) {
    							for (int j = 0; j < remoteHost.length; j++) {
    								// remote host portion should not be local for an accept call
    								if (remoteHost[j].getIsLoopBackOrLocal()) {
    									continue;
    								}
    								addRule(new HostPortRule(action, primaryHosts[p], range, remoteHost[j], provider));
    							}
    						}
    					}
    				}
    			} else {
    				// client address portion is null
    				for (int p = 0; p < primaryHosts.length; p++) {
    					// primary host portion should not be local for a connect operation
    					if ((action != SocketAction.BIND) && primaryHosts[p].getIsLoopBackOrLocal()) {
    						continue;
    					}
    					addRule(new HostPortRule(action, primaryHosts[p], range, null, provider));
    				}
    			}
    		}
    	} finally {
    		scanner.close();
    	}
    }
    
    /**
	 * Initialize the protocol usage rules
	 */
	static boolean initializeRules() {
		// flag indicating whether rules are loaded from the configuration file
		boolean rulesLoaded = false;
		// retrieve the configuration file
		String confFile = AccessController.doPrivileged(new GetPropertyAction("com.ibm.net.rdma.conf"));
		if (confFile != null) {
			bindRules = new LinkedList<Rule>();
			connectRules = new LinkedList<Rule>();
			acceptRules = new LinkedList<Rule>();
			providerMap = new HashMap<String, NetworkProvider>();
			try {
				loadRulesFromFile(confFile);
				rulesLoaded = true;
			} catch (IOException e) {
				// Ignore...
			}
			if (rulesLoaded) {
				// Register a shutdown hook to cleanup the network resources
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						Iterator<NetworkProvider> it = providerMap.values().iterator();
						while (it.hasNext()) {
							NetworkProvider networkProvider = it.next();
							networkProvider.cleanup();
						}
					}
				});
			} else {
				bindRules = null;
				connectRules = null;
				acceptRules = null;
				providerMap = null;
			}
		}
		return rulesLoaded;
	}
	
	/*
	 *  this implementation currently supported on linux-x86_64 platform only
	 */
	static {
		// flag indicating whether rules are initialized
		boolean rulesInited = false;

		GetPropertyAction propAct = new GetPropertyAction("os.name");
		osName = AccessController.doPrivileged(propAct);
		propAct = new GetPropertyAction("os.arch");
		osArch = AccessController.doPrivileged(propAct);

		if (osName.equalsIgnoreCase("linux") && (osArch.equalsIgnoreCase("amd64") || osArch.equalsIgnoreCase("x86")
                    || osArch.equalsIgnoreCase("ppc") || osArch.equalsIgnoreCase("ppc64"))) {
			rulesInited = initializeRules();
		}
		enabled = rulesInited;
		// uncomment the following block for rules dump
		/*
		if (rulesInited) {
			System.out.println(""+printRules());
		}*/
	}

	/**
	 * Find the first rule that matches the specified attributes in a rule set
	 */
	private static Rule findFirstMatchingRule(List<Rule> rules, SocketAction action, InetAddress primaryHost,
			int port, InetAddress secondaryHost) {
		Iterator<Rule> it = rules.iterator();
		while (it.hasNext()) {
			Rule rule = it.next();
			if (rule.match(action, primaryHost, port, secondaryHost)) {
				return rule;
			}
		}
		return null;
	}
	    
	/**
	 * Retrieve the underlying network provider
	 */
	public static NetworkProvider getNetworkProvider(SocketAction action,
			InetAddress primaryHost, int port, InetAddress secondaryHost) {
		Rule matchedRule = null;
		if (!enabled) {
			return null;
		}
		switch (action) {
		case BIND:
			matchedRule = findFirstMatchingRule(bindRules, action, primaryHost, port, secondaryHost);
			break;
		case ACCEPT:
			// loopback or local connection for secondar host, fall back to TCP/IP
			if (secondaryHost == null || Host.isLoopbackOrLocal(secondaryHost)) {
				return null;
 			}
			matchedRule = findFirstMatchingRule(acceptRules, action, primaryHost, port, secondaryHost);
			break;
		case CONNECT:
			// loopback or local connection for primary host, fall back to TCP/IP
			if (primaryHost == null || Host.isLoopbackOrLocal(primaryHost)) {
				return null;
			}
			matchedRule = findFirstMatchingRule(connectRules, action, primaryHost, port, secondaryHost);
			break;
		default:
			break;
		}
		if (matchedRule != null) {
			return matchedRule.getNetworkProvider();
		}
		return null;
	}
	
    /**
     * Load the specified network provider - currently,
     * we only check RDMA network provider
     */
    private static NetworkProvider loadProvider(String name) {
        NetworkProvider provider = null;
        if (providerMap.containsKey(name)) {
            provider = providerMap.get(name);
    	} else if (name.equalsIgnoreCase("rdma")) {
            try {
            	// Let's rely on the bootstrap class loader to locate and load the RDMANetworkProvider class
            	Class netProvider = Class.forName("java.net.RDMANetworkProvider", true, null);
                provider = (NetworkProvider)netProvider.newInstance();
                Method intializeProvider = netProvider.getMethod("initialize", (Class[])null);
                intializeProvider.invoke(provider, (Object[])null);
				// Invoke the network provider's set preferred address routine
				provider.setPreferredAddress(Host.ibAddresses, Host.ethAddresses);
				preferredAddress = provider.getPreferredAddress();
            } catch (Exception e) {
                return null;
            }
            providerMap.put(name, provider);
    	}
    	return provider;
    }
    
    /**
     * dump the rules book and associated details
     */
    public static String printRules() {
        String result = null;
        try (StringWriter sw = new StringWriter()) {
            try (PrintWriter pw = new PrintWriter(sw)) {
                pw.println("<Rules>");	
	
                // check for bind rules
                for (Rule rule: bindRules) {
                    pw.println(rule);
	        	}

	        	// check for connect rules
                for (Rule rule: connectRules) {
                    pw.println(rule);
	        	}
                
	        	// check for accept rules
                for (Rule rule: acceptRules) {
                    pw.println(rule);
	        	}
	
	        	// localhost interfaces
	        	pw.println();
	        	pw.println("<Host Addresses>");
	        	pw.println("InfiniBand: " + Host.ibAddresses);
        		pw.println("Ethernet: " + Host.ethAddresses);
        		pw.println("Other: " + Host.othAddresses);

        		// preferred address
        		pw.println();
        		pw.print("Preferred Address: ");
        		if (preferredAddress != null) {
                    pw.println(preferredAddress);
	        	} else {
	            	pw.println("null");
	        	}
            } finally {
                 result = sw.toString();
            }
        } catch (IOException e) {
            // Ignore exception during implicit close of sw
        }
        return result; 
    }
}
