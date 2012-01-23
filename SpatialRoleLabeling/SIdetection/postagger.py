import nltk
import cPickle
import xml.dom.minidom
from subprocess import call

def POStag_dataset():
    # Read xml file                                                                      
    doc = xml.dom.minidom.parse("tokenized.xml")
    contents = doc.getElementsByTagName("CONTENT")

    for content in contents:
        sentence = content.childNodes[0].data
        print "\n***Original sentence:\n", sentence
        tagged = postag(sentence.split())
        print tagged
        string = ''
        for t in tagged:
            string = string + t[0] + '/' + t[1] + ' '
        print string
        postags = doc.createElement('POSTAGS')
        text = doc.createTextNode(' '.join(string.split()))
        postags.appendChild(text)
        content.parentNode.appendChild(postags)
    f = open('postagged.xml', 'w')
    doc.writexml(f)
    f.close()

def postag(words):
    tagged = nltk.pos_tag(words)
    pos_tags = [pos for (token,pos) in tagged]
    return tagged

if __name__ == '__main__':
    POStag_dataset()
