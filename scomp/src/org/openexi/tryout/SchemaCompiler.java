package org.openexi.tryout;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.UIManager;

public class SchemaCompiler {

  public SchemaCompiler() {
    SchemaCompilerMainFrame tryoutMainFrame = new SchemaCompilerMainFrame();
    tryoutMainFrame.validate();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = tryoutMainFrame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    tryoutMainFrame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    tryoutMainFrame.setVisible(true);
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    new SchemaCompiler();
  }

}
