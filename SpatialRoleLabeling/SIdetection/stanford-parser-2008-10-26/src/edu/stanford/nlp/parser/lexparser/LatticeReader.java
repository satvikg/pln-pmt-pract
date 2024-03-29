package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.trees.Tree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatticeReader {

  public static boolean DEBUG = false;
  public static boolean PRETTYPRINT = false;
  public static final boolean USESUM = true;
  public static final boolean USEMAX = false;
  private static boolean mergeType = USESUM;
  public static final String SILENCE = "<SIL>";

  private int numStates;
  private List<LatticeReader.LatticeWord> latticeWords;
  private int[] nodeTimes;
  private ArrayList<LatticeWord>[] wordsAtTime;
  private ArrayList<LatticeWord>[] wordsStartAt;
  private ArrayList<LatticeWord>[] wordsEndAt;

  private void readInput(BufferedReader in) throws Exception {

    // GET RID OF COMMENT LINES
    String line = in.readLine();
    while (line.trim().startsWith("#")) {
      line = in.readLine();
    }

    // READ LATTICE
    latticeWords = new ArrayList<LatticeReader.LatticeWord>();

    Pattern wordLinePattern = Pattern.compile("(\\d+)\\s+(\\d+)\\s+lm=(-?\\d+\\.\\d+),am=(-?\\d+\\.\\d+)\\s+([^( ]+)(?:\\((\\d+)\\))?.*");
    Matcher wordLineMatcher = wordLinePattern.matcher(line);

    while (wordLineMatcher.matches()) {
      int startNode = Integer.parseInt(wordLineMatcher.group(1)) - 1;
      int endNode = Integer.parseInt(wordLineMatcher.group(2)) - 1;
      double lm = Double.parseDouble(wordLineMatcher.group(3));
      double am = Double.parseDouble(wordLineMatcher.group(4));
      String word = wordLineMatcher.group(5).toLowerCase();
      String pronun = wordLineMatcher.group(6);

      if (word.equalsIgnoreCase("<s>")) {
        line = in.readLine();
        wordLineMatcher = wordLinePattern.matcher(line);
        continue;
      }
      if (word.equalsIgnoreCase("</s>")) {
        word = Lexicon.BOUNDARY;
      }

      int pronunciation;
      if (pronun == null) {
        pronunciation = 0;
      } else {
        pronunciation = Integer.parseInt(pronun);
      }

      LatticeWord lw = new LatticeWord(word, startNode, endNode, lm, am, pronunciation);
      if (DEBUG) {
        System.err.println(lw);
      }
      latticeWords.add(lw);

      line = in.readLine();
      wordLineMatcher = wordLinePattern.matcher(line);
    }

    // GET NUMBER OF NODES
    numStates = Integer.parseInt(line.trim());
    if (DEBUG) {
      System.err.println(numStates);
    }

    // READ NODE TIMES
    nodeTimes = new int[numStates];

    Pattern nodeTimePattern = Pattern.compile("(\\d+)\\s+t=(\\d+)\\s*");
    Matcher nodeTimeMatcher;

    for (int i = 0; i < numStates; i++) {
      nodeTimeMatcher = nodeTimePattern.matcher(in.readLine());

      if (!nodeTimeMatcher.matches()) {
        System.err.println("Input File Error");
        System.exit(1);
      }

      // assert ((Integer.parseInt(nodeTimeMatcher.group(1))-1) == i) ;

      nodeTimes[i] = Integer.parseInt(nodeTimeMatcher.group(2));

      if (DEBUG) {
        System.err.println(i + "\tt=" + nodeTimes[i]);
      }
    }
  }

  private void mergeSimultaneousNodes() {

    int[] indexMap = new int[nodeTimes.length];

    indexMap[0] = 0;
    int prevNode = 0;
    int prevTime = nodeTimes[0];
    if (DEBUG) {
      System.err.println(0 + " (" + nodeTimes[0] + ")" + "-->" + 0 + " (" + nodeTimes[0] + ") ++");
    }
    for (int i = 1; i < nodeTimes.length; i++) {
      if (prevTime == nodeTimes[i]) {
        indexMap[i] = prevNode;
        if (DEBUG) {
          System.err.println(i + " (" + nodeTimes[i] + ")" + "-->" + prevNode + " (" + nodeTimes[prevNode] + ") **");
        }
      } else {
        indexMap[i] = prevNode = i;
        prevTime = nodeTimes[i];
        if (DEBUG) {
          System.err.println(i + " (" + nodeTimes[i] + ")" + "-->" + prevNode + " (" + nodeTimes[prevNode] + ") ++");
        }
      }
    }

    for  (LatticeWord lw : latticeWords) {
      lw.startNode = indexMap[lw.startNode];
      lw.endNode = indexMap[lw.endNode];
      if (DEBUG) {
        System.err.println(lw);
      }
    }
  }

  private void removeEmptyNodes() {
    int[] indexMap = new int[numStates];
    int j = 0;
    for (int i = 0; i < numStates; i++) {
      indexMap[i] = j;
      if (wordsStartAt[i].size() != 0 || wordsEndAt[i].size() != 0) {
        j++;
      }
    }

    for (LatticeReader.LatticeWord lw : latticeWords) {
      wordsStartAt[lw.startNode].remove(lw);
      wordsEndAt[lw.endNode].remove(lw);
      for (int i = lw.startNode; i < lw.endNode; i++) {
        wordsAtTime[i].remove(lw);
      }

      lw.startNode = indexMap[lw.startNode];
      lw.endNode = indexMap[lw.endNode];
      wordsStartAt[lw.startNode].add(lw);
      wordsEndAt[lw.endNode].add(lw);
      for (int i = lw.startNode; i < lw.endNode; i++) {
        wordsAtTime[i].add(lw);
      }
    }

    numStates = j;
    ArrayList<LatticeWord>[] tmp = wordsAtTime;
    wordsAtTime = new ArrayList[numStates];
    System.arraycopy(tmp, 0, wordsAtTime, 0, numStates);

    tmp = wordsStartAt;
    wordsStartAt = new ArrayList[numStates];
    System.arraycopy(tmp, 0, wordsStartAt, 0, numStates);

    tmp = wordsEndAt;
    wordsEndAt = new ArrayList[numStates];
    System.arraycopy(tmp, 0, wordsEndAt, 0, numStates);

  }

  private void buildWordTimeArrays() {
    buildWordsAtTime();
    buildWordsStartAt();
    buildWordsEndAt();
  }

  private void buildWordsAtTime() {
    wordsAtTime = new ArrayList[numStates];
    for (int i = 0; i < wordsAtTime.length; i++) {
      wordsAtTime[i] = new ArrayList<LatticeWord>();
    }

    for (LatticeWord lw : latticeWords) {
      for (int j = lw.startNode; j <= lw.endNode; j++) {
        wordsAtTime[j].add(lw);
      }
    }
  }

  private void buildWordsStartAt() {
    wordsStartAt = new ArrayList[numStates];
    for (int i = 0; i < wordsStartAt.length; i++) {
      wordsStartAt[i] = new ArrayList<LatticeWord>();
    }

    for (LatticeWord lw : latticeWords) {
      wordsStartAt[lw.startNode].add(lw);
    }
  }

  private void buildWordsEndAt() {
    wordsEndAt = new ArrayList[numStates];
    for (int i = 0; i < wordsEndAt.length; i++) {
      wordsEndAt[i] = new ArrayList<LatticeWord>();
    }

    for (LatticeWord lw : latticeWords) {
      wordsEndAt[lw.endNode].add(lw);
    }
  }

  private void removeRedundency() {

    boolean changed = true;

    while (changed) {
      changed = false;
      for (int i = 0; i < wordsAtTime.length; i++) {
        if (wordsAtTime[i].size() < 2) {
          continue;
        }
        INNER: for (int j = 0; j < wordsAtTime[i].size() - 1; j++) {
          LatticeWord w1 = wordsAtTime[i].get(j);
          for (int k = j + 1; k < wordsAtTime[i].size(); k++) {
            LatticeWord w2 = wordsAtTime[i].get(k);
            if (w1.word.equalsIgnoreCase(w2.word)) {
              if (removeRedundentPair(w1, w2)) {
                //int numMerged = mergeDuplicates();
                //if (DEBUG) { System.err.println("merged " + numMerged + " identical entries."); }
                changed = true;
                //printWords();
                //j--;
                continue INNER;
                //return;
              }
            }
          }
        }
      }
    }
  }

  private boolean removeRedundentPair(LatticeWord w1, LatticeWord w2) {

    if (DEBUG) {
      System.err.println("trying to remove:");
      System.err.println(w1);
      System.err.println(w2);
    }

    int w1Start = w1.startNode;
    int w2Start = w2.startNode;
    int w1End = w1.endNode;
    int w2End = w2.endNode;

    // we must pick new start and end times that are legal
    int newStart, oldStart;
    if (w1Start < w2Start) {
      newStart = w2Start;
      oldStart = w1Start;
    } else {
      newStart = w1Start;
      oldStart = w2Start;
    }

    int newEnd, oldEnd;
    if (w1End < w2End) {
      newEnd = w1End;
      oldEnd = w2End;
    } else {
      newEnd = w2End;
      oldEnd = w1End;
    }

    // check legality (illegality not guarenteed)
    for (LatticeWord lw : wordsStartAt[oldStart]) {
      if (lw.endNode < newStart || ((lw.endNode == newStart) && (lw.endNode != lw.startNode))) {
        if (DEBUG) {
          System.err.println("failed");
        }
        return false;
      }
    }
    for (LatticeWord lw : wordsEndAt[oldEnd]) {
      if (lw.startNode > newEnd || ((lw.startNode == newEnd) && (lw.endNode != lw.startNode))) {
        if (DEBUG) {
          System.err.println("failed");
        }
        return false;
      }
    }

    // change start/end times of adjacent entries
    changeStartTimes(wordsStartAt[oldEnd], newEnd);
    changeEndTimes(wordsEndAt[oldStart], newStart);

    // change start/end times of words adjacent to adjacent entries
    changeStartTimes(wordsStartAt[oldStart], newStart);
    changeEndTimes(wordsEndAt[oldEnd], newEnd);

    if (DEBUG) {
      System.err.println("succeeded");
    }
    return true;
  }


  private void changeStartTimes(List<LatticeWord> words, int newStartTime) {
    ArrayList<LatticeWord> toRemove = new ArrayList<LatticeWord>();
    for (LatticeWord lw : words) {
      latticeWords.remove(lw);
      int oldStartTime = lw.startNode;
      lw.startNode = newStartTime;

      if (latticeWords.contains(lw)) {
        if (DEBUG) {
          System.err.println("duplicate found");
        }
        LatticeWord twin = latticeWords.get(latticeWords.indexOf(lw));
        // assert (twin != lw) ;
        lw.startNode = oldStartTime;
        twin.merge(lw);
        //wordsStartAt[lw.startNode].remove(lw);
        toRemove.add(lw);
        wordsEndAt[lw.endNode].remove(lw);
        for (int i = lw.startNode; i <= lw.endNode; i++) {
          wordsAtTime[i].remove(lw);
        }
      } else {
        if (oldStartTime < newStartTime) {
          for (int i = oldStartTime; i < newStartTime; i++) {
            wordsAtTime[i].remove(lw);
          }
        } else {
          for (int i = newStartTime; i < oldStartTime; i++) {
            wordsAtTime[i].add(lw);
          }
        }
        latticeWords.add(lw);
        if (oldStartTime != newStartTime) {
          //wordsStartAt[oldStartTime].remove(lw);
          toRemove.add(lw);
          wordsStartAt[newStartTime].add(lw);
        }
      }
    }
    words.removeAll(toRemove);
  }

  private void changeEndTimes(List<LatticeWord> words, int newEndTime) {
    ArrayList<LatticeWord> toRemove = new ArrayList<LatticeWord>();
    for (LatticeWord lw : words) {
      latticeWords.remove(lw);
      int oldEndTime = lw.endNode;
      lw.endNode = newEndTime;

      if (latticeWords.contains(lw)) {
        if (DEBUG) {
          System.err.println("duplicate found");
        }
        LatticeWord twin = latticeWords.get(latticeWords.indexOf(lw));
        // assert (twin != lw) ;
        lw.endNode = oldEndTime;
        twin.merge(lw);
        wordsStartAt[lw.startNode].remove(lw);
        //wordsEndAt[lw.endNode].remove(lw);
        toRemove.add(lw);
        for (int i = lw.startNode; i <= lw.endNode; i++) {
          wordsAtTime[i].remove(lw);
        }
      } else {
        if (oldEndTime > newEndTime) {
          for (int i = newEndTime + 1; i <= oldEndTime; i++) {
            wordsAtTime[i].remove(lw);
          }
        } else {
          for (int i = oldEndTime + 1; i <= newEndTime; i++) {
            wordsAtTime[i].add(lw);
          }
        }
        latticeWords.add(lw);
        if (oldEndTime != newEndTime) {
          //wordsEndAt[oldEndTime].remove(lw);
          toRemove.add(lw);
          wordsEndAt[newEndTime].add(lw);
        }
      }
    }
    words.removeAll(toRemove);
  }

  private void removeSilence() {
    ArrayList<LatticeReader.LatticeWord> silences = new ArrayList<LatticeReader.LatticeWord>();
    for (LatticeWord lw : latticeWords) {
      if (lw.word.equalsIgnoreCase(SILENCE)) {
        silences.add(lw);
      }
    }
    for (LatticeWord lw : silences) {
      //if (lw.endNode == numStates) {
      changeEndTimes(wordsEndAt[lw.startNode], lw.endNode);
      //} else {
      //changeStartTimes(wordsStartAt[lw.endNode], lw.startNode);
      //}
    }
    silences.clear();
    for (LatticeReader.LatticeWord lw : latticeWords) {
      if (lw.word.equalsIgnoreCase(SILENCE)) {
        silences.add(lw);
      }
    }
    for (LatticeWord lw : silences) {
      if (lw.word.equalsIgnoreCase(SILENCE)) {
        latticeWords.remove(lw);
        wordsStartAt[lw.startNode].remove(lw);
        wordsEndAt[lw.endNode].remove(lw);
        for (int j = lw.startNode; j <= lw.endNode; j++) {
          wordsAtTime[j].remove(lw);
        }
      }
    }
  }

  private int mergeDuplicates() {
    int numMerged = 0;
    for (int i = 0; i < latticeWords.size() - 1; i++) {
      LatticeWord first = latticeWords.get(i);
      for (int j = i + 1; j < latticeWords.size(); j++) {
        LatticeWord second = latticeWords.get(j);
        if (first.equals(second)) {
          if (DEBUG) {
            System.err.println("removed duplicate");
          }
          first.merge(second);
          latticeWords.remove(j);
          wordsStartAt[second.startNode].remove(second);
          wordsEndAt[second.endNode].remove(second);
          for (int k = second.startNode; k <= second.endNode; k++) {
            wordsAtTime[k].remove(second);
          }
          numMerged++;
          j--;
        }
      }
    }
    return numMerged;
  }

  public void printWords() {
    Collections.sort(latticeWords);
    System.out.println("Words: ");
    for (LatticeWord lw : latticeWords) {
      System.out.println(lw);
    }
  }

  private double getProb(LatticeWord lw) {
    return lw.am * 100.0 + lw.lm;
  }

  //     private LatticeWord[][] nBest(int n) {

  //     }

  public void processLattice() {
    // System.err.println(1);
    buildWordTimeArrays();
    //System.err.println(2);
    removeSilence();
    //System.err.println(3);
    mergeDuplicates();
    //System.err.println(4);
    removeRedundency();
    //System.err.println(5);
    removeEmptyNodes();
    //System.err.println(6);
    if (PRETTYPRINT) {
      printWords();
    }

  }


  public LatticeReader(String filename) throws Exception {
    this(filename, USESUM, false, false);
  }

  public LatticeReader(String filename, boolean mergeType) throws Exception {
    this(filename, mergeType, false, false);
  }

  public LatticeReader(String filename, boolean mergeType, boolean debug, boolean prettyPrint) throws Exception {
    DEBUG = debug;
    PRETTYPRINT = prettyPrint;
    LatticeReader.mergeType = mergeType;

    BufferedReader in = new BufferedReader(new FileReader(filename));
    //System.err.println(-1);
    readInput(in);
    //System.err.println(0);
    if (PRETTYPRINT) {
      printWords();
    }

    processLattice();

  }

  public List<LatticeReader.LatticeWord> getLatticeWords() {
    return latticeWords;
  }

  public int getNumStates() {
    return numStates;
  }

  public List<LatticeReader.LatticeWord> getWordsOverSpan(int a, int b) {
    ArrayList<LatticeReader.LatticeWord> words = new ArrayList<LatticeReader.LatticeWord>();
    for (LatticeWord lw : wordsStartAt[a]) {
      if (lw.endNode == b) {
        words.add(lw);
      }
    }
    return words;
  }

  public static void main(String[] args) throws Exception {

    boolean mergeType = USESUM;
    boolean prettyPrint = true;
    boolean debug = false;
    String parseGram = null;
    String filename = args[0];

    for (int i = 1; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-debug")) {
        debug = true;
      } else if (args[i].equalsIgnoreCase("-useMax")) {
        mergeType = USEMAX;
      } else if (args[i].equalsIgnoreCase("-useSum")) {
        mergeType = USESUM;
      } else if (args[i].equalsIgnoreCase("-noPrettyPrint")) {
        prettyPrint = false;
      } else if (args[i].equalsIgnoreCase("-parser")) {
        parseGram = args[++i];
      } else {
        System.err.println("unrecognized flag: " + args[i]);
        System.err.println("usage: java LatticeReader <file> [ -debug ] [ -useMax ] [ -useSum ] [ -noPrettyPrint ] [ -parser parserFile ]");
        System.exit(0);
      }
    }

    LatticeReader lr = new LatticeReader(filename, mergeType, debug, prettyPrint);

    if (parseGram != null) {
      Options op = new Options();
      op.doDep = false;
      Test.maxLength = 80;
      LexicalizedParser lp = new LexicalizedParser(parseGram, op);
      Test.maxSpanForTags = 80;
      lp.parse(lr);
      Tree t = lp.getBestParse();
      t.pennPrint();
    }
    //lr.processLattice();
  }

  public static class LatticeWord implements Comparable<LatticeWord> {
    public String word;
    public int startNode, endNode;
    public double lm, am;
    public int pronunciation;

    public LatticeWord(String word, int startNode, int endNode, double lm, double am, int pronunciation) {

      this.word = word;
      this.startNode = startNode;
      this.endNode = endNode;
      this.lm = lm;
      this.am = am;
      this.pronunciation = pronunciation;
    }

    public void merge(LatticeWord lw) {
      if (mergeType == USEMAX) {
        am = Math.max(am, lw.am);
        lw.am = am;
      } else if (mergeType == USESUM) {
        double tmp = lw.am;
        lw.am += am;
        am += tmp;
      }
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(startNode).append("\t");
      sb.append(endNode).append("\t");
      sb.append("lm=").append(lm).append(",");
      sb.append("am=").append(am).append("\t");
      sb.append(word);//.append("(").append(pronunciation).append(")");
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof LatticeWord)) {
        return false;
      }
      LatticeWord other = (LatticeWord) o;
      if (!word.equalsIgnoreCase(other.word)) {
        return false;
      }
      if (startNode != other.startNode) {
        return false;
      }
      if (endNode != other.endNode) {
        return false;
      }
      //if (pronunciation != other.pronunciation) { return false; }
      return true;
    }

    public int compareTo(LatticeWord other) {
      if (startNode < other.startNode) {
        return -1;
      } else if (startNode > other.startNode) {
        return 1;
      }

      if (endNode < other.endNode) {
        return -1;
      } else if (endNode > other.endNode) {
        return 1;
      }

      return 0;
    }

  }

}
