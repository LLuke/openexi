package openexi.sample;

import java.io.ByteArrayInputStream;
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

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.schema.EXISchema;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class DecodeEXI {
    public DecodeEXI() {
        super();
    }

// This first example uses default options, so only a source file and destination file are required.
    public void decodeEXI(String sourceFile, String destinationFile) throws FileNotFoundException, IOException,
        SAXException, EXIOptionsException, TransformerConfigurationException {

        FileInputStream in = null;
        Writer out = null;
        StringWriter stringWriter = new StringWriter();

// The Grammar Cache stores schema and EXI options information. The settings nust match when encoding
// and subsequently decoding a data set.
        GrammarCache grammarCache;

        try {
            
// Standard SAX methods parse content and lexical values.
            SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();

// EXIReader infers and reconstructs the XML file structure.
            EXIReader reader = new EXIReader();
            
            File inputFile = new File(sourceFile);
            in = new FileInputStream(inputFile);
            out = new OutputStreamWriter(new FileOutputStream(destinationFile), "UTF-8");

// Create a Grammar Cache based on the schema (null, in this case) and the default EXI Options
            grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);
            
// Set the Grammar Cache to EXIReader.
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
