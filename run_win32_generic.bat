set JARS="aware.jar;Libs\lwjgl.jar;Libs\jinput.jar;Libs\json_simple-1.1.jar;Libs\lwjgl_util.jar"


java -ea -Xmx64m "-Djava.library.path=.;Libs\native\windows" -cp %JARS% -Dfullscreen cruxic.aware.Main > aware-engine.log 2>&1
