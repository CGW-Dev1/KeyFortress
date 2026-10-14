@rem KeyFortress Gradle launcher
@echo off
setlocal
if not defined JAVA_HOME (
    set JAVA_HOME=F:\jdk17
)
"C:\Users\cuigw56442\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat" %*
