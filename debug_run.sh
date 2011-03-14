#!/bin/sh

#cd to directory containing this script
cd `dirname $0`

#/home/cruxic/Programming/libs
jar_lib_dir=../libs

CLASSPATH=./dist/aware.jar:\
${jar_lib_dir}/lwjgl-2.5/jar/jinput.jar:\
${jar_lib_dir}/lwjgl-2.5/jar/lwjgl_util.jar:\
${jar_lib_dir}/lwjgl-2.5/jar/lwjgl.jar:\
${jar_lib_dir}/json_simple-1.x/lib/json_simple-1.1.jar

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./dist/linux

gdb --args java -ea -Ddevel -Xmx64m -classpath "${CLASSPATH}" cruxic.aware.Main

