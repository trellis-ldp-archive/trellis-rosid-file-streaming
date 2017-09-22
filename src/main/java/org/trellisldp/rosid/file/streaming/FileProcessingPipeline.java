/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.rosid.file.streaming;

import static java.lang.Long.parseLong;
import static java.util.stream.Collectors.toMap;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_CACHE;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_CACHE_AGGREGATE;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_EVENT;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_LDP_CONTAINMENT_ADD;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_LDP_CONTAINMENT_DELETE;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_LDP_MEMBERSHIP_ADD;
import static org.trellisldp.rosid.common.RosidConstants.TOPIC_LDP_MEMBERSHIP_DELETE;

import java.util.Map;
import java.util.Properties;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.joda.time.Duration;

import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class FileProcessingPipeline {

    private static final String STORAGE_PREFIX = "trellis.partitions.";

    private final Map<String, String> config;
    private final String bootstrapServers;
    private final long aggregateSeconds;

    /**
     * Build a file processing pipeline
     * @param configuration the configuration
     */
    public FileProcessingPipeline(final Properties configuration) {
        this.config = configuration.stringPropertyNames().stream().filter(k -> k.startsWith(STORAGE_PREFIX))
            .collect(toMap(k -> k.substring(STORAGE_PREFIX.length()), configuration::getProperty));
        this.bootstrapServers = configuration.getProperty("kafka.bootstrapServers");
        this.aggregateSeconds = parseLong(configuration.getProperty("trellis.aggregateSeconds", "1"));
    }

    /**
     * Get the beam pipeline
     * @return the pipeline
     */
    public Pipeline getPipeline() {

        final PipelineOptions options = PipelineOptionsFactory.create();
        final Pipeline p = Pipeline.create(options);

        // Add membership triples
        p.apply(KafkaIO.<String, String>read().withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(StringDeserializer.class)
                    .withValueDeserializer(StringDeserializer.class)
                    .withTopic(TOPIC_LDP_MEMBERSHIP_ADD).withoutMetadata())
            .apply(ParDo.of(new BeamProcessor(config, LDP.PreferMembership.getIRIString(), true)))
            .apply(KafkaIO.<String, String>write().withBootstrapServers(bootstrapServers)
                    .withKeySerializer(StringSerializer.class)
                    .withValueSerializer(StringSerializer.class)
                    .withTopic(TOPIC_CACHE_AGGREGATE));

        // Delete membership triples
        p.apply(KafkaIO.<String, String>read().withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(StringDeserializer.class)
                    .withValueDeserializer(StringDeserializer.class)
                    .withTopic(TOPIC_LDP_MEMBERSHIP_DELETE).withoutMetadata())
            .apply(ParDo.of(new BeamProcessor(config, LDP.PreferMembership.getIRIString(), false)))
            .apply(KafkaIO.<String, String>write().withBootstrapServers(bootstrapServers)
                    .withKeySerializer(StringSerializer.class)
                    .withValueSerializer(StringSerializer.class)
                    .withTopic(TOPIC_CACHE_AGGREGATE));

        // Add containment triples
        p.apply(KafkaIO.<String, String>read().withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(StringDeserializer.class)
                    .withValueDeserializer(StringDeserializer.class)
                    .withTopic(TOPIC_LDP_CONTAINMENT_ADD).withoutMetadata())
            .apply(ParDo.of(new BeamProcessor(config, LDP.PreferContainment.getIRIString(), true)))
            .apply(KafkaIO.<String, String>write().withBootstrapServers(bootstrapServers)
                    .withKeySerializer(StringSerializer.class)
                    .withValueSerializer(StringSerializer.class)
                    .withTopic(TOPIC_CACHE_AGGREGATE));

        // Delete containment triples
        p.apply(KafkaIO.<String, String>read().withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(StringDeserializer.class)
                    .withValueDeserializer(StringDeserializer.class)
                    .withTopic(TOPIC_LDP_CONTAINMENT_DELETE).withoutMetadata())
            .apply(ParDo.of(new BeamProcessor(config, LDP.PreferContainment.getIRIString(), false)))
            .apply(KafkaIO.<String, String>write().withBootstrapServers(bootstrapServers)
                    .withKeySerializer(StringSerializer.class)
                    .withValueSerializer(StringSerializer.class)
                    .withTopic(TOPIC_CACHE_AGGREGATE));

        // Aggregate cache writes
        p.apply(KafkaIO.<String, String>read().withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(StringDeserializer.class)
                    .withValueDeserializer(StringDeserializer.class)
                    .withTopic(TOPIC_CACHE_AGGREGATE).withoutMetadata())
            .apply(Window.<KV<String, String>>into(FixedWindows.of(Duration.standardSeconds(aggregateSeconds)))
                    .triggering(AfterProcessingTime.pastFirstElementInPane()
                            .plusDelayOf(Duration.standardSeconds(aggregateSeconds)))
                    .discardingFiredPanes().withAllowedLateness(Duration.ZERO))
            .apply(Combine.perKey(x -> x.iterator().next()))
            .apply(KafkaIO.<String, String>write().withBootstrapServers(bootstrapServers)
                    .withKeySerializer(StringSerializer.class)
                    .withValueSerializer(StringSerializer.class)
                    .withTopic(TOPIC_CACHE));

        // Write to cache and dispatch to the event bus
        p.apply(KafkaIO.<String, String>read().withBootstrapServers(bootstrapServers)
                    .withKeyDeserializer(StringDeserializer.class)
                    .withValueDeserializer(StringDeserializer.class)
                    .withTopic(TOPIC_CACHE).withoutMetadata())
            .apply(ParDo.of(new CacheWriter(config)))
            .apply(ParDo.of(new EventProcessor()))
            .apply(KafkaIO.<String, String>write().withBootstrapServers(bootstrapServers)
                    .withKeySerializer(StringSerializer.class)
                    .withValueSerializer(StringSerializer.class)
                    .withTopic(TOPIC_EVENT));

        return p;
    }
}
