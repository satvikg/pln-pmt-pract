/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package indicators.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Bogdan
 */
public class FeatureExtractor {
    private List<Map<String, Object>> data;
    public FeatureExtractor(String file) throws SAXException, IOException{
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
                        //System.out.println(fVector);
                        data.add(fVector);
                    }
                    
                    
                }
                
            }
        }
        
        
    }
    public List<Map<String, Object>> getFeatures(){
        return data;
    }
}
