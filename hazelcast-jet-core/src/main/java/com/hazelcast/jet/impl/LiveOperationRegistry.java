/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.impl;

import com.hazelcast.jet.impl.operation.AsyncJobOperation;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.LiveOperations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiveOperationRegistry {
    // memberAddress -> callId -> operation
    final ConcurrentHashMap<Address, Map<Long, AsyncJobOperation>> liveOperations = new ConcurrentHashMap<>();

    public void register(AsyncJobOperation operation) {
        Map<Long, AsyncJobOperation> callIds = liveOperations.computeIfAbsent(operation.getCallerAddress(),
                (key) -> new ConcurrentHashMap<>());
        if (callIds.putIfAbsent(operation.getCallId(), operation) != null) {
            throw new IllegalStateException("Duplicate operation during registration of operation=" + operation);
        }
    }

    public void deregister(AsyncJobOperation operation) {
        Map<Long, AsyncJobOperation> operations = liveOperations.get(operation.getCallerAddress());

        if (operations == null) {
            throw new IllegalStateException("Missing address during de-registration of operation=" + operation);
        }

        if (operations.remove(operation.getCallId()) == null) {
            throw new IllegalStateException("Missing operation during de-registration of operation=" + operation);
        }
    }

    void populate(LiveOperations liveOperations) {
        this.liveOperations.forEach((key, value) -> value.keySet().forEach(callId -> liveOperations.add(key, callId)));
    }

}
