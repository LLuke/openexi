package openexi.sample;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;

import org.xml.sax.InputSource;


// The Grammar Cache stores the XML schema and options used to encode an EXI file.

// The Transmogrifier performs the translation from XML to EXI format.


public class EncodeEXI {
    public EncodeEXI() {
        super();
    }

// This example uses default options and no schema. Only the source and destination file names are required.
    public void encodeEXI(String sourceFile, String destinationFile) 
        throws FileNotFoundException, IOException, ClassNotFoundException, TransmogrifierException,
        EXIOptionsException 
    {
        FileInputStream in = null;
        FileOutputStream out = null;
        GrammarCache grammarCache;

        try {

// Encoding always requires the same steps.
            
// 1. Instantiate a Transmogrifier
            Transmogrifier transmogrifier = new Transmogrifier();
            
// 2. Initialize the input and output streams.
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destinationFile);
            
// 3. Create a Grammar Cache. This example uses default options and no schema.
            grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);
            
// 4. Set the configuration options in the Transmogrifier. Later examples will show more possible settings.
            transmogrifier.setGrammarCache(grammarCache);
            
// 5. Set the output stream.
            transmogrifier.setOutputStream(out);

// 6. Encode the input stream.
            transmogrifier.encode(new InputSource(in));
        }

// 7.  Verify that the streams are closed.
        finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }
}
