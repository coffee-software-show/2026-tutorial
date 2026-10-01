#!/usr/bin/env bash

mise use java@graalvm-community-25.0.2

./mvnw -DskipTests -Pnative native:compile