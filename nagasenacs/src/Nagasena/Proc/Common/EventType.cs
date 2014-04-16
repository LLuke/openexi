using System.Diagnostics;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.Common {

  /// <summary>
  /// EventType denotes terminal symbols of grammar productions  
  /// defined in the EXI 1.0 specification. 
  /// </summary>
  public class EventType : EventCode, EventDescription {

    // DO NOT CHANGE CODE ASSIGNMENT of ITEM_PI ... ITEM_EE 
    /// <summary>
    /// Event type for a Processing Instruction. Value is 0.
    /// </summary>
    public const sbyte ITEM_PI = 0;
    /// <summary>
    /// Event type for a Comment.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_CM = 1;
    /// <summary>
    /// Event type for an Entity Reference.
    /// <br/>Value is {@value}.
    /// </summary>

    public const sbyte ITEM_ER = 2;
    /// <summary>
    /// Event type for a Character event (character events store values as strings).
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_CH = 3;
    /// <summary>
    /// Event type for End of Document.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_ED = 4;
    /// <summary>
    /// Wildcard event type for an element. OpenEXI will first attempt
    /// to find a corresponding element name in the schema, if present. If 
    /// no definition is available, it is given this tag.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SE_WC = 5;
    /// <summary>
    /// Self-contained items are not supported in this release of OpenEXI. 
    /// Event type for self-contained item.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SC = 6;
    /// <summary>
    /// Event type for a Namespace declaration.<br/>
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_NS = 7;
    /// <summary>
    ///  Wildcard event type for an Attribute where the attribute's defined 
    ///  datatype (if any) is disregarded. 
    ///  This is the "catch-all" for Attributes that do not match any of the 
    ///  other Event Types in an EXI stream processed using Default options. 
    ///  <br />Value is {@value}.
    /// </summary>
    public const sbyte ITEM_AT_WC_ANY_UNTYPED = 8;
    /// <summary>
    /// Event type for End of Element.
    /// <br />Value is {@value}.
    /// </summary>
    public const sbyte ITEM_EE = 9;
    /// <summary>
    /// Event type for a Document Type Definition.
    /// <br />Value is {@value}.
    /// </summary>
    public const sbyte ITEM_DTD = 10;
    /// <summary>
    /// Event type for Start Element.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SE = 11;
    /// <summary>
    /// Event type for an Attribute learned by built-in element grammars 
    /// from prior attribute occurrences.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_AT = 12;
    /// <summary>
    /// Event type for Start Document.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SD = 13;
    /// <summary>
    /// Attribute wildcard event type stemming from a schema where the 
    /// attribute's defined datatype (if any) is applied. 
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_WC_ANY = 14;
    /// <summary>
    /// Event type for an element defined in a namespace in an EXI stream
    /// processed using a schema. 
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_WC_NS = 15;
    /// <summary>
    /// Event type for AttributeUse that matches an attribute event with
    /// a valid value.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_AT = 16;
    /// <summary>
    /// Attribute wildcard event type stemming from a schema, where the attribute's 
    /// defined datatype (if any) is applied.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_AT_WC_ANY = 17;
    /// <summary>
    /// Attribute wildcard event type, qualified with a specific namespace, stemming
    /// from a schema where the attribute's defined datatype (if any) is applied. 
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_AT_WC_NS = 18;
    /// <summary>
    /// Event type for a defined Character event in an EXI stream processed
    /// using a schema. 
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_CH = 19;
    /// <summary>
    /// Event type for a Character event that occurs in the context of an element 
    /// defined so as to permit mixed content (mark up and data) in an EXI 
    /// stream processed using a schema. 
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_CH_MIXED = 20;
    /// <summary>
    /// Special Attribute that indicates the value of the associated element is
    /// explicitly <i>nil</i> rather than an empty string.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_NIL = 21;
    /// <summary>
    /// Special Attribute that describes a data type for the associated
    /// element. For example, the schema might define a String value, but
    /// the XML document being processed can declare that the element contains a
    /// date-time field.
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_TYPE = 22;
    /// <summary>
    /// Event type for AttributeUse that matches an attribute event with an 
    /// invalid value. 
    /// <br/>Value is {@value}.
    /// </summary>
    public const sbyte ITEM_SCHEMA_AT_INVALID_VALUE = 23;

    /// <summary>
    /// @y.exclude </summary>
    public readonly sbyte depth;
    /// <summary>
    /// URI of event type definition. </summary>
    public readonly string uri;
    /// <summary>
    /// Local name of event type definition. </summary>
    public readonly string name;
    /// <summary>
    /// @y.exclude </summary>
    public readonly IGrammar subsequentGrammar;

    private readonly EventCode[] m_path;
    private readonly int m_uriId;
    private readonly int m_nameId;

    private readonly sbyte m_eventKind;

    /// <summary>
    /// The index of this event type in the EventTypeList to which it belongs.
    /// </summary>
    private int m_index;

    private readonly EventTypeList m_ownerList;

    internal EventType(sbyte depth, EventTypeList eventTypeList, sbyte itemType, IGrammar subsequentGrammar) : 
      this((string)null, (string)null, -1, -1, depth, eventTypeList, itemType, EventDescription_Fields.NOT_AN_EVENT, subsequentGrammar) {
    }

    internal EventType(string uri, string name, int uriId, int nameId, sbyte depth, EventTypeList eventTypeList, sbyte itemType, IGrammar subsequentGrammar) : 
      this(uri, name, uriId, nameId, depth, eventTypeList, itemType, EventDescription_Fields.NOT_AN_EVENT, subsequentGrammar) {
    }

    internal EventType(string uri, string name, int uriId, int nameId, sbyte depth, EventTypeList eventTypeList, sbyte itemType, sbyte eventKind, IGrammar subsequentGrammar) : 
      base(itemType) {
      this.depth = depth;
      m_path = new EventCode[depth];
      this.uri = uri;
      this.name = name;
      m_uriId = uriId;
      m_nameId = nameId;
      m_index = -1;
      m_ownerList = eventTypeList;
      this.subsequentGrammar = subsequentGrammar;
      m_eventKind = eventKind;
    }

    internal int Depth {
      get {
        return depth;
      }
    }

    internal EventCode[] ItemPath {
      get {
        return m_path;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int URIId {
      get {
        return m_uriId;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int NameId {
      get {
        return m_nameId;
      }
    }

    internal int Index {
      get {
        Debug.Assert(m_index != -1);
        return m_ownerList.isReverse ? m_ownerList.Length - (m_index + 1) : m_index;
      }
      set {
        m_index = value;
      }
    }

    internal EventTypeList EventTypeList {
      get {
        return m_ownerList;
      }
    }

    internal EventDescription asEventDescription() {
      return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EventDescription interface
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// @y.exclude </summary>
    public sbyte EventKind {
      get {
        return m_eventKind;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string URI {
      get {
        return uri;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public string Name {
      get {
        return name;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public virtual string Prefix {
      get {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public virtual Characters Characters {
      get {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public virtual BinaryDataSource BinaryDataSource {
      get {
        return null;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public EventType getEventType() {
      return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Other APIs
    ///////////////////////////////////////////////////////////////////////////

    internal void computeItemPath() {
      int depth = Depth;
      int i;
      EventCode item;
      for (i = 0, item = this; i < depth; i++) {
        m_path[(depth - 1) - i] = item;
        item = item.parent;
      }
    }

  }

}