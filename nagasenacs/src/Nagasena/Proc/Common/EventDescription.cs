using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.Common {

  /// <summary>
  /// <para>EventDescription provides accessors to the current EXI event data
  /// during the decode process.</para>
  /// 
  /// <para>Note that the content of EventDescription is transient, which means
  /// its content may change when the decoder is asked for access to the
  /// next EXI event data.</para>
  /// </summary>
  public interface EventDescription {
    /// <summary>
    /// @y.exclude </summary>
    /// <summary>
    /// Start Document event.
    /// </summary>
    /// <summary>
    /// End Document event.
    /// </summary>
    /// <summary>
    /// Start Element event.
    /// </summary>
    /// <summary>
    /// Attribute event.
    /// </summary>
    /// <summary>
    /// Attribute <i>xsi:type</i>.
    /// </summary>
    /// <summary>
    ///  Attribute <i>xsi:nil</i>.
    /// </summary>
    /// <summary>
    /// Character event (content of an element).
    /// </summary>
    /// <summary>
    /// End Element event.
    /// </summary>
    /// <summary>
    /// Namespace declaration event.
    /// </summary>
    /// <summary>
    /// Processing Instruction event.
    /// </summary>
    /// <summary>
    /// Comment event.
    /// </summary>
    /// <summary>
    /// Entity Reference event.
    /// </summary>
    /// <summary>
    /// Document Type Definition event.
    /// </summary>
    /// <summary>
    /// BLOB event (content of an element).
    /// </summary>

    /// <summary>
    /// Gets the event kind of which instance data this EventDescription is describing. </summary>
    /// <returns> a byte representing the event kind. </returns>
    sbyte EventKind { get; }

    /// <summary>
    /// Gets the URI of the EXI event. </summary>
    /// <returns> the URI as a String. </returns>
    string URI { get; }

    /// <summary>
    /// Gets the name of the EXI event. </summary>
    /// <returns> the name of the event as a String. </returns>
    string Name { get; }

    /// <summary>
    /// Gets the namespace prefix of the event. </summary>
    /// <returns> the prefix as a String. </returns>
    string Prefix { get; }

    /// <summary>
    /// @y.exclude </summary>
    int URIId { get; }

    /// <summary>
    /// @y.exclude </summary>
    int NameId { get; }

    /// <summary>
    /// Gets the value of an EVENT_CH, 
    /// Attribute (EVENT_AT, EVENT_NL, EVENT_TP), EVENT_CM,
    /// EVENT_DTD or EVENT_PI event. </summary>
    /// <returns> a Characters of the corresponding value </returns>
    Characters Characters { get; }

    /// <summary>
    /// Gets the value of an EVENT_BLOB. </summary>
    /// <returns> a BinaryData of the corresponding value </returns>
    BinaryDataSource BinaryDataSource { get; }

    /// <summary>
    /// Returns the EventType from which this event is derived.
    /// </summary>
    EventType getEventType();

  }

  public static class EventDescription_Fields {
    public const sbyte NOT_AN_EVENT = -1;
    public const sbyte EVENT_SD = 0;
    public const sbyte EVENT_ED = 1;
    public const sbyte EVENT_SE = 2;
    public const sbyte EVENT_AT = 3;
    public const sbyte EVENT_TP = 4;
    public const sbyte EVENT_NL = 5;
    public const sbyte EVENT_CH = 6;
    public const sbyte EVENT_EE = 7;
    public const sbyte EVENT_NS = 8;
    public const sbyte EVENT_PI = 9;
    public const sbyte EVENT_CM = 10;
    public const sbyte EVENT_ER = 11;
    public const sbyte EVENT_DTD = 12;
    public const sbyte EVENT_BLOB = 13;
  }

}