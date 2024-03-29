package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.io.NumberRangeFileFilter;
import edu.stanford.nlp.ling.StringLabelFactory;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.Pair;
import java.io.Reader;
import java.text.NumberFormat;
import java.util.*;

/**
 * See what parent annotation helps in treebank, based on support and
 * KL divergence.
 *
 * @author Christopher Manning
 * @version 2003/01/04
 */
public class ParentAnnotationStats implements TreeVisitor {

  private TreebankLanguagePack tlp;

  public ParentAnnotationStats() {
    this(null);
  }

  private ParentAnnotationStats(TreebankLanguagePack tlp) {
    this.tlp = tlp;
  }

  private static boolean doTags = false;

  private Map nodeRules = new HashMap();
  private Map pRules = new HashMap();
  private Map gPRules = new HashMap();

  // corresponding ones for tags
  private Map tagNodeRules = new HashMap();
  private Map tagPRules = new HashMap();
  private Map tagGPRules = new HashMap();

  /**
   * Minimum support * KL to be included in output and as feature
   */
  public static final double[] CUTOFFS = {100.0, 200.0, 500.0, 1000.0};

  /**
   * Minimum support of parent annotated node for grandparent to be
   * studied.  Just there to reduce runtime and printout size.
   */
  public static final double SUPPCUTOFF = 100.0;

  /**
   * Does whatever one needs to do to a particular parse tree
   */
  public void visitTree(Tree t) {
    processTreeHelper("TOP", "TOP", t);
  }

  public static List kidLabels(Tree t) {
    Tree[] kids = t.children();
    List l = new ArrayList(kids.length);
    for (int i = 0; i < kids.length; i++) {
      l.add(kids[i].label().value());
    }
    return l;
  }

  public void processTreeHelper(String gP, String p, Tree t) {
    if (!t.isLeaf() && (doTags || !t.isPreTerminal())) { // stop at words/tags
      Map nr;
      Map pr;
      Map gpr;
      if (t.isPreTerminal()) {
        nr = tagNodeRules;
        pr = tagPRules;
        gpr = tagGPRules;
      } else {
        nr = nodeRules;
        pr = pRules;
        gpr = gPRules;
      }
      String n = t.label().value();
      if (tlp != null) {
        p = tlp.basicCategory(p);
        gP = tlp.basicCategory(gP);
      }
      List kidn = kidLabels(t);
      ClassicCounter cntr = (ClassicCounter) nr.get(n);
      if (cntr == null) {
        cntr = new ClassicCounter();
        nr.put(n, cntr);
      }
      cntr.incrementCount(kidn);
      List pairStr = new ArrayList(2);
      pairStr.add(n);
      pairStr.add(p);
      cntr = (ClassicCounter) pr.get(pairStr);
      if (cntr == null) {
        cntr = new ClassicCounter();
        pr.put(pairStr, cntr);
      }
      cntr.incrementCount(kidn);
      List tripleStr = new ArrayList(3);
      tripleStr.add(n);
      tripleStr.add(p);
      tripleStr.add(gP);
      cntr = (ClassicCounter) gpr.get(tripleStr);
      if (cntr == null) {
        cntr = new ClassicCounter();
        gpr.put(tripleStr, cntr);
      }
      cntr.incrementCount(kidn);
      Tree[] kids = t.children();
      for (int i = 0; i < kids.length; i++) {
        processTreeHelper(p, n, kids[i]);
      }
    }
  }


  public void printStats() {
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMaximumFractionDigits(2);
    // System.out.println("Node rules");
    // System.out.println(nodeRules);
    // System.out.println("Parent rules");
    // System.out.println(pRules);
    // System.out.println("Grandparent rules");
    // System.out.println(gPRules);

    // Store java code for selSplit
    StringBuffer[] javaSB = new StringBuffer[CUTOFFS.length];
    for (int i = 0; i < CUTOFFS.length; i++) {
      javaSB[i] = new StringBuffer("  private static String[] splitters" + (i + 1) + " = new String[] {");
    }

    ClassicCounter allScores = new ClassicCounter();
    // do value of parent
    for (Iterator it = nodeRules.keySet().iterator(); it.hasNext();) {
      ArrayList answers = new ArrayList();
      String node = (String) it.next();
      ClassicCounter cntr = (ClassicCounter) nodeRules.get(node);
      double support = (cntr.totalCount());
      System.out.println("Node " + node + " support is " + support);
      for (Iterator it2 = pRules.keySet().iterator(); it2.hasNext();) {
        List key = (List) it2.next();
        if (key.get(0).equals(node)) {   // only do it if they match
          ClassicCounter cntr2 = (ClassicCounter) pRules.get(key);
          double support2 = (cntr2.totalCount());
          double kl = Counters.klDivergence(cntr2, cntr);
          System.out.println("KL(" + key + "||" + node + ") = " + nf.format(kl) + "\t" + "support(" + key + ") = " + support2);
          double score = kl * support2;
          answers.add(new Pair(key, new Double(score)));
          allScores.setCount(key,score);
        }
      }
      System.out.println("----");
      System.out.println("Sorted descending support * KL");
      Collections.sort(answers, new Comparator() {
        public int compare(Object o1, Object o2) {
          Pair p1 = (Pair) o1;
          Pair p2 = (Pair) o2;
          Double p12 = (Double) p1.second();
          Double p22 = (Double) p2.second();
          return p22.compareTo(p12);
        }
      });
      for (int i = 0, size = answers.size(); i < size; i++) {
        Pair p = (Pair) answers.get(i);
        double psd = ((Double) p.second()).doubleValue();
        System.out.println(p.first() + ": " + nf.format(psd));
        if (psd >= CUTOFFS[0]) {
          List lst = (List) p.first();
          String nd = (String) lst.get(0);
          String par = (String) lst.get(1);
          for (int j = 0; j < CUTOFFS.length; j++) {
            if (psd >= CUTOFFS[j]) {
              javaSB[j].append("\"").append(nd).append("^");
              javaSB[j].append(par).append("\", ");
            }
          }
        }
      }
      System.out.println();
    }

    /*
          // do value of parent with info gain -- yet to finish this
          for (Iterator it = nodeRules.entrySet().iterator(); it.hasNext(); ) {
              Map.Entry pair = (Map.Entry) it.next();
              String node = (String) pair.getKey();
              Counter cntr = (Counter) pair.getValue();
              double support = (cntr.totalCount());
              System.out.println("Node " + node + " support is " + support);
              ArrayList dtrs = new ArrayList();
              for (Iterator it2 = pRules.entrySet().iterator(); it2.hasNext();) {
                  HashMap annotated = new HashMap();
                  Map.Entry pair2 = (Map.Entry) it2.next();
                  List node2 = (List) pair2.getKey();
                  Counter cntr2 = (Counter) pair2.getValue();
                  if (node2.get(0).equals(node)) {   // only do it if they match
                      annotated.put(node2, cntr2);
                  }
              }

              // upto

              List answers = new ArrayList();
              System.out.println("----");
              System.out.println("Sorted descending support * KL");
              Collections.sort(answers,
                               new Comparator() {
                                   public int compare(Object o1, Object o2) {
                                       Pair p1 = (Pair) o1;
                                       Pair p2 = (Pair) o2;
                                       Double p12 = (Double) p1.second();
                                       Double p22 = (Double) p2.second();
                                       return p22.compareTo(p12);
                                   }
                               });
              for (int i = 0, size = answers.size(); i < size; i++) {
                  Pair p = (Pair) answers.get(i);
                  double psd = ((Double) p.second()).doubleValue();
                  System.out.println(p.first() + ": " + nf.format(psd));
                  if (psd >= CUTOFFS[0]) {
                      List lst = (List) p.first();
                      String nd = (String) lst.get(0);
                      String par = (String) lst.get(1);
                      for (int j=0; j < CUTOFFS.length; j++) {
                          if (psd >= CUTOFFS[j]) {
                              javaSB[j].append("\"").append(nd).append("^");
                              javaSB[j].append(par).append("\", ");
                          }
                      }
                  }
              }
              System.out.println();
          }
    */

    // do value of grandparent
    for (Iterator it = pRules.keySet().iterator(); it.hasNext();) {
      ArrayList answers = new ArrayList();
      List node = (List) it.next();
      ClassicCounter cntr = (ClassicCounter) pRules.get(node);
      double support = (cntr.totalCount());
      if (support < SUPPCUTOFF) {
        continue;
      }
      System.out.println("Node " + node + " support is " + support);
      for (Iterator it2 = gPRules.keySet().iterator(); it2.hasNext();) {
        List key = (List) it2.next();
        if (key.get(0).equals(node.get(0)) && key.get(1).equals(node.get(1))) {  // only do it if they match
          ClassicCounter cntr2 = (ClassicCounter) gPRules.get(key);
          double support2 = (cntr2.totalCount());
          double kl = Counters.klDivergence(cntr2, cntr);
          System.out.println("KL(" + key + "||" + node + ") = " + nf.format(kl) + "\t" + "support(" + key + ") = " + support2);
          double score = kl * support2;
          answers.add(new Pair(key, new Double(score)));
          allScores.setCount(key,score);
        }
      }
      System.out.println("----");
      System.out.println("Sorted descending support * KL");
      Collections.sort(answers, new Comparator() {
        public int compare(Object o1, Object o2) {
          Pair p1 = (Pair) o1;
          Pair p2 = (Pair) o2;
          Double p12 = (Double) p1.second();
          Double p22 = (Double) p2.second();
          return p22.compareTo(p12);
        }
      });
      for (int i = 0, size = answers.size(); i < size; i++) {
        Pair p = (Pair) answers.get(i);
        double psd = ((Double) p.second()).doubleValue();
        System.out.println(p.first() + ": " + nf.format(psd));
        if (psd >= CUTOFFS[0]) {
          List lst = (List) p.first();
          String nd = (String) lst.get(0);
          String par = (String) lst.get(1);
          String gpar = (String) lst.get(2);
          for (int j = 0; j < CUTOFFS.length; j++) {
            if (psd >= CUTOFFS[j]) {
              javaSB[j].append("\"").append(nd).append("^");
              javaSB[j].append(par).append("~");
              javaSB[j].append(gpar).append("\", ");
            }
          }
        }
      }
      System.out.println();
    }
    System.out.println();

    System.out.println("All scores:");
    edu.stanford.nlp.util.PriorityQueue pq = Counters.toPriorityQueue(allScores);
    while(! pq.isEmpty()) {
      Object key = pq.getFirst();
      double score = pq.getPriority(key);
      pq.removeFirst();
      System.out.println(key + "\t" + score);
    }

    System.out.println("  // Automatically generated by ParentAnnotationStats -- preferably don't edit");
    for (int i = 0; i < CUTOFFS.length; i++) {
      int len = javaSB[i].length();
      javaSB[i].replace(len - 2, len, "};");
      System.out.println(javaSB[i]);
    }
    System.out.print("  public static HashSet splitters = new HashSet(Arrays.asList(");
    for (int i = CUTOFFS.length; i > 0; i--) {
      if (i == 1) {
        System.out.print("splitters1");
      } else {
        System.out.print("selectiveSplit" + i + " ? splitters" + i + " : (");
      }
    }
    // need to print extra one to close other things open
    for (int i = CUTOFFS.length; i >= 0; i--) {
      System.out.print(")");
    }
    System.out.println(";");
  }


  private void getSplitters(double cutOff, Map nr, Map pr, Map gpr, Set splitters) {

    // do value of parent
    for (Iterator it = nr.keySet().iterator(); it.hasNext();) {
      ArrayList answers = new ArrayList();
      String node = (String) it.next();
      ClassicCounter cntr = (ClassicCounter) nr.get(node);
      double support = (cntr.totalCount());
      for (Iterator it2 = pr.keySet().iterator(); it2.hasNext();) {
        List key = (List) it2.next();
        if (key.get(0).equals(node)) {   // only do it if they match
          ClassicCounter cntr2 = (ClassicCounter) pr.get(key);
          double support2 = (cntr2.totalCount());
          double kl = Counters.klDivergence(cntr2, cntr);
          answers.add(new Pair(key, new Double(kl * support2)));
        }
      }
      Collections.sort(answers, new Comparator() {
        public int compare(Object o1, Object o2) {
          Pair p1 = (Pair) o1;
          Pair p2 = (Pair) o2;
          Double p12 = (Double) p1.second();
          Double p22 = (Double) p2.second();
          return p22.compareTo(p12);
        }
      });
      for (int i = 0, size = answers.size(); i < size; i++) {
        Pair p = (Pair) answers.get(i);
        double psd = ((Double) p.second()).doubleValue();
        if (psd >= cutOff) {
          List lst = (List) p.first();
          String nd = (String) lst.get(0);
          String par = (String) lst.get(1);
          String name = nd + "^" + par;
          splitters.add(name);
        }
      }
    }

    /*
          // do value of parent with info gain -- yet to finish this
          for (Iterator it = nr.entrySet().iterator(); it.hasNext(); ) {
              Map.Entry pair = (Map.Entry) it.next();
              String node = (String) pair.getKey();
              Counter cntr = (Counter) pair.getValue();
              double support = (cntr.totalCount());
              ArrayList dtrs = new ArrayList();
              for (Iterator it2 = pr.entrySet().iterator(); it2.hasNext();) {
                  HashMap annotated = new HashMap();
                  Map.Entry pair2 = (Map.Entry) it2.next();
                  List node2 = (List) pair2.getKey();
                  Counter cntr2 = (Counter) pair2.getValue();
                  if (node2.get(0).equals(node)) {   // only do it if they match
                      annotated.put(node2, cntr2);
                  }
              }

              // upto

              List answers = new ArrayList();
              Collections.sort(answers,
                               new Comparator() {
                                   public int compare(Object o1, Object o2) {
                                       Pair p1 = (Pair) o1;
                                       Pair p2 = (Pair) o2;
                                       Double p12 = (Double) p1.second();
                                       Double p22 = (Double) p2.second();
                                       return p22.compareTo(p12);
                                   }
                               });
              for (int i = 0, size = answers.size(); i < size; i++) {
                  Pair p = (Pair) answers.get(i);
                  double psd = ((Double) p.second()).doubleValue();
                  if (psd >= cutOff) {
                      List lst = (List) p.first();
                      String nd = (String) lst.get(0);
                      String par = (String) lst.get(1);
                      String name = nd + "^" + par;
                      splitters.add(name);
                  }
              }
          }
    */

    // do value of grandparent
    for (Iterator it = pr.keySet().iterator(); it.hasNext();) {
      ArrayList answers = new ArrayList();
      List node = (List) it.next();
      ClassicCounter cntr = (ClassicCounter) pr.get(node);
      double support = (cntr.totalCount());
      if (support < SUPPCUTOFF) {
        continue;
      }
      for (Iterator it2 = gpr.keySet().iterator(); it2.hasNext();) {
        List key = (List) it2.next();
        if (key.get(0).equals(node.get(0)) && key.get(1).equals(node.get(1))) {
          // only do it if they match
          ClassicCounter cntr2 = (ClassicCounter) gpr.get(key);
          double support2 = (cntr2.totalCount());
          double kl = Counters.klDivergence(cntr2, cntr);
          answers.add(new Pair(key, new Double(kl * support2)));
        }
      }
      Collections.sort(answers, new Comparator() {
        public int compare(Object o1, Object o2) {
          Pair p1 = (Pair) o1;
          Pair p2 = (Pair) o2;
          Double p12 = (Double) p1.second();
          Double p22 = (Double) p2.second();
          return p22.compareTo(p12);
        }
      });
      for (int i = 0, size = answers.size(); i < size; i++) {
        Pair p = (Pair) answers.get(i);
        double psd = ((Double) p.second()).doubleValue();
        if (psd >= cutOff) {
          List lst = (List) p.first();
          String nd = (String) lst.get(0);
          String par = (String) lst.get(1);
          String gpar = (String) lst.get(2);
          String name = nd + "^" + par + "~" + gpar;
          splitters.add(name);
        }
      }
    }
  }


  /**
   * Calculate parent annotation statistics suitable for doing
   * selective parent splitting in the PCFGParser inside
   * FactoredParser.  <p>
   * Usage: java edu.stanford.nlp.parser.lexparser.ParentAnnotationStats
   * [-tags] treebankPath
   *
   * @param args One argument: path to the Treebank
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: java edu.stanford.nlp.parser.lexparser.ParentAnnotationStats [-tags] treebankPath");
    } else {
      int i = 0;
      boolean useCutOff = false;
      double cutOff = 0.0;
      while (args[i].startsWith("-")) {
        if (args[i].equals("-tags")) {
          doTags = true;
          i++;
        } else if (args[i].equals("-cutOff") && i + 1 < args.length) {
          useCutOff = true;
          cutOff = Double.parseDouble(args[i + 1]);
          i += 2;
        } else {
          System.err.println("Unknown option: " + args[i]);
          i++;
        }
      }

      Treebank treebank = new DiskTreebank(new TreeReaderFactory() {
        public TreeReader newTreeReader(Reader in) {
          return new PennTreeReader(in, new LabeledScoredTreeFactory(new StringLabelFactory()), new BobChrisTreeNormalizer());
        }
      });
      treebank.loadPath(args[i]);

      if (useCutOff) {
        Set<String> splitters = getSplitCategories(treebank, doTags, 0, cutOff, cutOff, null);
        System.out.println(splitters);
      } else {
        ParentAnnotationStats pas = new ParentAnnotationStats();
        treebank.apply(pas);
        pas.printStats();
      }
    }
  }


  /**
   * Call this method to get a String array of categories to split on.
   * It calculates parent annotation statistics suitable for doing
   * selective parent splitting in the PCFGParser inside
   * FactoredParser.  <p>
   * If tlp is non-null tlp.basicCategory() will be called on parent and
   * grandparent nodes. <p>
   * This version just defaults some parameters.
   * <i>Implementation note:</i> This method is not designed for concurrent
   * invocation: it uses static state variables.
   */
  public static Set<String> getSplitCategories(Treebank t, double cutOff, TreebankLanguagePack tlp) {
    return getSplitCategories(t, true, 0, cutOff, cutOff, tlp);
  }


  /**
   * Call this method to get a String array of categories to split on.
   * It calculates parent annotation statistics suitable for doing
   * selective parent splitting in the PCFGParser inside
   * FactoredParser.  <p>
   * If tlp is non-null tlp.basicCategory() will be called on parent and
   * grandparent nodes. <p>
   * <i>Implementation note:</i> This method is not designed for concurrent
   * invocation: it uses static state variables.
   */
  public static Set<String> getSplitCategories(Treebank t, boolean doTags, int algorithm, double phrasalCutOff, double tagCutOff, TreebankLanguagePack tlp) {
    ParentAnnotationStats.doTags = doTags;
    ParentAnnotationStats pas = new ParentAnnotationStats(tlp);
    t.apply(pas);
    Set<String> splitters = new HashSet<String>();
    pas.getSplitters(phrasalCutOff, pas.nodeRules, pas.pRules, pas.gPRules, splitters);
    pas.getSplitters(tagCutOff, pas.tagNodeRules, pas.tagPRules, pas.tagGPRules, splitters);
    return splitters;
  }


  /**
   * This is hardwired to calculate the split categories from English
   * Penn Treebank sections 2-21 with a default cutoff of 300 (as used
   * in ACL03PCFG).  It was added to upgrading of code in cases where no
   * Treebank was available, and the pre-stored list was being used).
   */
  public static Set<String> getEnglishSplitCategories(String treebankRoot) {
    TreebankLangParserParams tlpParams = new EnglishTreebankParserParams();
    Treebank trees = tlpParams.memoryTreebank();
    trees.loadPath(treebankRoot, new NumberRangeFileFilter(200, 2199, true));
    return getSplitCategories(trees, 300.0, tlpParams.treebankLanguagePack());
  }

}
