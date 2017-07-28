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

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static java.util.stream.Stream.empty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.rosid.common.RDFUtils.deserialize;
import static org.trellisldp.rosid.file.FileUtils.resourceDirectory;
import static org.trellisldp.spi.RDFUtils.getInstance;

import java.io.File;
import java.util.Map;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.RDF;
import org.slf4j.Logger;
import org.trellisldp.rosid.file.VersionedResource;

/**
 * @author acoburn
 */
class BeamProcessor extends DoFn<KV<String, String>, KV<String, String>> {

    private static final RDF rdf = getInstance();

    private static final Logger LOGGER = getLogger(BeamProcessor.class);

    private final Map<String, String> config;
    private final Boolean add;
    private final String graph;

    /**
     * A beam processor that handles raw NQUAD graphs
     * @param config the configuration
     * @param graph the relevant graph to use
     * @param add if true, quads will be added; otherwise they will be deleted
     */
    public BeamProcessor(final Map<String, String> config, final String graph, final Boolean add) {
        super();
        this.config = config;
        this.graph = graph;
        this.add = add;
    }

    /**
     * Process the element
     * @param c the context
     */
    @ProcessElement
    public void processElement(final ProcessContext c) {
        final KV<String, String> element = c.element();
        final File dir = resourceDirectory(config, element.getKey());
        if (!isNull(dir)) {
            final Dataset dataset = deserialize(element.getValue());
            if (VersionedResource.write(dir,
                        add ? empty() : dataset.stream(of(rdf.createIRI(graph)), null, null, null),
                        add ? dataset.stream(of(rdf.createIRI(graph)), null, null, null) : empty(), now())) {
                c.output(c.element());
            } else if (add) {
                LOGGER.error("Error adding {} quads to {}", graph, element.getKey());
            } else {
                LOGGER.error("Error removing {} quads from {}", graph, element.getKey());
            }
        } else {
            LOGGER.error("Unable to write {} quads to {}", graph, element.getKey());
        }
    }
}
