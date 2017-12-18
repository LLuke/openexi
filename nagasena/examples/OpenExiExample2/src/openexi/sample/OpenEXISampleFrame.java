package openexi.sample;

// AWT helps keep the UI simple.
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.ButtonGroup;

import javax.swing.JRadioButton;

import javax.xml.transform.TransformerConfigurationException;

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.sax.TransmogrifierException;

import org.xml.sax.SAXException;


// Catching errors here allows "finally" to verify files are closed in the methods called.


// Catch and report exceptions from the methods called.


public class OpenEXISampleFrame extends JFrame {
    
    // Set to true for verbose error messages.
    private Boolean debug = false; 

// Create UI components.    
    private Label label_sourceFile = new Label();
    private Label label_desinationFile = new Label();
    private TextField textField_sourceFile = new TextField();
    private TextField textField_destinationFile = new TextField();
    private Button button_encode = new Button();
    private Button button_decode = new Button();
    private Label label_results = new Label();
    private Button button_browse = new Button();
    private Label label_alignment = new Label();
    private Checkbox checkbox_bitPacked = new Checkbox();
    private Checkbox checkbox_byteAligned = new Checkbox();
    private Checkbox checkbox_preCompress = new Checkbox();
    private Checkbox checkbox_compress = new Checkbox();
    private CheckboxGroup checkboxgroup_align = new CheckboxGroup();
    public OpenEXISampleFrame() {
        try {
            jbInit();
        } catch (Exception e) {
            if (debug) e.printStackTrace();
        }
    }

// Initialize UI components.
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(null);
        this.setSize(new Dimension(455, 600));
        this.setTitle("OpenEXI Sandbox");

// The frame is large, but subsequent examples will require additional controls.
        this.setBounds(new Rectangle(10, 10, 455, 600));
        label_sourceFile.setText("Source File:");
        label_sourceFile.setBounds(new Rectangle(10, 10, 100, 25));
        label_sourceFile.setFont(new Font("Tahoma", 0, 14));
        label_desinationFile.setText("Destination File:");
        label_desinationFile.setBounds(new Rectangle(10, 40, 105, 20));
        label_desinationFile.setFont(new Font("Tahoma", 0, 14));
        textField_sourceFile.setBounds(new Rectangle(120, 10, 225, 25));
        textField_sourceFile.setText("test.xml");
        textField_sourceFile.setFont(new Font("Tahoma", 0, 12));
        textField_destinationFile.setBounds(new Rectangle(120, 40, 225, 25));
        textField_destinationFile.setText("test.exi");
        textField_destinationFile.setFont(new Font("Tahoma", 0, 12));
        button_encode.setLabel("Encode");
        button_encode.setBounds(new Rectangle(10, 75, 200, 30));
        button_encode.setFont(new Font("Tahoma", 0, 14));
        button_encode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                button_encode_actionPerformed(e);
            }
        });
        button_decode.setLabel("Decode");
        button_decode.setBounds(new Rectangle(235, 75, 200, 30));
        button_decode.setFont(new Font("Tahoma", 0, 14));
        button_decode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                button_decode_actionPerformed(e);
            }
        });
        label_results.setText("Results display here.");
        label_results.setBounds(new Rectangle(10, 540, 420, 25));
        button_browse.setLabel("Browse...");
        button_browse.setBounds(new Rectangle(350, 10, 85, 25));
        button_browse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                button_browse_actionPerformed(e);
            }
        });
        label_alignment.setText("Alignment");
        label_alignment.setBounds(new Rectangle(10, 115, 70, 20));
        label_alignment.setFont(new Font("Tahoma", 0, 14));
        checkbox_bitPacked.setLabel("bitPacked");
        checkbox_bitPacked.setBounds(new Rectangle(365, 110, 70, 35));
        checkbox_bitPacked.setCheckboxGroup(checkboxgroup_align);
        checkbox_bitPacked.setState(true);
        checkbox_byteAligned.setLabel("byteAligned");
        checkbox_byteAligned.setBounds(new Rectangle(275, 110, 80, 35));
        checkbox_byteAligned.setCheckboxGroup(checkboxgroup_align);
        checkbox_preCompress.setLabel("preCompress");
        checkbox_preCompress.setBounds(new Rectangle(175, 110, 90, 35));
        checkbox_preCompress.setCheckboxGroup(checkboxgroup_align);
        checkbox_compress.setLabel("compress");
        checkbox_compress.setBounds(new Rectangle(90, 110, 75, 35));
        checkbox_compress.setCheckboxGroup(checkboxgroup_align);
        this.getContentPane().add(checkbox_compress, null);
        this.getContentPane().add(checkbox_preCompress, null);
        this.getContentPane().add(checkbox_byteAligned, null);
        this.getContentPane().add(checkbox_bitPacked, null);
        this.getContentPane().add(label_alignment, null);
        this.getContentPane().add(button_browse, null);
        this.getContentPane().add(label_results, null);
        this.getContentPane().add(button_decode, null);
        this.getContentPane().add(button_encode, null);
        this.getContentPane().add(textField_destinationFile, null);
        this.getContentPane().add(textField_sourceFile, null);
        this.getContentPane().add(label_desinationFile, null);
        this.getContentPane().add(label_sourceFile, null);
    }

// Encode XML to EXI format.
    private void button_encode_actionPerformed(ActionEvent e) {
        label_results.setText("");

// Instantiate the example class EncodeEXI.
        EncodeEXI encode = new EncodeEXI();
        try {
            
// This example adds a string argument to set alignment and compression..             
            encode.encodeEXI(
                textField_sourceFile.getText(), 
                textField_destinationFile.getText(),
                checkboxgroup_align.getSelectedCheckbox().getLabel()
            );
            label_results.setText("Encoded " + textField_destinationFile.getText() + ".");
        } catch (ClassNotFoundException f) {
            if (debug)
                label_results.setText(f.toString());
        } catch (FileNotFoundException f) {
            label_results.setText("File not found.");
            if (debug) f.printStackTrace();
        } catch (IOException f) {
            if (debug)
                label_results.setText(f.toString());
        } catch (EXIOptionsException f) {
            if (debug)
                label_results.setText(f.toString());
        } catch (TransmogrifierException f) {
            if (debug)
                label_results.setText(f.toString());
         } 
    }

// Decode EXI to XML format.
    private void button_decode_actionPerformed(ActionEvent e) {
        label_results.setText("");

// Instantiate the example class DecodeEXI
        DecodeEXI decode = new DecodeEXI();
        
        try {
// This example adds a string argument to set alignment and compression..             
            decode.decodeEXI(
                textField_sourceFile.getText(), 
                textField_destinationFile.getText(),
                checkboxgroup_align.getSelectedCheckbox().getLabel()
            );
            label_results.setText("Decoded to " + textField_destinationFile.getText() + ".");
        } catch (FileNotFoundException f) {
            label_results.setText("File not found.");
            if (debug) f.printStackTrace();        
        } catch (IOException f) {
            if (debug)
                label_results.setText(f.toString());
        } catch (TransformerConfigurationException f) {
            if (debug)
                label_results.setText(f.toString());
        } catch (EXIOptionsException f) {
            if (debug)
                label_results.setText(f.toString());
        } catch (SAXException f) {
            if (debug)
                label_results.setText(f.toString());
        }
    }

// The File Dialog avoids the hassle of typos in the file name.
    private void button_browse_actionPerformed(ActionEvent e) {
        label_results.setText("");
        FileDialog fd = new FileDialog(this);
        String fileName;
        String extension;
        fd.setVisible(true);

        textField_sourceFile.setText(fd.getDirectory() + fd.getFile());
        
// For convenience, and to help protect original source files, the method provides suggested 
// destination file extensions. Users are free to change the values.
        fileName = fd.getFile();
        extension = fileName.substring(fileName.lastIndexOf("."));
        extension = extension.equals(".xml") ? "_encode.exi": "_decode.xml";
        textField_destinationFile.setText(
            fd.getDirectory() 
            + fileName.substring(0,fileName.lastIndexOf(".")) 
            + extension
        );
    }
}
