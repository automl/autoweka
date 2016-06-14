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
 *    RepositoryIndexGenerator.java
 *    Copyright (C) 2000-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class for generating html index files and supporting text files for a Weka
 * package meta data repository. To Run<br>
 * <br>
 * 
 * <code>java weka.core.RepositoryIndexGenerator <path to repository></code>
 * <br><br>
 * A file called "forcedRefreshCount.txt" can be used to force a cache-refresh
 * for all internet connected users. Just increment the number in this file by
 * 1 (or create the file with an initial value of 1 if it doesn't exist yet).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12051 $
 */
public class RepositoryIndexGenerator {

  public static String HEADER =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n"
      + "<html>\n<head>\n<title>Waikato Environment for Knowledge Analysis (WEKA)</title>\n"
      + "<!-- CSS Stylesheet -->\n<style>body\n{\nbackground: #ededed;\ncolor: #666666;\n"
      + "font: 14px Tahoma, Helvetica, sans-serif;;\nmargin: 5px 10px 5px 10px;\npadding: 0px;\n"
      + "}\n</style>\n\n</head>\n<body bgcolor=\"#ededed\" text=\"#666666\">\n";

  public static String BIRD_IMAGE1 = "<img src=\"Title-Bird-Header.gif\">\n";
  public static String BIRD_IMAGE2 = "<img src=\"../Title-Bird-Header.gif\">\n";
  public static String PENTAHO_IMAGE1 =
    "<img src=\"pentaho_logo_rgb_sm.png\">\n\n";
  public static String PENTAHO_IMAGE2 =
    "<img src=\"../pentaho_logo_rgb_sm.png\">\n\n";

  private static int[] parseVersion(String version) {
    int major = 0;
    int minor = 0;
    int revision = 0;
    int[] majMinRev = new int[3];

    try {
      String tmpStr = version;
      tmpStr = tmpStr.replace('-', '.');
      if (tmpStr.indexOf(".") > -1) {
        major = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
        tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);
        if (tmpStr.indexOf(".") > -1) {
          minor = Integer.parseInt(tmpStr.substring(0, tmpStr.indexOf(".")));
          tmpStr = tmpStr.substring(tmpStr.indexOf(".") + 1);
          if (!tmpStr.equals("")) {
            revision = Integer.parseInt(tmpStr);
          } else {
            revision = 0;
          }
        } else {
          if (!tmpStr.equals("")) {
            minor = Integer.parseInt(tmpStr);
          } else {
            minor = 0;
          }
        }
      } else {
        if (!tmpStr.equals("")) {
          major = Integer.parseInt(tmpStr);
        } else {
          major = 0;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      major = -1;
      minor = -1;
      revision = -1;
    } finally {
      majMinRev[0] = major;
      majMinRev[1] = minor;
      majMinRev[2] = revision;
    }

    return majMinRev;
  }

  private static String cleansePropertyValue(String propVal) {
    propVal = propVal.replace("<", "&#60;");
    propVal = propVal.replace(">", "&#62;");
    propVal = propVal.replace("@", "{[at]}");
    propVal = propVal.replace("\n", "<br/>");

    return propVal;
  }

  protected static int compare(String version1, String version2) {

    // parse both of the versions
    int[] majMinRev1 = parseVersion(version1);
    int[] majMinRev2 = parseVersion(version2);

    int result;

    if (majMinRev1[0] < majMinRev2[0]) {
      result = -1;
    } else if (majMinRev1[0] == majMinRev2[0]) {
      if (majMinRev1[1] < majMinRev2[1]) {
        result = -1;
      } else if (majMinRev1[1] == majMinRev2[1]) {
        if (majMinRev1[2] < majMinRev2[2]) {
          result = -1;
        } else if (majMinRev1[2] == majMinRev2[2]) {
          result = 0;
        } else {
          result = 1;
        }
      } else {
        result = 1;
      }
    } else {
      result = 1;
    }

    return result;
  }

  private static String[] processPackage(File packageDirectory)
    throws Exception {
    System.err.println("Processing " + packageDirectory);
    File[] contents = packageDirectory.listFiles();
    File latest = null;
    ArrayList<File> propsFiles = new ArrayList<File>();
    StringBuffer versionsTextBuffer = new StringBuffer();

    for (File content : contents) {
      if (content.isFile() && content.getName().endsWith(".props")) {
        propsFiles.add(content);
        if (content.getName().equals("Latest.props")) {
          latest = content;
        } /*
           * else { String versionNumber = contents[i].getName().substring(0,
           * contents[i].getName().indexOf(".props"));
           * versionsTextBuffer.append(versionNumber + "\n"); }
           */
      }
    }

    File[] sortedPropsFiles = propsFiles.toArray(new File[0]);
    Arrays.sort(sortedPropsFiles, new Comparator<File>() {
      @Override
      public int compare(File first, File second) {
        String firstV =
          first.getName().substring(0, first.getName().indexOf(".props"));
        String secondV =
          second.getName().substring(0, second.getName().indexOf(".props"));
        if (firstV.equalsIgnoreCase("Latest")) {
          return -1;
        } else if (secondV.equalsIgnoreCase("Latest")) {
          return 1;
        }
        return -RepositoryIndexGenerator.compare(firstV, secondV);
      }
    });

    StringBuffer indexBuff = new StringBuffer();
    indexBuff.append(HEADER + "\n\n");
    // indexBuff.append(HEADER + BIRD_IMAGE2);
    // indexBuff.append(PENTAHO_IMAGE2);
    Properties latestProps = new Properties();
    latestProps.load(new BufferedReader(new FileReader(latest)));
    String packageName = latestProps.getProperty("PackageName") + ": ";
    String packageTitle = latestProps.getProperty("Title");
    String packageCategory = latestProps.getProperty("Category");
    String latestVersion = latestProps.getProperty("Version");
    if (packageCategory == null) {
      packageCategory = "";
    }
    indexBuff.append("<h2>" + packageName + packageTitle + "</h2>\n\n");

    String author = latestProps.getProperty("Author");
    author = cleansePropertyValue(author);
    String URL = latestProps.getProperty("URL");
    if (URL != null) {
      URL = cleansePropertyValue(URL);
    }
    String maintainer = latestProps.getProperty("Maintainer");
    maintainer = cleansePropertyValue(maintainer);

    indexBuff.append("\n<table>\n");
    if (URL != null && URL.length() > 0) {
      indexBuff.append("<tr><td valign=top>" + "URL"
        + ":</td><td width=50></td>");
      URL = "<a href=\"" + URL + "\">" + URL + "</a>";
      indexBuff.append("<td>" + URL + "</td></tr>\n");
    }
    indexBuff.append("<tr><td valign=top>" + "Author"
      + ":</td><td width=50></td>");
    indexBuff.append("<td>" + author + "</td></tr>\n");
    indexBuff.append("<tr><td valign=top>" + "Maintainer"
      + ":</td><td width=50></td>");
    indexBuff.append("<td>" + maintainer + "</td></tr>\n");
    indexBuff.append("</table>\n<p>\n");

    String description = latestProps.getProperty("Description");
    indexBuff.append("<p>" + description.replace("\n", "<br/>") + "</p>\n\n");

    indexBuff.append("<p>All available versions:<br>\n");
    for (int i = 0; i < sortedPropsFiles.length; i++) {
      if (i > 0) {
        String versionNumber =
          sortedPropsFiles[i].getName().substring(0,
            sortedPropsFiles[i].getName().indexOf(".props"));
        versionsTextBuffer.append(versionNumber + "\n");
        System.err.println(versionNumber);
      }
      String name = sortedPropsFiles[i].getName();
      name = name.substring(0, name.indexOf(".props"));
      indexBuff.append("<a href=\"" + name + ".html" + "\">" + name
        + "</a><br>\n");

      StringBuffer version = new StringBuffer();
      version.append(HEADER + "\n\n");
      // version.append(HEADER + BIRD_IMAGE2);
      // version.append(PENTAHO_IMAGE2);
      version.append("<table summary=\"Package " + packageName
        + " summary\">\n");
      Properties versionProps = new Properties();
      versionProps
        .load(new BufferedReader(new FileReader(sortedPropsFiles[i])));

      Set<Object> keys = versionProps.keySet();
      String[] sortedKeys = keys.toArray(new String[0]);
      Arrays.sort(sortedKeys);
      // Iterator<Object> keyI = keys.iterator();
      // while (keyI.hasNext()) {
      for (String key : sortedKeys) {
        // String key = (String)keyI.next();
        if (key.equalsIgnoreCase("PackageName")
          || key.equalsIgnoreCase("Title") ||
          /* key.equalsIgnoreCase("Description") || */
          key.equalsIgnoreCase("DoNotLoadIfFileNotPresentMessage")
          || key.equalsIgnoreCase("DoNotLoadIfClassNotPresentMessage")
          || key.equalsIgnoreCase("DoNotLoadIfEnvVarNotSetMessage")) {

        } else {
          version.append("<tr><td valign=top>" + key
            + ":</td><td width=50></td>");
          String propValue = versionProps.getProperty(key);

          if (!key.equalsIgnoreCase("Description")) {
            propValue = propValue.replace("<", "&#60;");
            propValue = propValue.replace(">", "&#62;");
            propValue = propValue.replace("@", "{[at]}");
            propValue = propValue.replace("\n", "<br/>");
          }

          /*
           * if (key.equals("Author") || key.equals("Maintainer")) { propValue =
           * propValue.replace(".", "{[dot]}"); }
           */

          if (key.equals("PackageURL") || key.equals("URL")) {
            propValue = "<a href=\"" + propValue + "\">" + propValue + "</a>";
          }

          version.append("<td>" + propValue + "</td></tr>\n");
        }
      }

      version.append("</table>\n</body>\n</html>\n");
      String versionHTMLFileName =
        packageDirectory.getAbsolutePath() + File.separator + name + ".html";
      BufferedWriter br =
        new BufferedWriter(new FileWriter(versionHTMLFileName));
      br.write(version.toString());
      br.flush();
      br.close();
    }

    indexBuff.append("</body>\n</html>\n");
    String packageIndexName =
      packageDirectory.getAbsolutePath() + File.separator + "index.html";
    BufferedWriter br = new BufferedWriter(new FileWriter(packageIndexName));
    br.write(indexBuff.toString());
    br.flush();
    br.close();

    // write the versions file to the directory
    String packageVersionsName =
      packageDirectory.getAbsolutePath() + File.separator + "versions.txt";
    br = new BufferedWriter(new FileWriter(packageVersionsName));
    br.write(versionsTextBuffer.toString());
    br.flush();
    br.close();

    // return indexBuff.toString();
    String[] returnInfo = new String[3];
    returnInfo[0] = packageTitle;
    returnInfo[1] = packageCategory;
    returnInfo[2] = latestVersion;
    return returnInfo;
  }

  private static void writeMainIndex(Map<String, String[]> packages,
    File repositoryHome) throws Exception {
    StringBuffer indexBuf = new StringBuffer();
    StringBuffer packageList = new StringBuffer();

    // new package list that includes version number of latest version of each
    // package. We keep the old package list as well for backwards compatibility
    StringBuffer packageListPlusVersion = new StringBuffer();

    indexBuf.append(HEADER + BIRD_IMAGE1);
    indexBuf.append(PENTAHO_IMAGE1);
    indexBuf.append("<h1>WEKA Packages </h1>\n\n");
    indexBuf.append(/*
                     * "<h3>Download WekaLite</h3>\n<a href=\"wekaLite.jar\">wekaLite.jar</a>"
                     * +
                     */
    "<p><b>IMPORTANT: make sure there are no old versions of Weka (<3.7.2) in "
      + "your CLASSPATH before starting Weka</b>\n\n");

    indexBuf.append("<h3>Installation of Packages</h3>\n");
    indexBuf
      .append("A GUI package manager is available from the \"Tools\" menu of"
        + " the GUIChooser<br><br><code>java -jar weka.jar</code><p>\n\n");

    indexBuf.append("For a command line package manager type"
      + ":<br><br<code>java weka.core.WekaPackageManager -h"
      + "</code><br><br>\n");
    indexBuf.append("<hr/>\n");

    indexBuf
      .append("<h3>Running packaged algorithms from the command line</h3>"
        + "<code>java weka.Run [algorithm name]</code><p>"
        + "Substring matching is also supported. E.g. try:<br><br>"
        + "<code>java weka.Run Bayes</code><hr/>");

    Set<String> names = packages.keySet();
    indexBuf.append("<h3>Available Packages (" + packages.keySet().size()
      + ")</h3>\n\n");

    indexBuf.append("<table>\n");
    Iterator<String> i = names.iterator();
    while (i.hasNext()) {
      String packageName = i.next();
      String[] info = packages.get(packageName);
      String packageTitle = info[0];
      String packageCategory = info[1];
      String latestVersion = info[2];
      String href =
        "<a href=\"./" + packageName + "/index.html\">" + packageName + "</a>";

      indexBuf.append("<tr valign=\"top\">\n");
      indexBuf.append("<td>" + href + "</td><td width=50></td><td>"
        + packageCategory + "</td><td width=50></td><td>" + packageTitle
        + "</td></tr>\n");

      // append to the package list
      packageList.append(packageName + "\n");
      packageListPlusVersion.append(packageName).append(":")
        .append(latestVersion).append("\n");
    }

    indexBuf.append("</table>\n<hr/>\n</body></html>\n");
    String indexName =
      repositoryHome.getAbsolutePath() + File.separator + "index.html";
    BufferedWriter br = new BufferedWriter(new FileWriter(indexName));
    br.write(indexBuf.toString());
    br.flush();
    br.close();

    String packageListName =
      repositoryHome.getAbsolutePath() + File.separator + "packageList.txt";
    br = new BufferedWriter(new FileWriter(packageListName));
    br.write(packageList.toString());
    br.flush();
    br.close();

    packageListName =
      repositoryHome.getAbsolutePath() + File.separator
        + "packageListWithVersion.txt";
    br = new BufferedWriter(new FileWriter(packageListName));
    br.write(packageListPlusVersion.toString());
    br.flush();
    br.close();

    String numPackagesName =
      repositoryHome.getAbsolutePath() + File.separator + "numPackages.txt";
    br = new BufferedWriter(new FileWriter(numPackagesName));
    br.write(packages.keySet().size() + "\n");
    br.flush();
    br.close();

    // create zip archive
    writeRepoZipFile(repositoryHome, packageList);
  }

  private static void transBytes(BufferedInputStream bi, ZipOutputStream z)
    throws Exception {
    int b;
    while ((b = bi.read()) != -1) {
      z.write(b);
    }
  }

  protected static void writeZipEntryForPackage(File repositoryHome,
    String packageName, ZipOutputStream zos) throws Exception {

    ZipEntry packageDir = new ZipEntry(packageName + "/");
    zos.putNextEntry(packageDir);

    ZipEntry z = new ZipEntry(packageName + "/Latest.props");
    ZipEntry z2 = new ZipEntry(packageName + "/Latest.html");

    FileInputStream fi =
      new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
        + packageName + File.separator + "Latest.props");
    BufferedInputStream bi = new BufferedInputStream(fi);
    zos.putNextEntry(z);
    transBytes(bi, zos);
    bi.close();

    fi =
      new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
        + packageName + File.separator + "Latest.html");
    bi = new BufferedInputStream(fi);
    zos.putNextEntry(z2);
    transBytes(bi, zos);
    bi.close();

    // write the versions.txt file to the zip
    z = new ZipEntry(packageName + "/versions.txt");
    fi = new FileInputStream(packageName + File.separator + "versions.txt");
    bi = new BufferedInputStream(fi);
    zos.putNextEntry(z);
    transBytes(bi, zos);
    bi.close();

    // write the index.html to the zip
    z = new ZipEntry(packageName + "/index.html");
    fi =
      new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
        + packageName + File.separator + "index.html");
    bi = new BufferedInputStream(fi);
    zos.putNextEntry(z);
    transBytes(bi, zos);
    bi.close();

    // Now process the available versions
    FileReader vi =
      new FileReader(repositoryHome.getAbsolutePath() + File.separator
        + packageName + File.separator + "versions.txt");
    BufferedReader bvi = new BufferedReader(vi);
    String version;
    while ((version = bvi.readLine()) != null) {
      z = new ZipEntry(packageName + "/" + version + ".props");
      fi =
        new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
          + packageName + File.separator + version + ".props");
      bi = new BufferedInputStream(fi);
      zos.putNextEntry(z);
      transBytes(bi, zos);
      bi.close();

      z = new ZipEntry(packageName + "/" + version + ".html");
      fi =
        new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
          + packageName + File.separator + version + ".html");
      bi = new BufferedInputStream(fi);
      zos.putNextEntry(z);
      transBytes(bi, zos);
      bi.close();
    }
    bvi.close();
  }

  protected static void writeRepoZipFile(File repositoryHome,
    StringBuffer packagesList) {

    System.out.print("Writing repo archive");
    StringReader r = new StringReader(packagesList.toString());
    BufferedReader br = new BufferedReader(r);
    String packageName;

    try {
      FileOutputStream fo =
        new FileOutputStream(repositoryHome.getAbsolutePath() + File.separator
          + "repo.zip");
      ZipOutputStream zos = new ZipOutputStream(fo);

      while ((packageName = br.readLine()) != null) {
        writeZipEntryForPackage(repositoryHome, packageName, zos);
        System.out.print(".");
      }
      System.out.println();

      // include the package list (legacy) in the zip
      ZipEntry z = new ZipEntry("packageList.txt");
      FileInputStream fi =
        new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
          + "packageList.txt");
      BufferedInputStream bi = new BufferedInputStream(fi);
      zos.putNextEntry(z);
      transBytes(bi, zos);
      bi.close();

      // include the package list with latest version numbers
      z = new ZipEntry("packageListWithVersion.txt");
      fi =
        new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
          + "packageListWithVersion.txt");
      bi = new BufferedInputStream(fi);
      zos.putNextEntry(z);
      transBytes(bi, zos);
      bi.close();

      // include the forced refresh count file (if it exists)
      File forced =
        new File(repositoryHome.getAbsolutePath() + File.separator
          + WekaPackageManager.FORCED_REFRESH_COUNT_FILE);
      if (forced.exists()) {
        z = new ZipEntry(WekaPackageManager.FORCED_REFRESH_COUNT_FILE);
        fi = new FileInputStream(forced);
        bi = new BufferedInputStream(fi);
        zos.putNextEntry(z);
        transBytes(bi, zos);
        bi.close();
      }

      // Include the top level images
      FileReader fr =
        new FileReader(repositoryHome.getAbsolutePath() + File.separator
          + "images.txt");
      br = new BufferedReader(fr);
      String imageName;
      while ((imageName = br.readLine()) != null) {
        z = new ZipEntry(imageName);
        fi =
          new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
            + imageName);
        bi = new BufferedInputStream(fi);
        zos.putNextEntry(z);
        transBytes(bi, zos);
        bi.close();
      }

      // include the image list in the zip
      z = new ZipEntry("images.txt");
      fi =
        new FileInputStream(repositoryHome.getAbsolutePath() + File.separator
          + "images.txt");
      bi = new BufferedInputStream(fi);
      zos.putNextEntry(z);
      transBytes(bi, zos);
      bi.close();

      zos.close();

      File f =
        new File(repositoryHome.getAbsolutePath() + File.separator + "repo.zip");
      long size = f.length();

      // write the size of the repo (in KB) to a file
      FileWriter fw =
        new FileWriter(repositoryHome.getAbsolutePath() + File.separator
          + "repoSize.txt");
      if (size > 1024) {
        size /= 1024;
      }
      fw.write("" + size);
      fw.flush();
      fw.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Main method for running the RepositoryIndexGenerator
   * 
   * @param args first argument needs to be the path the the repository.
   */
  public static void main(String[] args) {
    try {

      if (args.length < 1) {
        System.err
          .println("Usage:\n\n\tRepositoryIndexGenerator <path to repository>");
        System.exit(1);
      }

      File repositoryHome = new File(args[0]);
      TreeMap<String, String[]> packages = new TreeMap<String, String[]>();

      // ArrayList<File> packages = new ArrayList<File>();
      File[] contents = repositoryHome.listFiles();

      for (File content : contents) {
        if (content.isDirectory()) {
          // packages.add(contents[i]);

          // process this package and create its index
          String[] packageInfo = processPackage(content);
          packages.put(content.getName(), packageInfo);
        }
      }

      // write the main index file
      writeMainIndex(packages, repositoryHome);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
