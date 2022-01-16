package com.xatkit.RBAC;

public class User {
    private String name;
    private Role role;

    public User(String myName, Role myRole){
        name = myName;
        role = myRole;
    }

    public String getName(){
        return name;
    }

    public Role getRole(){
        return role;
    }

}
