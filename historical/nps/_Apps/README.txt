This bat files execute GUI EXI utilities based on
Siemens open-source implementation of EXI
obtainable from http://exificient.sourceforge.net/


RunEXIcompairison.bat 
  -  is a tool that enables you to run a XML document 
     against GZip, Zip, EXI (schemaless) and EXI (schema informed)

  -  results are in a JSwing table format listing % or original 
     document for each technique

  -  1) select an input XML document by clicking the 
       XML File "set" button
 
  -  2) (Optionally) select an XML Schema by clicking the
       Schema File "set" button

  -  3) execute with a click of the "Compute" button

  -  4) (As needed) to purge the exisiting schema, click the "clear schema" button

  -  results of each technique are placed in the data directory


RunEXIoptions.bat
  -  is a tool to flex the EXI options
  
  -  1) Set the desired EXI options

  -  2) specify an input XML document by clicking the "InputXMLfile" button

  -  3) (Optionally) specify an XML schema for the input XML document by clicking
        the "SchemaFile" button [only after a input XML document is provided]

  -  4) specify the destination for the EXI results by clicking the "EXI output" button

  -  5) Execute with a click of the "Generate EXI" button

  -  6) (As needed)  Restore to default options by clicking the "Reset Options" button

  -  7) (As needed)  Clear the current input XML/XSD files and output EXI file
        by clicking the "Rest IO Files" button

  -  EXI results as well as a round trip of the XML document (name.exi.xml) are 
     put in the specified destiantion location in step 4)

  -  The text box are describes what is going on during the processing



