#!/bin/sh
mvn clean eclipse:clean
mvn -DdownloadSources=true aspectj:compile aspectj:test-compile eclipse:eclipse
