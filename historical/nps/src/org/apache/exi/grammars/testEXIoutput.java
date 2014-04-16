/**
 * Copyright 2010 Naval Postgraduate School
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.exi.grammars;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.exi.core.byteToHex;
import org.apache.exi.core.headerOptions.HeaderPreserveRules;
import org.apache.exi.io.EXI_ByteAlignedInput;
import org.apache.exi.io.EXI_ByteAlignedOutput;
import org.apache.exi.io.EXI_OutputStreamIface;
import org.apache.exi.io.EXI_abstractInput;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class testEXIoutput {

    static GrammarRunner grammars;
    static boolean CM = false;
    static boolean PI = false;
    static boolean DE = false;
    static boolean NS = false;
    static boolean verboseGrammar = false;
    static boolean makeTXT = false;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {

        /**
         * use these to set all preservation rules....
         *
         * Will encdoe with my code, then try to SeimensDecodeMyEXI_XML with seimens
         */
        CM = false;
        PI = false;
        DE = false;
        NS = true;
        
        verboseGrammar = true;
        makeTXT = true;
        boolean doSeimens = false;
        boolean printTables = false;

        HeaderPreserveRules.PRESERVE_CM.setPreserved(CM);
        HeaderPreserveRules.PRESERVE_PI.setPreserved(PI);
        HeaderPreserveRules.PRESERVE_DTD_ENTITY.setPreserved(DE);
        HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.setPreserved(NS);


        // seimens SeimensDecodeMyEXI_XML of my inputXML exi
        String SeimensDecodeMyEXI_XML = "sampleOutput/_mySeimensDecoded.xml";
        // my exi file
        String saveMyEXI = "sampleOutput/_myBYTEexi.exi";
        // seimens exi file
        String seimensEncodeEXI = "sampleOutput/_seimensEXI.exi";
        // my XML output decoded from EXI
        String myXMLoutput = "sampleOutput/_myXMLDecoded.xml";

        // the initial inputXML xml file
        String inputXML = "sampleXML/notebook.xml";
//        String inputXML = "sampleXML/namespaceExample.xml";
//        String inputXML = "sampleXML/comment.xml";
//        String inputXML = "sampleXML/pi.xml";
//        String inputXML = "sampleXML/repeat.xml";
//        String inputXML = "sampleXML/otherNoSchema.xml";
//        String inputXML = "sampleXML/multipleNS.xml";
//        String inputXML = "sampleXML/customers.xml";
//        String inputXML = "sampleXML/dup.xml";
//        String inputXML = "sampleXML/box.x3d";
//        String inputXML = "sampleXML/HelloWorld.x3d";
        
/** PROBLEM AREAS FILES */
//        String inputXML = "sampleXML/box_mod.xml";
//        String inputXML = "sampleXML/future001.xml";
//        String inputXML = "sampleXML/RHIB.x3d";
//        String inputXML = "sampleXML/LT_switch_mod.x3d";
//        String inputXML = "sampleXML/LARGEbasicXML.xml";


        

        





        try {
            /** Write EXI byte aligned */
            writeMyEXI(inputXML, saveMyEXI);
            /** Print the string tables created in my encoding process */
            if (printTables) {
                printTables();
            }
            writeMyXML(myXMLoutput, saveMyEXI);            



        } catch (Exception e) { // catch anything and everything...not sure what to expect
            System.out.println("***RUNNER ERROR*****\n" + e);
        }

        System.out.println("\n\n");
    }



    /**
     *
     * @param inputXML - inputXML XML File
     * @param inputEXI - output EXI file
     */
//    public static void encodeWithSeimens(String inputXML, String outputEXI){
//        try{
//            System.out.println("\n****ENCODING WITH SEIMENS TO EXI (" + inputXML + ")****");
//
//            EXIFactory exiFactory = DefaultEXIFactory.newInstance();
//            exiFactory.setCodingMode(CodingMode.BYTE_PACKED);
//            FidelityOptions fo = FidelityOptions.createDefault();
//            if (CM) {
//                fo.setFidelity(FidelityOptions.FEATURE_COMMENT, true);
//            }
//            if (DE) {
//                fo.setFidelity(FidelityOptions.FEATURE_DTD, true);
//            }
//            if (NS) {
//                fo.setFidelity(FidelityOptions.FEATURE_PREFIX, true);
//            }
//            if (PI) {
//                fo.setFidelity(FidelityOptions.FEATURE_PI, true);
//            }
//            exiFactory.setFidelityOptions(fo);
//
//            FileOutputStream exiOut = new FileOutputStream(outputEXI);
//            EXIResult saxResult = new EXIResult(exiOut, exiFactory);
//
//            XMLReader parser = XMLReaderFactory.createXMLReader();
//            parser.setContentHandler(saxResult.getHandler());
//            parser.parse(new InputSource(inputXML));
//            exiOut.flush();
//            exiOut.close();
//
//            if(makeTXT)
//                makeByteTXT(outputEXI);
//            System.out.println("\tDone Encode with Seimens!!!");
//        }
//        catch(Exception e){
//            System.out.println("ERROR ENCODING WITH SEIMENS\n" + e);
//        }
//    }
//
//
//    public static void seimensEncodeSimple(String inputXML, String outputEXI, String schema){
//        try{
//            // create EXI instance
//            EXIFactory exiFactory = DefaultEXIFactory.newInstance();
//            // set output mode
//            exiFactory.setCodingMode(CodingMode.COMPRESSION);
//            // create Grammar processor
//            GrammarFactory grammarFactory = GrammarFactory.newInstance();
//            Grammar g = grammarFactory.createGrammar(schema);
//            exiFactory.setGrammar(g);
//            // IO files
//            FileOutputStream exiOut = new FileOutputStream(outputEXI);
//            EXIResult saxResult = new EXIResult(exiOut, exiFactory);
//            XMLReader parser = XMLReaderFactory.createXMLReader();
//            // parse and write EXI from XML
//            parser.setContentHandler(saxResult.getHandler());
//            parser.parse(new InputSource(inputXML));
//            exiOut.flush();
//            exiOut.close();
//        }
//        catch(Exception e){
//            System.out.println("ERROR ENCODING WITH SEIMENS\n" + e);
//        }
//    }


//    public static void seimensDecodeSimple(String inputEXI, String outputXML){
//        try {
//            EXIFactory exiFactory = DefaultEXIFactory.newInstance();
//            exiFactory.setCodingMode(CodingMode.COMPRESSION);
//            EXISource saxSource = new EXISource(exiFactory);
//            XMLReader xmlReader = saxSource.getXMLReader();
//            FileInputStream exiIn = new FileInputStream(inputEXI);
//            FileOutputStream exiOutXml = new FileOutputStream(outputXML);
//            TransformerFactory tf = TransformerFactory.newInstance();
//            Transformer transformer = tf.newTransformer();
//            SAXSource exiSource = new SAXSource(new InputSource(exiIn));
//            exiSource.setXMLReader(xmlReader);
//            transformer.transform(exiSource, new StreamResult(exiOutXml));
//            exiIn.close();
//            exiOutXml.flush();
//            exiOutXml.close();
//        }
//        catch(Exception e){
//            System.out.println("ERROR DECODING MINE WITH SEIMENS\n" + e);
//        }
//    }

    /**
     *
     * @param inputEXI - inputXML EXI file
     * @param outputXML - output XML file
     */
//    public static void decodeWithSeimens(String inputEXI, String outputXML){
//        try {
//            System.out.println("\n****DECODING WITH SEIMENS TO XML (" + inputEXI + ")****");
//            EXIFactory exiFactory = DefaultEXIFactory.newInstance();
//            exiFactory.setCodingMode(CodingMode.BYTE_PACKED);
//            FidelityOptions fo = FidelityOptions.createDefault();
//            if (CM) {
//                fo.setFidelity(FidelityOptions.FEATURE_COMMENT, true);
//            }
//            if (DE) {
//                fo.setFidelity(FidelityOptions.FEATURE_DTD, true);
//            }
//            if (NS) {
//                fo.setFidelity(FidelityOptions.FEATURE_PREFIX, true);
//            }
//            if (PI) {
//                fo.setFidelity(FidelityOptions.FEATURE_PI, true);
//            }
//            exiFactory.setFidelityOptions(fo);
//
//            EXISource saxSource = new EXISource(exiFactory);
//            XMLReader xmlReader = saxSource.getXMLReader();
//            FileInputStream exiIn = new FileInputStream(inputEXI);
//            FileOutputStream exiOutXml = new FileOutputStream(outputXML);
//            TransformerFactory tf = TransformerFactory.newInstance();
//            Transformer transformer = tf.newTransformer();
//            SAXSource exiSource = new SAXSource(new InputSource(exiIn));
//            exiSource.setXMLReader(xmlReader);
//            transformer.transform(exiSource, new StreamResult(exiOutXml));
//            exiIn.close();
//            exiOutXml.flush();
//            exiOutXml.close();
//            System.out.println("\tDone decoding with Seimens!!!!");
//        }
//        catch(Exception e){
//            System.out.println("ERROR DECODING MINE WITH SEIMENS\n" + e);
//        }
//
//    }


    public static void printTables(){
        try{
            System.out.println("\n****THE RESULTING STRING TABLES****");
            grammars.NStables.prettyPrint();
            System.out.println("Done printing tables!!!!");
        }
        catch(Exception e){
            System.out.println("ERROR STRING TABLE PRINT\n" + e);
        }
    }



    public static void makeByteTXT(String inputEXI){
        try{
            System.out.println("\tMaking ByteTxt of " + inputEXI + " ****");
            byteToHex.makeByteToHex(inputEXI);
            System.out.println("\tDone making txt!!!!");
        }
        catch(Exception e){
            System.out.println("ERROR BYTE TEXT CONVERT\n" + e);
        }
    }



    public static void writeMyXML(String outXML, String inputEXI){
        System.out.println("\n****DECODING WITH MINE to XML (" + inputEXI + " -> " + outXML + ")****");
        try{
            EXI_abstractInput xmlOUT = new EXI_ByteAlignedInput(new FileInputStream(new File(inputEXI)), inputEXI);

            grammars = new GrammarRunner(outXML, xmlOUT, verboseGrammar);
            xmlOUT.cleanAndClose();
            System.out.println("\tDone my XML write!!!");
        }
        catch(Exception e){
            System.out.println(e + "\nERROR WRITING XML");
        }
    }

    
    public static void writeMyEXI(String inputXML, String outputEXI){
        System.out.println("\n****ENCODING WITH MINE to EXI (" + inputXML + ")****");
        try {
            /**Encode to EXI using my code*/
            EXI_OutputStreamIface exiOUT = new EXI_ByteAlignedOutput(new FileOutputStream(new File(outputEXI)), outputEXI);
            // Grammar
            grammars = new GrammarRunner(inputXML, exiOUT, verboseGrammar);
            // clean house
            exiOUT.cleanAndClose();

            if(makeTXT)
                makeByteTXT(outputEXI);
            
            System.out.println("\tDone my EXI write!!!");
        }
        catch(Exception e){
            System.out.println("ERROR WRITING MY EXI\n" + e);
        }
    }


    /**
     *
     * @param inEXI
     * @param align
     *      1 = Byte
     *      2 = Compress
     *      3 = bit
     */
//    public static void decodeAnyEXI(String inEXI, int align,
//            boolean CMb, boolean DEb, boolean NSb, boolean PIb){
//        try {
//            System.out.println("\n****ANY FILE DECODING WITH SEIMENS TO XML ANY FILE(" + inEXI + ")****");
//            EXIFactory exiFactory = DefaultEXIFactory.newInstance();
//            if(align == 0)
//                exiFactory.setCodingMode(CodingMode.BYTE_PACKED);
//            else if(align == 1)
//                exiFactory.setCodingMode(CodingMode.COMPRESSION);
//            else
//                exiFactory.setCodingMode(CodingMode.BIT_PACKED);
//
////            exiFactory.setCodingMode(CodingMode.BYTE_PACKED);
//            FidelityOptions fo = FidelityOptions.createDefault();
//            if (CMb) {
//                fo.setFidelity(FidelityOptions.FEATURE_COMMENT, true);
//            }
//            if (DEb) {
//                fo.setFidelity(FidelityOptions.FEATURE_DTD, true);
//            }
//            if (NSb) {
//                fo.setFidelity(FidelityOptions.FEATURE_PREFIX, true);
//            }
//            if (PIb) {
//                fo.setFidelity(FidelityOptions.FEATURE_PI, true);
//            }
//            exiFactory.setFidelityOptions(fo);
//
//            EXISource saxSource = new EXISource(exiFactory);
//            XMLReader xmlReader = saxSource.getXMLReader();
//            FileInputStream exiIn = new FileInputStream(inEXI);
//            FileOutputStream exiOutXml = new FileOutputStream(inEXI+".xml");
//            TransformerFactory tf = TransformerFactory.newInstance();
//            Transformer transformer = tf.newTransformer();
//            SAXSource exiSource = new SAXSource(new InputSource(exiIn));
//            exiSource.setXMLReader(xmlReader);
//            transformer.transform(exiSource, new StreamResult(exiOutXml));
//            exiIn.close();
//            exiOutXml.flush();
//            exiOutXml.close();
//            System.out.println("\tDone decoding ALL FILES with Seimens!!!!");
//        }
//        catch(Exception e){
//            System.out.println("ERROR DECODING --ANY-- WITH SEIMENS ALL\n" + e);
//        }
//
//    }

    
}
