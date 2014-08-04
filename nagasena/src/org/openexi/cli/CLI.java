/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openexi.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.sax.TransmogrifierException;
import org.openexi.scomp.EXISchemaFactoryException;

import org.xml.sax.SAXException;

/** 
 * A Command Line Interface for OpenEXI. Performs a single-file XML to EXI encode
 * or EXI-to-XML decode based on user-specified configuration options. Intended
 * as a utility for automating/scripting tasks.
 * 
 * @author hillbw
 */
public class CLI {
    
        private final CommandLineParser parser;
        private final Options opts;
        private CommandLine cmd;
        
        // encoding parameters
        String sourceFile;
        String destinationFile;
        String alignment;
        Boolean preserveComments;
        Boolean preservePIs;
        Boolean preserveDTD;
        Boolean preserveNamespace;
        Boolean preserveLexicalValues;
        Boolean preserveWhitespace;
        int blockSize;
        int maxValueLength;
        int maxValuePartitions;
        String schemaFileName;
        String exiSchemaFileName;
        Boolean strict;
        String useSchema;

    /**
     * Default constructor. Prepares the Command Line Interface prior to parsing
     * user inputs. Note that this creates a default set of
     * <a href="http://www.w3.org/TR/2014/REC-exi-20140211/#options">EXI
     * options</a>, so users need not specify values for all options.
     */
    @SuppressWarnings("static-access")
    public CLI(){
            
            // Initialize default encode/decode parameters
            sourceFile = null;
            destinationFile = null;
            alignment = "bitPacked";
            preserveComments = false;
            preservePIs = false;
            preserveDTD = false;
            preserveNamespace = true;
            preserveLexicalValues = false;
            preserveWhitespace = true;
            blockSize = 1000000;
            maxValueLength = -1;
            maxValuePartitions = -1;
            schemaFileName = null;
            exiSchemaFileName = null;
            strict = false;
            useSchema = "None";
            
            parser = new BasicParser();
            opts = new Options();
            
            //3 primary modes: Help/Encode/Decode. Only mandatory option
            OptionGroup modeOpts = new OptionGroup();
            modeOpts.addOption(OptionBuilder.withLongOpt("help")
                                            .withDescription("show this message")
                                            .hasArg(false)
                                            .create("h"));
            modeOpts.addOption(OptionBuilder.withLongOpt("encode")
                                            .withDescription("encode to EXI")
                                            .hasArgs(2)
                                            .withArgName("src-file, dst-file")
                                            .create("E"));
            modeOpts.addOption(OptionBuilder.withLongOpt("decode")
                                            .withDescription("decode to XML")
                                            .hasArgs(2)
                                            .withArgName("src-file, dst-file")
                                            .create("D"));
            modeOpts.setRequired(true);
            opts.addOptionGroup(modeOpts);

            // EXI compression and alignment options
            OptionGroup alignOpts = new OptionGroup();
            alignOpts.addOption(OptionBuilder.withLongOpt("bitpacked")
                                             .withDescription("alignment: bitpacked")
                                             .hasArg(false)
                                             .create("b"));
            alignOpts.addOption(OptionBuilder.withLongOpt("byte-aligned")
                                             .withDescription("alignment: byte-aligned")
                                             .hasArg(false)
                                             .create("B"));
            alignOpts.addOption(OptionBuilder.withLongOpt("pre-compress")
                                             .withDescription("alignment: pre-compress")
                                             .hasArg(false)
                                             .create("p"));
            alignOpts.addOption(OptionBuilder.withLongOpt("compress")
                                             .withDescription("alignment: compress")
                                             .hasArg(false)
                                             .create("c"));
            opts.addOptionGroup(alignOpts);

            
            // Handle fidelity options as one argument
            String fDesc = "set XML preservation options. " +
                           "Format: six comma separated 1/0 values in order:" +
                           "\n1. Preserve comments" +
                           "\n2. Preserve programming instructions" +
                           "\n3. Preserve DTD" +
                           "\n4. Preserve namespace" +
                           "\n5. Preserve lexical values" +
                           "\n6. Preserve whitespace" +
                           "\nExample: 0,0,0,1,1,0";                                                  

            opts.addOption(OptionBuilder.withLongOpt("fidelity")
                                        .withDescription(fDesc)
                                        .hasArgs(6)
                                        .withValueSeparator(',')
                                        .create("f"));

            opts.addOption(OptionBuilder.withDescription("block size for EXI compression")
                                        .withLongOpt("blocksize")
                                        .hasArg()
                                        .withArgName("integer")
                                        .create("s"));

            
            // String table options
            opts.addOption(OptionBuilder.withDescription("string table max length")
                                        .withLongOpt("maxlength")
                                        .hasArg()
                                        .withArgName("integer")
                                        .create("l"));

            opts.addOption(OptionBuilder.withDescription("string table max partitions")
                                        .withLongOpt("maxpartitions")
                                        .hasArg()
                                        .withArgName("integer")
                                        .create("P"));

            // Schema options
            opts.addOption(OptionBuilder.withDescription("strict schema interpretation")
                                        .withLongOpt("strict")
                                        .hasArg(false)
                                        .create("t"));

            OptionGroup schemaOpts = new OptionGroup();
            schemaOpts.addOption(OptionBuilder.withArgName("file")
                                              .withDescription("use XSD schema")
                                              .withLongOpt("xsd")
                                              .hasArg()
                                              .create("x"));

            schemaOpts.addOption(OptionBuilder.withArgName("file")
                                              .withDescription("use ESD schema")
                                              .withLongOpt("esd")
                                              .hasArg()
                                              .create("e"));
            opts.addOptionGroup(schemaOpts);
        }

    /**
     * Parses user inputs from the command line, sets EXI options accordingly, and calls the CLIDecoder 
     * or CLIEncoder class to perform the operation. 
     * 
     * @param args  Command line arguments
     * @return true if parsing is successful, false otherwise.
     */
    public Boolean parse(String[] args) {
        try {
                cmd = parser.parse(opts, args);
                
                // Help mode: print help and return
                if(cmd.hasOption("help")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp( "[-E|-D|-h] SOURCE DEST [OPTIONS]...\n" +
                                         "Example: cli -E in.xml out.exi -B " +
                                         "-f 1,0,0,1,0,1 -x sch.xsd", opts );
                    return true;
                }
                
                // process alignment mode
                if(cmd.hasOption("bitpacked")) {
                    alignment = "bitPacked";
                    System.out.println("Bitpacked mode set.");
                }
                else if (cmd.hasOption("compress")) {
                    alignment = "compress";
                    System.out.println("Compress mode set.");
                }
                else if (cmd.hasOption("byte-aligned")) {
                    alignment = "byteAligned";
                    System.out.println("Byte-aligned mode set.");
                }
                else if (cmd.hasOption("pre-compress")) {
                    alignment = "preCompress";
                    System.out.println("Pre-compress mode set.");
                } 
                else { 
                    alignment = "bitPacked";
                    System.out.println("Default mode set (Bitpacked).");
                }

                // process preservation options
                if (cmd.hasOption("preserve")) {
                    String[] flags = cmd.getOptionValues("preserve");
                    System.out.println("Preservation options set:");

                    preserveComments = Boolean.parseBoolean(flags[0]);
                    if (preserveComments){
                        System.out.println("  - Preserve comments");
                    }

                    preservePIs = Boolean.parseBoolean(flags[1]);
                    if (preservePIs){
                        System.out.println("  - Preserve PIs");
                    }

                    preserveDTD = Boolean.parseBoolean(flags[2]);
                    if (preserveDTD){
                        System.out.println("  - Preserve DTD");
                    }

                    preserveNamespace = Boolean.parseBoolean(flags[3]);
                    if (preserveNamespace){
                        System.out.println("  - Preserve namespace");
                    }

                    preserveLexicalValues = Boolean.parseBoolean(flags[4]);
                    if (preserveLexicalValues){
                        System.out.println("  - Preserve lexical values");
                    }

                    preserveWhitespace = Boolean.parseBoolean(flags[5]);             
                    if (preserveWhitespace){
                        System.out.println("  - Preserve whitespace");
                    }
                }

                if (cmd.hasOption("t")){
                    strict = true;
                    System.out.println("Using strict schema interpretation." +
                                        "(Other preservation options will be ignored.)");
                    if (cmd.hasOption("preserve")) {
                        String[] flags = cmd.getOptionValues("preserve");
                        preserveComments = Boolean.parseBoolean(flags[0]);
                        preservePIs = Boolean.parseBoolean(flags[1]);
                        preserveDTD = Boolean.parseBoolean(flags[2]);
                        preserveNamespace = Boolean.parseBoolean(flags[3]);
                        preserveLexicalValues = Boolean.parseBoolean(flags[4]);
                        preserveWhitespace = Boolean.parseBoolean(flags[5]);
                    }
                }

                //
                if (cmd.hasOption("s")){
                    blockSize = Integer.parseInt(cmd.getOptionValue("s"));
                    System.out.println("Block size = " + blockSize);
                }
                if (cmd.hasOption("l")){
                    maxValueLength = Integer.parseInt(cmd.getOptionValue("l"));
                    System.out.println("String table max length = " + maxValueLength);
                }
                if (cmd.hasOption("P")){
                    maxValuePartitions = Integer.parseInt(cmd.getOptionValue("P"));
                    System.out.println("String table max partitions = " + maxValuePartitions);
                }

                if (cmd.hasOption("xsd")){
                    useSchema = "XSD";
                    schemaFileName = cmd.getOptionValue("xsd");
                    System.out.println("Using XSD schema file " + schemaFileName);
                }

                if (cmd.hasOption("esd")){
                    useSchema = "ESD";
                    exiSchemaFileName = cmd.getOptionValue("esd");
                    System.out.println("Using ESD schema file " + exiSchemaFileName);
                }

                // process filenames
                if (cmd.hasOption("encode")) {
                    String[] fileNames = cmd.getOptionValues("encode");
                    sourceFile = fileNames[0];
                    destinationFile = fileNames[1];
                    System.out.println("Encoding " + fileNames[0] +
                                        " to " + fileNames[1]);

                    // perform encoding
                    CLIEncoder encode = new CLIEncoder();
                    try {
                        encode.encodeEXI(
                            sourceFile, 
                            destinationFile,
                            alignment,
                            preserveComments,
                            preservePIs,
                            preserveDTD,
                            preserveNamespace,
                            preserveLexicalValues,
                            preserveWhitespace,
                            blockSize,
                            maxValueLength,
                            maxValuePartitions,
                            schemaFileName,
                            exiSchemaFileName,
                            strict,
                            useSchema
                        );
                    } catch (ClassNotFoundException ex) {
                        System.out.println("Encoding failed: " +  ex.getMessage());
                    } catch (FileNotFoundException ex) {
                        System.out.println("Encoding failed: " +  ex.getMessage());
                    } catch (IOException ex) {
                        System.out.println("Encoding failed: " +  ex.getMessage());
                    } catch (EXIOptionsException ex) {
                        System.out.println("Encoding failed: " +  ex.getMessage());
                    } catch (TransmogrifierException ex) {
                        System.out.println("Encoding failed: " +  ex.getMessage());
                    } catch (EXISchemaFactoryException ex) {
                        System.out.println("Encoding failed: " +  ex.getMessage());
                    }    
                }


                if (cmd.hasOption("decode")) {
                    String[] fileNames = cmd.getOptionValues("decode");
                    sourceFile = fileNames[0];
                    destinationFile = fileNames[1];
                    System.out.println("Decoding " + fileNames[0] +
                                        " to " + fileNames[1]);

                    CLIDecoder decode = new CLIDecoder();
                    try {
                        decode.decodeEXI(
                            sourceFile, 
                            destinationFile,
                            alignment,
                            preserveComments,
                            preservePIs,
                            preserveDTD,
                            preserveNamespace,
                            preserveLexicalValues,
                            blockSize,
                            maxValueLength,
                            maxValuePartitions,
                            schemaFileName,
                            exiSchemaFileName,
                            strict,
                            useSchema);

                    } catch (ClassNotFoundException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    } catch (FileNotFoundException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    } catch (IOException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    } catch (EXIOptionsException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    } catch (EXISchemaFactoryException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    } catch (SAXException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    } catch (TransformerConfigurationException ex) {
                        System.out.println("Decoding failed: " +  ex.getMessage());
                        return false;
                    }
                }
                return true;
            } catch (ParseException ex) {
                System.out.println("Command line parsing failed: " + ex);
                return false;
            }
        }
        
       
}
