/*
  * Copyright 2011-2022 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      https://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package io.lettuce.core;
 
 import java.io.Closeable;
 import java.net.SocketAddress;
 import java.time.Duration;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.*;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import reactor.core.publisher.Mono;
 import io.lettuce.core.Transports.NativeTransports;
 import io.lettuce.core.internal.*;
 import io.lettuce.core.protocol.ConnectionWatchdog;
 import io.lettuce.core.protocol.RedisHandshakeHandler;
 import io.lettuce.core.resource.ClientResources;
 import io.lettuce.core.resource.DefaultClientResources;
 import io.netty.bootstrap.Bootstrap;
 import io.netty.buffer.ByteBufAllocator;
 import io.netty.channel.*;
 import io.netty.channel.group.ChannelGroup;
 import io.netty.channel.group.DefaultChannelGroup;
 import io.netty.channel.nio.NioEventLoopGroup;
 import io.netty.util.concurrent.EventExecutorGroup;
 import io.netty.util.concurrent.Future;
 import io.netty.util.internal.logging.InternalLogger;
 import io.netty.util.internal.logging.InternalLoggerFactory;
 
 /**
  * Base Redis client. This class holds the netty infrastructure, {@link ClientOptions} and the basic connection procedure. This
  * class creates the netty {@link EventLoopGroup}s for NIO ({@link NioEventLoopGroup}) and EPoll (
  * {@link io.netty.channel.epoll.EpollEventLoopGroup}) with a default of {@code Runtime.getRuntime().availableProcessors() * 4}
  * threads. Reuse the instance as much as possible since the {@link EventLoopGroup} instances are expensive and can consume a
  * huge part of your resources, if you create multiple instances.
  * <p>
  * You can set the number of threads per {@link NioEventLoopGroup} by setting the {@code io.netty.eventLoopThreads} system
  * property to a reasonable number of threads.
  * </p>
  *
  * @author Mark Paluch
  * @author Jongyeol Choi
  * @author Poorva Gokhale
  * @since 3.0
  * @see ClientResources
  */
 public abstract class AbstractRedisClient {
 
     private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRedisClient.class);
 
     private static final int EVENTLOOP_ACQ_INACTIVE = 0;
 
     private static final int EVENTLOOP_ACQ_ACTIVE = 1;
 
     private final AtomicInteger eventLoopGroupCas = new AtomicInteger();
 
     protected final ConnectionEvents connectionEvents = new ConnectionEvents();
 
     protected final Set<Closeable> closeableResources = ConcurrentHashMap.newKeySet();
 
     protected final ChannelGroup channels;
 
     private final ClientResources clientResources;
 
     private final Map<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroups = new ConcurrentHashMap<>(2);
 
     private final boolean sharedResources;
 
     private final AtomicBoolean shutdown = new AtomicBoolean();
 
     private volatile ClientOptions clientOptions = ClientOptions.create();
 
     private volatile Duration defaultTimeout = RedisURI.DEFAULT_TIMEOUT_DURATION;
 
     /**
      * Create a new instance with client resources.
      *
      * @param clientResources the client resources. If {@code null}, the client will create a new dedicated instance of
      *        client resources and keep track of them.
      */
     protected AbstractRedisClient(ClientResources clientResources) {
 
         if (clientResources == null) {
             this.sharedResources = false;
             this.clientResources = DefaultClientResources.create();
         } else {
             this.sharedResources = true;
             this.clientResources = clientResources;
         }
 
         this.channels = new DefaultChannelGroup(this.clientResources.eventExecutorGroup().next());
     }
 
     protected int getChannelCount() {
         return channels.size();
     }
 
     /**
      * Returns the default {@link Duration timeout} for commands.
      *
      * @return the default {@link Duration timeout} for commands.
      */
     public Duration getDefaultTimeout() {
         return defaultTimeout;
     }
 
     /**
      * Set the default timeout for connections created by this client. The timeout applies to connection attempts and
      * non-blocking commands.
      *
      * @param timeout default connection timeout, must not be {@code null}.
      * @since 5.0
      */
     public void setDefaultTimeout(Duration timeout) {
 
         LettuceAssert.notNull(timeout, "Timeout duration must not be null");
         LettuceAssert.isTrue(!timeout.isNegative(), "Timeout duration must be greater or equal to zero");
 
         this.defaultTimeout = timeout;
     }
 
     /**
      * Set the default timeout for connections created by this client. The timeout applies to connection attempts and
      * non-blocking commands.
      *
      * @param timeout Default connection timeout.
      * @param unit Unit of time for the timeout.
      * @deprecated since 5.0, use {@link #setDefaultTimeout(Duration)}.
      */
     @Deprecated
     public void setDefaultTimeout(long timeout, TimeUnit unit) {
         setDefaultTimeout(Duration.ofNanos(unit.toNanos(timeout)));
     }
 
     /**
      * Returns the {@link ClientOptions} which are valid for that client. Connections inherit the current options at the moment
      * the connection is created. Changes to options will not affect existing connections.
      *
      * @return the {@link ClientOptions} for this client
      */
     public ClientOptions getOptions() {
         return clientOptions;
     }
 
     /**
      * Set the {@link ClientOptions} for the client.
      *
      * @param clientOptions client options for the client and connections that are created after setting the options
      */
     protected void setOptions(ClientOptions clientOptions) {
         LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
         this.clientOptions = clientOptions;
     }
 
     /**
      * Returns the {@link ClientResources} which are used with that client.
      *
      * @return the {@link ClientResources} for this client.
      * @since 6.0
      *
      */
     public ClientResources getResources() {
         return clientResources;
     }
 
     protected int getResourceCount() {
         return closeableResources.size();
     }
 
     /**
      * Add a listener for the RedisConnectionState. The listener is notified every time a connect/disconnect/IO exception
      * happens. The listeners are not bound to a specific connection, so every time a connection event happens on any
      * connection, the listener will be notified. The corresponding netty channel handler (async connection) is passed on the
      * event.
      *
      * @param listener must not be {@code null}
      */
     public void addListener(RedisConnectionStateListener listener) {
         LettuceAssert.notNull(listener, "RedisConnectionStateListener must not be null");
         connectionEvents.addListener(listener);
     }
 
     /**
      * Removes a listener.
      *
      * @param listener must not be {@code null}
      */
     public void removeListener(RedisConnectionStateListener listener) {
 
         LettuceAssert.notNull(listener, "RedisConnectionStateListener must not be null");
         connectionEvents.removeListener(listener);
     }
 
     /**
      * Populate connection builder with necessary resources.
      *
      * @param socketAddressSupplier address supplier for initial connect and re-connect
      * @param connectionBuilder connection builder to configure the connection
      * @param redisURI URI of the Redis instance
      */
     protected void connectionBuilder(Mono<SocketAddress> socketAddressSupplier, ConnectionBuilder connectionBuilder,
             RedisURI redisURI) {
 
         Bootstrap redisBootstrap = new Bootstrap();
         redisBootstrap.option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
 
         ClientOptions clientOptions = getOptions();
         SocketOptions socketOptions = clientOptions.getSocketOptions();
 
         redisBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                 Math.toIntExact(socketOptions.getConnectTimeout().toMillis()));
 
         if (LettuceStrings.isEmpty(redisURI.getSocket())) {
             redisBootstrap.option(ChannelOption.SO_KEEPALIVE, socketOptions.isKeepAlive());
             redisBootstrap.option(ChannelOption.TCP_NODELAY, socketOptions.isTcpNoDelay());
         }
 
         connectionBuilder.apply(redisURI);
 
         connectionBuilder.bootstrap(redisBootstrap);
         connectionBuilder.channelGroup(channels).connectionEvents(connectionEvents);
         connectionBuilder.socketAddressSupplier(socketAddressSupplier);
     }
 
     protected void channelType(ConnectionBuilder connectionBuilder, ConnectionPoint connectionPoint) {
 
         LettuceAssert.notNull(connectionPoint, "ConnectionPoint must not be null");
 
         connectionBuilder.bootstrap().group(getEventLoopGroup(connectionPoint));
 
         if (connectionPoint.getSocket() != null) {
             NativeTransports.assertAvailable();
             connectionBuilder.bootstrap().channel(NativeTransports.domainSocketChannelClass());
         } else {
             connectionBuilder.bootstrap().channel(Transports.socketChannelClass());
         }
     }
 
     private EventLoopGroup getEventLoopGroup(ConnectionPoint connectionPoint) {
 
         for (;;) {
             if (!eventLoopGroupCas.compareAndSet(EVENTLOOP_ACQ_INACTIVE, EVENTLOOP_ACQ_ACTIVE)) {
                 continue;
             }
 
             try {
                 return doGetEventExecutor(connectionPoint);
             } finally {
                 eventLoopGroupCas.set(EVENTLOOP_ACQ_INACTIVE);
             }
         }
     }
 
     private EventLoopGroup doGetEventExecutor(ConnectionPoint connectionPoint) {
 
         if (connectionPoint.getSocket() == null && !eventLoopGroups.containsKey(Transports.eventLoopGroupClass())) {
             eventLoopGroups.put(Transports.eventLoopGroupClass(),
                     clientResources.eventLoopGroupProvider().allocate(Transports.eventLoopGroupClass()));
         }
 
         if (connectionPoint.getSocket() != null) {
 
             NativeTransports.assertAvailable();
 
             Class<? extends EventLoopGroup> eventLoopGroupClass = NativeTransports.eventLoopGroupClass();
 
             if (!eventLoopGroups.containsKey(NativeTransports.eventLoopGroupClass())) {
                 eventLoopGroups.put(eventLoopGroupClass,
                         clientResources.eventLoopGroupProvider().allocate(eventLoopGroupClass));
             }
         }
 
         if (connectionPoint.getSocket() == null) {
             return eventLoopGroups.get(Transports.eventLoopGroupClass());
         }
 
         if (connectionPoint.getSocket() != null) {
             NativeTransports.assertAvailable();
             return eventLoopGroups.get(NativeTransports.eventLoopGroupClass());
         }
 
         throw new IllegalStateException("This should not have happened in a binary decision. Please file a bug.");
     }
 
     /**
      * Retrieve the connection from {@link ConnectionFuture}. Performs a blocking {@link ConnectionFuture#get()} to synchronize
      * the channel/connection initialization. Any exception is rethrown as {@link RedisConnectionException}.
      *
      * @param connectionFuture must not be null.
      * @param <T> Connection type.
      * @return the connection.
      * @throws RedisConnectionException in case of connection failures.
      * @since 4.4
      */
     protected <T> T getConnection(ConnectionFuture<T> connectionFuture) {
 
         try {
             return connectionFuture.get();
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), e);
         } catch (Exception e) {
             throw RedisConnectionException.create(connectionFuture.getRemoteAddress(), Exceptions.unwrap(e));
         }
     }
 
     /**
      * Retrieve the connection from {@link ConnectionFuture}. Performs a blocking {@link ConnectionFuture#get()} to synchronize
      * the channel/connection initialization. Any exception is rethrown as {@link RedisConnectionException}.
      *
      * @param connectionFuture must not be null.
      * @param <T> Connection type.
      * @return the connection.
      * @throws RedisConnectionException in case of connection failures.
      * @since 5.0
      */
     protected <T> T getConnection(CompletableFuture<T> connectionFuture) {
 
         try {
             return connectionFuture.get();
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             throw RedisConnectionException.create(e);
         } catch (Exception e) {
             throw RedisConnectionException.create(Exceptions.unwrap(e));
         }
     }
 
 
/** Connect and initialize a channel from {@link ConnectionBuilder}. */
 protected ConnectionFuture<T> initializeChannelAsync(ConnectionBuilder connectionBuilder){}

 

}