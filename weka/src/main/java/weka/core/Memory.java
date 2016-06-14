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
 * Memory.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

/**
 * A little helper class for Memory management. The memory management can be
 * disabled by using the setEnabled(boolean) method.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 11271 $
 * @see #setEnabled(boolean)
 */
public class Memory implements RevisionHandler {

  public static final long OUT_OF_MEMORY_THRESHOLD = 52428800L;

  public static final long LOW_MEMORY_MINIMUM = 104857600L;

  public static final long MAX_SLEEP_TIME = 10L;

  /** whether memory management is enabled */
  protected boolean m_Enabled = true;

  /** whether a GUI is present */
  protected boolean m_UseGUI = false;

  /** the managed bean to use */
  protected static MemoryMXBean m_MemoryMXBean = ManagementFactory
    .getMemoryMXBean();

  /** the last MemoryUsage object obtained */
  protected MemoryUsage m_MemoryUsage = null;

  /** the delay before testing for out of memory */
  protected long m_SleepTime = MAX_SLEEP_TIME;

  /**
   * initializes the memory management without GUI support
   */
  public Memory() {
    this(false);
  }

  /**
   * initializes the memory management
   * 
   * @param useGUI whether a GUI is present
   */
  public Memory(boolean useGUI) {
    m_UseGUI = useGUI;
  }

  /**
   * returns whether the memory management is enabled
   * 
   * @return true if enabled
   */
  public boolean isEnabled() {
    return m_Enabled;
  }

  /**
   * sets whether the memory management is enabled
   * 
   * @param value true if the management should be enabled
   */
  public void setEnabled(boolean value) {
    m_Enabled = value;
  }

  /**
   * whether to display a dialog in case of a problem (= TRUE) or just print on
   * stderr (= FALSE)
   * 
   * @return true if the GUI is used
   */
  public boolean getUseGUI() {
    return m_UseGUI;
  }

  /**
   * returns the initial size of the JVM heap, obtains a fresh MemoryUsage
   * object to do so.
   * 
   * @return the initial size in bytes
   */
  public long getInitial() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    return m_MemoryUsage.getInit();
  }

  /**
   * returns the currently used size of the JVM heap, obtains a fresh
   * MemoryUsage object to do so.
   * 
   * @return the used size in bytes
   */
  public long getCurrent() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    return m_MemoryUsage.getUsed();
  }

  /**
   * returns the maximum size of the JVM heap, obtains a fresh MemoryUsage
   * object to do so.
   * 
   * @return the maximum size in bytes
   */
  public long getMax() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    return m_MemoryUsage.getMax();
  }

  /**
   * checks if there's still enough memory left by checking whether there is
   * still a 50MB margin between getUsed() and getMax(). if ENABLED is true,
   * then false is returned always. updates the MemoryUsage variable before
   * checking.
   * 
   * @return true if out of memory (only if management enabled, otherwise always
   *         false)
   */
  public boolean isOutOfMemory() {
    try {
      Thread.sleep(m_SleepTime);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }

    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    if (isEnabled()) {

      long avail = m_MemoryUsage.getMax() - m_MemoryUsage.getUsed();
      if (avail > OUT_OF_MEMORY_THRESHOLD) {
        long num = (avail - OUT_OF_MEMORY_THRESHOLD) / 5242880 + 1;

        m_SleepTime = (long) (2.0 * (Math.log(num) + 2.5));
        if (m_SleepTime > MAX_SLEEP_TIME) {
          m_SleepTime = MAX_SLEEP_TIME;
        }
        // System.out.println("Delay = " + m_SleepTime);
      }

      return avail < OUT_OF_MEMORY_THRESHOLD;
    } else {
      return false;
    }
  }

  /**
   * Checks to see if memory is running low. Low is defined as available memory
   * less than 20% of max memory.
   * 
   * @return true if memory is running low
   */
  public boolean memoryIsLow() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();

    if (isEnabled()) {
      long lowThreshold = (long) (0.2 * m_MemoryUsage.getMax());

      // min threshold of 100Mb
      if (lowThreshold < LOW_MEMORY_MINIMUM) {
        lowThreshold = LOW_MEMORY_MINIMUM;
      }

      long avail = m_MemoryUsage.getMax() - m_MemoryUsage.getUsed();

      return (avail < lowThreshold);
    } else {
      return false;
    }
  }

  /**
   * returns the amount of bytes as MB
   * 
   * @return the MB amount
   */
  public static double toMegaByte(long bytes) {
    return (bytes / (double) (1024 * 1024));
  }

  /**
   * prints an error message if OutOfMemory (and if GUI is present a dialog),
   * otherwise nothing happens. isOutOfMemory() has to be called beforehand,
   * since it sets all the memory parameters.
   * 
   * @see #isOutOfMemory()
   * @see #m_Enabled
   */
  public void showOutOfMemory() {
    if (!isEnabled() || (m_MemoryUsage == null)) {
      return;
    }

    System.gc();

    String msg = "Not enough memory (less than 50MB left on heap). Please load a smaller "
      + "dataset or use a larger heap size.\n"
      + "- initial heap size:   "
      + Utils.doubleToString(toMegaByte(m_MemoryUsage.getInit()), 1)
      + "MB\n"
      + "- current memory (heap) used:  "
      + Utils.doubleToString(toMegaByte(m_MemoryUsage.getUsed()), 1)
      + "MB\n"
      + "- max. memory (heap) available: "
      + Utils.doubleToString(toMegaByte(m_MemoryUsage.getMax()), 1)
      + "MB\n"
      + "\n"
      + "Note:\n"
      + "The Java heap size can be specified with the -Xmx option.\n"
      + "E.g., to use 128MB as heap size, the command line looks like this:\n"
      + "   java -Xmx128m -classpath ...\n"
      + "This does NOT work in the SimpleCLI, the above java command refers\n"
      + "to the one with which Weka is started. See the Weka FAQ on the web\n"
      + "for further info.";

    System.err.println(msg);

    if (getUseGUI()) {
      JOptionPane.showMessageDialog(null, msg, "OutOfMemory",
        JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Prints a warning message if memoryIsLow (and if GUI is present a dialog).
   * 
   * @return true if user opts to continue, disabled or GUI is not present.
   */
  public boolean showMemoryIsLow() {
    if (!isEnabled() || (m_MemoryUsage == null)) {
      return true;
    }

    String msg = "Warning: memory is running low - available heap space is less than "
      + "20% of maximum or 100MB (whichever is greater)\n\n"
      + "- initial heap size:   "
      + Utils.doubleToString(toMegaByte(m_MemoryUsage.getInit()), 1)
      + "MB\n"
      + "- current memory (heap) used:  "
      + Utils.doubleToString(toMegaByte(m_MemoryUsage.getUsed()), 1)
      + "MB\n"
      + "- max. memory (heap) available: "
      + Utils.doubleToString(toMegaByte(m_MemoryUsage.getMax()), 1)
      + "MB\n\n"
      + "Consider deleting some results before continuing.\nCheck the Weka FAQ "
      + "on the web for suggestions on how to save memory.\n"
      + "Note that Weka will shut down when less than 50MB remain."
      + "\nDo you wish to continue regardless?\n\n";

    System.err.println(msg);

    if (getUseGUI()) {
      if (!Utils.getDontShowDialog("weka.core.Memory.LowMemoryWarning")) {
        JCheckBox dontShow = new JCheckBox("Do not show this message again");
        Object[] stuff = new Object[2];
        stuff[0] = msg;
        stuff[1] = dontShow;

        int result = JOptionPane.showConfirmDialog(null, stuff, "Memory",
          JOptionPane.YES_NO_OPTION);

        if (dontShow.isSelected()) {
          try {
            Utils.setDontShowDialog("weka.core.Memory.LowMemoryWarning");
          } catch (Exception ex) {
            // quietly ignore
          }
        }

        return (result == JOptionPane.YES_OPTION);
      }
    }

    return true;
  }

  /**
   * stops all the current threads, to make a restart possible
   */
  @SuppressWarnings("deprecation")
  public void stopThreads() {
    int i;
    Thread[] thGroup;
    Thread t;

    thGroup = new Thread[Thread.activeCount()];
    Thread.enumerate(thGroup);

    for (i = 0; i < thGroup.length; i++) {
      t = thGroup[i];
      if (t != null) {
        if (t != Thread.currentThread()) {
          if (t.getName().startsWith("Thread")) {
            t.stop();
          } else if (t.getName().startsWith("AWT-EventQueue")) {
            t.stop();
          }
        }
      }
    }

    thGroup = null;

    System.gc();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11271 $");
  }

  /**
   * prints only some statistics
   * 
   * @param args the commandline arguments - ignored
   */
  public static void main(String[] args) {
    Memory mem = new Memory();
    System.out.println("Initial memory: "
      + Utils.doubleToString(Memory.toMegaByte(mem.getInitial()), 1) + "MB"
      + " (" + mem.getInitial() + ")");
    System.out.println("Max memory: "
      + Utils.doubleToString(Memory.toMegaByte(mem.getMax()), 1) + "MB" + " ("
      + mem.getMax() + ")");
  }
}
