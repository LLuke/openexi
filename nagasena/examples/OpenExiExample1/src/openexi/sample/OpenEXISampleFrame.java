package openexi.sample;

// AWT helps keep the UI simple.
import java.awt.Button;
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

import javax.swing.JFrame;

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
        button_encode.setBounds(new Rectangle(10, 80, 200, 30));
        button_encode.setFont(new Font("Tahoma", 0, 14));
        button_encode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                button_encode_actionPerformed(e);
            }
        });
        button_decode.setLabel("Decode");
        button_decode.setBounds(new Rectangle(235, 80, 200, 30));
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
            
// This example uses default settings, so all that the method requires is XML source and encoded destination file names.             
            encode.encodeEXI(textField_sourceFile.getText(), textField_destinationFile.getText());
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
// This example uses default settings, so all that the method requires is EXI source and decoded destination file names.                         
            decode.decodeEXI(textField_sourceFile.getText(), textField_destinationFile.getText());
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
