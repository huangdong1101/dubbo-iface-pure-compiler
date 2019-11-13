#!/bin/bash
cd $(cd "$(dirname $0)"; pwd)

rm -rf src/main

mvn clean test

mvn clean package -Dmaven.test.skip=true

rm -rf src/main