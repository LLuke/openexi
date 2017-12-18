package openexi.sample;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactory;
import org.openexi.scomp.EXISchemaFactoryException;
import org.openexi.scomp.EXISchemaReader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class DecodeEXI {
    public DecodeEXI() {
        super();
    }

    public void decodeEXI(
            String sourceFile, 
            String destinationFile,
            String alignment,
// Preservation options.
            Boolean preserveComments,
            Boolean preservePIs,
            Boolean preserveDTD,
            Boolean preserveNamespace,
            Boolean preserveLexicalValues,
            int blockSize,
            int maxValueLength,
            int maxValuePartitions,
// Schema options.
            String schemaFileName,
            String exiSchemaFileName,
            Boolean strict,
            String useSchema
   ) throws FileNotFoundException, IOException,
        SAXException, EXIOptionsException, TransformerConfigurationException,
                                                   EXISchemaFactoryException, ClassNotFoundException {

        FileInputStream in = null;
        Writer out = null;
        StringWriter stringWriter = new StringWriter();

// The Grammar Cache stores schema and EXI options information. The settings nust match when encoding
// and subsequently decoding a data set.
        GrammarCache grammarCache;

// All EXI options can expressed in a single short integer. DEFAULT_OPTIONS=2;
        short options = GrammarOptions.DEFAULT_OPTIONS;

        try {
            
// Standard SAX methods parse content and lexical values.
            SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();

// EXIReader infers and reconstructs the XML file structure.
            EXIReader reader = new EXIReader();
        
// Set alignment and compression
            if (alignment.equals("bitPacked"))
                    reader.setAlignmentType(AlignmentType.bitPacked);
            if (alignment.equals("compress"))
                    reader.setAlignmentType(AlignmentType.compress);
            if (alignment.equals("preCompress"))
                    reader.setAlignmentType(AlignmentType.preCompress);
            if(alignment.equals("byteAligned"))
                    reader.setAlignmentType(AlignmentType.byteAligned);

// If using strict interpretation of the schema, set STRICT_OPTIONS and continue.
            if (strict) {
                options = GrammarOptions.STRICT_OPTIONS;
            }
// Otherwise, check for preservation settings.            
            else 
            {
                if (preserveComments) options = GrammarOptions.addCM(options);
                if (preservePIs) options = GrammarOptions.addPI(options);
                if (preserveDTD) options = GrammarOptions.addDTD(options);
                if (preserveNamespace) options = GrammarOptions.addNS(options);
            }             

            // Set preservation preferences handled directly in the transmogrifier.
            reader.setPreserveLexicalValues(preserveLexicalValues);
            
            // Set the number of elements processed as a block.
            if (blockSize!=1000000) reader.setBlockSize(blockSize);
            
            // Set the maximum length for values stored in the String Table for reuse.
            if (maxValueLength>-1) reader.setValueMaxLength(maxValueLength);  
            
            // Set the maximum number of values stored in the String Table.
            if (maxValuePartitions >-1) 
                reader.setValuePartitionCapacity(maxValuePartitions);
            
            File inputFile = new File(sourceFile);
            in = new FileInputStream(inputFile);
            out = new OutputStreamWriter(new FileOutputStream(destinationFile), "UTF-8");

// Set the schema and EXI Options in the Grammar Cache.
            FileInputStream fis = null;
            
    // Create a schema and set it to null. If useSchema == "None" it remains null.
            EXISchema schema = null;
            
    // If using an XSD, it must be converted to an EXISchema.
            if(useSchema.equals("XSD")) {
                try {
                    InputSource is = new InputSource(schemaFileName);
                    EXISchemaFactory factory = new EXISchemaFactory();
                    schema = factory.compile(is); 
                }
                finally {
                }
            }
// If using an EXIG, it can be read directly.
            else if (useSchema.equals("EXIG")) {
                try {
                    fis = new FileInputStream(exiSchemaFileName);
                    schema = new EXISchemaReader().parse(fis);
                }
                finally {
                    if (fis != null) fis.close();
                }
            }
            else if (!"None".equals(useSchema))
              assert false;
            grammarCache = new GrammarCache(schema, options);

            
// Use the Grammar Cache to set the schema and grammar options for EXIReader.
            reader.setGrammarCache(grammarCache);

// Prepare to send the results from the transformer to a StringWriter object.
            transformerHandler.setResult(new StreamResult(stringWriter));
            
// Read the file into a byte array.
            byte fileContent[] = new byte[(int)inputFile.length()];
            in.read(fileContent);
            
// Assign the transformer handler to interpret XML content.
            reader.setContentHandler(transformerHandler);
            
// Parse the file information.
            reader.parse(new InputSource(new ByteArrayInputStream(fileContent)));

// Get the resulting string, write it to the output file, and flush the buffer contents.
            final String reconstitutedString;
            reconstitutedString = stringWriter.getBuffer().toString();
            out.write(reconstitutedString);
            out.flush();
        }
// Verify that the input and output files are closed.
        finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }
}
