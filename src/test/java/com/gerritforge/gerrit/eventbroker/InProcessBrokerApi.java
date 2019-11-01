// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.eventbroker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Ignore;

@Ignore
public class InProcessBrokerApi implements BrokerApi {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final UUID instanceId;
  private final Gson gson;
  private final Map<String, EventBus> eventBusMap;
  private final Multimap<String, Consumer<EventMessage>> eventConsumers;

  public InProcessBrokerApi(UUID instanceId) {
    this.instanceId = instanceId;
    this.gson = new Gson();
    this.eventBusMap = new MapMaker().concurrencyLevel(1).makeMap();
    this.eventConsumers = HashMultimap.create();
  }

  @Override
  public boolean send(String topic, EventMessage message) {
    EventBus topicEventConsumers = eventBusMap.get(topic);
    try {
      if (topicEventConsumers != null) {
        topicEventConsumers.post(message);
      }
    } catch (RuntimeException e) {
      log.atSevere().withCause(e).log();
      return false;
    }
    return true;
  }

  @Override
  public void receiveAsync(String topic, Consumer<EventMessage> eventConsumer) {
    EventBus topicEventConsumers = eventBusMap.get(topic);
    if (topicEventConsumers == null) {
      topicEventConsumers = new EventBus(topic);
      eventBusMap.put(topic, topicEventConsumers);
    }
    topicEventConsumers.register(eventConsumer);
    eventConsumers.put(topic, eventConsumer);
  }

  @Override
  public Multimap<String, Consumer<EventMessage>> consumersMap() {
    return ImmutableMultimap.copyOf(eventConsumers);
  }

  @Override
  public void disconnect() {
    this.eventBusMap.clear();
  }
}