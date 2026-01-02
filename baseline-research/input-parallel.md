Language: English.

Clojure production pain points

Research:
1. Error messages and stack traces - cryptic JVM exceptions, missing line numbers, macro expansion noise, debugging difficulty in production
2. Startup time and memory footprint - JVM cold start impact on serverless/CLI tools, heap requirements, GraalVM native-image tradeoffs
3. REPL-driven development gaps - state management pitfalls, namespace reloading issues, production debugging without REPL access
4. Concurrency debugging - core.async channel leaks, deadlock diagnosis, STM edge cases, thread pool exhaustion
5. Interop friction - Java library integration pain, type hints verbosity, reflection warnings in hot paths
6. Dependency management - conflicting library versions, AOT compilation surprises, uberjar size bloat
7. Performance profiling - lazy sequence memory traps, transducer vs sequence performance, JIT warmup issues
8. Deployment challenges - Docker image size, classpath complexity, environment-specific configuration