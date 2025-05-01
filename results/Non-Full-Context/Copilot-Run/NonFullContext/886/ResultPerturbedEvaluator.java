/*
  * dCache - http://www.dcache.org/
  *
  * Copyright (C) 2018 Deutsches Elektronen-Synchrotron
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package diskCacheV111.namespace;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static com.google.common.base.Preconditions.checkState;
 import static com.google.common.collect.Multimaps.synchronizedListMultimap;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Throwables;
 import com.google.common.collect.ListMultimap;
 import com.google.common.collect.MultimapBuilder;
 import com.google.common.io.BaseEncoding;
 import diskCacheV111.util.PnfsId;
 import dmg.cells.nucleus.CellAddressCore;
 import dmg.cells.nucleus.CellLifeCycleAware;
 import dmg.cells.nucleus.CellMessageReceiver;
 import dmg.cells.nucleus.CellPath;
 import java.io.ByteArrayOutputStream;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Queue;
 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.Executor;
 import java.util.concurrent.RejectedExecutionException;
 import java.util.function.Consumer;
 import java.util.stream.Collectors;
 import org.apache.curator.framework.CuratorFramework;
 import org.apache.curator.framework.recipes.cache.ChildData;
 import org.apache.curator.framework.recipes.cache.PathChildrenCache;
 import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
 import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
 import org.apache.curator.utils.CloseableUtils;
 import org.apache.curator.utils.ZKPaths;
 import org.dcache.cells.CellStub;
 import org.dcache.cells.CuratorFrameworkAware;
 import org.dcache.events.Event;
 import org.dcache.events.NotificationMessage;
 import org.dcache.events.SystemEvent;
 import org.dcache.namespace.FileType;
 import org.dcache.namespace.events.EventType;
 import org.dcache.namespace.events.InotifyEvent;
 import org.dcache.util.RepeatableTaskRunner;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Required;
 
 /**
  * This class is responsible for accepting inotify(7)-like events and sending them to event
  * receivers: cells within dCache.  An event receiver must subscribe (so selecting the subset of
  * events that are of interest) before it receives any events.
  * <p>
  * The two main design goals are that the thread delivering an event to this class never blocks (see
  * EventReceiver) and that the system "degrades gracefully" under overload.  The work is handled by
  * executors, allowing the CPU resources are limited.  This ensures that sending events can never
  * monopolise activity, preventing dCache from doing useful work.  Instead, under overload, users
  * will experience a delay in receiving events (as events are queued).  If the overload persists
  * then events will be dropped, which users are informed of through the OVERFLOW event.
  * <p>
  * Note that, it is possible (under heavy load) that some undesired events are sent to an event
  * receive if the event receiver's subscription changes and the events have already been queued for
  * delivery.
  * <p>
  * Subscription is handled through ZooKeeper: the event receiver updates a ZK node using an encoded
  * version of the desired events.  ZK places a limit on the size of any node, which limits the
  * number of concurrent "watches" any event receiver may make.  The binary format of the ZK node is
  * versioned, to support future changes.
  * <p>
  * Incoming events are immediately queued for processing.  This avoids blocking while this class is
  * processing any changes in the watches/subscriptions.  If this queue exceeds the maximum allowed
  * size then all event receivers will receive the OVERFLOW event.
  * <p>
  * Queued events are processed to determine which (if any) event receivers are interested.  Each
  * event receiver has a queue of events: those that match this event receiver's list of watches.
  * These events are sent to the event receiver as a sequence of messages directed explicitly to that
  * event receiver.
  * <p>
  * A single threaded task is responsible for sending events.  This task sends all outstanding events
  * for an event receiver before moving onto the next event receiver.  To reduce overheads in message
  * sending, multiple events may be sent in a single message.  The number of events sent in any one
  * message is limited to prevent sending very large messages.
  */
 public class EventNotifier implements EventReceiver, CellMessageReceiver,
       CuratorFrameworkAware, CellLifeCycleAware, PathChildrenCacheListener {
 
     private static final Logger LOGGER = LoggerFactory.getLogger(EventNotifier.class);
     public static final String INOTIFY_PATH = ZKPaths.makePath("dcache", "inotify");
     private static final Event OVERFLOW_EVENT = new SystemEvent(SystemEvent.Type.OVERFLOW);
 
     /**
      * Convert an event receiver's list of desired events to the binary representation.  Each
      * Map.Entry is a Watch subscribing to some events of some file or directory.
      *
      * @param entries the watches.  The watches to which the event receiver is subscribing.
      * @return the binary data for the ZooKeeper node.
      */
     public static byte[] toZkData(Map<PnfsId, Collection<EventType>> entries) {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         out.write(0); // Format #0: simple list.
         for (Map.Entry<PnfsId, Collection<EventType>> entry : entries.entrySet()) {
             // 16-bit mask of interested events; currently 13 of 16 bits are used.
             short bitmask = (short) entry.getValue().stream()
                   .mapToInt(EventType::ordinal)
                   .map(o -> 1 << o)
                   .reduce(0, (a, b) -> a | b);
             out.write(bitmask >> 8);
             out.write(bitmask & 0xff);
 
             PnfsId id = entry.getKey();
             // REVISIT this assumes that the PNFS-ID value is (upper case) hex value.
             byte[] pnfsid = BaseEncoding.base16().decode(id.toString());
             checkState(pnfsid.length < 256, "PNFS-ID length exceeds 256 bytes");
             out.write((byte) pnfsid.length);
             out.write(pnfsid, 0, pnfsid.length);
             LOGGER.debug("encoded id={} bitmask={}", id, bitmask);
         }
         return out.toByteArray();
     }
 
 
/** Convert a binary representation back into list of desired events of the event receiver. */
 public static Map<PnfsId, EnumSet<EventType>> fromZkData(byte[] data){
      ByteArrayInputStream in = new ByteArrayInputStream(data);
      int format = in.read();
      checkState(format == 0, "Unsupported format: %d", format);
      Map<PnfsId, EnumSet<EventType>> entries = new ConcurrentHashMap<>();
      while (in.available() > 0) {
          short bitmask = (short) (in.read() << 8 | in.read());
          int pnfsidLength = in.read();
          byte[] pnfsid = new byte[pnfsidLength];
          in.read(pnfsid);
          PnfsId id = PnfsId.valueOf(BaseEncoding.base16().encode(pnfsid));
          EnumSet<EventType> events = EnumSet.noneOf(EventType.class);
          for (EventType event : EventType.values()) {
              if ((bitmask & (1 << event.ordinal())) != 0) {
                  events.add(event);
              }
          }
          entries.put(id, events);
          LOGGER.debug("decoded id={} bitmask={}", id, bitmask);
      }
      return entries; 
 }

 

}