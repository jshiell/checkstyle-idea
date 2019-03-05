package org.infernus.idea.checkstyle.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.intellij.openapi.util.IconLoader;

public abstract class ConfigGeneratorWindow extends JFrame {
  private static final long serialVersionUID = 1L;

  public ConfigGeneratorWindow() {
    super();
    setIconImage(iconToImage(IconLoader.getIcon("/org/infernus/idea/checkstyle/images/checkstyle32.png")));
    createWindowContent();
    setLocationByPlatform(true);
  }

  protected abstract void createWindowContent();

  protected void arrange() {
    pack();
    revalidate();
    repaint();
  }

  protected void display() {
    if (!isVisible()) {
      setVisible(true);
    } else {
      requestFocus();
    }
  }

  /**
   * Converts an Icon to an Image.
   * 
   * @param ico The Icon to convert
   * @return The Icon converted to an Image
   */
  public static Image iconToImage(Icon ico) {
    if (ico instanceof ImageIcon) {
      return ((ImageIcon) ico).getImage();
    } else {
      BufferedImage image = new BufferedImage(ico.getIconWidth(), ico.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      ico.paintIcon(null, g, 0, 0);
      g.dispose();
      return image;
    }
  }

  public static String camelToTitle(String camel) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      if (i == 0) {
        sb.append(Character.toUpperCase(camel.charAt(0)));
      } else if (Character.isUpperCase(camel.charAt(i))) {
        sb.append(" " + camel.charAt(i));
      } else {
        sb.append(camel.charAt(i));
      }
    }
    return sb.toString();
  }
}