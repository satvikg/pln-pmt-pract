/**
 * 
 */
package xprolog;

/**
 * @author bgutmann
 * 
 */
// /////////////////////////////////////////////////////////////////////
final class Step extends TermList
// /////////////////////////////////////////////////////////////////////
{
    public Step(TermList t) {
        super();
        next = t.next;
        t.next = this;
        term = new Term("STEP", 0);
    }
}