/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package indicators.classifier;

import java.util.Map;

/**
 *
 * @author Bogdan
 */
public interface Classifier {
    public void learn(Map<String, Object> instance, String cls);
    public String classify(Map<String, Object> instance);
}
