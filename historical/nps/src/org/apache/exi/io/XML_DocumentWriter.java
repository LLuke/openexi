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
package org.apache.exi.io;

import java.io.PrintWriter;
import java.util.Stack;
import org.apache.exi.grammars.events.EventAbstract;
import org.apache.exi.grammars.events.EventAttribute;
import org.apache.exi.grammars.events.EventCharacters;
import org.apache.exi.grammars.events.EventComment;
import org.apache.exi.grammars.events.EventEndElement;
import org.apache.exi.grammars.events.EventNamespace;
import org.apache.exi.grammars.events.EventProcessingInstruction;
import org.apache.exi.grammars.events.EventStartElement;
import org.apache.exi.grammars.events.EventType;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 * PrintWriter(String fileName)

 */
public class XML_DocumentWriter implements XML_OutputStreamIface
{
    PrintWriter xmlOutWriter = null;
    boolean writingElement = false;
    private Stack <String> elementStack = new Stack<String>();


    public XML_DocumentWriter(String out){
        try{
            
            xmlOutWriter = new PrintWriter(out);
            defaultXMLHeader();
        }
        catch(Exception e){
            System.out.println("XML_DocumentWriter Constructor ERROR");
        }
    }
    private void writeElement(EventStartElement se) {
        if(writingElement)
            xmlOutWriter.println(">");        
        String name = se.getQName();
        elementStack.push(name);
        writingElement = true;
        xmlOutWriter.print("<" + name);
        flush();
    }
    private void writeAttribute(EventAttribute evt) {
        String name = evt.getQName();
        String value  = evt.getValue();
        xmlOutWriter.print(" " + name + "=\"" + value + "\"");
        flush();
    }
    private void writeProcessingInstruciton(EventProcessingInstruction evt) {
        //<?xml-stylesheet type="text/css" href="bike.css" ?>
        String target = evt.getTarget();
        String data = evt.getData();
        
        if(writingElement){
            xmlOutWriter.print(">");
            writingElement = false;
        }
        xmlOutWriter.print("<?" + target + " " + data + "?>");
        flush();
    }
    private void writeNamespace(EventNamespace evt) {
        //xmlns:mod="https://jacksonelect.com/models"
        String prefix = evt.getPrefix();
        String uri = evt.getUri();
        xmlOutWriter.print("xmlns:" + prefix + "=\""+ uri + "\"");
        flush();
    }   
    private void writeCharacters(EventCharacters evt) {
        String value = evt.getValue();
        if(writingElement){
            xmlOutWriter.print(">");
            writingElement = false;
        }        
        xmlOutWriter.print(value);
        flush();
    }
    private void writeComment(EventComment evt) {
        String value = evt.getValue();
        if(writingElement){
            xmlOutWriter.println(">");
            writingElement = false;
        }
        String s = "<!--" + value + "-->";
        xmlOutWriter.println(s);
        flush();
    }
    private void writeEndElement(EventEndElement evt) {
        String name = elementStack.pop();
        writingElement = false;
        if(evt.isGramTrans())
            xmlOutWriter.println("</" + name + ">");
        else
            xmlOutWriter.println("/>");
        flush();
    }








    public void cleanAndClose() {
        flush();
        xmlOutWriter.close();
    }
    private void flush(){
        xmlOutWriter.flush();
    }
    public void defaultXMLHeader() {
        String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        xmlOutWriter.println(s);
        flush();
    }
    public void testing(String s) {
        xmlOutWriter.println(s);
        flush();
    }
    public void writeEvent(EventAbstract evt) {
        EventType evtType = evt.getEventType();
        
        if(evtType.equals(EventType.AT))
            this.writeAttribute((EventAttribute)evt);
        if(evtType.equals(EventType.CH))
            this.writeCharacters((EventCharacters)evt);
        if(evtType.equals(EventType.CM))
            this.writeComment((EventComment)evt);
        if(evtType.equals(EventType.SE))
            this.writeElement((EventStartElement)evt);
        if(evtType.equals(EventType.EE))
            this.writeEndElement((EventEndElement)evt);
        if(evtType.equals(EventType.NS))
            this.writeNamespace((EventNamespace)evt);
        if(evtType.equals(EventType.PI))
            this.writeProcessingInstruciton((EventProcessingInstruction)evt);
    }
}