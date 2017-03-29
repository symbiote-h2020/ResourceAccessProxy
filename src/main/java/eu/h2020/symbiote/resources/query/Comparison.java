/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.resources.query;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class Comparison {
    public enum Cmp {
        EQ, NE, LT, LE, GT, GE;
    }
    
    Cmp cmp;
    
    public Comparison(Cmp cmp) {
        this.cmp = cmp;
    }
    
    public Cmp getCmp() {
        return cmp;
    }
    
    public boolean execute(String val1, String val2) throws Exception {    
        boolean result;
        switch(cmp) {
            case EQ:
                result = val1.equals(val2);
                break;
            case NE:
                result = !(val1.equals(val2));
                break;
            default:
                throw new Exception("Comparison to allowed between Strings");
        }
        
        return result;
    }
    
    public boolean execute(double val1, double val2) throws Exception {    
        boolean result;
        switch(cmp) {
            case EQ:
                result = (val1 == val2);
                break;
            case NE:
                result = (val1 != val2);
                break;
            case LT:
                result = (val1 < val2);
                break;
            case LE:
                result = (val1 <= val2);
                break;
            case GT:
                result = (val1 > val2);
                break;
            case GE:
                result = (val1 >= val2);
                break;
            default:
                throw new Exception("Not yet implemented");
        }
        
        return result;
    }
}