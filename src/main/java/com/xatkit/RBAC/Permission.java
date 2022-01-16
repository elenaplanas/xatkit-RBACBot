package com.xatkit.RBAC;

public class Permission {
    private Role role;
    private Action action;
    private Resource resource;

    public Permission(Role myRole, Action myAction, Resource myResource){
        role = myRole;
        action = myAction;
        resource = myResource;
    }

    public String getRoleName(){
        return role.getName();
    }

    public String getActionName(){
        return action.getName();
    }

    public String getResourceName(){
        return resource.getName();
    }
}
