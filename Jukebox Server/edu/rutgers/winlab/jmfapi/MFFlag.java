/**
 *
 * File: JMFAPI.java
 * Author: Francesco Bronzino
 *
 * Description: MobilityFirst flags used to request features and service Ids to the host protocol stack
 *
 */

package edu.rutgers.winlab.jmfapi;

/**
 * Object that provides support functions to express flags in inter process communications with the stack
 *
 */
public class MFFlag {

    /**
     * Multihoming flag
     */
    static public int MF_MHOME =  0x00000001;
    /**
     * Anycast flag
     */
    static public int MF_ANYCAST = 0x00000002;

    private int value;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    boolean isMFFlagSet(int f){
        return (value & f) != 0;
    }

    void setMFFlag(int f){
        value = value | f;
    }

    boolean isMFFlagSet(MFFlag f){
        return (value & f.getValue()) != 0;
    }

    void setMFFlag(MFFlag f){
        value = value | f.getValue();
    }

    void setMultihoming(){
        value = value | MF_MHOME;
    }

    boolean isMultihomingSet(){
        return (value & MF_MHOME) != 0;
    }

    void setAnycast(){
        value = value | MF_ANYCAST;
    }

    boolean isAnycast(){
        return (value & MF_ANYCAST) != 0;
    }

}
