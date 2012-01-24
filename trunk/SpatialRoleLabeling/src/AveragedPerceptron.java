/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Bogdan
 */
public class AveragedPerceptron {
    protected int noInputs = 3;
    protected int noClasses;    
    protected double[] weights;
    protected double[] classes = {  -1,//trajector
                                    0, //nothing
                                    1 //landmark
    };
    public AveragedPerceptron(){        
        this.weights = new double[noInputs];
        this.classes = classes;        
    }
    
    protected boolean equalArrays(double[] a, double[] b){
        if(a.length!=b.length)
            return false;
        for(int i = 0;i < a.length; i++)
            if(a[i]!=b[i])
                return false;
        return true;
    }
    
    protected double[] F(double[][] instance, double[] tags){
        double[] output = new double[noInputs];
        for(int i = 0; i < instance.length; i++){
            double sum = 0;
            for(int j = 0; j < instance[i].length; j++){
                sum+=instance[i][j];
            }
            output[i] = 1/(1+Math.exp(-sum*tags[i]));
        }
        return output;
    }
    protected double[][] Gen(double[][] input){
        // generate something independent of the input
        double[][] pos = {
            {0,0,0},
            {1,0,0},
            {0,1,0},
            {0,0,1},
            {-1,0,0},
            {0,-1,0},
            {0,0,-1},
            {1,-1,0},
            {0,1,-1},
            {-1,1,0},
            {0,-1,1},    
            {1,0,-1},
            {-1,0,1}
        };
        return pos;
    }
    
    
    public void train(ArrayList features, ArrayList tags, int iterations){
        // init the weights
        for(int i = 0; i < noInputs; i++){
            weights[i] = 0.0;
        }
        for(int iter = 0; iter < iterations; iter++){
            System.out.println(iter);
            boolean modified = false;
            // adjust the weights for every instance
            
            for(int i = 0; i < features.size(); i++){
                // optimize the dot product
                double max = -Double.MAX_VALUE;
                int outIdx = -1;
                double[] outValue = {0,0,0};
                double[][] possibleOutputs = Gen((double[][])features.get(i));
                for(int j = 0; j < possibleOutputs.length; j++){
                    double sum = 0.0;
                    for(int k = 0; k < weights.length; k++){
                        sum+=weights[k]*F((double[][])features.get(i),possibleOutputs[j])[k];
                    }
                    if(sum > max){
                        max = sum;
                        outIdx = j;
                        outValue = possibleOutputs[j];
                    }                
                }
                if(!equalArrays(outValue, (double[])tags.get(i))){
                    modified = true;
                    double[] newWeights = new double[noInputs];
                    for(int j = 0; j < noInputs; j++){
                        newWeights[j] = weights[j] + F((double[][])features.get(i), (double[])tags.get(i))[j] - F((double[][])features.get(i), outValue)[j];
                    }
                    weights = newWeights;
                }
                
            }
            if(!modified)
                break;
             
        }
        
        
    }
    public static String ats(double[] a){
        String s = "{";
        for(int i = 0; i < a.length; i++){
            s+=a[i]+", ";
        }
        return s+="}";
    }
    public double[] classify(double[][] instance){
        System.out.println(ats(weights));
        double[] out = new double[noInputs];
        double[][] possibleOutputs = Gen(instance);
        double max = -Double.MAX_VALUE;
        int outIdx = -1;
        double[] outValue = {0,0,0};
        for (int j = 0; j < possibleOutputs.length; j++) {
            double sum = 0.0;
                    for(int k = 0; k < weights.length; k++){
                        sum+=weights[k]*F(instance,possibleOutputs[j])[k];
                    }
                    //System.out.println("sum: "+sum);
                    if(sum > max){
                        System.out.println("changed");
                        max = sum;
                        outIdx = j;
                        outValue = possibleOutputs[j];
                    }
        }
        
        return outValue;
    }
}
/*
 * for(int j = 0; j < classes.length; j++){
                    double sum = 0.0;
                    for(int k = 0; k < weights.length; k++){
                        sum+=weights[k]*F(instances[i],classes[j]).get(k);
                    }
                    if(sum > max){
                        max = sum;
                        classIdx = j;
                        classValue = classes[j];
                    }
                }
                // adjust the weights
                if(classValue != assignments.get(i)){
                    modified = true;
                    double[] newWeights = new double[noInputs];
                    for(int j = 0; j < noInputs; j++){
                        newWeights[j] = weights[j] + F(instances[i], assignments.get(i)).get(j) - F(instances[i], classValue).get(j);
                    }
                    weights = newWeights;
                }
                */
 