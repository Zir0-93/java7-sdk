package java.io;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates a ClassCache which is used for Caching the results of the className
 * lookups and hence to avoid multiple Lookup for same Class Instance.
 * ClassCache provides a ConcurrentHash based ClassCache which is looked up
 * prior to calling the class.forName.
 *  <p>
 *Method resolveClass() from @code ObjectInputStream uses this Cache.Caching is
 * done only when the actually used loader is one of the Sytem loaders.
 * (ie)App Class Loader, System Extension loader and BootStrap loader
 *
 *
 */
final class ClassCache {
/* Main Cache for storing the @code Class.forName results  */
    private final ConcurrentHashMap<Key,Object> cache =
        new ConcurrentHashMap<Key,Object>();
    /* Initiating Loader to Cache Entry Mapping */
    private final ConcurrentHashMap<LoaderRef,CacheKey> loaderKeys =
        new ConcurrentHashMap<LoaderRef,CacheKey>();
    /* Initiating loader to actual loader mapping */
    private final ConcurrentHashMap<LoaderRef,LoaderRef> canonicalLoaderRefs =
        new ConcurrentHashMap<LoaderRef,LoaderRef>();
    /* Reference Queue registered for notification on stale Loaders */
    private final ReferenceQueue<Object> staleLoaderRefs =
        new ReferenceQueue<Object>();
    /**
     * Populates Canonical Loader Refs with System Loader Entries, initializes
     * the reaper thread which monitors the ReferenceQueue for stale loaders
     * <p>
     * @param None
     *
     */

    public ClassCache() {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        while (loader != null) {
            setCanonicalSystemLoaderRef(loader);
            loader = loader.getParent();
        }
        setCanonicalSystemLoaderRef(null);
        AccessController.doPrivileged(
                new CreateReaperAction(this, staleLoaderRefs)).start();
    }
    /**
     * CanonicalLoaderRefs is updated for the input loader, this is a mapping
     * to Initiating loader and the actual loader used to load
     * @param Loader
     * @returns none
     *
     */

    private void setCanonicalSystemLoaderRef(ClassLoader loader) {
        LoaderRef newKey = new LoaderRef(loader, staleLoaderRefs, true);
        assert (canonicalLoaderRefs.put(newKey, newKey) == null);
    }

    /**
     * get Canonical Loader reference for the input loader object. This can be
     * called using the actual loader or inputLoader.
     * <P>
     * @param loaderobj Loader
     * @returns key of the Actual loader used to load the object
     *
     */
    LoaderRef getCanonicalLoaderRef(Object loaderObj) {
        LoaderRef newKey = new LoaderRef(loaderObj, staleLoaderRefs);

        LoaderRef key = canonicalLoaderRefs.get(newKey);
        if (key == null) {
            key = canonicalLoaderRefs.putIfAbsent(newKey, newKey);
            if (key == null) {
                return newKey;
            }
        }

        newKey.clear();
        return key;
    }
    /**
     * This is called from reaperthread to remove the stale reference of the
     * loader in the loaderKeys and corresponding entry in the ClassCache cache
     * @Param loaderref this is stale loader obtained from Reaper thread
     * @returns None
     *
     */
    void removeStaleRef(LoaderRef loaderRef) {
        canonicalLoaderRefs.remove(loaderRef);
        CacheKey key = loaderKeys.remove(loaderRef);
        while (key != null) {
            cache.remove(key);
            key = key.next;
        }
    }

    /**
     * updates the cache and the LoaderKey if the actual loader used is
     *one of the system loaders.
     * <P>
     * @param Key - CacheKey which needs to be added to the cache
     * @param result - Class object which is the value of the cache
     *
     *
     */
    void update(CacheKey key, Class<?> result) {
        Object resultLoaderObj =
            LoaderRef.getLoaderObj(result.getClassLoader());
        if (getCanonicalLoaderRef(resultLoaderObj).isSystem == false) {
            return;
        }

        Object oldValue = cache.replace(key, result);
        assert (oldValue instanceof FutureValue) :
            ("Value replaced is of type '" + oldValue.getClass().getName() +
             "', not of type '" + FutureValue.class.getName() + "'.");

        LoaderRef loaderRef = key.loaderRef;
        if (loaderRef.isSystem == false) {
            key.next = loaderKeys.get(loaderRef);
            if (key.next == null) {
                key.next = loaderKeys.putIfAbsent(loaderRef, key);
                if (key.next == null) return;
            }
            while (!loaderKeys.replace(loaderRef, key.next, key)) {
                key.next = loaderKeys.get(loaderRef);
            }
        }
    }
    /**
     * Adds a New Entry in the Cache
     * @param key Input key which needs to be Cache
     * @return  Value of the Cache, after lookup or add.
     */
    private Object createEntry(CacheKey key) {

        FutureValue newValue = new FutureValue(key, this);
        Object value = cache.putIfAbsent(key, newValue);
        if (value == null) value = newValue;
        return value;
    }
    /**
     * This is the entry point in to the cache from @code ObjectInputStream.
     * First Lookup is done in the cache based on the className and Loader.
     * Cache and loaderkey are updated if it is the first time a
     * (className,loader) pair is called.
     * <p>
     *   @param className Name of the class that is being looked up
     *   @loader initiating loader
     *   @returns the Class object which is either loaded using class.forName
     *   or looked up from the cache.
     * @throws ClassNotFoundException
     *   
     */
    public Class<?> get(String className, ClassLoader loader)
        throws ClassNotFoundException {
        LookupKey key = new LookupKey(className, loader, this);
        Object value = cache.get(key);
        if (value == null) {
        	value = createEntry(key.createCacheKey());
        }

        if (value instanceof FutureValue) {
        	
            return ((FutureValue)value).get();
        }

        return (Class<?>)value;
    }
   
    /**
     * FutureValue implements Future Mechanics that is required for
     *  addressing  contention issues in the HashMap. 
     *  
     *  <pre>
     *  @code get() - gets the value from the cache, or initiates 
     * @code class.forName
     *  
     */
    private static final class FutureValue {
        private final CacheKey key;
        private final LoaderRef loaderRef;
        private final ClassCache cache;
        private Class<?> value = null;

        FutureValue(CacheKey key, ClassCache cache) {
            this.key = key;
            this.loaderRef = key.loaderRef;
            this.cache = cache;
        }

        /**
         * tries to get the value from Cache, if not available tries to 
         * get class.forName with active loader and replace the entry in 
         * the cache as required
         * 
         */
        Class<?> get() throws ClassNotFoundException {
            synchronized (this) {
                if (value != null) return value;
                value = Class.forName(key.className, false,
                        loaderRef.getActiveLoader());
            }
            if (value != null) {
                cache.update(key, value);
            }

            return value;
        }
    }

    private static final class CreateReaperAction
            implements PrivilegedAction<Thread> {
        private final ClassCache cache;
        private final ReferenceQueue<Object> queue;

        CreateReaperAction(ClassCache cache, ReferenceQueue<Object> queue) {
            this.cache = cache;
            this.queue = queue;
        }

        public Thread run() {
            return new Reaper(cache, queue);
        }
    }

    /**
     * Background thread that blocks on remove() on the ReferenceQueue and calls
     * processStaleRef() when any loader is removed
     */
    private static final class Reaper extends Thread {
        private final WeakReference<ClassCache> cacheRef;
        private final ReferenceQueue<Object> queue;

        Reaper(ClassCache cache, ReferenceQueue<Object> queue) {
            super("ClassCache Reaper");
            this.queue = queue;
            cacheRef = new WeakReference<ClassCache>(cache, queue);
            setDaemon(true);
            setContextClassLoader(null);
        }
        /**
         * @see java.lang.Thread#run()
         */
        public void run() {
            Object staleRef = null;
            do {
                try {
                    staleRef = queue.remove();
                    if (staleRef == cacheRef) break;

                    processStaleRef((LoaderRef)staleRef);
                } catch (InterruptedException e) { }
            } while (true);
        }
        
        private void processStaleRef(LoaderRef staleRef) {
            ClassCache cache = cacheRef.get();
            if (cache == null) return;

            cache.removeStaleRef(staleRef);
        }
    }
   
    /**
     * The use of the loaderRefs map is to  allow efficient processing of one
     * WeakReference for each stale ClassLoader, rather than one WR for each
     * entry in the cache. CacheKey as well as Lookup Key will be refering
     * to this LoaderRef.
     * <P>
     * Initiating Class Loaders needs to be referred for both lookup and 
     * Caching,so for performance reasons a LoaderRef is maintained which
     * would be used by both LookupKey and CachingKey(ie) LookupKey will 
     * actually store the LoaderObj, but it will be canonically refered 
     * via a loaderRed by the caching Key.
     * <P>
     * LoaderKey has LoaderRef Objects as well and is used to link the
     * Initiating Loader with the actual cache Entries which is used to 
     * remove Stale reference entries.
     */
    private static class LoaderRef extends WeakReference<Object> {
        private static final String NULL_LOADER = new String("");
        private final int hashcode;
        public final boolean isSystem;

        static Object getLoaderObj(ClassLoader loader) {
            return ((loader == null) ? NULL_LOADER : loader);
        }

        LoaderRef(Object loaderObj, ReferenceQueue<Object> queue) {
            this(false, Objects.requireNonNull(loaderObj), queue);
        }

        LoaderRef(ClassLoader loader, ReferenceQueue<Object> queue,
                boolean isSystem) {
            this(isSystem, getLoaderObj(loader), queue);
        }
        
        /**
         * Creates a new weak reference that refers to the given object and 
         * is registered with the given queue.
         * <P>
         * @param isSystem - false if the loaderobj is not a system loader
         * @param loaderObj - loaderobj  used in Cachekey and LookupKey
         * @param queue - ReferenceQueue to which the loader is registered.
         */
        private LoaderRef(boolean isSystem, Object loaderObj,
                ReferenceQueue<Object> queue) {
            super(loaderObj, queue);
            String loaderClassName = ((loaderObj == NULL_LOADER) ?
                    NULL_LOADER : loaderObj.getClass().getName());
            hashcode = (loaderClassName.hashCode() +
                    System.identityHashCode(loaderObj));
            this.isSystem = isSystem;
        }

        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LoaderRef)) return false;

            Object loader = get();
            return ((loader != null) && (loader == ((LoaderRef)o).get()));
        }

        public final int hashCode() {
            return hashcode;
        }

        ClassLoader getActiveLoader() {
            Object loaderObj = Objects.requireNonNull(get());
            return ((loaderObj == NULL_LOADER) ? null : (ClassLoader)loaderObj);
        }
    }

    /**
     * For better clarity and to avoid multiple lookups to the cache. Key is 
     * implemented to haveone abstract key to final sub classes which serve
     * specific purpose.
     * <P>
     * LookupKey -This is a short lived key, not part of any hashmap and stores
     * the strong reference to loaderobject
     * CachingKey - uses the same hash as LookupKey,can be creates using
     * LookupKey.createCacheKey()and has Weakreference to the Loaderobj 
     * 
     */
    private static abstract class Key {
        public final String className;
        protected final int hashcode;

        protected Key(String className, int hashcode) {
            this.className = className;
            this.hashcode = hashcode;
        }

        abstract Object getLoaderObj();

        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key k = (Key)o;
            Object loaderObj = getLoaderObj();
            return (className.equals(k.className) &&
                    (loaderObj != null) && (loaderObj == k.getLoaderObj()));
        }
        
        public final int hashCode() {
            return hashcode;
        }
    }
    /**
     * Lookup Key hash code is framed using loadername hash + loader's system 
     * identity hashcode.  This is same as the hashcode maintained in CacheKey
     */

    private static final class LookupKey extends Key {
        private final Object loaderObj;
        private final ClassCache cache;
       
       /**
        * computes the cache using className hash and identityhashcode of
		* the loader
        * @param className
        * @param loader
        * @returns hashcode for the Lookup key
        */
        private static int hashCode(String className, ClassLoader loader) {
            int hashcode = className.hashCode();
            if (loader != null) {
                hashcode += (loader.getClass().getName().hashCode() +
                        System.identityHashCode(loader));
            }
            return hashcode;
        }

        public LookupKey(String className, ClassLoader loader,
                ClassCache cache) {
            super(Objects.requireNonNull(className),
                    hashCode(className, loader));
            loaderObj = LoaderRef.getLoaderObj(loader);
            this.cache = cache;
        }

        Object getLoaderObj() {
            return loaderObj;
        }

        CacheKey createCacheKey() {
            return new CacheKey(className, hashcode,
                    cache.getCanonicalLoaderRef(loaderObj));
        }
    }

    /**
     * CacheKey is the actual key that is stored in the cache, and it stores
     * the weakreference of the Initiating loader object via loaderRef
     * 
     */
    private static final class CacheKey extends Key {
        public final LoaderRef loaderRef;
        public CacheKey next = null;

        CacheKey(String className, int hashcode, LoaderRef loaderRef) {
            super(className, hashcode);
            this.loaderRef = loaderRef;
        }

        Object getLoaderObj() {
            return loaderRef.get();
        }
    }
}
