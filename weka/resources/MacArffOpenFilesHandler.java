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
 *    MacArffOpenFilesHandler.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.io.*;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.AppEvent.OpenFilesEvent;

/**
 * Helper class for use under Mac OS X. Associates the Explorer
 * with arff and xrff file types. The build.xml script copies the
 * compiled MacArffOpenFilesHandler class from the resources
 * directory into build/classes/weka/gui before the executable
 * jar file is made. Reflection is used in the GUIChooser to
 * determine if the OS is OS X and, if so, this handler is
 * registered.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 8033 $
 */
public class MacArffOpenFilesHandler implements OpenFilesHandler {
  public void openFiles( OpenFilesEvent arg0 ) {
    System.out.println( "Opening an arff/xrff file under Mac OS X..." );
    File toOpen = arg0.getFiles().get(0);

    if (toOpen.toString().toLowerCase().endsWith(".arff") || 
        toOpen.toString().toLowerCase().endsWith(".xrff")) {
      weka.gui.GUIChooser.createSingleton();
      weka.gui.GUIChooser.getSingleton().showExplorer(toOpen.toString());    
    } else if (toOpen.toString().toLowerCase().endsWith(".kf") ||
               toOpen.toString().toLowerCase().endsWith(".kfml")) {
      weka.gui.GUIChooser.createSingleton();
      weka.gui.GUIChooser.getSingleton().showKnowledgeFlow(toOpen.toString());
    }
  }
}
