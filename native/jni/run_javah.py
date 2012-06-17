#!/usr/bin/python

import subprocess
from os import path

CLASSPATH='../../dist/aware.jar'

#add fully qualified Java classes here
classes = [
'cruxic.aware.GLImage',
'cruxic.aware.GLImageUpload',
'cruxic.aware.overlays.Liquid_StaticNoiseOverlay_Impl'
]

#call javah tool on each one
for fullClassName in classes:
	simpleName = path.splitext(fullClassName)[1][1:]
	headerName = simpleName + '_jni.h'
	print headerName
	subprocess.check_call(['javah', '-force', '-classpath', CLASSPATH, '-o', headerName, fullClassName])
