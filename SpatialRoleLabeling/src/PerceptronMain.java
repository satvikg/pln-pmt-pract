import com.aliasi.classify.PerceptronClassifier;
import com.aliasi.matrix.DotProductKernel;
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
    
    public static void main(String args[]){
        try {
            ArrayList<Map<String, Object>> features = WordsFeatureExtractor("SItrain.xml");
            System.out.println(features);
            //TODO: It misses the Corpus and the FeatureExtractor attributes...
            //PerceptronClassifier p = new PerceptronClassifier(null, null, new DotProductKernel(), null, "SI", numIterations);
            //TODO: Classifying instances
            //p.classify(null);
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
        ArrayList<String> sentenceDep = new ArrayList<String>();

        try {
            LexicalizedParser lp = new LexicalizedParser("grammar/englishPCFG.ser.gz");
            MaxentTagger tagger = new MaxentTagger("left3words-wsj-0-18.tagger");
            ArrayList<String> sentences = new ArrayList<String>();

            DOMParser parser = new DOMParser();
            parser.parse(file);
            Document doc = (Document) parser.getDocument();
            NodeList adnotatedSentences = doc.getElementsByTagName("SENTENCE");
            for(int i = 0; i < adnotatedSentences.getLength(); i++){
                Node adnotatedSentence = adnotatedSentences.item(i);
                NodeList children = adnotatedSentence.getChildNodes();
                for(int j = 0; j < children.getLength(); j++){
                    Node section = children.item(j);
                    if(section.getNodeName().equals("CONTENT"))
                        sentences.add(section.getFirstChild().getNodeValue());
                }
            }
            for (int i = 0; i < sentences.size(); i++) {
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
                    
                    //TODO: Missing extract "The binary linear position of w with respect to the s (e.g., before or not)."
                    //PUT HERE THE VALUE: wordFeatures.put("WORD_BINARY", binary);
                    
                    words.add(wordFeatures);
                    //System.out.println(wordFeatures);
                } 
            } 
        } catch (Exception ex) {
            Logger.getLogger(PerceptronMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return words;
    }
    
}
