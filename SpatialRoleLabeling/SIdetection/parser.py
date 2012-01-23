#!/usr/bin/env jython
import sys
sys.path.append("/Users/alberto/LLAVERO/Universidad/PLN-MLT/practica/stanford-parser-2008-10-26/stanford-parser.jar")  
from java.io import CharArrayReader
from edu.stanford.nlp import *
from   edu.stanford.nlp.trees import TreePrint
from pprint import pprint
import cPickle
import xml.dom.minidom

################################CONFIGURATION#################################
# pointer to Stanford Parser 
lp = parser.lexparser.LexicalizedParser("/Users/alberto/LLAVERO/Universidad/PLN-MLT/practica/stanford-parser-2008-10-26/englishPCFG.ser.gz")
#lp.setOptionFlags(["-maxLength", "80", "-retainTmpSubcategories", "-outputFormat", "wordsAndTags,penn,typedDependenciesCollapsed"])

# Peen TreeBank language pack 
tlp = trees.PennTreebankLanguagePack()
lp.setOptionFlags(["-maxLength", "80", "-retainTmpSubcategories", "-outputFormat", "wordsAndTags,penn,typedDependenciesCollapsed", "-outputFormatOptions", "xml", "-writeOutputFiles"])

#lp.setOptionFlags(["-maxLength", "80", "-retainTmpSubcategories", "-outputFormat", "wordsAndTags,penn,typedDependencies" "-outputFormatOptions", "basicDependencies"])

dtprnt = TreePrint("typedDependencies", "basicDependencies,stem", tlp)

###############################PARSING########################################
doc = xml.dom.minidom.parse("postagged.xml")
contents = doc.getElementsByTagName("CONTENT")

for content in contents:
    tokens = content.childNodes[0].data
    tokens = tokens.split()
    if (lp.parse(tokens)):
        parse = lp.getBestParse()

    # Get tree and dependencies                                                             
    gsf = tlp.grammaticalStructureFactory() #PennTreeBanck factory                          
    gs = gsf.newGrammaticalStructure(parse) #PennTreeBank from parsed sentence               
    tdl = gs.typedDependenciesCollapsed() #syntantic dependencies (TypeDependency list) 
#    tdl = gs.typedDependencies()
    # Add tree into the xml
    tree = parse.toString()
    treenode = doc.createElement('TREE')
    treetext = doc.createTextNode(parse.toString())
    treenode.appendChild(treetext)
    content.parentNode.appendChild(treenode)

    # Add dependencies into the xml
    depsnode = doc.createElement('DEPENDENCIES')
    dtprnt.printTree(parse)
    for td in tdl:
        print "td to string: ", td.toString()
        deprelnode = doc.createElement('DEPREL')
        print "LONG NAME*****", td.reln().getLongName()
        name = td.reln().getShortName()
        specific = td.reln().getSpecific()
        gov = td.gov().label().toString('value')
        dep = td.dep().label().toString('value')
        print name
        print specific
        print gov
        print dep
        depreltext = doc.createTextNode('name:' + name + ' specific:' + str(specific) +\
                                         ' gov:' + gov + ' dep:' + dep)
        deprelnode.appendChild(depreltext)
        depsnode.appendChild(deprelnode)
    content.parentNode.appendChild(depsnode)
        
    f = open('parsedtest.xml', 'w')
    doc.writexml(f)
    f.close()

# Parse

#tokens = cPickle.load(open('tokens.p', 'rb')) 
#if (lp.parse(tokens)):
#    parse = lp.getBestParse()

# Get tree and dependencies 
#gsf = tlp.grammaticalStructureFactory() #PennTreeBanck factory
#gs = gsf.newGrammaticalStructure(parse) #PennTreeBank from parsed sentence
#tdl = gs.typedDependenciesCollapsed() #syntantic dependencies (TypeDependency list)

# Map prep_in(gov, dep) into a dictionary
#list = []
#for td in tdl:
#    print "LONG NAME*****", td.reln().getLongName()
#    name = td.reln().getShortName()
#    specific = td.reln().getSpecific()
#    gov = td.gov().label().toString('value')
#    dep = td.dep().label().toString('value')
#    list.append({'name': name, 'specific': specific, 'gov': gov, 'dep': dep})


#print "TDL:\n", tdl
#print "Dict:\n", list
# write tree and dependencies to files
#f = open('tree.p', 'w')
#f.write(parse.toString())
#f.close()

#cPickle.dump(list, open('dependencies.p', 'wb'))  

#http://nlp.stanford.edu/software/stanford-dependencies.shtml
