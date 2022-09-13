package org.cis120;

import java.util.*;

/*
 * Make sure to write your own tests in ServerModelTest.java.
 * The tests we provide for each task are NOT comprehensive!
 */

/**
 * The {@code ServerModel} is the class responsible for tracking the
 * state of the server, including its current users and the channels
 * they are in.
 * This class is used by subclasses of {@link Command} to:
 * 1. handle commands from clients, and
 * 2. handle commands from {@link ServerBackend} to coordinate
 * client connection/disconnection.
 */
public final class ServerModel {

    /**
     * Constructs a {@code ServerModel}. Make sure to initialize any collections
     * used to model the server state here.
     */
    private Map<String, Channel> channels;
    private Map<Integer, String> users;

    public ServerModel() {
        users = new TreeMap<>();
        channels = new TreeMap<>();
    }

    // =========================================================================
    // == Task 2: Basic Server model queries
    // == These functions provide helpful ways to test the state of your model.
    // == You may also use them in later tasks.
    // =========================================================================

    /**
     * Gets the user ID currently associated with the given
     * nickname. The returned ID is -1 if the nickname is not
     * currently in use.
     *
     * @param nickname The nickname for which to get the associated user ID
     * @return The user ID of the user with the argued nickname if
     *         such a user exists, otherwise -1
     */
    public int getUserId(String nickname) {

        for (Map.Entry<Integer, String> entry : users.entrySet()) {
            // if nickname matches the value then return its key
            if (nickname.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        // if no value matched nickname then return -1
        return -1;
    }

    /**
     * Gets the nickname currently associated with the given user
     * ID. The returned nickname is null if the user ID is not
     * currently in use.
     *
     * @param userId The user ID for which to get the associated
     *               nickname
     * @return The nickname of the user with the argued user ID if
     *         such a user exists, otherwise null
     */
    public String getNickname(int userId) {
        for (Map.Entry<Integer, String> entry : users.entrySet()) {
            if (entry.getKey() == userId) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Gets a collection of the nicknames of all users who are
     * registered with the server. Changes to the returned collection
     * should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of registered user nicknames
     */
    public Collection<String> getRegisteredUsers() {
        Set<String> usersCollection = new TreeSet<String>();
        usersCollection.addAll(users.values());
        return usersCollection;
    }

    /**
     * Gets a collection of the names of all the channels that are
     * present on the server. Changes to the returned collection
     * should not affect the server state.
     * 
     * This method is provided for testing.
     *
     * @return The collection of channel names
     */
    public Collection<String> getChannels() {
        Set<String> channelsCollection = new TreeSet<String>();
        channelsCollection.addAll(channels.keySet());
        return channelsCollection;

    }

    /**
     * Gets a collection of the nicknames of all the users in a given
     * channel. The collection is empty if no channel with the given
     * name exists. Modifications to the returned collection should
     * not affect the server state.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get member nicknames
     * @return A collection of all user nicknames in the channel
     */
    public Collection<String> getUsersInChannel(String channelName) {
        Collection<String> result = new TreeSet<>();
        if (channels.get(channelName) == null) {
            return result;
        } else {
            result = channels.get(channelName).getUserList();
            return result;
        }
    }

    /**
     * Gets the nickname of the owner of the given channel. The result
     * is {@code null} if no channel with the given name exists.
     *
     * This method is provided for testing.
     *
     * @param channelName The channel for which to get the owner nickname
     * @return The nickname of the channel owner if such a channel
     *         exists; otherwise, return null
     */
    public String getOwner(String channelName) {
        if (channels.get(channelName) == null) {
            return null;
        } else {
            return channels.get(channelName).getOwner();
        }
    }

    // ===============================================
    // == Task 3: Connections and Setting Nicknames ==
    // ===============================================

    /**
     * This method is automatically called by the backend when a new client
     * connects to the server. It should generate a default nickname with
     * {@link #generateUniqueNickname()}, store the new user's ID and username
     * in your data structures for {@link ServerModel} state, and construct
     * and return a {@link Broadcast} object using
     * {@link Broadcast#connected(String)}}.
     *
     * @param userId The new user's unique ID (automatically created by the
     *               backend)
     * @return The {@link Broadcast} object generated by calling
     *         {@link Broadcast#connected(String)} with the proper parameter
     */
    public Broadcast registerUser(int userId) {
        String nickname = generateUniqueNickname();
        // We have taken care of generating the nickname and returning
        // the Broadcast for you. You need to modify this method to
        // store the new user's ID and username in this model's internal state.
        users.put(userId, nickname);
        return Broadcast.connected(nickname);
    }

    /**
     * Helper for {@link #registerUser(int)}. (Nothing to do here.)
     *
     * Generates a unique nickname of the form "UserX", where X is the
     * smallest non-negative integer that yields a unique nickname for a user.
     * 
     * @return The generated nickname
     */
    private String generateUniqueNickname() {
        int suffix = 0;
        String nickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            nickname = "User" + suffix++;
        } while (existingUsers.contains(nickname));
        return nickname;
    }

    /**
     * This method is automatically called by the backend when a client
     * disconnects from the server. This method should take the following
     * actions, not necessarily in this order:
     *
     * (1) All users who shared a channel with the disconnected user should be
     * notified that they left
     * (2) All channels owned by the disconnected user should be deleted
     * (3) The disconnected user's information should be removed from
     * {@link ServerModel}'s internal state
     * (4) Construct and return a {@link Broadcast} object using
     * {@link Broadcast#disconnected(String, Collection)}.
     *
     * @param userId The unique ID of the user to deregister
     * @return The {@link Broadcast} object generated by calling
     *         {@link Broadcast#disconnected(String, Collection)} with the proper
     *         parameters
     */

    public Broadcast deregisterUser(int userId) {
        String nickname = getNickname(userId);
        Collection<String> recipients = deleteChannelsAndReturnRecipients(userId);
        users.remove(userId);
        return Broadcast.disconnected(nickname, recipients);
    }

    /**
     * Helper for {@link #deregisterUser(int)}
     * This method deletes all channels where user was the owner while also
     * returning a list of
     * recipients where user deregistering was a member of.
     * 
     * @param id
     * @return collection of all users who share a channel with a given user
     *         EXCLUDING the user itself
     */
    private Set<String> deleteChannelsAndReturnRecipients(int id) {
        Set<String> result = new TreeSet<>();
        Collection<String> listOfAllChannels = channels.keySet();
        String username = getNickname(id);
        Set<String> clone = new TreeSet<>();

        for (String c : listOfAllChannels) {
            if (channels.get(c).contains(username)) {
                channels.get(c).removeUser(username);
                result.addAll(channels.get(c).getUserList());
            }
            if (channels.get(c).getOwner().equals(username)) {
                clone.add(c);
            }
        }
        // the only way to get rid of my ConcurrentModificationError is to create a
        // clone of removed channels
        // and use that to alter channels state.
        for (String c : clone) {
            channels.remove(c);
        }

        return result;
    }

    /**
     * This method is called when a user wants to change their nickname.
     * 
     * @param nickCommand The {@link NicknameCommand} object containing
     *                    all information needed to attempt a nickname change
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the nickname
     *         change is successful. The command should be the original nickCommand
     *         and the collection of recipients should be any clients who
     *         share at least one channel with the sender, including the sender.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#INVALID_NAME} if the proposed nickname
     *         is not valid according to
     *         {@link ServerModel#isValidName(String)}
     *         (2) {@link ServerResponse#NAME_ALREADY_IN_USE} if there is
     *         already a user with the proposed nickname
     */
    public Broadcast changeNickname(NicknameCommand nickCommand) {
        int userId = nickCommand.getSenderId();
        String nickname = nickCommand.getNewNickname();
        String oldName = getNickname(userId);
        Collection<String> recipients = recipientUsersInclusive(userId);
        Map<String, ServerResponse> temp = validAndResponse(nickname);

        for (Map.Entry<String, ServerResponse> entry : temp.entrySet()) {
            if (entry.getKey().equals("true")) {
                // replace username in user list
                users.replace(userId, oldName, nickname);

                // replace new nickname of user in channels where user exists
                for (Channel c : channels.values()) {
                    if (c.contains(oldName)) {
                        c.setNickName(oldName, nickname);
                    }
                }

                return Broadcast.okay(nickCommand, recipients);
            } else if (entry.getKey().equals("false nameInUse")) {
                return Broadcast.error(nickCommand, entry.getValue());
            } else {
                return Broadcast.error(nickCommand, entry.getValue());
            }
        } // end for loop

        // this line of code should never execute. if it does this method needs to be
        // fixed
        return Broadcast.error(nickCommand, ServerResponse.NO_SUCH_USER);
    }

    /**
     * Determines if a given nickname is valid or invalid (contains at least
     * one alphanumeric character, and no non-alphanumeric characters).
     * (Nothing to do here.)
     * 
     * @param name The channel or nickname string to validate
     * @return true if the string is a valid name
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper for {@link #changeNickname(NicknameCommand)}
     * Generates a collection of all users who share a channel with a given userID.
     * Does include the given user itself as well.
     * Does not alter server's internal state
     * 
     * @param id
     * @return collection of all users who share a channel with a given user
     *         including the user itself
     */
    private Collection<String> recipientUsersInclusive(int id) {
        Collection<String> result = new TreeSet<>();
        String username = getNickname(id);
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            if (entry.getValue().contains(username)) {
                result.addAll(entry.getValue().getUserList());
            }
        }
        return result;
    }

    /**
     * Determines if a given username is valid and returns the appropriate
     * ServerResponse.
     * IF desired name is already in use then it returns false and
     * #NAME_ALREADY_IN_USE
     * otherwise IF desired name is not already in use then it checks if it is valid
     * through
     * isValid() method.
     * 
     * @param name The name string to validate
     * @return Map<String, ServerResponse>. String will just say true or false and
     *         server response will be either #OKAY or #INVALID_NAME or
     *         #NAME_ALREADY_IN_USE
     */
    private Map<String, ServerResponse> validAndResponse(String name) {
        Map<String, ServerResponse> result = new TreeMap<>();
        boolean temp = isValidName(name);
        // username currently in use
        if (getUserId(name) != -1) {
            result.put("false nameInUse", ServerResponse.NAME_ALREADY_IN_USE);
        } else {
            // username not currently in use and isValid returns true
            if (temp) {
                result.put("true", ServerResponse.OKAY);
                // username not currently in use but isValid returns false
            } else {
                result.put("false invalidName", ServerResponse.INVALID_NAME);
            } // end inner else

        } // end outer else
        return result;
    }

    // ===================================
    // == Task 4: Channels and Messages ==
    // ===================================

    /**
     * This method is called when a user wants to create a channel.
     * You can ignore the privacy aspect of this method for task 4, but
     * make sure you come back and implement it in task 5.
     * 
     * @param createCommand The {@link CreateCommand} object containing all
     *                      information needed to attempt channel creation
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the channel
     *         creation is successful. The only recipient should be the new
     *         channel's owner.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#INVALID_NAME} if the proposed
     *         channel name is not valid according to
     *         {@link ServerModel#isValidName(String)}
     *         (2) {@link ServerResponse#CHANNEL_ALREADY_EXISTS} if there is
     *         already a channel with the proposed name
     */
    public Broadcast createChannel(CreateCommand createCommand) {
        String name = createCommand.getChannel(); // key to the channels map
        String ownerName = createCommand.getSender();

        Map<String, ServerResponse> temp = channelValidAndResponse(name);

        for (Map.Entry<String, ServerResponse> entry : temp.entrySet()) {
            if (entry.getKey().equals("true")) {

                // create a new channel with ownerName and isPrivate status desired
                Channel channel = new Channel(ownerName, createCommand.isInviteOnly());

                // add the new channel to server model internal state
                channels.put(name, channel);

                // return the broadcast OKAY with the given command and owner as recipients
                Collection<String> recipients = channels.get(name).getUserList();
                return Broadcast.okay(createCommand, recipients);
            } else if (entry.getKey().equals("false nameInUse")) {
                return Broadcast.error(createCommand, entry.getValue());
            } else {
                return Broadcast.error(createCommand, entry.getValue());
            }

        } // end for loop

        // this line of code should never execute. if it does this method needs to be
        // fixed
        return Broadcast.error(createCommand, ServerResponse.NO_SUCH_CHANNEL);
    }

    /**
     * Determines if a channel name is valid and is not already in use. Also gives
     * the
     * correct server response.
     * Does not alter state of channel.
     * Checks for valid channel name not username. Use appropriately
     * 
     * @param name of the channel
     * @return String of true or stating false and the reason why.
     */
    private Map<String, ServerResponse> channelValidAndResponse(String name) {
        Map<String, ServerResponse> result = new TreeMap<>();
        boolean temp = isValidName(name);
        // channel name currently in use
        if (channels.containsKey(name)) {
            result.put("false nameInUse", ServerResponse.CHANNEL_ALREADY_EXISTS);
        } else {
            // channel name not currently in use and isValid returns true
            if (temp) {
                result.put("true", ServerResponse.OKAY);
                // channel name not currently in use but isValid returns false
            } else {
                result.put("false invalidName", ServerResponse.INVALID_NAME);
            } // end inner else

        } // end outer else
        return result;
    }

    /**
     * This method is called when a user wants to join a channel.
     * You can ignore the privacy aspect of this method for task 4, but
     * make sure you come back and implement it in task 5.
     * 
     * @param joinCommand The {@link JoinCommand} object containing all
     *                    information needed for the user's join attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#names(Command, Collection, String)} if the user
     *         joins the channel successfully. The recipients should be all
     *         people in the joined channel (including the sender).
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no
     *         channel with the specified name
     *         (2) (after Task 5) {@link ServerResponse#JOIN_PRIVATE_CHANNEL} if
     *         the sender is attempting to join a private channel
     */
    public Broadcast joinChannel(JoinCommand joinCommand) {
        String username = getNickname(joinCommand.getSenderId());
        String channelName = joinCommand.getChannel();

        if (!(channels.containsKey(channelName))) {
            return Broadcast.error(joinCommand, ServerResponse.NO_SUCH_CHANNEL);
        } else if (channels.get(channelName).getPrivacy()) {
            return Broadcast.error(joinCommand, ServerResponse.JOIN_PRIVATE_CHANNEL);
        } else {
            Collection<String> recipients = channels.get(channelName).getUserList();
            channels.get(channelName).addUser(username);
            recipients.add(username);
            return Broadcast.names(joinCommand, recipients, channels.get(channelName).getOwner());
        }
    }

    /**
     * This method is called when a user wants to send a message to a channel.
     * 
     * @param messageCommand The {@link MessageCommand} object containing all
     *                       information needed for the messaging attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the message
     *         attempt is successful. The recipients should be all clients
     *         in the channel.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no
     *         channel with the specified name
     *         (2) {@link ServerResponse#USER_NOT_IN_CHANNEL} if the sender is
     *         not in the channel they are trying to send the message to
     */
    public Broadcast sendMessage(MessageCommand messageCommand) {
        String channelName = messageCommand.getChannel();

        if (!(channels.containsKey(channelName))) {
            return Broadcast.error(messageCommand, ServerResponse.NO_SUCH_CHANNEL);
        } else if (!(channels.get(channelName).contains(messageCommand.getSender()))) {
            return Broadcast.error(messageCommand, ServerResponse.USER_NOT_IN_CHANNEL);
        } else {
            return Broadcast.okay(messageCommand, channels.get(channelName).getUserList());
        }

    }

    /**
     * This method is called when a user wants to leave a channel.
     * 
     * @param leaveCommand The {@link LeaveCommand} object containing all
     *                     information about the user's leave attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the user leaves
     *         the channel successfully. The recipients should be all clients
     *         who were in the channel, including the user who left.
     * 
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no
     *         channel with the specified name
     *         (2) {@link ServerResponse#USER_NOT_IN_CHANNEL} if the sender is
     *         not in the channel they are trying to leave
     */
    public Broadcast leaveChannel(LeaveCommand leaveCommand) {
        String channelName = leaveCommand.getChannel();
        String username = leaveCommand.getSender();
        Channel chan = channels.get(channelName);

        Collection<String> recipients = chan.getUserList();

        if (channels.containsKey(channelName) &&
                chan.contains(username)) {
            if (chan.getOwner().equals(username)) {
                channels.remove(channelName);
            } else {
                channels.get(channelName).removeUser(username);
            }
            return Broadcast.okay(leaveCommand, recipients);

        } else if (!chan.contains(username)) {
            return Broadcast.error(leaveCommand, ServerResponse.USER_NOT_IN_CHANNEL);
        } else {
            return Broadcast.error(leaveCommand, ServerResponse.NO_SUCH_CHANNEL);
        }

    }

    // =============================
    // == Task 5: Channel Privacy ==
    // =============================

    // Go back to createChannel and joinChannel and add
    // all privacy-related functionalities, then delete this when you're done.

    /**
     * This method is called when a channel's owner adds a user to that channel.
     * 
     * @param inviteCommand The {@link InviteCommand} object containing all
     *                      information needed for the invite attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#names(Command, Collection, String)} if the user
     *         joins the channel successfully as a result of the invite.
     *         The recipients should be all people in the joined channel
     *         (including the new user).
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_USER} if the invited user
     *         does not exist
     *         (2) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no channel
     *         with the specified name
     *         (3) {@link ServerResponse#INVITE_TO_PUBLIC_CHANNEL} if the
     *         invite refers to a public channel
     *         (4) {@link ServerResponse#USER_NOT_OWNER} if the sender is not
     *         the owner of the channel
     */
    public Broadcast inviteUser(InviteCommand inviteCommand) {
        String sender = inviteCommand.getSender();
        String channelName = inviteCommand.getChannel();
        Channel channel = channels.get(channelName);
        String invitedUser = inviteCommand.getUserToInvite();

        // if user is not contained in server model internal user state
        if (!(users.containsValue(invitedUser))) {
            return Broadcast.error(inviteCommand, ServerResponse.NO_SUCH_USER);
        } else if (!(channels.containsValue(channel))) {
            // if channel is not contained in server model internal channel state
            return Broadcast.error(inviteCommand, ServerResponse.NO_SUCH_CHANNEL);
        } else if (!(channel.getPrivacy())) { // if channel being invited to is public
            return Broadcast.error(inviteCommand, ServerResponse.INVITE_TO_PUBLIC_CHANNEL);
        } else if (!(channel.getOwner().equals(sender))) { // if the invite sender is not owner
            return Broadcast.error(inviteCommand, ServerResponse.USER_NOT_OWNER);
        } else {
            // if all above conditions are met then add user to channel and Broadcast.names
            Collection<String> recipients = channel.getUserList();
            recipients.add(invitedUser);
            channel.addUser(invitedUser);
            return Broadcast.names(inviteCommand, recipients, sender);
        }

    }

    /**
     * This method is called when a channel's owner removes a user from
     * that channel.
     * 
     * @param kickCommand The {@link KickCommand} object containing all
     *                    information needed for the kick attempt
     * @return The {@link Broadcast} object generated by
     *         {@link Broadcast#okay(Command, Collection)} if the user is
     *         successfully kicked from the channel. The recipients should be
     *         all clients who were in the channel, including the user
     *         who was kicked.
     *
     *         If an error occurs, use
     *         {@link Broadcast#error(Command, ServerResponse)} with either:
     *         (1) {@link ServerResponse#NO_SUCH_USER} if the user being kicked
     *         does not exist
     *         (2) {@link ServerResponse#NO_SUCH_CHANNEL} if there is no channel
     *         with the specified name
     *         (3) {@link ServerResponse#USER_NOT_IN_CHANNEL} if the
     *         user being kicked is not a member of the channel
     *         (4) {@link ServerResponse#USER_NOT_OWNER} if the sender is not
     *         the owner of the channel
     */
    public Broadcast kickUser(KickCommand kickCommand) {
        String sender = kickCommand.getSender();
        String channelName = kickCommand.getChannel();
        Channel channel = channels.get(channelName);
        String kickUser = kickCommand.getUserToKick();

        // if user is not contained in server model internal user state
        if (!(users.containsValue(kickUser))) {
            return Broadcast.error(kickCommand, ServerResponse.NO_SUCH_USER);
        } else if (!(channels.containsValue(channel))) {
            // if channel is not contained in server model internal channel state
            return Broadcast.error(kickCommand, ServerResponse.NO_SUCH_CHANNEL);
        } else if (!(channel.contains(kickUser))) { // if kickUser is not in channel
            return Broadcast.error(kickCommand, ServerResponse.USER_NOT_IN_CHANNEL);
        } else if (!(channel.getOwner().equals(sender))) {
            // if the invite sender is not owner of the channel
            return Broadcast.error(kickCommand, ServerResponse.USER_NOT_OWNER);
        } else if (sender.equals(kickUser) && (channel.getOwner().equals(sender))) {
            // if owner kicks himself out, delete the channel
            Collection<String> recipients = channel.getUserList();
            channels.remove(channelName);
            return Broadcast.okay(kickCommand, recipients);
        } else {
            Collection<String> recipients = channel.getUserList();
            channel.removeUser(kickUser);
            return Broadcast.okay(kickCommand, recipients);
        }
    }

}
