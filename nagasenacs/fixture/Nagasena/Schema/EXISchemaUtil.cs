using System.Diagnostics;

namespace Nagasena.Schema {

  public class EXISchemaUtil {

    /// <summary>
    /// Permit access to m_attrs in EXISchema.
    /// </summary>
    public static int[] getAttrs(EXISchema schema) {
      return schema.Attrs;
    }

    /// <summary>
    /// Count the number of types in a schema.
    /// </summary>
    public static int countTypesOfSchema(EXISchema schema, bool globalOnly) {
      int n_globalTypes, n_types;
      n_globalTypes = n_types = 0;
      int pos, typesLen;
      for (pos = 0, typesLen = schema.Types.Length; pos < typesLen;) {
        int tp = pos;
        ++n_types;
        if (!"".Equals(schema.getNameOfType(tp))) {
          ++n_globalTypes;
        }
        pos += EXISchema._getTypeSize(tp, schema.Types, schema.ancestryIds);
      }
      return globalOnly ? n_globalTypes : n_types;
    }

    /// <summary>
    /// Count the total number of elements in a schema.
    /// </summary>
    public static int countElemsOfSchema(EXISchema schema) {
      int n_elems = 0;
      int nodesLen = schema.Elems.Length;
      for (int pos = 0; pos < nodesLen; pos += EXISchemaLayout.SZ_ELEM, ++n_elems) {
        ;
      }
      return n_elems;
    }

    /// <summary>
    /// Count the total number of attributes in a schema.
    /// </summary>
    public static int countAttrsOfSchema(EXISchema schema) {
      int n_attrs = 0;
      int length = schema.Attrs.Length;
      for (int pos = 0; pos < length; pos += EXISchemaLayout.SZ_ATTR, ++n_attrs) {
        ;
      }
      return n_attrs;
    }

    /// <summary>
    /// Count the number of simple types in a schema by going through 
    /// SIMPLE_TYPE_NEXT_SIMPLE_TYPE link.
    /// </summary>
    public static int countLinkedSimpleTypesOfSchema(EXISchema schema) {
      int tp = schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
      int n_simpleTypes = 0;
      do {
        ++n_simpleTypes;
        tp = schema.getNextSimpleType(tp);
      }
      while (tp != EXISchema.NIL_NODE);
      return n_simpleTypes;
    }

    public static int getTypeCountOfSchema(string uri, EXISchema schema) {
      int n_types = 0;
      int length = schema.Types.Length;
      for (int tp = 0; tp < length; tp += EXISchema._getTypeSize(tp, schema.Types, schema.ancestryIds)) {
        if (!"".Equals(schema.getNameOfType(tp))) {
           if (uri.Equals(schema.uris[schema.getUriOfType(tp)])) {
             ++n_types;
           }
        }
      }
      return n_types;
    }

    public static int getContentDataTypeOfType(int tp, EXISchema schema) {
      return schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
    }

    public static string getTargetNamespaceNameOfType(int tp, EXISchema schema) {
      Debug.Assert(0 <= tp);
      int uriId;
      if ((uriId = schema.Types[tp + EXISchemaLayout.TYPE_URI]) != -1) {
        return schema.uris[uriId];
      }
      return null;
    }

    /// <summary>
    /// Returns localName of an attribute. </summary>
    /// <param name="attr"> attribute node </param>
    /// <returns> localName of an attribute node. </returns>
    public static string getNameOfAttr(int attr, EXISchema schema) {
     Debug.Assert(0 <= attr);
     int localName = schema.getLocalNameOfAttr(attr);
     int uri = schema.getUriOfAttr(attr);
     return schema.localNames[uri][localName];
    }

  }

}