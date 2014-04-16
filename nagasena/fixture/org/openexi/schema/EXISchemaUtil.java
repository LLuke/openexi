package org.openexi.schema;

public class EXISchemaUtil {

  /**
   * Permit access to m_attrs in EXISchema.
   */
  public static int[] getAttrs(EXISchema schema) {
    return schema.getAttrs();
  }
  
  /**
   * Count the number of types in a schema.
   */
  public static int countTypesOfSchema(EXISchema schema, boolean globalOnly) {
    int n_globalTypes, n_types;
    n_globalTypes = n_types = 0;
    int pos, typesLen;
    for (pos = 0, typesLen = schema.getTypes().length; pos < typesLen;) {
      final int tp = pos;
      ++n_types;
      if (!"".equals(schema.getNameOfType(tp))) {
        ++n_globalTypes;
      }
      pos += EXISchema._getTypeSize(tp, schema.getTypes(), schema.ancestryIds);
    }
    return globalOnly ? n_globalTypes : n_types;
  }

  /**
   * Count the total number of elements in a schema.
   */
  public static int countElemsOfSchema(EXISchema schema) {
    int n_elems = 0;
    final int nodesLen = schema.getElems().length;
    for (int pos = 0; pos < nodesLen; pos += EXISchemaLayout.SZ_ELEM, ++n_elems);
    return n_elems;
  }

  /**
   * Count the total number of attributes in a schema.
   */
  public static int countAttrsOfSchema(EXISchema schema) {
    int n_attrs = 0;
    final int length = schema.getAttrs().length;
    for (int pos = 0; pos < length; pos += EXISchemaLayout.SZ_ATTR, ++n_attrs);
    return n_attrs;
  }

  /**
   * Count the number of simple types in a schema by going through 
   * SIMPLE_TYPE_NEXT_SIMPLE_TYPE link.
   */
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

  public static int getTypeCountOfSchema(String uri, EXISchema schema) {
    int n_types = 0;
    final int length = schema.getTypes().length;
    for (int tp = 0; tp < length; tp += EXISchema._getTypeSize(tp, schema.getTypes(), schema.ancestryIds)) {
      if (!"".equals(schema.getNameOfType(tp))) {
         if (uri.equals(schema.uris[schema.getUriOfType(tp)]))
           ++n_types;
      }
    }
    return n_types;
  }
  
  public static int getContentDataTypeOfType(int tp, EXISchema schema) {
    return schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
  }
  
  public static String getTargetNamespaceNameOfType(int tp, EXISchema schema) {
    assert 0 <= tp;
    final int uriId;
    if ((uriId = schema.getTypes()[tp + EXISchemaLayout.TYPE_URI]) != -1) {
      return schema.uris[uriId];
    }
    return null;
  }
  
  /**
  * Returns localName of an attribute.
  * @param attr attribute node
  * @return localName of an attribute node.
  */
  public static String getNameOfAttr(int attr, EXISchema schema) {
   assert 0 <= attr;
   final int localName = schema.getLocalNameOfAttr(attr);
   final int uri = schema.getUriOfAttr(attr);
   return schema.localNames[uri][localName];
  }

}
