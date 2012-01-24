import xml.dom.minidom
import nltk
from pprint import pprint
from random import shuffle

def generate_dataset():
    doc = xml.dom.minidom.parse("SItrain.xml")
    preps = doc.getElementsByTagName("PREPOSITION")

    instances = []
    for prep in preps:
        features = []
        head1 = prep.getElementsByTagName("HEAD1")[0].childNodes[0].data
        head1_lemma = prep.getElementsByTagName("HEAD1_LEMMA")[0].childNodes[0].data
        head1_pos = prep.getElementsByTagName("HEAD1_POS")[0].childNodes[0].data
        head2 = prep.getElementsByTagName("HEAD2")[0].childNodes[0].data
        head2_lemma = prep.getElementsByTagName("HEAD2_LEMMA")[0].childNodes[0].data
        head2_pos = prep.getElementsByTagName("HEAD2_POS")[0].childNodes[0].data
        prepos = prep.getElementsByTagName("PREP")[0].childNodes[0].data
        prepos_pos = prep.getElementsByTagName("PREP_POS")[0].childNodes[0].data
        prepos_spatial = prep.getElementsByTagName("PREP_SPATIAL")[0].childNodes[0].data
        tag = prep.getElementsByTagName("CLASS")[0].childNodes[0].data
        if tag == "SI": tag = 1
        else: tag = 0
        print tag
 
        features = {}
        features["head1"] = head1
        features["head1_lemma"] = head1_lemma
        features["head1_pos"] = head1_pos
        features["head2"] = head2
        features["head2_lemma"] = head2_lemma
        features["head2_pos"] = head2_pos
        features["prep"] = prepos
        features["prep_pos"] = prepos_pos
        features["prep_spatial"] = prepos_spatial
        
        instances.append([features, tag])
        
    return instances


def cvtrain(train, K):

    shuffle(train)
    total_accuracy = 0
    print "dataset: " + str(train)
    for k in xrange(K):
        training = [x for i, x in enumerate(train) if i % K != k]
        validation = [x for i, x in enumerate(train) if i % K == k]
        # create transition probability matrix
        ####################################
        # START MODIFYING HERE
        #A = numpy.array( [ [ 0.95,  0.05],[ 0.05,  0.95 ] ] )

        # create observable probability distribution matrix
        #B = numpy.array( [ [ 1.0/6,  1.0/6,  1.0/6,  1.0/6,  1.0/6,  1.0/6, ], \
                           #[ 1.0/10, 1.0/10, 1.0/10, 1.0/10, 1.0/10, 1.0/2 ] ] )

        # create set of all observabB = [ (-1,.1), (1,.1) ]
        #A = numpy.array( [ [ 0.99, 0.01 ], \
                           #[ 0.01, 0.99 ] ] )

        #le symbols
        V =['L', 'T', 'N']

        # instantiate an hmm, note Pi is uniform probability distribution
        # by default
        classifier = nltk.HMM(4, A=A, B=B, C=C, V=V)
        accuracy =  nltk.classify.accuracy(classifier, validation)
        total_accuracy += accuracy 
        
    avg_accuracy = float(total_accuracy)/K
    print "CV Accuracy: " + str(avg_accuracy)

def test():
    pass


if __name__ == "__main__":
    
    instances = generate_dataset()
    size = len(instances)
    cut = int(size*0.9)
    train = instances[:cut]
    test = instances[cut:]
    cv_accuracy = cvtrain(train, 5)
    #classifier = nltk.NaiveBayesClassifier.train(train)
    #accuracy =  nltk.classify.accuracy(classifier, test)
    #print "Accuracy: " + str(accuracy)
    #pprint(classifier.show_most_informative_features(5))

    classifier = nltk.NaiveBayesClassifier.train(train)
    for x in test:
        pred = classifier.classify(x[0])
        gold = x[1]
        print ""
        print x[0]
        print "predicted:", pred
        print "gold:", gold
