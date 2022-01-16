package com.xatkit.RBAC;

import java.util.ArrayList;

public class PolicyRules {
    private ArrayList<Permission> policyRules = new ArrayList();

    public void addPermission(Role role, Action action, Resource resource){
        Permission permission = new Permission(role, action, resource);
        policyRules.add(permission);
    }

    //PDP??
    public Boolean checkPermission(String roleName, String actionName, String resourceName){
        System.out.println("*********** checking permissions from role = " + roleName + ", action = " + actionName + ", resourceName = " + resourceName);
        Boolean permission = false;
        Permission p;
        int i = 0;
        while (i < policyRules.size() && !permission){
            p = policyRules.get(i);
            if (p.getRoleName() == roleName && p.getActionName() == actionName && p.getResourceName() == resourceName){
            permission = true;
            }
            i++;
        }
        return permission;
    }
}
