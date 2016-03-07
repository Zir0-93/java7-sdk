/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package com.ibm.lang.management;

import java.lang.management.PlatformLoggingMXBean;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingMXBean;

/**
 * Runtime type for {@link java.util.logging.LoggingMXBean}.
 * 
 * @author gharley
 * @since 1.5
 */
public class LoggingMXBeanImpl extends DynamicMXBeanImpl implements
        LoggingMXBean, PlatformLoggingMXBean {

    private static LoggingMXBeanImpl instance = new LoggingMXBeanImpl();

    /**
     * Constructor intentionally private to prevent instantiation by others.
     * Sets the metadata for this bean.
     */
    LoggingMXBeanImpl() {
        super(ManagementUtils.createObjectName(LogManager.LOGGING_MXBEAN_NAME));
    }
    
    public javax.management.MBeanInfo getMBeanInfo() {
        if (info == null) {
            setMBeanInfo(ManagementUtils
                .getMBeanInfo(LoggingMXBean.class.getName()));
        }
        return info;
    }

    /**
     * Singleton accessor method.
     * 
     * @return the <code>LoggingMXBeanImpl</code> singleton.
     */
    static LoggingMXBeanImpl getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#getLoggerLevel(java.lang.String)
     */
    public String getLoggerLevel(String loggerName) {
        String result = null;

        Logger logger = LogManager.getLogManager().getLogger(loggerName);
        if (logger != null) {
            // The named Logger exists. Now attempt to obtain its log level.
            Level level = logger.getLevel();
            if (level != null) {
                result = level.getName();
            } else {
                // A null return from getLevel() means that the Logger
                // is inheriting its log level from an ancestor. Return an
                // empty string to the caller.
                result = "";
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#getLoggerNames()
     */
    public List<String> getLoggerNames() {
        // By default, return an empty list to caller
        List<String> result = new ArrayList<String>();

        Enumeration<String> enumeration = LogManager.getLogManager()
                .getLoggerNames();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                result.add(enumeration.nextElement());
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#getParentLoggerName(java.lang.String)
     */
    public String getParentLoggerName(String loggerName) {
        String result = null;

        Logger logger = LogManager.getLogManager().getLogger(loggerName);
        if (logger != null) {
            // The named Logger exists. Now attempt to obtain its parent.
            Logger parent = logger.getParent();
            if (parent != null) {
                // There is a parent
                result = parent.getName();
            } else {
                // logger must be the root Logger
                result = "";
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.logging.LoggingMXBean#setLoggerLevel(java.lang.String,
     *      java.lang.String)
     */
    public void setLoggerLevel(String loggerName, String levelName) {
        final Logger logger = LogManager.getLogManager().getLogger(loggerName);
        if (logger != null) {
            // The named Logger exists. Now attempt to set its level. The
            // below attempt to parse a Level from the supplied levelName
            // will throw an IllegalArgumentException if levelName is not
            // a valid level name.
            Level newLevel = Level.parse(levelName);
            logger.setLevel(newLevel);
        } else {
            // Named Logger does not exist.
            throw new IllegalArgumentException(
                    "Unable to find Logger with name " + loggerName);
        }
    }
}

/*
 * $Log$
 * Revision 1.8  2005/07/07 15:44:35  gharley
 * Remove unnecessary PrivilegedAction blocks
 *
 * Revision 1.7  2005/06/21 09:24:30  gharley
 * Add in security "doPrivileged" code to get set new level on logger.
 *
 * Revision 1.6  2005/02/10 12:15:08  gharley
 * Moved invoke method into superclass
 *
 * Revision 1.5  2005/02/09 22:23:55  gharley
 * Moved getAttribute into superclass
 *
 * Revision 1.4  2005/02/04 23:13:55  gharley
 * Added in security permission code : either explicitly or added a comment
 * where an invoked method carries out the check on our behalf.
 *
 * Revision 1.3  2005/02/02 14:09:58  gharley
 * Moved MBeanInfo setup into ManagementUtils on the assumption that the
 * metadata is going to be useful for the proxy bean support.
 *
 * Revision 1.2  2005/01/25 15:43:46  gharley
 * First pass at implementation. Will return to this to reduce the code size later.
 * Revision 1.1 2005/01/24 22:09:56 gharley
 * Initial version. No implementation yet.
 * 
 */
