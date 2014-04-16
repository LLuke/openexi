package org.openexi.scomp;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Extended SAX EntityResolver interface for resolving entities and
 * schema documents.
 */
public interface EntityResolverEx extends EntityResolver {
  
  /**
   * This method will be called for resolving schema documents upon
   * occurrences of XML Schema directives such as "include", "import" and
   * "redefine" within schemas.
   * @param publicId Public identifier of the schema document that is being resolved
   * @param systemId System identifier of the schema document that is being resolved
   * @param namespaceURI Target namespace name of the schema document that is being resolved
   * @return InputSource that represents the schema document if resolved otherwise null
   */
  public InputSource resolveEntity(String publicId, String systemId,
                                   String namespaceURI)
      throws SAXException, IOException;

}
