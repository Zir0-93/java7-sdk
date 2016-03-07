/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 2005, 2012  All Rights Reserved.
 */

package java.lang.management;

/**
 * @author gharley
 * @since 1.5
 */
public enum MemoryType {

    /**
     * Memory on the heap. The heap is the runtime area in the virtual machine,
     * created upon the start-up of the virtual machine, from which memory for
     * instances of types and arrays is allocated. The heap is shared among all
     * threads in the virtual machine.
     */
    HEAP,
    /**
     * Memory that is not on the heap. This encompasses all other storage used
     * by the virtual machine at runtime.
     */
    NON_HEAP;
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String result = null;
        switch (this) {
        case HEAP:
            result = "Heap memory";
            break;
        case NON_HEAP:
            result = "Non-heap memory";
            break;
        }
        return result;
    }
}

/*
 * $Log$
 * Revision 1.2  2005/02/11 17:26:41  gharley
 * Added in a toString() method
 *
 * Revision 1.1  2005/01/11 10:56:10  gharley
 * Initial upload
 *
 * Revision 1.1  2005/01/07 10:05:53  gharley
 * Initial creation
 *
 */
