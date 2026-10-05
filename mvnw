#!/bin/sh

MVNW_HOME="$(cd "$(dirname "$0")" && pwd -P)"
PROJECT_JDK="/c/jdk"
JAVA_BIN="${PROJECT_JDK}/bin/java"

if [ ! -x "$JAVA_BIN" ]; then
	JAVA_BIN="${JAVA_HOME}/bin/java"
fi

if [ ! -x "$JAVA_BIN" ]; then
	JAVA_BIN="java"
fi

exec "$JAVA_BIN" -Dmaven.multiModuleProjectDirectory=$MVNW_HOME -classpath "$MVNW_HOME/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
