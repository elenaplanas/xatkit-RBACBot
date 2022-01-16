package com.xatkit.RBAC;

enum ActionType{
    MATCHING, TRANSITION_NAVIGATION;
}

public class Action {
    private String name;
    private ActionType type;

    public Action(String myName, ActionType myType){
        name = myName;
        type = myType;
    }

    public String getName(){
        return name;
    }

    public ActionType getType() {
        return type;
    }
}