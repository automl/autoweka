/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    ImageEvent.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.image.BufferedImage;
import java.util.EventObject;

/**
 * Event that encapsulates an Image
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 10882 $
 */
public class ImageEvent extends EventObject {

  /** For serialization */
  private static final long serialVersionUID = -8126533743311557969L;

  /** The image */
  protected BufferedImage m_image;

  /** The name of the image */
  protected String m_imageName = "";

  /**
   * Construct a new ImageEvent
   * 
   * @param source the source of this event
   * @param image the image to encapsulate
   */
  public ImageEvent(Object source, BufferedImage image) {
    this(source, image, "");
  }

  /**
   * Construct an ImageEvent
   * 
   * @param source the source of this event
   * @param image the image to encapsulate
   * @param imageName the name of the image
   */
  public ImageEvent(Object source, BufferedImage image, String imageName) {
    super(source);

    m_image = image;
    m_imageName = imageName;
  }

  /**
   * Get the encapsulated image
   * 
   * @return the encapsulated image
   */
  public BufferedImage getImage() {
    return m_image;
  }

  /**
   * Get the name of the image
   * 
   * @return
   */
  public String getImageName() {
    return m_imageName;
  }
}
