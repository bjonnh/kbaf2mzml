#!/usr/bin/env bash

LINUX_JVM=/usr/lib/jvm/java-15-openjdk
WIN_JVM=stuff/jdk-15.0.1.9-hotspot

echo "Building Linux Lib"
gcc -O3 -march=native baf2sql_adapter/src/main/c/baf2sql_adapter.c -I"${LINUX_JVM}"/include -I/usr/lib/jvm/java-15-openjdk/include/linux -shared -o lib/libbaf2sql_adapter.so -Llib -lbaf2sql_c
echo "Building Windows DLL"
x86_64-w64-mingw32-gcc -O3 -march=native baf2sql_adapter/src/main/c/baf2sql_adapter.c -I"${WIN_JVM}"/include -I"${WIN_JVM}"/include/win32 -shared -o lib/baf2sql_adapter.dll -Llib -lbaf2sql_c
