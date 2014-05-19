using System.Diagnostics;

namespace Nagasena.Proc.Common {

  /// <exclude/>
  public abstract class EventTypeList {

    public readonly bool isReverse;

    protected internal EventTypeList(bool isReverse) {
      this.isReverse = isReverse;
    }

    public abstract int Length { get; }

    public abstract EventType item(int i);

    public abstract EventType SD { get; }

    public EventType ED {
      get {
        int len = Length;
        for (int i = 0; i < len; i++) {
          EventType eventType = item(i);
          if (eventType.itemType == EventType.ITEM_ED) {
            return eventType;
          }
        }
        return null;
      }
    }

    public abstract EventType EE { get; }

    public abstract EventType getSchemaAttribute(string uri, string name);
    public abstract EventType getSchemaAttributeInvalid(string uri, string name);
    public abstract EventType getLearnedAttribute(string uri, string name);
    public abstract EventType SchemaAttributeWildcardAny { get; }
    public abstract EventType AttributeWildcardAnyUntyped { get; }
    public abstract EventType getSchemaAttributeWildcardNS(string uri);

    public abstract EventType SchemaCharacters { get; }
    /// <summary>
    /// Returns a characters event type that corresponds to either a mixed content or
    /// schema-deviation characters if any. The one corresponding to a mixed content is
    /// returned when both are available.
    /// </summary>
    public abstract EventType Characters { get; }

    public abstract EventType NamespaceDeclaration { get; }

    public static readonly EventTypeList EMPTY;
    static EventTypeList() {
      EMPTY = new EventTypeListAnonymousInnerClassHelper();
    }

    private class EventTypeListAnonymousInnerClassHelper : EventTypeList {
      public EventTypeListAnonymousInnerClassHelper() : base(false) {
      }

      public override sealed int Length {
        get {
          return 0;
        }
      }
      public override sealed EventType item(int i) {
        Debug.Assert(false);
        return null;
      }
      public override sealed EventType SD {
        get {
          return null;
        }
      }
      public override sealed EventType EE {
        get {
          return null;
        }
      }
      public override sealed EventType getSchemaAttribute(string uri, string name) {
        return null;
      }
      public override sealed EventType getSchemaAttributeInvalid(string uri, string name) {
        return null;
      }
      public override sealed EventType getLearnedAttribute(string uri, string name) {
        return null;
      }
      public override sealed EventType SchemaAttributeWildcardAny {
        get {
          return null;
        }
      }
      public override sealed EventType AttributeWildcardAnyUntyped {
        get {
          return null;
        }
      }
      public override sealed EventType getSchemaAttributeWildcardNS(string uri) {
        return null;
      }
      public override sealed EventType SchemaCharacters {
        get {
          return (EventType)null;
        }
      }
      public override sealed EventType Characters {
        get {
          return (EventType)null;
        }
      }
      public override sealed EventType NamespaceDeclaration {
        get {
          return (EventType)null;
        }
      }
    }

  }

}