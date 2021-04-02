package com.vertispan.j2cl.build.task;

public interface Dependency {
    Project getProject();

    Scope getScope();

    enum Scope {
        COMPILE,
        RUNTIME,
        BOTH;

        private boolean isCompileScope() {
            return this != RUNTIME;
        }

        private boolean isRuntimeScope() {
            return this != COMPILE;
        }
    }
}