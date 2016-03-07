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
 * ===========================================================================
 (C) Copyright Sun Microsystems Inc, 1992, 2004. All rights reserved.
 * ===========================================================================
 */

/*
 * ===========================================================================
 * Module Information:
 *
 * DESCRIPTION: Win32 specific file canonical name comparison, optimized to
 *              minimize calls to getCanonicalPath.
 * ===========================================================================
 */

package java.io;

import java.security.AccessController;
import sun.security.action.GetPropertyAction;

import java.util.Hashtable;

final class Win32CanonicalPath extends CanonicalPath {

    private String workingPath = null;

    static Win32FileSystem fs =
        (Win32FileSystem)(FileSystem.getFileSystem());
    static Hashtable baseNames = new Hashtable();

    public Win32CanonicalPath(String path, boolean directory) {

        super(path,directory,false);
        // To satisfy the compiler.
        final boolean temp_dir_flag = directory;
        // need a doPrivileged block as getAbsolutePath
        // might attempt to access user.dir to turn a relative
        // path into an absolute path.

        workingPath = path;

        if (workingPath.equals("")) {
            workingPath = (String)
                java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction("user.dir"));
        }

        workingPath = (String)
        AccessController.doPrivileged(
            new java.security.PrivilegedAction(){
            public Object run(){
            File file = new File(workingPath);
            String absolute_path = file.getAbsolutePath();
            if (temp_dir_flag &&
            (!absolute_path.endsWith(File.separator))){
            return absolute_path + File.separator;}else{
            return absolute_path;}}});

        try {
            workingPath = fs.collapsePath(workingPath);
        }
        catch (IOException ioe) {
            // Following the example of FilePermission, we ignore
            // difficulty in handling the path name.
        }
    }


    Win32CanonicalPathComponentRetriever getComponentRetriever() {
        return new Win32CanonicalPathComponentRetriever(workingPath);
    }


    boolean equals(CanonicalPath that) {
        // Get the first canonical component of both paths.
        Win32CanonicalPathComponentRetriever thisComponentRetriever =
            getComponentRetriever();
        Win32CanonicalPathComponentRetriever thatComponentRetriever =
            ((Win32CanonicalPath) that).getComponentRetriever();
        String this_component = thisComponentRetriever.firstComponent();
        String that_component = thatComponentRetriever.firstComponent();
        // While both current components are non-null.
        while ((this_component != null) && (that_component != null)) {
            if (!this_component.equals(that_component)) {
                return false;          // Mismatch in component means unequal.
            }
            this_component = thisComponentRetriever.nextComponent();
            that_component = thatComponentRetriever.nextComponent();
        }

        // At least one component is null.  Must be both for equality.
        return ((this_component == null) && (that_component == null));
    }


    boolean hasBaseDir(CanonicalPath that) {
        // Get the first canonical component of both paths.
        Win32CanonicalPathComponentRetriever thisComponentRetriever =
            getComponentRetriever();
        Win32CanonicalPathComponentRetriever thatComponentRetriever =
            ((Win32CanonicalPath) that).getComponentRetriever();
        String this_component = thisComponentRetriever.firstComponent();
        String that_component = thatComponentRetriever.firstComponent();

        // While both current components are non-null.
        while ((this_component != null) && (that_component != null)) {
            if (!this_component.equals(that_component)) {
                return false;          // Mismatch in component means unequal.
            }
            this_component = thisComponentRetriever.nextComponent();
            that_component = thatComponentRetriever.nextComponent();
        }

        // At least one component is null.  If it's "that" we're happy
        //  so long as "this" has exactly one more path component.
        //  This is indicated by the fact that the next separator is
        //  beyond the end of the string (i.e., nonexistent).
	if((that_component == null) && (this_component == null)) {              //IBM-net_perf
		return((thisComponentRetriever.currentPosition == thisComponentRetriever.pathLength) && (thatComponentRetriever.currentPosition == thatComponentRetriever.pathLength)); //IBM-net_perf
	} else                                                                  //IBM-net_perf
        	return ((that_component == null) &&                             //IBM-net_perf
                (thisComponentRetriever.currentPosition == thisComponentRetriever.pathLength));
    }

    boolean startsWith(CanonicalPath that) {
        // Get the first canonical component of both paths.
        Win32CanonicalPathComponentRetriever thisComponentRetriever =
            getComponentRetriever();
        Win32CanonicalPathComponentRetriever thatComponentRetriever =
            ((Win32CanonicalPath) that).getComponentRetriever();
	String this_component = thisComponentRetriever.firstComponent();
        String that_component = thatComponentRetriever.firstComponent();

        // While both current components are non-null.
        while ((this_component != null) && (that_component != null)) {
            if (!this_component.equals(that_component)) {
                return false;          // Mismatch in component means unequal.
            }
            this_component = thisComponentRetriever.nextComponent();
            that_component = thatComponentRetriever.nextComponent();
        }

        // At least one component is null.  If it's "that" we're happy
        //  since we don't care whether "this" is equal or longer.
        return(that_component == null);
    }

    boolean startsWithAndLonger(CanonicalPath that) {
        // Get the first canonical component of both paths.
        Win32CanonicalPathComponentRetriever thisComponentRetriever =
            getComponentRetriever();
        Win32CanonicalPathComponentRetriever thatComponentRetriever =
        ((Win32CanonicalPath) that).getComponentRetriever();
        String this_component = thisComponentRetriever.firstComponent();
        String that_component = thatComponentRetriever.firstComponent();

        // While both current components are non-null.
        while ((this_component != null) && (that_component != null)) {
            if (!this_component.equals(that_component)) {
                return false;          // Mismatch in component means unequal.
            }
            this_component = thisComponentRetriever.nextComponent();
            that_component = thatComponentRetriever.nextComponent();
        }

        // At least one component is null.  If it's "that" we're happy
        //  so long as "this" has some remaining components.
        return((that_component == null) && (this_component != null));
    }
}



// Container for canonical translation and hashtable for names lying under
//   this one in the directory tree.
final class Win32CanonicalPathNode {

    String canonicalName;
    Hashtable nextLevel;

    Win32CanonicalPathNode(String canonicalName,
                           Hashtable nextLevel) {
        this.canonicalName = canonicalName;
        this.nextLevel = nextLevel;
    }
}



final class Win32CanonicalPathComponentRetriever {
    private String path = null;
    int pathLength = 0;
    int currentPosition = 0;
    private Win32CanonicalPathNode currentNode = null;

    private static char separator = File.separatorChar;

    Win32CanonicalPathComponentRetriever(String path) {
        this.path = path;
        pathLength = path.length();
    }

    String firstComponent() {
        // Valid names start one of two ways:
        //    D:\ or \\machine\share\
        // and nothing else should be possible at this point
        // So now find the first real separator.
        currentPosition = path.indexOf(separator);
        if (currentPosition == 0) {
            currentPosition = path.indexOf(separator,2);
        }
        if (currentPosition > 0) {
            String key = path.substring(0,currentPosition);
            // Is the base of the path in the cache?
            Win32CanonicalPathNode baseNode =
                (Win32CanonicalPathNode) Win32CanonicalPath.baseNames.get(key);
            if (baseNode == null) {
                // Not in cache, so get it the hard way and put it there.
                try {
                    String basename = Win32CanonicalPath.fs.getBaseName(path);
                    // See if we already have the canonical name.  If so, fetch the 
                    //   appropriate node.  If not, create one.
                    Win32CanonicalPathNode canonicalBaseNode =
                        (Win32CanonicalPathNode) Win32CanonicalPath.baseNames.get(basename);
                    if (canonicalBaseNode == null) {
                        Hashtable baseHashtable = new Hashtable();
                        canonicalBaseNode =
                            new Win32CanonicalPathNode(basename,
                            baseHashtable);
                        Win32CanonicalPath.baseNames.put(basename,canonicalBaseNode);
                    }
                    // Finally we can create the hash table entry.
                    Win32CanonicalPath.baseNames.put(key,canonicalBaseNode);
                    baseNode = canonicalBaseNode;
                }
                catch (IOException ioe) {
                    // Following the example of FilePermission, we ignore
                    // difficulty in handling the path name.
                }
            }
            currentNode = baseNode;
            return baseNode.canonicalName;
        }
        // Should never get here.  Implies that _fullpath has produced a name
        //   that's out of spec.
        return null;
    }

    String nextComponent() {
        // indexOf is nice enough to allow out-of-range parameters,
        //   so we don't have to check.
        int nextPosition = path.indexOf(separator,currentPosition + 1);
        if ((nextPosition >= 0) ||
            ((currentPosition + 1) < pathLength)) {    
            if (nextPosition < 0) {
                nextPosition = pathLength;
            }
            String key = path.substring(currentPosition + 1,
                nextPosition);
            // Is this component of the path in the cache?
            Win32CanonicalPathNode nextNode =
                (Win32CanonicalPathNode) (currentNode.nextLevel).get(key);
            if (nextNode == null) {
                // Not in cache, so get it the hard way and put it there.
                try {
                    String nextname = Win32CanonicalPath.fs.getComponentName(
                        path.substring(0,nextPosition));
                    // See if we already have the canonical name.  If so, fetch the
                    //   appropriate node.  If not, create one.
                    Win32CanonicalPathNode canonicalComponentNode =
                        (Win32CanonicalPathNode)
                        currentNode.nextLevel.get(nextname);
                    if (canonicalComponentNode == null) {
                        Hashtable componentHashtable = new Hashtable();
                        canonicalComponentNode =
                            new Win32CanonicalPathNode(nextname,
                            componentHashtable);
                        currentNode.nextLevel.put(nextname,
                            canonicalComponentNode);
                    }
                    // Finally we can create the hash table entry.
                    currentNode.nextLevel.put(key,canonicalComponentNode);
                    nextNode = canonicalComponentNode;
                }
                catch (IOException ioe) {
                    // Following the example of FilePermission, we ignore
                    // difficulty in handling the path name.
					// Unfortunately the strategy fails if a bad volume name is supplied ibm@120874
                    return null;   
                }
            }
            currentNode = nextNode;
            currentPosition = nextPosition;
            return nextNode.canonicalName;
        }
        // A null return means we've run out of path name components.
        return null;
    }
}
//IBM-net_perf
