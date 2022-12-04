# Memcached Middleware

A load balancer for multi-server multi-client key-value store based on `memcached`'s protocol.


## Compile and run

In the folder `middleware`:

1. Compile with `ant`,
2. Run the middleware with the following command:

```bash
java -Dlog4j.configurationFile=lib/log4j2.xml -cp dist/middleware-zhiyang.jar:lib/* ch.ethz.asltest.RunMW [arguments]
```

This will include the log4j logging library and its configuration file. The [arguments] part should be in the following format: ```-l <MyIP> -p <MyListenPort> -t <NumberOfThreadsInPool> -s <readSharded> -m <MemcachedIP:Port> <MemcachedIP2:Port2> ...```.

## Repository structure

- ./logs: all experiment logs and processed data.
- ./middleware: the Java codebase for the middleware.
- ./report: source files for the report.
- ./scripts: shell scripts used to run all experiments.

## Acknowledgement

This project is initiated from the Advanced Systems Lab course at ETH Zurich, Fall 2018. Final grade: 6.0/6.0.
