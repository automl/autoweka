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
 *    Run.java
 *    Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka;

import java.util.ArrayList;
import java.util.List;

import weka.core.Utils;

/**
 * Helper class that executes Weka schemes from the command line. Performs
 * Suffix matching on the scheme name entered by the user - e.g.<br>
 * <br>
 * 
 * java weka.Run NaiveBayes <br>
 * <br>
 * 
 * will prompt the user to choose among
 * weka.classifiers.bayes.ComplementNaiveBayes,
 * weka.classifiers.bayes.NaiveBayes,
 * weka.classifiers.bayes.NaiveBayesMultinomial,
 * weka.classifiers.bayes.NaiveBayesMultinomialUpdateable,
 * weka.classifiers.bayes.NaiveBayesSimple,
 * weka.classifiers.bayes.NaiveBayesUpdateable
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12774 $
 * 
 */
public class Run {

  public enum SchemeType {
    CLASSIFIER("classifier"), CLUSTERER("clusterer"), ASSOCIATOR(
      "association rules"), ATTRIBUTE_SELECTION("attribute selection"), FILTER(
      "filter"), LOADER("loader"), SAVER("saver"), DATAGENERATOR(
      "data generator"), COMMANDLINE("general commandline runnable");

    private final String m_stringVal;

    SchemeType(String name) {
      m_stringVal = name;
    }

    @Override
    public String toString() {
      return m_stringVal;
    }
  }

  /**
   * Find a scheme that matches the supplied suffix
   * 
   * @param classType matching schemes must be of this class type
   * @param schemeToFind the name of the scheme to find
   * @param matchAnywhere if true, the name is matched anywhere in the
   *          non-package part of candidate schemes
   * @return a list of fully qualified matching scheme names
   */
  public static List<String> findSchemeMatch(Class<?> classType,
    String schemeToFind, boolean matchAnywhere, boolean notJustRunnables) {
    weka.core.ClassDiscovery.clearCache();
    ArrayList<String> matches = weka.core.ClassDiscovery.find(schemeToFind);
    ArrayList<String> prunedMatches = new ArrayList<String>();
    // prune list for anything that isn't a runnable scheme
    for (int i = 0; i < matches.size(); i++) {
      if (matches.get(i).endsWith(schemeToFind) || matchAnywhere) {
        try {
          Object scheme = java.beans.Beans.instantiate((new Run()).getClass()
            .getClassLoader(), matches.get(i));
          if (classType == null
            || classType.isAssignableFrom(scheme.getClass())) {
            if (notJustRunnables
              || scheme instanceof weka.classifiers.Classifier
              || scheme instanceof weka.clusterers.Clusterer
              || scheme instanceof weka.associations.Associator
              || scheme instanceof weka.attributeSelection.ASEvaluation
              || scheme instanceof weka.filters.Filter
              || scheme instanceof weka.core.converters.AbstractFileLoader
              || scheme instanceof weka.core.converters.AbstractFileSaver
              || scheme instanceof weka.datagenerators.DataGenerator
              || scheme instanceof weka.core.CommandlineRunnable) {
              prunedMatches.add(matches.get(i));
            }
          }
        } catch (Exception ex) {
          // ignore any classes that we can't instantiate due to no no-arg
          // constructor
        }
      }
    }

    return prunedMatches;
  }

  /**
   * Find a scheme that matches the supplied suffix
   * 
   * @param schemeToFind the name of the scheme to find
   * @param matchAnywhere if true, the name is matched anywhere in the
   *          non-package part of candidate schemes
   * @return a list of fully qualified matching scheme names
   */
  public static List<String> findSchemeMatch(String schemeToFind,
    boolean matchAnywhere) {
    return findSchemeMatch(null, schemeToFind, matchAnywhere, false);
  }

  /**
   * Main method for this class. -help or -h prints usage info.
   * 
   * @param args
   */
  public static void main(String[] args) {
    System.setProperty("apple.awt.UIElement", "true");
    try {
      if (args.length == 0 || args[0].equalsIgnoreCase("-h")
        || args[0].equalsIgnoreCase("-help")) {
        System.err
          .println("Usage:\n\tweka.Run [-no-scan] [-no-load] [-match-anywhere] <scheme name [scheme options]>");
        return;
      }
      boolean noScan = false;
      boolean noLoad = false;
      boolean matchAnywhere = false;
      boolean dontPromptIfMultipleMatches = false;

      if (Utils.getFlag("list-packages", args)) {
        weka.core.WekaPackageManager.loadPackages(true, true, false);
        return;
      }

      int schemeIndex = 0;
      if (Utils.getFlag("no-load", args)) {
        noLoad = true;
        schemeIndex++;
      }

      if (Utils.getFlag("no-scan", args)) {
        noScan = true;
        schemeIndex++;
      }

      if (Utils.getFlag("match-anywhere", args)) {
        matchAnywhere = true;
        schemeIndex++;
      }

      if (Utils.getFlag("do-not-prompt-if-multiple-matches", args)) {
        dontPromptIfMultipleMatches = true;
        schemeIndex++;
      }

      if (!noLoad) {
        weka.core.WekaPackageManager.loadPackages(false, true, false);
      }

      String schemeToRun = null;
      String[] options = null;

      if (schemeIndex >= args.length) {
        System.err.println("No scheme name given.");
        return;
      }
      schemeToRun = args[schemeIndex];
      options = new String[args.length - schemeIndex - 1];
      if (options.length > 0) {
        System.arraycopy(args, schemeIndex + 1, options, 0, options.length);
      }

      if (!noScan) {
        List<String> prunedMatches = findSchemeMatch(schemeToRun, matchAnywhere);

        if (prunedMatches.size() == 0) {
          System.err.println("Can't find scheme " + schemeToRun
            + ", or it is not runnable.");
          // System.exit(1);
          return;
        } else if (prunedMatches.size() > 1) {
          if (dontPromptIfMultipleMatches) {
            System.out.println("There are multiple matches:");
            for (int i = 0; i < prunedMatches.size(); i++) {
              System.out.println("\t" + (i + 1) + ") " + prunedMatches.get(i));
            }
            System.out.println("\nPlease make your scheme name more specific "
              + "(i.e. qualify it with more of the package name).");
            return;
          }
          java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.InputStreamReader(System.in));
          boolean done = false;
          while (!done) {
            System.out.println("Select a scheme to run, or <return> to exit:");
            for (int i = 0; i < prunedMatches.size(); i++) {
              System.out.println("\t" + (i + 1) + ") " + prunedMatches.get(i));
            }
            System.out.print("\nEnter a number > ");
            String choice = null;
            int schemeNumber = 0;
            try {
              choice = br.readLine();
              if (choice.equals("")) {
                // System.exit(0);
                return;
              } else {
                schemeNumber = Integer.parseInt(choice);
                schemeNumber--;
                if (schemeNumber >= 0 && schemeNumber < prunedMatches.size()) {
                  schemeToRun = prunedMatches.get(schemeNumber);
                  done = true;
                }
              }
            } catch (java.io.IOException ex) {
              // ignore
            }
          }
        } else {
          schemeToRun = prunedMatches.get(0);
        }
      }

      Object scheme = null;
      try {
        scheme = java.beans.Beans.instantiate((new Run()).getClass()
          .getClassLoader(), schemeToRun);
      } catch (Exception ex) {
        System.err.println(schemeToRun + " is not runnable!");
        // System.exit(1);
        return;
      }
      // now see which interfaces/classes this scheme implements/extends
      ArrayList<SchemeType> types = new ArrayList<SchemeType>();
      if (scheme instanceof weka.core.CommandlineRunnable) {
        types.add(SchemeType.COMMANDLINE);
      } else {
        if (scheme instanceof weka.classifiers.Classifier) {
          types.add(SchemeType.CLASSIFIER);
        }
        if (scheme instanceof weka.clusterers.Clusterer) {
          types.add(SchemeType.CLUSTERER);
        }
        if (scheme instanceof weka.associations.Associator) {
          types.add(SchemeType.ASSOCIATOR);
        }
        if (scheme instanceof weka.attributeSelection.ASEvaluation) {
          types.add(SchemeType.ATTRIBUTE_SELECTION);
        }
        if (scheme instanceof weka.filters.Filter) {
          types.add(SchemeType.FILTER);
        }
        if (scheme instanceof weka.core.converters.AbstractFileLoader) {
          types.add(SchemeType.LOADER);
        }
        if (scheme instanceof weka.core.converters.AbstractFileSaver) {
          types.add(SchemeType.SAVER);
        }
        if (scheme instanceof weka.datagenerators.DataGenerator) {
          types.add(SchemeType.DATAGENERATOR);
        }
      }

      SchemeType selectedType = null;
      if (types.size() == 0) {
        System.err.println("" + schemeToRun + " is not runnable!");
        // System.exit(1);
        return;
      }
      if (types.size() == 1) {
        selectedType = types.get(0);
      } else {
        java.io.BufferedReader br = new java.io.BufferedReader(
          new java.io.InputStreamReader(System.in));
        boolean done = false;
        while (!done) {
          System.out.println("" + schemeToRun
            + " can be executed as any of the following:");
          for (int i = 0; i < types.size(); i++) {
            System.out.println("\t" + (i + 1) + ") " + types.get(i));
          }
          System.out.print("\nEnter a number > ");
          String choice = null;
          int typeNumber = 0;
          try {
            choice = br.readLine();
            if (choice.equals("")) {
              // System.exit(0);
              return;
            } else {
              typeNumber = Integer.parseInt(choice);
              typeNumber--;
              if (typeNumber >= 0 && typeNumber < types.size()) {
                selectedType = types.get(typeNumber);
                done = true;
              }
            }
          } catch (java.io.IOException ex) {
            // ignore
          }
        }
      }

      if (selectedType == SchemeType.CLASSIFIER) {
        weka.classifiers.AbstractClassifier.runClassifier(
          (weka.classifiers.Classifier) scheme, options);
      } else if (selectedType == SchemeType.CLUSTERER) {
        weka.clusterers.AbstractClusterer.runClusterer(
          (weka.clusterers.Clusterer) scheme, options);
      } else if (selectedType == SchemeType.ATTRIBUTE_SELECTION) {
        weka.attributeSelection.ASEvaluation.runEvaluator(
          (weka.attributeSelection.ASEvaluation) scheme, options);
      } else if (selectedType == SchemeType.ASSOCIATOR) {
        weka.associations.AbstractAssociator.runAssociator(
          (weka.associations.Associator) scheme, options);
      } else if (selectedType == SchemeType.FILTER) {
        weka.filters.Filter.runFilter((weka.filters.Filter) scheme, options);
      } else if (selectedType == SchemeType.LOADER) {
        weka.core.converters.AbstractFileLoader.runFileLoader(
          (weka.core.converters.AbstractFileLoader) scheme, options);
      } else if (selectedType == SchemeType.SAVER) {
        weka.core.converters.AbstractFileSaver.runFileSaver(
          (weka.core.converters.AbstractFileSaver) scheme, options);
      } else if (selectedType == SchemeType.DATAGENERATOR) {
        weka.datagenerators.DataGenerator.runDataGenerator(
          (weka.datagenerators.DataGenerator) scheme, options);
      } else if (selectedType == SchemeType.COMMANDLINE) {
        ((weka.core.CommandlineRunnable) scheme).run(scheme, options);
      }
    } catch (Exception e) {
      if (((e.getMessage() != null) && (e.getMessage().indexOf(
        "General options") == -1))
        || (e.getMessage() == null)) {
        e.printStackTrace();
      } else {
        System.err.println(e.getMessage());
      }
    }
  }
}
