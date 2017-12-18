package openexi.sample;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.openexi.proc.common.AlignmentType;
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
    public void encodeEXI(
        String sourceFile, 
        String destinationFile,
        String alignment,
        Boolean preserveComments,
        Boolean preservePIs,
        Boolean preserveDTD,
        Boolean preserveNamespace,
        Boolean preserveLexicalValues,
        Boolean preserveWhitespace,
        int blockSize,
        int maxValueLength,
        int maxValuePartitions
    ) 
        throws FileNotFoundException, IOException, ClassNotFoundException, TransmogrifierException,
        EXIOptionsException 
    {
        FileInputStream in = null;
        FileOutputStream out = null;
        GrammarCache grammarCache;
        Boolean debug = true;

// All EXI options can be stored in a single short integer. DEFAULT_OPTIONS=2.
        short options = GrammarOptions.DEFAULT_OPTIONS;
        try {

// Encoding always requires the same steps.
            
// 1. Instantiate a Transmogrifier
            Transmogrifier transmogrifier = new Transmogrifier();
            
            // Set alignment and compression
            if (alignment.equals("bitPacked"))
                    transmogrifier.setAlignmentType(AlignmentType.bitPacked);
            if (alignment.equals("compress"))
                    transmogrifier.setAlignmentType(AlignmentType.compress);
            if (alignment.equals("preCompress"))
                    transmogrifier.setAlignmentType(AlignmentType.preCompress);
            if(alignment.equals("byteAligned"))
                                transmogrifier.setAlignmentType(AlignmentType.byteAligned);

            // Set preservation preferences in Grammar Options
            if (preserveComments) options = GrammarOptions.addCM(options);
            if (preservePIs) options = GrammarOptions.addPI(options);
            if (preserveDTD) options = GrammarOptions.addDTD(options);
            if (preserveNamespace) options = GrammarOptions.addNS(options);

            // Set preservation preferences handled directly in the transmogrifier.
            transmogrifier.setPreserveLexicalValues(preserveLexicalValues);
            transmogrifier.setPreserveWhitespaces(preserveWhitespace);
            
            // Set the number of elements processed as a block.
            if (blockSize!=1000000) transmogrifier.setBlockSize(blockSize);
            
            // Set the maximum length for values stored in the String Table for reuse.
            if (maxValueLength>-1) transmogrifier.setValueMaxLength(maxValueLength);
            
            // Set the maximum number of values stored in the String Table.
            if (maxValuePartitions >-1) 
                transmogrifier.setValuePartitionCapacity(maxValuePartitions);
            
// 2. Initialize the input and output streams.
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destinationFile);
            
            if (debug) System.out.println(options);
            
// 3. Create a Grammar Cache based on the EXI options. This example uses no schema.
            grammarCache = new GrammarCache((EXISchema)null, options);
            
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
