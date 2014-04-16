nagasena_0000.0002.xxxx.x.zip contains the latest Nagasena libraries. 

There are two jar files that contain compiled Java classes in the zip file.

- nagasena.jar
- nagasena-rta.jar

The first jar file nagasena.jar is *always* required to be put in your class 
path.

On the other hand, you will need to include nagasena-rta.jar in your class
path only when you intend to do either of the followings.

- Process XML Schema files into EXISchema objects using EXISchemaFactory class.

- Read interchangeable EXI grammars represented in EXI format and turn them 
  into EXISchema objects, using EXISchemaReader class.

You will additionally need Xerces2 Java 2.11.0 in order to use 
EXISchemaFactory class that is contained in nagasena-rta.jar. Otherwise, 
you do *not* need Xerces2. See http://xerces.apache.org/ for downloading
xerces.

