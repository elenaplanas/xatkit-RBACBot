package com.xatkit.RBAC;

enum ResourceType{
    INTENT, TRANSITION, STATE;
}

public class Resource {
    private String name;
    private ResourceType type;

    public Resource(String myName, ResourceType myType){
        name = myName;
        type = myType;
    }

    public String getName(){
        return name;
    }

    public ResourceType getType() {
        return type;
    }
}