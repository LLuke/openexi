package org.openexi.cli;

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

import org.xml.sax.InputSource;

/**
 * Class to perform XML to EXI encoding using a set of EXI Options parameters.
 * Supports optional use of ESD or XSD schema files. Code adapted from work by
 * <a href="http://openexi.sourceforge.net/tutorial/">Fujitsu
 * Labs</a>.
 * 
 * @author hillbw
 */

public class CLIEncoder {

    /**
     * Default constructor.
     */
    public CLIEncoder() {
        super();
    }

    /**
     * Encode a single source XML file to a single destination EXI file.
     * 
     * @param sourceFile
     * @param destinationFile
     * @param alignment
     * @param preserveComments
     * @param preservePIs
     * @param preserveDTD
     * @param preserveNamespace
     * @param preserveLexicalValues
     * @param preserveWhitespace
     * @param blockSize
     * @param maxValueLength
     * @param maxValuePartitions
     * @param schemaFileName
     * @param exiSchemaFileName
     * @param strict
     * @param useSchema
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws TransmogrifierException
     * @throws EXIOptionsException
     * @throws EXISchemaFactoryException
     */
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
        int maxValuePartitions,
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
        //Boolean debug = true;

        // All EXI options can be stored in a single short integer. DEFAULT_OPTIONS=2.
        short options = GrammarOptions.DEFAULT_OPTIONS;
        try {
            
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
            ObjectInputStream ois = null;
            
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
            // If using an ESD, it can be read directly.            
            if (useSchema.equals("ESD")) {
                try {
                    fis = new FileInputStream(exiSchemaFileName);
                    ois = new ObjectInputStream(fis);
                    schema = (EXISchema)ois.readObject();
                }
                finally {
                    if (fis != null) fis.close();
                    if (ois != null) ois.close();
                }
            }
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
