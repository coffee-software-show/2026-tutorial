#!/usr/bin/env bash
export MISE_JAVA_VERSION="graalvm-community-25.0.2"
mise exec java@graalvm-community-25.0.2 -- ./mvnw -DskipTests -Pnative native:compile
