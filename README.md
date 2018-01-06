# trellis-rosid-file-streaming

**NOTE**: this project has been migrated into the [Trellis/Rosid repository](https://github.com/trellis-ldp/trellis-rosid).

A <a href="https://beam.apache.org">Beam</a>-based resource processing application suitable for various distributed backends.

## Building

This code requires Java 8 and can be built with Gradle:

    ./gradlew build

To build this application for a particular backend, use the `-P` flag to specify `spark`, `flink`, `apex` or `google`. The default is the `direct` runner.

## Running

To run the code, use this command:

    java -jar ./build/libs/trellis-processing.jar config.properties --defaultWorkerLogLevel=WARN --workerLogLevelOverrides={"org.trellisldp":"INFO"}

where `./config.properties` is a file such as:

```
# The Kafka cluster
kafka.bootstrapServers = host1:port,host2:port,host3:port

# The Trellis data/URL locations
trellis.partitions.<partition1-name>.data = /path/to/partition1/data/objects
trellis.partitions.<partition1-name>.baseUrl = http://repo1.example.org/

trellis.partitions.<partition2-name>.data = /path/to/partition2/data/objects
trellis.partitions.<partition2-name>.baseUrl = http://repo2.example.org/

trellis.partitions.<partition3-name>.data = /path/to/partition3/data/objects
trellis.partitions.<partition3-name>.baseUrl = http://repo3.example.org/

# A time in seconds to aggregate cache writes
trellis.aggregateSeconds = 4
```

