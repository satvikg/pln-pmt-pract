import xml.dom.minidom

def generate_dataset():
    doc = xml.dom.minidom.parse("output.xml")
    preps = doc.getElementsByTagName("PREPOSITION")
    multiwords = 0
    uniwords = 0
    redundancies = 0
    for prep in preps:
        classnode = doc.createElement("CLASS")
        tag = "NSI"
        preptext = prep.getElementsByTagName("PREP")[0].childNodes[0].data
        sis = prep.parentNode.parentNode.getElementsByTagName("SPATIAL_INDICATOR")
        sentence = prep.parentNode.parentNode.getElementsByTagName("CONTENT")[0].childNodes[0].data
        print "\nSENTENCE:\n", sentence
        for si in sis:
            sitext = si.childNodes[0].data.split() # split to delete spureous white spaces
            if len(sitext) > 1: multiwords +=1
            else: uniwords +=1

            sitext = " ".join(sitext)
            print "PREP-SI coincidence?:"
            print "SI:", sitext
            print "PREP:", preptext
            if preptext == sitext:
                print "...YES"
                tag = "SI"
            else: print "...NO"

        textnode = doc.createTextNode(tag)
        classnode.appendChild(textnode)
        prep.appendChild(classnode)

    f = open('SItrain.xml', 'w')
    doc.writexml(f)
    f.close()

    print "Multiwords SI:", multiwords
    print "Uniwords SI:", uniwords
    total = multiwords + uniwords
    print "total:", total

if __name__ == "__main__":
    generate_dataset()
