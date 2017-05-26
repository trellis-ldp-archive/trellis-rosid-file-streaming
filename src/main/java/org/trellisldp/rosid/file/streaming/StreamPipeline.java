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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.beam.sdk.PipelineResult;
import org.slf4j.Logger;
import org.trellisldp.rosid.file.FileProcessingPipeline;

/**
 * @author acoburn
 */
public final class StreamPipeline {

    private static final Logger LOGGER = getLogger(StreamPipeline.class);

    /**
     * Start and run the pipeline
     * @param args the arguments
     * @throws IOException in the event of an error loading the configuration
     */
    public static void main(final String[] args) throws IOException {
        if (args.length >= 1) {
            final Properties config = new Properties();
            config.load(new FileInputStream(args[0]));
            final FileProcessingPipeline p = new FileProcessingPipeline(config);
            LOGGER.info("Starting stream processor");
            final PipelineResult res = p.getPipeline().run();
            LOGGER.info("Pipeline state: {}", res.getState().toString());
            res.waitUntilFinish();
        } else {
            LOGGER.error("No configuration file provided!");
        }
    }

    private StreamPipeline() {
        // prevent instantiation
    }
}
