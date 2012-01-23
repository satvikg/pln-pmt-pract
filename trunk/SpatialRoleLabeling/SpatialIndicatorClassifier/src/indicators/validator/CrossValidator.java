/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package indicators.validator;

import indicators.classifier.Classifier;
import indicators.classifier.NaiveBayes;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Bogdan
 */
public class CrossValidator {
    protected String[] classes;
    protected String[] attributes;
    List<Map<String, Object>> data;
    List<String> assignemnts;
    public CrossValidator(List<Map<String, Object>> data, List<String> assignments, String[] classes, String[] attributes){
        this.attributes = attributes;
        this.classes = classes;
        this.data = data;
        this.assignemnts = assignments;
    }
    public Classifier crossValidate(int folds){
        
        Classifier[] classifiers = new Classifier[folds];
        double[] accuracy = new double[folds];
        double[] noExamples = new double[folds];
        for(int i = 0; i < folds; i++){
            classifiers[i] = new NaiveBayes(classes, attributes);
            accuracy[i] = 0;
            noExamples[i] = 0;
        }
        // learn part
        for(int i = 0; i < data.size(); i++){
            for(int j = 0; j < folds; j++){
                if(j%folds != i%folds){
                    classifiers[j].learn(data.get(i), assignemnts.get(i));
                }
            }
        }
        // test part
        for(int i = 0; i < data.size(); i++){
            for(int j = 0; j < folds; j++){
                if(j%folds == i%folds){
                    String cls = classifiers[j].classify(data.get(i));
                    if(cls.equals(assignemnts.get(i)))
                        accuracy[j]++;
                    noExamples[j]++;
                }
            }
        }
        System.out.println(((NaiveBayes)classifiers[0]).index);
        // choose the best classifier
        for(int i = 0; i < folds; i++)
            accuracy[i] /= noExamples[i];
        double max = 0;
        int idx = -1;
        for(int i = 0; i < folds; i++){
            if (accuracy[i] > max){
                max = accuracy[i];
                idx = i;
            }
        }
        
        if(idx >= 0){
            System.out.println("Accuracy: "+max);
            return classifiers[idx];
        }
        else
            return null;
    }
}
