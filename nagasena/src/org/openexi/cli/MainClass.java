package org.openexi.cli;

/**
 * The entry class for the OpenEXI Command Line Interface
 * 
 * @author hillbw
 */
public class MainClass 
{
    /**
     *
     * @param args
     */
    public static void main(String[] args){
        
        CLI cli = new CLI();
        if(args.length > 0) {
            cli.parse(args);   
        }
        else {
            String[] help = new String[1];
            help[0] = "-h";
            cli.parse(help);  //default to help message
        }        
    }
}
