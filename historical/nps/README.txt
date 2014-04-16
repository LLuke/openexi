~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
+++SOURCE OF ALGORITHM/TECHNIQUE+++
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This is a partial encoding of the W3C proposed alternative XML format,
Efficient XML Interchange (EXI), defined at
    http://www.w3.org/TR/2008/WD-exi-20080919/
            
A rough paraphrase of the intent of EXI 
   --Enabling XML where XML previously could not go--



~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
+++_Apps Directory (GUI Tools)+++
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
These tools are based on the Siemens EXI open source implementation with NPS
GUI code wrapped around it.
Siemens source and binarys can be obtained from http://exificient.sourceforge.net/


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
+++WHO STARTED THIS CODE+++
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This work was started as a portion of a thesis project at the
Naval Postgraduate school: https://www.movesinstitute.org/exi/EXI.html



~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
+++STATUS OF THE CODE+++
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Current status of this code set is limited....
	Byte aligned
	Schemaless
	Breaks alot
	Works on trivally small cases only



~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
+++FOCUS OF THE CODE+++
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Ultimate intent of this effort is to implement it into the Apache Group,
perhaps their HTTP web server to become another negotiable compression
technique for XML data exchanges across the Web.  Given the high volumn of
XML data being exchanged on the Web (XHTML for example) a XML specific
compression technique that statistically surpasses other compression techniques
is justified.

This better compression technique will reduce bandwidth needs and reduce XML
processing complexity.  This will enable XML to low-bnadwidth environments
and to small CPU devices (micro, mobile, handheld).

