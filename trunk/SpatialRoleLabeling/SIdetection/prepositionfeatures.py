from nltk.tree import *
from nltk.stem.wordnet import WordNetLemmatizer
import xml.dom.minidom
from pprint import pprint

def prepfeatures_dataset():
    doc = xml.dom.minidom.parse("parsed.xml")
    contents = doc.getElementsByTagName("CONTENT")
    
    for content in contents:
        sentence = content.childNodes[0].data
        taggedwords = content.parentNode.getElementsByTagName("POSTAGS")[0].childNodes[0].data
        taggedwords = taggedwords.split(" ")
        tw = []
        for pair in taggedwords:
            tw.append([pair.split("/")[0], pair.split("/")[1]])
        taggedwords = tw

        stringtree = content.parentNode.getElementsByTagName("TREE")[0].childNodes[0].data

        dependencies = content.parentNode.getElementsByTagName("DEPENDENCIES")[0]
        

        preps = doc.createElement("PREPS")

        for depen in dependencies.childNodes:
            stringdepen = depen.childNodes[0].data
            if "name:prep" in stringdepen:
                #Cast to dict
                listdepen = stringdepen.split(" ")
                name = listdepen[0].split(":")[1]
                specific = listdepen[1].split(":")[1]                                                                                      
                gov = listdepen[2].split(":")[1]                                                                                   
                dep = listdepen[3].split(":")[1]
                dictdepen = {'name': name, 'specific': specific, 'gov': gov, 'dep': dep}

                features = extract_features(dictdepen, taggedwords, stringtree)
                print "Features extracted:"
                pprint(features)
                
                # Introduce preps and features in the xml                                                                                                            
                head1 = doc.createElement('HEAD1')
                head1_LEMMA = doc.createElement('HEAD1_LEMMA')
                head1_POS = doc.createElement('HEAD1_POS')
                head2 = doc.createElement('HEAD2')
                head2_LEMMA =  doc.createElement('HEAD2_LEMMA')
                head2_POS =  doc.createElement('HEAD2_POS')
                prep = doc.createElement('PREP')
                prep_POS = doc.createElement('PREP_POS')
                prep_spatial = doc.createElement('PREP_SPATIAL')

                head1_text = doc.createTextNode(features['head1'])
                head1_LEMMA_text = doc.createTextNode(features['head1_LEMMA'])
                head1_POS_text = doc.createTextNode(features['head1_POS'])
                head2_text = doc.createTextNode(features['head2'])
                head2_LEMMA_text = doc.createTextNode(features['head2_LEMMA'])
                head2_POS_text = doc.createTextNode(features['head2_POS'])
                prep_text = doc.createTextNode(str(features['preposition'])) #str to avoid Nones                                               
                prep_POS_text = doc.createTextNode(str(features['preposition_POS']))
                prep_spatial_text = doc.createTextNode(str(features['preposition_spatial']))
                
                head1.appendChild(head1_text)
                head1_LEMMA.appendChild(head1_LEMMA_text)
                head1_POS.appendChild(head1_POS_text)
                head2.appendChild(head2_text)
                head2_LEMMA.appendChild(head2_LEMMA_text)
                head2_POS.appendChild(head2_POS_text)
                prep.appendChild(prep_text)
                prep_POS.appendChild(prep_POS_text)
                prep_spatial.appendChild(prep_spatial_text)

                prepnode = doc.createElement("PREPOSITION")
                prepnode.appendChild(head1)
                prepnode.appendChild(head1_LEMMA)
                prepnode.appendChild(head1_POS)
                prepnode.appendChild(head2)
                prepnode.appendChild(head2_LEMMA)
                prepnode.appendChild(head2_POS)
                prepnode.appendChild(prep)
                prepnode.appendChild(prep_POS)
                prepnode.appendChild(prep_spatial)

                preps.appendChild(prepnode)

        content.parentNode.appendChild(preps)


    f = open('output.xml', 'w')
    doc.writexml(f)
    f.close()
    



def preposition_deps(dependencies):
    """
    Extract prepositions from a dependency list (Stanford).
    The dependencies are expected to be a list of dictionaries like:
    {'name': name, 'specific': specific, 'gov': gov, 'dep': dep}
    """
    preps = [dep for dep in dependencies if dep['name'] == 'prep']
    return preps

def extract_features(prepdeps, taggedwords, stringtree):
    """ Extract preposition features as explained in the SpRL paper """
    features = {}
    lmtzr = WordNetLemmatizer()
    
    def getPOS(word, tagged):
        if word is None: return None
        try:
            print "**debug: Looking word POS"
            print word
            print tagged
            pos = filter(lambda t: t[0] == word, tagged)
            if len(pos) > 0:
                return pos[0][1]
            else:
                return None

        except Exception as e:
            print "Error getting POS of:", word
            print e

    prep = prepdeps['specific']
    features['preposition'] = prepdeps['specific']
    features['head1'] = prepdeps['gov']
    features['head2'] = prepdeps['dep']
    features['preposition_POS'] = getPOS(prepdeps['specific'], taggedwords)
    features['head1_POS'] = getPOS(prepdeps['gov'], taggedwords)
    features['head2_POS'] = getPOS(prepdeps['dep'], taggedwords)
    features['head1_LEMMA'] = lmtzr.lemmatize(prepdeps['gov'])
    features['head2_LEMMA'] = lmtzr.lemmatize(prepdeps['dep'])
    features['preposition_spatial'] = get_spatial_sense(prepdeps['specific'])

    return features

def get_spatial_sense(preposition):
    """
    Gets proportion of spatial sense for preposition 
    in PPT dictionary (The Preposition Project)
    """
    print "GEtting spatial sense of", preposition 
    doc = xml.dom.minidom.parse("tpp.xml")
    entries = [node for node in doc.getElementsByTagName("hw")
               if node.firstChild.nodeValue == preposition]
    try:
        entry = entries[0].parentNode.parentNode.parentNode
    except Exception as e:
        print e
        return -1

    spatial = 0 #percentage of spatial                     
    for sup in entry.getElementsByTagName('sup'):
        if sup.firstChild.nodeValue == 'Spatial': spatial += 1
    spatial = float(spatial)/len(entry.getElementsByTagName('sup'))

    return spatial


if __name__ == "__main__":
    prepfeatures_dataset()
