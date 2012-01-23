import com.aliasi.classify.PerceptronClassifier;
import com.aliasi.corpus.Corpus;
import com.aliasi.matrix.DotProductKernel;
import com.aliasi.matrix.KernelFunction;
import com.aliasi.features.*;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
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
   
    private static int numIterations = 100;
    
    private static ArrayList<Map<String, Object>> data;
 
    public static void main(String args[]){
        try {
            FeatureExtractor("SItrain.xml");
        } catch (SAXException ex) {
            Logger.getLogger(PerceptronMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PerceptronMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(data);
        try {
            //TODO: It misses the Corpus and the FeatureExtractor attributes...
            PerceptronClassifier p = new PerceptronClassifier(null, new DotProductKernel(), null, "SI", numIterations);
            //TODO: Classifying instances
            p.classify(null);
        } catch (IOException ex) {
            Logger.getLogger(PerceptronMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void FeatureExtractor(String file) throws SAXException, IOException{
        data = new ArrayList<Map<String, Object>>();
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
        
        
    }
}
