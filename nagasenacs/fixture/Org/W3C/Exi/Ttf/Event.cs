using System;

/*
 * EXI Testing Task Force Measurement Suite: http://www.w3.org/XML/EXI/
 *
 * Copyright [2006] World Wide Web Consortium, (Massachusetts Institute of
 * Technology, European Research Consortium for Informatics and Mathematics,
 * Keio University). All Rights Reserved. This work is distributed under the
 * W3C Software License [1] in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

namespace Org.W3C.Exi.Ttf {

  /// <summary>
  /// Common class to record a reading/writing 'event'.
  /// </summary>
  public class Event {
      public const short START_ELEMENT = 1;
      public const short END_ELEMENT = 2;
      public const short CHARACTERS = 3;
      public const short NAMESPACE = 4;
      public const short END_NAMESPACE = 5;
      public const short ATTRIBUTE = 6;
      public const short COMMENT = 7;
      public const short PROCESSING_INSTRUCTION = 8;
      public const short UNEXPANDED_ENTITY = 9;
      public const short DOCTYPE = 10;
      public const short END_DTD = 12;
      public const short NOTATION = 13;
      public const short UNPARSED_ENTITY = 14;
      public const short EXTERNAL_ENTITY = 15;

      public const short DATATYPE_NONE = 0;
      public const short DATATYPE_STRING = 1;
      public const short DATATYPE_CHAR_ARRAY = 2;
      public const short DATATYPE_BOOLEAN = 3;
      public const short DATATYPE_INT = 4;
      public const short DATATYPE_LONG = 5;
      public const short DATATYPE_FLOAT = 6;
      public const short DATATYPE_DOUBLE = 7;
      public const short DATATYPE_CALENDAR = 8;
      public const short DATATYPE_BYTE_ARRAY = 9;

      /// <summary>
      /// event type (START_ELEMENT, END_ELEMENT...)
      /// </summary>
      public short type;

      /// <summary>
      /// datatype for the value (if there is a value)
      /// </summary>
      public short datatype;

      /// <summary>
      /// name/QName/target depending on event
      /// </summary>
      public string name; // name/qname/target

      /// <summary>
      /// local-name or public-id, depending on event
      /// </summary>
      public string localName; // local-name/public-id

      /// <summary>
      /// namespace-uri or system-id, depending on event
      /// </summary>
      public string @namespace; // namespace/system-id

      // Value fields.  Which field depends on datatype value.
      public string stringValue; // value/notation
      public char[] charValue;
      public long longValue;
      public double doubleValue;
      public DateTime calendarValue;
      public sbyte[] binaryValue;

      /// <summary>
      /// Construct a new Event object representing a start element 
      /// with the given namespace, local-name, and qualified-name
      /// </summary>
      /// <param name="namespace"> </param>
      /// <param name="localName"> </param>
      /// <param name="qName"> </param>
      public static Event newStartElement(string @namespace, string localName, string qName) {
          Event e = new Event();
          e.type = START_ELEMENT;
          e.name = qName;
          e.localName = localName;
          e.@namespace = @namespace;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing the end of an element
      /// with the given namespace, local-name, and qualified-name.
      /// </summary>
      /// <param name="namespace"> </param>
      /// <param name="localName"> </param>
      /// <param name="qName"> </param>
      public static Event newEndElement(string @namespace, string localName, string qName) {
          Event e = new Event();
          e.type = END_ELEMENT;
          e.name = qName;
          e.localName = localName;
          e.@namespace = @namespace;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing an attribute
      /// with the given namespace, local-name, qualified-name, and value.
      /// </summary>
      /// <param name="namespace"> </param>
      /// <param name="localName"> </param>
      /// <param name="qName"> </param>
      /// <param name="value"> </param>
      public static Event newAttribute(string @namespace, string localName, string qName, string value) {
          Event e = new Event();
          e.type = ATTRIBUTE;
          e.datatype = DATATYPE_STRING;
          e.name = qName;
          e.localName = localName;
          e.@namespace = @namespace;
          e.stringValue = value;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing the start of a namespace mapping
      /// for the given prefix and namespace-uri.
      /// </summary>
      /// <param name="prefix"> </param>
      /// <param name="namespace"> </param>
      public static Event newNamespace(string prefix, string @namespace) {
          Event e = new Event();
          e.type = NAMESPACE;
          e.datatype = DATATYPE_STRING;
          e.name = prefix;
          e.@namespace = @namespace;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing the end of a namespace mapping
      /// for the given prefix.
      /// </summary>
      /// <param name="prefix"> </param>
      public static Event newEndNamespace(string prefix) {
          Event e = new Event();
          e.type = END_NAMESPACE;
          e.datatype = DATATYPE_STRING;
          e.name = prefix;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing some character data.
      /// </summary>
      /// <param name="data"> </param>
      public static Event newCharacters(char[] data) {
          Event e = new Event();
          e.type = CHARACTERS;
          e.datatype = DATATYPE_CHAR_ARRAY;
          e.charValue = data;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing a Comment.
      /// </summary>
      /// <param name="data"> </param>
      public static Event newComment(char[] data) {
          Event e = new Event();
          e.type = COMMENT;
          e.datatype = DATATYPE_CHAR_ARRAY;
          e.charValue = data;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing a Processing-Instruction
      /// with the given target and data.
      /// </summary>
      /// <param name="target"> </param>
      /// <param name="data"> </param>
      public static Event newProcessingInstruction(string target, string data) {
          Event e = new Event();
          e.type = PROCESSING_INSTRUCTION;
          e.datatype = DATATYPE_STRING;
          e.name = target;
          e.stringValue = data;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing an unexpanded Entity
      /// with the given name.
      /// </summary>
      /// <param name="name"> </param>
      public static Event newUnexpandedEntity(string name) {
          Event e = new Event();
          e.type = UNEXPANDED_ENTITY;
          e.name = name;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing a Doctype declaration.
      /// </summary>
      /// <param name="name"> </param>
      /// <param name="publicId"> </param>
      /// <param name="systemId"> </param>
      public static Event newDoctype(string name, string publicId, string systemId) {
          Event e = new Event();
          e.type = DOCTYPE;
          e.name = name;
          e.localName = publicId;
          e.@namespace = systemId;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing the end of a DTD.
      /// </summary>
      public static Event newEndDTD() {
          Event e = new Event();
          e.type = END_DTD;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing a Notation declaration.
      /// </summary>
      /// <param name="name"> </param>
      /// <param name="publicId"> </param>
      /// <param name="systemId"> </param>
      public static Event newNotation(string name, string publicId, string systemId) {
          Event e = new Event();
          e.type = NOTATION;
          e.name = name;
          e.localName = publicId;
          e.@namespace = systemId;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing an Unparsed Entity declaration
      /// </summary>
      /// <param name="name"> </param>
      /// <param name="publicId"> </param>
      /// <param name="systemId"> </param>
      /// <param name="notationName"> </param>
      public static Event newUnparsedEntity(string name, string publicId, string systemId, string notationName) {
          Event e = new Event();
          e.type = UNPARSED_ENTITY;
          e.name = name;
          e.localName = publicId;
          e.@namespace = systemId;
          e.stringValue = notationName;
          return e;
      }

      /// <summary>
      /// Construct a new Event object representing an external Entity declaration.
      /// (Note: this is needed to support serializing unexpanded entities.)
      /// </summary>
      /// <param name="name"> </param>
      /// <param name="publicId"> </param>
      /// <param name="systemId"> </param>
      public static Event newExternalEntity(string name, string publicId, string systemId) {
          Event e = new Event();
          e.type = EXTERNAL_ENTITY;
          e.name = name;
          e.localName = publicId;
          e.@namespace = systemId;
          return e;
      }

      //////////////////////////////////////////////////////////////////////
      // value accessors

      /// <summary>
      /// Return value as String.
      /// Error if datatype != DATATYPE_STRING.
      /// </summary>
      public virtual string ValueString {
        get {
            if (datatype != DATATYPE_STRING) {
                throw new InvalidOperationException();
            }
            return stringValue;
        }
      }

      /// <summary>
      /// Return value as char[].
      /// Error if datatype != DATATYPE_CHAR_ARRAY.
      /// </summary>
      public virtual char[] ValueCharArray {
        get {
            if (datatype != DATATYPE_CHAR_ARRAY) {
                throw new InvalidOperationException();
            }
            return charValue;
        }
      }

      /// <summary>
      /// Return value as boolean.
      /// Error if datatype != DATATYPE_BOOLEAN.
      /// </summary>
      public virtual bool ValueBoolean {
        get {
            if (datatype != DATATYPE_BOOLEAN) {
                throw new InvalidOperationException();
            }
            return (0 != longValue);
        }
      }

      /// <summary>
      /// Return value as int.
      /// Error if datatype != DATATYPE_INT.
      /// </summary>
      public virtual int ValueInt {
        get {
            if (datatype != DATATYPE_INT) {
                throw new InvalidOperationException();
            }
            return (int) longValue;
        }
      }

      /// <summary>
      /// Return value as long.
      /// Error if datatype != DATATYPE_LONG.
      /// </summary>
      public virtual long ValueLong {
        get {
            if (datatype != DATATYPE_LONG) {
                throw new InvalidOperationException();
            }
            return longValue;
        }
      }

      /// <summary>
      /// Return value as float.
      /// Error if datatype != DATATYPE_FLOAT.
      /// </summary>
      public virtual float ValueFloat {
        get {
            if (datatype != DATATYPE_FLOAT) {
                throw new InvalidOperationException();
            }
            return (float) doubleValue;
        }
      }

      /// <summary>
      /// Return value as double.
      /// Error if datatype != DATATYPE_DOUBLE.
      /// </summary>
      public virtual double ValueDouble {
        get {
            if (datatype != DATATYPE_DOUBLE) {
                throw new InvalidOperationException();
            }
            return doubleValue;
        }
      }

      /// <summary>
      /// Return value as byte[].
      /// Error if datatype != DATATYPE_BYTE_ARRAY.
      /// </summary>
      public virtual sbyte[] ValueByteArray {
        get {
            if (datatype != DATATYPE_BYTE_ARRAY) {
                throw new InvalidOperationException();
            }
            return binaryValue;
        }
      }
  }

}