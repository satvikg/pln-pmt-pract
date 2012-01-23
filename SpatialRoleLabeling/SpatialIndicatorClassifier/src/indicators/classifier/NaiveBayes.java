package indicators.classifier;


import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Bogdan
 */
public class NaiveBayes implements Classifier{
    protected List<Map<String, Object>> instances;
    protected List<String> classes;
    protected List<String> possibleClasses;
    protected List<String> attributes;
    public Map<String, Map<String, Map<Object, Integer>>> index;
    protected Map<String, Integer> classCount;
    private void p(String s){
        System.err.println(s);
    }
    public NaiveBayes(String[] classes, String[] attributes){
        this.possibleClasses = Arrays.asList(classes);
        this.classes = new ArrayList<String>();
        this.attributes = Arrays.asList(attributes);
        this.instances = new ArrayList<Map<String, Object>>();
        index = new HashMap<String, Map<String, Map<Object, Integer>>>();
        classCount = new HashMap<String, Integer>();
        for(int i = 0; i < classes.length; i++){
            index.put(classes[i], new HashMap<String, Map<Object, Integer>>());
            for(int j = 0; j < attributes.length; j++){
                //HashMap<String, Map<Object, Integer>> aux = index.get(classes[i]);
                index.get(classes[i]).put(attributes[j], new HashMap<Object, Integer>());
            }
             index.get(classes[i]).put("<?>", new HashMap<Object, Integer>());
                    
        }
        for(int i = 0; i < classes.length; i++){
            classCount.put(classes[i], 0);
        }
        
    }
    @Override
    public void learn(Map<String, Object> instance, String cls){
        for(String a: attributes){
            if(instance.containsKey(a)){
                if(!index.get(cls).get(a).containsKey(instance.get(a))){
                    index.get(cls).get(a).put(instance.get(a), 0);
                }
                int count = index.get(cls).get(a).get(instance.get(a));
                index.get(cls).get(a).put(instance.get(a), count+1);
            } else {
                if(!index.get(cls).get(a).containsKey("<?>")){
                    index.get(cls).get(a).put("<?>", 0);
                }
                int count = index.get(cls).get(a).get("<?>");
                index.get(cls).get(a).put("<?>", count+1);
            }
        }
        int clsCnt = classCount.get(cls);
        classCount.put(cls, clsCnt+1);
        classes.add(cls);
        instances.add(instance);
        
        
    }
    @Override
    public String classify(Map<String, Object> instance){
        double maxProbability = 0.0;
        String maxClass = "";
        for(int i = 0; i < classes.size(); i++){
            double currentProbability = 1.0;
            String currentClass = classes.get(i);
            for(int j = 0; j < attributes.size(); j++){
                String attrName = attributes.get(j);
                if(!instance.containsKey(attrName)){
                    currentProbability *= probability(attrName, null, currentClass)+(0.1/((double)instances.size()));
                } else {
                    currentProbability *= probability(attrName, instance.get(attrName), currentClass)+(0.1/((double)instances.size()));
                }
            }
            if(currentProbability > maxProbability){
                maxProbability = currentProbability;
                maxClass = currentClass;
            }
        }
        System.err.println("class: "+maxClass+" probability: "+maxProbability);
        return maxClass;
    }
    
    protected double probability(String attr, Object value, String cls){
        /*long noInstances = 0;
        for(int i = 0; i < instances.size(); i++){
            if(value == null){
                if(classes.get(i).equals(cls) && !instances.get(i).containsKey(attr)){
                    noInstances++;
                }
            } else {
                if(classes.get(i).equals(cls) && instances.get(i).containsKey(attr) && instances.get(i).get(attr).equals(value)){
                    noInstances++;
                }
            }
        }
        
        return (((double)noInstances)/((double)classes.size()));        
         */
        if (value == null){
            return (((double)index.get(cls).get(attr).get("<?>")))/(((double)classCount.get(cls)));
        } else {
            
            if(!index.get(cls).get(attr).containsKey(value)){
                return 0;
            }            
            double vasilica = ((double)index.get(cls).get(attr).get(value))
                    /
                    ((double)classCount.get(cls));
            return 
                    vasilica;
                    
        }
        
        
                 
    }
}
