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

import static org.apache.beam.sdk.values.KV.of;
import static org.apache.jena.riot.Lang.NQUADS;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.apache.jena.sparql.core.DatasetGraphFactory.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.spi.EventService.serialize;

import java.io.StringReader;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.trellisldp.rosid.common.Notification;

/**
 * @author acoburn
 */
class EventProcessor extends DoFn<KV<String, String>, KV<String, String>> {

    private static final JenaRDF rdf = new JenaRDF();

    private static final Logger LOGGER = getLogger(EventProcessor.class);

    private static Dataset deserialize(final String data) {
        final DatasetGraph dataset = create();
        try (final StringReader reader = new StringReader(data)) {
            read(dataset, reader, null, NQUADS);
        }
        return rdf.asDataset(dataset);
    }

    /**
     * A beam processor that handles raw NQUAD graphs
     */
    public EventProcessor() {
        super();
    }

    /**
     * Process the element
     * @param c the context
     */
    @ProcessElement
    public void processElement(final ProcessContext c) {
        final KV<String, String> element = c.element();
        final Dataset data = deserialize(element.getValue());
        final Notification notification = new Notification(element.getKey(), data);
        LOGGER.debug("Serializing notification for {}", element.getKey());
        serialize(notification).ifPresent(evt -> c.output(of(element.getKey(), evt)));
    }
}
