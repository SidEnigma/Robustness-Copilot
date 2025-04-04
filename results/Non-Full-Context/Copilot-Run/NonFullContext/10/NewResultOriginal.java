package org.dcache.pool.classic;
 
 import static java.util.stream.Collectors.partitioningBy;
 import static java.util.stream.Collectors.toCollection;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.primitives.Ints;
 import diskCacheV111.pools.StorageClassFlushInfo;
 import diskCacheV111.util.CacheException;
 import diskCacheV111.util.FileNotInCacheException;
 import diskCacheV111.util.PnfsId;
 import dmg.cells.nucleus.CellCommandListener;
 import dmg.cells.nucleus.CellInfoProvider;
 import dmg.cells.nucleus.CellSetupProvider;
 import dmg.util.CommandException;
 import dmg.util.Formats;
 import dmg.util.command.Argument;
 import dmg.util.command.Command;
 import dmg.util.command.Option;
 import java.io.PrintWriter;
 import java.nio.channels.CompletionHandler;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.concurrent.Callable;
 import java.util.concurrent.TimeUnit;
 import org.dcache.pool.PoolDataBeanProvider;
 import org.dcache.pool.classic.MoverRequestScheduler.Order;
 import org.dcache.pool.classic.json.HSMFlushQManagerData;
 import org.dcache.pool.nearline.NearlineStorageHandler;
 import org.dcache.pool.repository.CacheEntry;
 import org.dcache.pool.repository.Repository;
 import org.dcache.vehicles.FileAttributes;
 import org.springframework.beans.factory.annotation.Required;
 
 /**
  * Manages tape flush queues.
  * <p>
  * A flush queue is created for each storage class and HSM pair. Queues can be explicitly defined or
  * created implicitly when a tape file for a particular storage class is first encountered.
  * <p>
  * Each queue is represented by a StorageClassInfo object.
  */
 public class StorageClassContainer
       implements CellCommandListener, CellSetupProvider, CellInfoProvider,
       PoolDataBeanProvider<HSMFlushQManagerData> {
 
     private final Map<String, StorageClassInfo> _storageClasses = new HashMap<>();
     private final Map<PnfsId, StorageClassInfo> _pnfsIds = new HashMap<>();
     private Repository _repository;
     private NearlineStorageHandler _storageHandler;
     private boolean _poolStatusInfoChanged = true;
 
     @Required
     public void setRepository(Repository repository) {
         _repository = repository;
     }
 
     @Required
     public void setNearlineStorageHandler(NearlineStorageHandler storageHandler) {
         _storageHandler = storageHandler;
     }
 
     public synchronized Collection<StorageClassInfo> getStorageClassInfos() {
         return new ArrayList<>(_storageClasses.values());
     }
 
     public synchronized StorageClassFlushInfo[] getFlushInfos() {
         return _storageClasses.values().stream().map(StorageClassInfo::getFlushInfo)
               .toArray(StorageClassFlushInfo[]::new);
     }
 
     public synchronized boolean poolStatusChanged() {
         boolean result = _poolStatusInfoChanged;
         _poolStatusInfoChanged = false;
         return result;
     }
 
     public synchronized int getStorageClassCount() {
         return _storageClasses.size();
     }
 
     public synchronized int getRequestCount() {
         return _pnfsIds.size();
     }
 
     public synchronized StorageClassInfo getStorageClassInfo(String hsmName, String storageClass) {
         return _storageClasses.get(storageClass + "@" + hsmName.toLowerCase());
     }
 
     public synchronized StorageClassInfo getStorageClassInfo(PnfsId pnfsId) {
         return _pnfsIds.get(pnfsId);
     }
 
     @VisibleForTesting
     synchronized StorageClassInfo defineStorageClass(String hsmName, String storageClass) {
         StorageClassInfo info =
               getStorageClassInfo(hsmName, storageClass);
         if (info == null) {
             info = new StorageClassInfo(_storageHandler, hsmName, storageClass);
         }
         info.setDefined(true);
         _storageClasses.put(info.getFullName(), info);
         return info;
     }
 
     private synchronized void removeStorageClass(String hsmName, String storageClass)
           throws CommandException {
         StorageClassInfo info = getStorageClassInfo(hsmName, storageClass);
         if (info != null) {
             if (info.size() > 0) {
                 throw new CommandException(1, "Class not empty");
             }
             _storageClasses.remove(info.getFullName());
         }
     }
 
     private synchronized void suspendStorageClass(String hsmName,
           String storageClass, boolean suspend) throws CommandException {
         StorageClassInfo info = getStorageClassInfo(hsmName, storageClass);
         if (info == null) {
             throw new CommandException(1,
                   "Storage class not found : " + storageClass + "@" + hsmName);
         }
         info.setSuspended(suspend);
     }
 
     private synchronized void suspendStorageClasses(boolean suspend) {
         for (StorageClassInfo info : _storageClasses.values()) {
             info.setSuspended(suspend);
         }
     }
 
     /**
      * Removes an entry from the list of HSM storage requests.
      *
      * @returns true if the entry was found and removed, false otherwise.
      */
     public synchronized boolean
     removeCacheEntry(PnfsId pnfsId) {
         StorageClassInfo info = _pnfsIds.remove(pnfsId);
         if (info == null) {
             return false;
         }
         boolean removed = info.remove(pnfsId);
         if (info.size() == 0 && !info.isDefined()) {
             _storageClasses.remove(info.getFullName());
         }
         return removed;
     }
 
 
/** adds a CacheEntry to the list of HSM storage requests. */

public synchronized void addCacheEntry(CacheEntry entry) throws CacheException, InterruptedException {
    PnfsId pnfsId = entry.getPnfsId();
    StorageClassInfo info = _pnfsIds.get(pnfsId);
    if (info == null) {
        throw new FileNotInCacheException(pnfsId);
    }
    if (info.isSuspended()) {
        throw new CacheException("Storage class is suspended: " + info.getFullName());
    }
    if (info.isFull()) {
        throw new CacheException("Storage class is full: " + info.getFullName());
    }
    if (info.contains(pnfsId)) {
        throw new CacheException("Cache entry already exists: " + pnfsId);
    }
    info.add(entry);
    _pnfsIds.put(pnfsId, info);
    _poolStatusInfoChanged = true;
}
 

}