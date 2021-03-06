/*
 * Copyright 2019 ukuz90
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ukuz.piccolo.api.service;

import io.github.ukuz.piccolo.api.PiccoloContext;
import io.github.ukuz.piccolo.api.external.common.Assert;
import io.github.ukuz.piccolo.api.external.common.utils.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ukuz90
 */
public abstract class AbstractService implements Service {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AtomicBoolean isStarted = new AtomicBoolean();

    protected String name;

    @Override
    public CompletableFuture<Boolean> startAsync() {
        return startAsyncWithCallback(null, null);
    }

    @Override
    public CompletableFuture<Boolean> startAsync(PiccoloContext context) {
        Assert.notNull(context, "context must not be null");
        return startAsyncWithCallback(context, null);
    }

    @Override
    public CompletableFuture<Boolean> stopAsync() {
        return stopAsyncWithCallback(null);
    }

    protected final CompletableFuture<Boolean> startAsyncWithCallback(PiccoloContext context, Callback callback) throws ServiceException {
        ServiceCallback serviceCallback = wrapServiceCallback(callback);
        if  (isStarted.compareAndSet(false, true)) {
            try {
                if (context == null) {
                    init();
                } else {
                    init(context);
                }
                CompletableFuture future = doStartAsync();
                future.whenCompleteAsync((result, throwable) -> {
                    Optional.of(result).ifPresent(r-> serviceCallback.success());
                    Optional.of(throwable).ifPresent(t->serviceCallback.failure(wrapServiceException((Throwable) t)));
                    if (throwable == null) {
                        logger.info("service {} start success.", getName());
                    } else {
                        logger.error("service {} start failure, cause: {}", getName(), ((Throwable)throwable).getMessage());
                    }

                });
            } catch (ServiceException e) {
                serviceCallback.failure(e);
                logger.error("service {} start failure, cause: {}", getName(), e);
            }
        } else {
            //duplicate start
            serviceCallback.failure(new DuplicateStartServiceException("Service " + this.getClass().getName() + " already started."));
        }
        return serviceCallback;
    }

    public final CompletableFuture<Boolean> stopAsyncWithCallback(Callback callback) {
        ServiceCallback serviceCallback = wrapServiceCallback(callback);
        if (!isStarted.get()) {
            serviceCallback.failure(new IllegalStateServiceException("Service " + this.getClass().getName() + " was not running."));
            return serviceCallback;
        }
        if (isStarted.compareAndSet(true, false)) {
            try {
                destroy();
                serviceCallback.success();
                logger.info("service {} stop success.", getName());
            } catch (ServiceException e) {
                serviceCallback.failure(e);
                logger.error("service {} stop failure, cause: {}", getName(), e.getMessage());
            }
        } else {
            //duplicate stop
            serviceCallback.failure(new DuplicateStopServiceException("Service " + this.getClass().getName() + " already stopped."));
        }
        return serviceCallback;
    }

    @Override
    public final boolean start() throws ServiceException {
        try {
            return startAsync().join();
        } catch (CompletionException e) {
            throw wrapServiceException(e.getCause());
        }
    }

    @Override
    public boolean start(PiccoloContext context) throws ServiceException {
        try {
            return startAsync(context).join();
        } catch (CompletionException e) {
            throw wrapServiceException(e.getCause());
        }
    }

    @Override
    public final boolean stop() throws ServiceException {
        try {
            return stopAsync().join();
        } catch (CompletionException e) {
            throw wrapServiceException(e.getCause());
        }

    }

    protected CompletableFuture<Boolean> doStartAsync() {
        CompletableFuture future = new CompletableFuture<>();
        future.complete(true);
        return future;
    }

    private ServiceCallback wrapServiceCallback(Callback callback) {
        if (callback instanceof ServiceCallback) {
            return (ServiceCallback) callback;
        }
        return new ServiceCallback(isStarted, callback);
    }

    protected ServiceException wrapServiceException(Throwable throwable) {
        if (throwable instanceof ServiceException) {
            return (ServiceException) throwable;
        }
        return new ServiceException(throwable);
    }

    @Override
    public boolean isRunning() {
        throw new UnsupportedOperationException();
    }

    protected String getName() {
        if (name == null) {
            String simpleClassName = ClassUtils.simpleClassName(this.getClass());
            name = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(simpleClassName), ' ');
        }
        return name;
    }
}
