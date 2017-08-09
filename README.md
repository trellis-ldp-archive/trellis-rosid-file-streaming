# trellis-rosid-file-streaming

[![Build Status](https://travis-ci.org/trellis-ldp/trellis-rosid-file-streaming.png?branch=master)](https://travis-ci.org/trellis-ldp/trellis-rosid-file-streaming)
[![Build status](https://ci.appveyor.com/api/projects/status/89bpi6s7kmky9ev8?svg=true)](https://ci.appveyor.com/project/acoburn/trellis-rosid-file-streaming)

A <a href="https://beam.apache.org">Beam</a>-based resource processing application suitable for various distributed backends.

## Building

This code requires Java 8 and can be built with Gradle:

    ./gradlew build

To build this application for a particular backend, use the `-P` flag to specify `spark`, `flink`, `apex` or `google`. The default is the `direct` runner.

## Running

To run the code, use this command:

    java -jar ./build/libs/trellis-processing.jar config.properties

where `./config.properties` is a file such as:

```
trellis.storage.<partition1-name>.ldprs = /path/to/partition1/data/objects
trellis.storage.<partition2-name>.ldprs = /path/to/partition2/data/objects
trellis.storage.<partition3-name>.ldprs = /path/to/partition3/data/objects
kafka.bootstrapServers = host:port
```

