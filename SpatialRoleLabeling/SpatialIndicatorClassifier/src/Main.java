/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Bogdan
 */

import indicators.extractor.FeatureExtractor;
import indicators.validator.CrossValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;



public class Main {
    public static void main(String[] args) throws SAXException, IOException{
        FeatureExtractor fe = new FeatureExtractor("SItrain.xml");
        System.out.println(fe.getFeatures());
        List<String> assignments = new ArrayList<String>();
        
        List<String> possibleClasses = new ArrayList<String>();
        List<String> possibleAttributes = new ArrayList<String>();
        
        List<Map<String, Object>> data = fe.getFeatures();
        
        for(int i = 0; i < data.size(); i++){
            Map<String, Object> fVector = data.get(i);
            String cls = (String)fVector.get("CLASS");
            if(!possibleClasses.contains(cls))
                possibleClasses.add(cls);            
            assignments.add(cls);
            fVector.remove("CLASS");
            for(String key: fVector.keySet()){
                if(!possibleAttributes.contains(key))
                    possibleAttributes.add(key);
            }
        }
        String[] possibleClassesArray = new String[possibleClasses.size()];
        String[] possibleAttributesArray = new String[possibleAttributes.size()];
        System.out.println("PossibleClasses: "+possibleClasses);
        System.out.println("PossibleAttributes: "+possibleAttributes);
        for(int i = 0; i < possibleAttributes.size(); i++)
            possibleAttributesArray[i] = possibleAttributes.get(i);
        for(int i = 0; i < possibleClasses.size(); i++)
            possibleClassesArray[i] = possibleClasses.get(i);
        
        CrossValidator validator = new CrossValidator(data, assignments, possibleClassesArray, possibleAttributesArray);
        validator.crossValidate(5); 
        
        
    }
}
