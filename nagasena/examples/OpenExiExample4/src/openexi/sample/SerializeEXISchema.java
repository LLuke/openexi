package openexi.sample;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactory;
import org.openexi.scomp.EXISchemaFactoryException;
import org.openexi.scomp.EXISchemaWriter;

import org.xml.sax.InputSource;


// EXISchemaFactory is used to compile an EXI schema.


public class SerializeEXISchema {
    public SerializeEXISchema() {
        super();
    }

// The only arguments required are the source and target file names.
    public void serializeEXISchema(
        String xsdFileName,
        String exigFileName
    ) throws IOException, EXISchemaFactoryException
    {
        EXISchemaFactory factory = new EXISchemaFactory();
        EXISchema newSchema;
        FileInputStream fis;
        InputSource is;
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
// Convert the input file to an input source.
            fis = new FileInputStream(xsdFileName);
            is = new InputSource(fis);
            is.setSystemId(new File(xsdFileName).toString());
// Compile a new schema.
            newSchema = factory.compile(is);
// Write the results to a file.
            fos = new FileOutputStream(exigFileName);
            new EXISchemaWriter().serialize(newSchema, fos);
            fos.flush();
        } finally {
            if (oos != null) oos.close();
            if (fos != null) fos.close();
        }
        
    }
}
