package com.gmi.nordborglab.browser.server.domain.acl;

public class PermissionPrincipal {

    protected String id;
    protected String name;
    protected boolean isUser = true;
    protected boolean isOwner = false;
    protected String avatarHash;

    public PermissionPrincipal() {
    }

    public PermissionPrincipal(String id, String name, boolean isUser, boolean isOwner) {
        this(id, name, isUser, isOwner, null);
    }

    public PermissionPrincipal(String id, String name, boolean isUser, boolean isOwner, String avatarHash) {
        this.id = id;
        this.name = name;
        this.isUser = isUser;
        this.isOwner = isOwner;
        this.avatarHash = avatarHash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getIsUser() {
        return isUser;
    }

    public void setIsUser(boolean isUser) {
        this.isUser = isUser;
    }

    public boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(boolean owner) {
        isOwner = owner;
    }

    public String getAvatarHash() {
        return avatarHash;
    }

    public void setAvatarHash(String hash) {
        this.avatarHash = avatarHash;
    }
}
