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
package io.github.ukuz.piccolo.cache.redis.operator;

import java.util.concurrent.TimeUnit;

/**
 * @author ukuz90
 */
public interface ValueOperator<V> {

    String set(V val);

    long setNx(V val);

    String setEx(V val, int seconds);

    String psetEx(V val, long millis);

    V get();

    long incr();

    long incrBy(long increment);

    long decr();

    long decrBy(long decrement);

    long del();

    default String ttl(V val, long time, TimeUnit unit) {
        if (unit == TimeUnit.MILLISECONDS) {
            return psetEx(val, unit.toMillis(time));
        } else {
            return setEx(val, (int) unit.toSeconds(time));
        }
    }


}
