@echo off
setlocal

set "MVNW_HOME=%~dp0"
set "MAVEN_WRAPPER_JAR=%MVNW_HOME%.mvn\wrapper\maven-wrapper.jar"
set "PROJECT_JDK=C:\jdk"
set "JAVA_EXE=%PROJECT_JDK%\bin\java.exe"

if not exist "%JAVA_EXE%" (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if exist "%JAVA_EXE%" (
  "%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory=%MVNW_HOME% -classpath "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
) else (
  java -Dmaven.multiModuleProjectDirectory=%MVNW_HOME% -classpath "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
)
