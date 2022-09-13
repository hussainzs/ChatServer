package org.cis120;

import java.util.*;

public class Channel implements Comparable {

    private String owner;
    private Set<String> userList;
    private boolean privacyPrivate;

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Channel)) {
            return false;
        }
        Channel channel = (Channel) o;
        return privacyPrivate == channel.privacyPrivate
                && owner.equals(channel.owner)
                && userList.equals(channel.userList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, userList, privacyPrivate);
    }

    /**
     * Constructor for Channel class
     * 
     * @param ownerName that created the channel
     */
    public Channel(String ownerName) {
        owner = ownerName;
        privacyPrivate = true;
        userList = new TreeSet<>();
        userList.add(owner);
    }

    /**
     * Constructor for Channel class
     * 
     * @param ownerName that created the channel, privacyStatus desired by owner
     */
    public Channel(String ownerName, boolean privacyStatus) {
        owner = ownerName;
        privacyPrivate = privacyStatus;
        userList = new TreeSet<>();
        userList.add(owner);
    }

    /**
     * This method returns the list of all users in a channel
     * For encapsulation, return a new collection so the userList in place is NOT
     * changed
     * 
     * @return list of all users in a channel from userList
     */
    public Collection<String> getUserList() {
        Collection<String> result = new TreeSet<>(userList);
        return result;
    }

    /**
     * Method to remove desired user from channel
     * 
     * @param user to be removed
     */
    public void removeUser(String user) {
        userList.remove(user);
    }

    /**
     * Method to add desired user from channel
     * 
     * @param user to be removed
     */
    public void addUser(String user) {
        userList.add(user);
    }

    /**
     * Checks to see if user is contained in the channel
     * 
     * @param username
     * @return boolean true or false
     */
    public boolean contains(String username) {
        for (String names : userList) {
            if (names.equals(username)) {
                return true;
            }
        }
        return userList.contains(username);
    }

    /**
     * Method to obtain owner's name
     * 
     * @return owner name
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Method to get privacy status
     * 
     * @return privacyStatus, if true means public , if false means private
     */
    public boolean getPrivacy() {
        return privacyPrivate;
    }

    /**
     * Method to set privacy to public or private
     * 
     * @param status
     *               if true means public , if false means private
     */
    public void setPrivacy(boolean status) {
        privacyPrivate = status;
    }

    public void setNickName(String oldname, String newname) {
        if (oldname.equals(owner)) {
            owner = newname;
        }
        userList.remove(oldname);
        userList.add(newname);

    }

    // end of Channel class
}
