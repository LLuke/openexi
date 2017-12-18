package openexi.sample;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactory;
import org.openexi.scomp.EXISchemaFactoryException;
import org.openexi.scomp.EXISchemaReader;

import org.xml.sax.InputSource;


// The Grammar Cache stores the XML schema and options used to encode an EXI file.

// The Transmogrifier performs the translation from XML to EXI format.


public class EncodeEXI {
    public EncodeEXI() {
        super();
    }

    public void encodeEXI(
        String sourceFile, 
        String destinationFile,
        String alignment,
        
//Preservation options
        Boolean preserveComments,
        Boolean preservePIs,
        Boolean preserveDTD,
        Boolean preserveNamespace,
        Boolean preserveLexicalValues,
        Boolean preserveWhitespace,
        int blockSize,
        int maxValueLength,
        int maxValuePartitions,
// Schema options
        String schemaFileName,
        String exiSchemaFileName,
        Boolean strict,
        String useSchema
    ) 
    throws FileNotFoundException, IOException, ClassNotFoundException, TransmogrifierException,
        EXIOptionsException, EXISchemaFactoryException
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

    // If using strict schema interpretation, set the options to STRICT_OPTIONS (1)
    // and move on. Other options are ignored.
            if (strict)
                {options = GrammarOptions.STRICT_OPTIONS;
            }
            else
            {
    // Otherwise, set preservation preferences in Grammar Options
                options = GrammarOptions.DEFAULT_OPTIONS;
                if (preserveComments) options = GrammarOptions.addCM(options);
                if (preservePIs) options = GrammarOptions.addPI(options);
                if (preserveDTD) options = GrammarOptions.addDTD(options);
                if (preserveNamespace) options = GrammarOptions.addNS(options);
            }

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
                        
// 3. Set the schema and EXI options in the Grammar Cache.
            FileInputStream fis = null;
            
    // Create a schema and set it to null. If useSchema == "None" it remains null.
            EXISchema schema = null;
            
    // If using an XSD, it must be converted as an EXISchema.
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
            
// 4. Set the configuration options in the Transmogrifier.
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
