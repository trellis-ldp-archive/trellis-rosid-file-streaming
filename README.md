# trellis-rosid-file-streaming

[![Build Status](https://travis-ci.org/trellis-ldp/trellis-rosid-file-streaming.png?branch=master)](https://travis-ci.org/trellis-ldp/trellis-rosid-file-streaming)
[![Build status](https://ci.appveyor.com/api/projects/status/89bpi6s7kmky9ev8?svg=true)](https://ci.appveyor.com/project/acoburn/trellis-rosid-file-streaming)
[![Coverage Status](https://coveralls.io/repos/github/trellis-ldp/trellis-rosid-file-streaming/badge.svg?branch=master)](https://coveralls.io/github/trellis-ldp/trellis-rosid-file-streaming?branch=master)
[![Maintainability](https://api.codeclimate.com/v1/badges/7674cf9587392c5abc2a/maintainability)](https://codeclimate.com/github/trellis-ldp/trellis-rosid-file-streaming/maintainability)

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

