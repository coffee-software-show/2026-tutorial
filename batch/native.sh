#!/usr/bin/env bash
ls -la target && rm -rf target
./mvnw -DskipTests -Pnative native:compile
./target/batch