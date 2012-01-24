import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Matias
 */
public class PerceptronMain {
   
    /*
    public class PrepositionFeature implements FeatureExtractor<Object> {

        @Override
        public Map<String, ? extends Number> features(Object e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    */
    
    private static int numIterations = 100;
    private static boolean TRACE = false;
    
    public static void main(String args[]){
        try {
            ArrayList<Map<String, Object>> features = WordsFeatureExtractor("SItrain.xml");
            ArrayList<Map<String, Float>> numFeatures = numerizeFeatures(features);
            System.out.println(features);
            System.out.println(numFeatures);

            //TODO: Here PERCEPTRON
            AveragedPerceptron avgPerceptron = new AveragedPerceptron();
            
            //TODO: MODIF
            double featureSentences[][][] = {
            {{1,2,1},{1,1,3},{1,2,2},{1,1,1},{1,2,3},{3,2,3},{1,1,3}},
            {{2,2,1},{3,1,3},{1,2,2},{1,2,1}},
            {{1,2,1},{3,2,3},{1,2,2},{1,2,3},{1,1,1}},
            {{1,1,1},{3,2,1},{1,1,2},{1,2,1},{1,1,1}},
            {{2,2,2},{3,3,3},{3,2,3},{1,2,1},{3,1,1},{1,3,1},{1,2,3},{1,3,2},{1,2,1},{1,2,1}},
            {{3,2,3},{3,2,3},{1,1,2},{1,2,1}}
            };
            double taggedSentences[][] = {
                {0,0,0,1,0,-1,0},
                {0,1,0,-1},
                {-1,0,0,1,0},
                {1,0,0,-1,0},
                {1,0,0,-1,0,0,0,0,0,1},
                {0,-1,0,1}
            };
            
            int seqSize = 3;
            
            //For all the instances ("sentences")
            for(int s = 0; s < numFeatures.size(); s++) {
                
                ArrayList featureVectors = new ArrayList(numFeatures.get(s).values());
                ArrayList tagVectors = new ArrayList(numFeatures.get(s).keySet());
                for(int i = 0; i < featureVectors.size(); i++){
                    double[][] featureSentence = featureSentences[i];
                    double[] taggedSentence = taggedSentences[i];
                    for(int j = 0; j <= featureSentence.length-seqSize; j++){
                        double[][] featureChunk = new double[seqSize][];
                        double[] taggedChunk = new double[seqSize];
                        System.arraycopy(featureSentence, j, featureChunk, 0, seqSize);
                        System.arraycopy(taggedSentence, j, taggedChunk, 0, seqSize);
                        //System.out.print(mts(featureChunk)+" ");
                        //System.out.print(ats(taggedChunk)+" ");
                        System.out.println();
                        featureVectors.add(featureChunk);
                        tagVectors.add(taggedChunk);

                    }
                }
                avgPerceptron.train(featureVectors, tagVectors, 10000);
                for(int i = 0; i < 20; i++ ){
                    //System.out.println("["+ats(avgPerceptron.classify((double[][])featureVectors.get(i))));
                    //System.out.println(ats((double[])tagVectors.get(i))+"]");
                }
                //System.out.println(ats(avgPerceptron.classify((double[][])featureVectors.get(0))));
                //System.out.println(ats(avgPerceptron.classify((double[][])featureVectors.get(1))));
            }
            
        } catch (SAXException ex) {
            
        } catch (IOException ex) {
            
        }
    }
    
    public static ArrayList<Map<String, Object>> PrepFeatureExtractor(String file) throws SAXException, IOException{
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        DOMParser parser = new DOMParser();
        parser.parse(file);
        Document doc = (Document) parser.getDocument();
        NodeList adnotatedSentences = doc.getElementsByTagName("SENTENCE");
        for(int i = 0; i < adnotatedSentences.getLength(); i++){
            Node adnotatedSentence = adnotatedSentences.item(i);
            NodeList children = adnotatedSentence.getChildNodes();
            for(int j = 0; j < children.getLength(); j++){
                Node section = children.item(j);
                if(section.getNodeName().equals("PREPS")){
                    
                    NodeList preps = section.getChildNodes();
                    for(int k = 0; k < preps.getLength(); k++){
                        NodeList prepFeatures = preps.item(k).getChildNodes();
                        Map<String, Object> fVector = new HashMap<String, Object>();
                        for(int l = 0; l < prepFeatures.getLength(); l++){
                            Node feature = prepFeatures.item(l);
                            String name = feature.getNodeName();
                            Object value = feature.getChildNodes().item(0).getNodeValue();
                            fVector.put(name, value);
                            
                        }
                        data.add(fVector);
                    }
                    
                    
                }
                
            }
        }
        return data;
    }
    
    public static ArrayList<Map<String, Object>> WordsFeatureExtractor(String file) throws SAXException, IOException{
        
        ArrayList<Map<String, Object>> words = new ArrayList<Map<String, Object>>();

        try {
            LexicalizedParser lp = new LexicalizedParser("grammar/englishPCFG.ser.gz");
            MaxentTagger tagger = new MaxentTagger("left3words-wsj-0-18.tagger");
            ArrayList<String> sentences = new ArrayList<String>();
            ArrayList<ArrayList<String>> prepositions = new ArrayList<ArrayList<String>>();

            DOMParser parser = new DOMParser();
            parser.parse(file);
            Document doc = (Document) parser.getDocument();
            NodeList adnotatedSentences = doc.getElementsByTagName("SENTENCE");
            for(int i = 0; i < adnotatedSentences.getLength(); i++){
                ArrayList<String> sentencePrep = new ArrayList<String>();
                Node adnotatedSentence = adnotatedSentences.item(i);
                NodeList children = adnotatedSentence.getChildNodes();
                for(int j = 0; j < children.getLength(); j++){
                    Node section = children.item(j);
                    if(section.getNodeName().equals("CONTENT"))
                        sentences.add(section.getFirstChild().getNodeValue());
                    if(section.getNodeName().equals("SPATIAL_INDICATOR"))
                        sentencePrep.add(section.getFirstChild().getNodeValue());
                }
                prepositions.add(sentencePrep);
            }

            //USE THIS TO TEST MOAR FASTER
            //for (int i = 0; i < 10; i++) {
            for (int i = 0; i < sentences.size(); i++) {
                
                ArrayList<String> sentencePrep = prepositions.get(i);
                Tree[] prepTrees = new Tree[sentencePrep.size()];
                    
                for (int p=0; p<sentencePrep.size(); p++) {
                    String sentence = sentences.get(i);
                    List<List<HasWord>> tagSentences = MaxentTagger.tokenizeText(new StringReader(sentence));
                    ArrayList<TaggedWord> tSentence = tagger.tagSentence(tagSentences.get(0));

                    //Obtains the tree
                    Tree sentenceTree = lp.apply(sentence); 
                    //Obtains typed dependency
                    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
                    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
                    GrammaticalStructure gs = gsf.newGrammaticalStructure(sentenceTree);
                    List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
                    
//                    System.out.println(sentenceTree.toStringBuilder(new StringBuilder("")));
                    List<Tree> allNodes = PerceptronMain.getAllNodes(
                            sentenceTree);
                    //sentenceTree.getChildrenAsList();
//                    System.out.println(allNodes.size());
                    prepTrees[p] = null; 
                    
                    for(Tree child : allNodes) {
//                        System.out.println(child.label().value());
                        if(child.label() != null && 
                                (child.label().value() + " ").equals(sentencePrep.get(p))) {
                            prepTrees[p] = child;
                        }
                    }

                    for (int t=0; t<tSentence.size(); t++) {
                        Map<String, Object> wordFeatures = new HashMap<String, Object>();
                        String word = tSentence.get(t).word();

                        wordFeatures.put("WORD", word);
                        wordFeatures.put("WORD_POS", tSentence.get(t).tag());
                        boolean found = false;
                        //System.out.println(tdl);
                        for(int typed=0; typed<tdl.size(); typed++) {
                            TypedDependency tdlword = tdl.get(typed);
                            String gov = tdlword.gov().toString().substring(0, tdlword.gov().toString().indexOf("-", 0));
                            //System.out.println(gov + " : " + word);
                            if(gov.equals(word)) {
                                wordFeatures.put("WORD_DEP", tdlword.reln().toString());
                                //System.out.println(tdlword + " " + word + " " + gov + " " + tdlword.reln());
                                found = true;
                            }
                        }    
                        if(!found) {
                            wordFeatures.put("WORD_DEP", "other");
                        }

                        //TODO: Missing extract "The path in the parse tree from the w to the s"
                        //PUT HERE THE VALUE: wordFeatures.put("WORD_PATH", path);
                        
                        Tree wordTree = null;

                        for(Tree child : allNodes) {
    //                        System.out.println(child.label().value());
                            if(child.label() != null && child.label().value().equals(word)) {
                                wordTree = child;
                            }
                        }

//                        System.out.println("!!! " + sentencePrep.get(p));
                        if(wordTree != null && prepTrees[p] != null) {
                            List<Tree> path = sentenceTree.pathNodeToNode(wordTree, prepTrees[p]);

//                            System.out.print("Path " + word + " - " +
//                                    sentencePrep.get(p) + ": ");

                            int counter = 0;
                            String pathString = "";
                            for(Tree el : path) {
                                if(counter > 0 && counter < path.size()-1) {
                                    String pos = PerceptronMain.getValueFromLabel(
                                            el.label().toString(), "ValueAnnotation=", "]");
                                    pathString+= pos;
                                    if(counter < path.size()-2) {
                                        pathString += "-";
                                    }
                                }
                                counter++;
                            }
//                            System.out.println(pathString);
                            wordFeatures.put("WORD_PATH", pathString);
                        }
                        else {
//                            System.err.println("Null wordtree for word " + 
//                                word + " or prep " + sentencePrep.get(p) + "!");
                        }
                    
                    

                        //Binary - LEFT of prep: TRUE, RIGHT of prep: FALSE
                        boolean binary = (sentence.indexOf(sentencePrep.get(p)) - sentence.indexOf(word) > 0);
                        wordFeatures.put("PREP", sentencePrep.get(p));
                        wordFeatures.put("WORD_BINARY", binary);
                        
                        words.add(wordFeatures);
                        if(TRACE) System.out.println(wordFeatures);
                    }
                } 
            } 
        } catch (Exception ex) {
            Logger.getLogger(PerceptronMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return words;
    }

    //Converts all the features in numbers
    private static ArrayList<Map<String, Float>> numerizeFeatures(ArrayList<Map<String, Object>> features) {
        ArrayList<Map<String, Float>> numbers = new ArrayList<Map<String, Float>>();
        for (int i = 0; i < features.size(); i++) {
            Map<String, Object> wordFeature = features.get(i);
            Map<String, Float> numberFeature = new HashMap<String, Float>();
            for (Map.Entry<String, Object> entry : wordFeature.entrySet()) {
                numberFeature.put(entry.getKey(),(float)wordFeature.get(entry.getKey()).toString().hashCode());
            }
            numbers.add(numberFeature);
        }
        return numbers;
    }   
    
    public static List<Tree> getAllNodes(Tree t) {
    if (t.isLeaf()) {
          List<Tree> children = new ArrayList<Tree>();
//          if (t.label() != null) {
              children.add(t);
//          }
          return children;
    } 
    else {
          Tree[] kids = t.children();
//          System.out.println(t.label() + " " + kids.length);
          List<Tree> children = new ArrayList<Tree>();

          if (kids != null) {
            for (Tree kid : kids) {
              children.addAll(PerceptronMain.getAllNodes(kid));
            }
          }
          return children;
          
        }
  }
    
    public static String getValueFromLabel(String label, String left, String right) {
        int indexLeft = label.indexOf(left) + left.length();
        int indexRight = label.indexOf(right);
//        System.out.print("(" + indexLeft + ", " + indexRight + ")");
        
        if(indexRight - indexLeft > 10)
            return " ";
        return label.substring(indexLeft, indexRight);
    }

    
}
