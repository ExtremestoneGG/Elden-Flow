package dev.eldensteve;

/** Tracks equipment identity, ignoring durability/count changes during combat. */
public final class EquipmentSelection {
    private Object main,off;
    private int mainType=-1,offType=-1;
    public boolean update(Object nextMain,Object nextOff,int nextMainType,int nextOffType){
        boolean changed=main!=nextMain||off!=nextOff||mainType!=nextMainType||offType!=nextOffType;
        main=nextMain;off=nextOff;mainType=nextMainType;offType=nextOffType;
        return changed;
    }
    public void reset(){main=off=null;mainType=offType=-1;}
}
