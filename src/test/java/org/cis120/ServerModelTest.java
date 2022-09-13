package org.cis120;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class ServerModelTest {
    private ServerModel model;

    /**
     * Before each test, we initialize model to be
     * a new ServerModel (with all new, empty state)
     */
    @BeforeEach
    public void setUp() {
        // We initialize a fresh ServerModel for each test
        model = new ServerModel();
    }

    /**
     * Here is an example test that checks the functionality of your
     * changeNickname error handling. Each line has commentary directly above
     * it which you can use as a framework for the remainder of your tests.
     */
    @Test
    public void testInvalidNickname() {
        // A user must be registered before their nickname can be changed,
        // so we first register a user with an arbitrarily chosen id of 0.
        model.registerUser(0);

        // We manually create a Command that appropriately tests the case
        // we are checking. In this case, we create a NicknameCommand whose
        // new Nickname is invalid.
        Command command = new NicknameCommand(0, "User0", "!nv@l!d!");

        // We manually create the expected Broadcast using the Broadcast
        // factory methods. In this case, we create an error Broadcast with
        // our command and an INVALID_NAME error.
        Broadcast expected = Broadcast.error(
                command, ServerResponse.INVALID_NAME
        );

        // We then get the actual Broadcast returned by the method we are
        // trying to test. In this case, we use the updateServerModel method
        // of the NicknameCommand.
        Broadcast actual = command.updateServerModel(model);

        // The first assertEquals call tests whether the method returns
        // the appropriate Broadcast.
        assertEquals(expected, actual, "Broadcast");

        // We also want to test whether the state has been correctly
        // changed.In this case, the state that would be affected is
        // the user's Collection.
        Collection<String> users = model.getRegisteredUsers();

        // We now check to see if our command updated the state
        // appropriately. In this case, we first ensure that no
        // additional users have been added.
        assertEquals(1, users.size(), "Number of registered users");

        // We then check if the username was updated to an invalid value
        // (it should not have been).
        assertTrue(users.contains("User0"), "Old nickname still registered");

        // Finally, we check that the id 0 is still associated with the old,
        // unchanged nickname.
        assertEquals(
                "User0", model.getNickname(0),
                "User with id 0 nickname unchanged"
        );
    }

    /*
     * Your TAs will be manually grading the tests that you write below this
     * comment block. Don't forget to test the public methods you have added to
     * your ServerModel class, as well as the behavior of the server in
     * different scenarios.
     * You might find it helpful to take a look at the tests we have already
     * provided you with in Task4Test, Task3Test, and Task5Test.
     */
    /**
     * My test -> Tests user disconnecting, owner of single channel. Deletes
     * associated channel.
     * Also checks if disconnected user is deleted from server internal user
     * database
     */
    @Test
    public void testDeregisterAOwnerDeletesChannel() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        model.deregisterUser(0);
        Command join2 = new JoinCommand(1, "User1", "java");

        ServerResponse s = ServerResponse.NO_SUCH_CHANNEL;
        Broadcast expected = Broadcast.error(join2, s);
        assertEquals(expected, join2.updateServerModel(model), "channel deleted");

        assertFalse(
                model.getRegisteredUsers().contains("User0"),
                "User0 still in server"
        );
    }

    /**
     * My test -> Tests user, owner of multiple channels, disconnecting from server
     * Tests if server properly deletes all user information and deletes all
     * associated
     * channels.
     * Also checks a user trying to join a channel that now does not exist (because
     * it was deleted)
     */
    @Test
    public void testDeregisterDeletesMultipleChannels() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command create2 = new CreateCommand(0, "User0", "java2", false);
        create2.updateServerModel(model);

        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);
        Command join2 = new JoinCommand(2, "User2", "java2");
        join2.updateServerModel(model);

        model.deregisterUser(0);

        assertFalse(
                model.getChannels().contains("java"),
                "channel1 not deleted"
        );

        assertFalse(
                model.getChannels().contains("java2"),
                "channel2 not deleted"
        );

        assertFalse(
                model.getRegisteredUsers().contains("User0"),
                "User0 not deleted"
        );

        Command join3 = new JoinCommand(1, "User1", "java2");
        ServerResponse s = ServerResponse.NO_SUCH_CHANNEL;
        Broadcast expected = Broadcast.error(join3, s);
        assertEquals(expected, join3.updateServerModel(model), "channels deleted");

    }

    /**
     * My test -> Tests user, also an owner of a channel, changing its nickname.
     * Also tests create, join, and if server internal state keeps track of updated
     * name.
     * Tests if channel maintains correct number of users.
     */
    @Test
    public void testNickOwnerChangesOwnerName() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);
        Command join2 = new JoinCommand(2, "User2", "java");
        join2.updateServerModel(model);

        assertTrue(
                model.getChannels().contains("java"),
                "channel still exists"
        );
        assertTrue(
                model.getUsersInChannel("java").contains("User2"),
                "User2 not in channel"
        );
        assertEquals(
                3, model.getUsersInChannel("java").size(),
                "3 users in channel"
        );

        Command nick = new NicknameCommand(0, "User0", "BOSSMAN");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User2");
        recipients.add("BOSSMAN");
        Broadcast expected = Broadcast.okay(nick, recipients);
        assertEquals(expected, nick.updateServerModel(model), "broadcast");

        assertTrue(
                model.getUsersInChannel("java").contains("User1"),
                "User1 not in channel"
        );
        assertTrue(
                model.getUsersInChannel("java").contains("User2"),
                "User2 not in channel"
        );
        assertFalse(
                model.getUsersInChannel("java").contains("User0"),
                "User0 not in channel"
        );
        assertTrue(
                model.getUsersInChannel("java").contains("BOSSMAN"),
                "new nick name in channel"
        );

        assertEquals(
                3, model.getUsersInChannel("java").size(),
                "3 users in channel"
        );

    }

    /**
     * My test -> Tests user trying to set invalid nickname.
     * Also tests if old usernames were not effected after a failed attempt to
     * change nicknames.
     * Tests if all users are still in channel and nothing got effected after a
     * failed attempt.
     */
    @Test
    public void nickInValidTest() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command nick = new NicknameCommand(0, "User0", ".83#/-_@';");
        ServerResponse s = ServerResponse.INVALID_NAME;
        Broadcast expected = Broadcast.error(nick, s);

        assertEquals(expected, nick.updateServerModel(model), "invalid name broadcast");

        assertTrue(
                model.getUsersInChannel("java").contains("User0"),
                "User0 in channel, name not changed"
        );
        assertTrue(
                model.getUsersInChannel("java").contains("User1"),
                "User1 in channel"
        );

    }

    /**
     * My Test -> Test creation of multiple channels by same user.
     * Also checks another user trying to create a channel that already exists.
     * Tests correct server response and correct update on internal server state
     */
    @Test
    public void testMultiChannelCreation() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command create2 = new CreateCommand(0, "User0", "java2", false);
        create2.updateServerModel(model);

        assertEquals(
                2, model.getChannels().size(), "2 channels created"
        );

        Command create3 = new CreateCommand(1, "User1", "java", true);
        ServerResponse s = ServerResponse.CHANNEL_ALREADY_EXISTS;
        Broadcast expected = Broadcast.error(create3, s);

        assertEquals(
                expected, create3.updateServerModel(model), "broadcast channel already exists"
        );

        assertEquals(
                2, model.getChannels().size(), "2 channels created"
        );

        assertTrue(
                model.getUsersInChannel("java").contains("User0")
        );
        assertTrue(
                model.getUsersInChannel("java2").contains("User0")
        );

    }

    /**
     * My Test -> tests messaging in a channel that does not exist.
     * Messaging in an existing channel already tested.
     */
    @Test
    public void testMsgChannelNonExistent() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command msg = new MessageCommand(0, "User0", "java2", "heyoo");
        ServerResponse s = ServerResponse.NO_SUCH_CHANNEL;
        Broadcast expected = Broadcast.error(msg, s);
        assertEquals(expected, msg.updateServerModel(model), "broadcast of channel not existing");
    }

    /**
     * My Test -> Tests if a user, who is not a member of a channel, tries
     * messaging.
     * Tests correct Server Response and Broadcast.
     */
    @Test
    public void testMsgButNotAMember() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);

        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command msg = new MessageCommand(2, "User2", "java", "hey let me in");
        ServerResponse s = ServerResponse.USER_NOT_IN_CHANNEL;
        Broadcast expected = Broadcast.error(msg, s);
        assertEquals(expected, msg.updateServerModel(model), "user2 not a member of channel");
    }

    /**
     * My test -> Tests the correct broadcast of a user messaging multiple times.
     * Checks if recipients are correctly determined.
     * Also checks if during messaging, a user is added, if that user is added to
     * the recipient list.
     * Check if all users, at any moment, receive messages from any user inside the
     * channel.
     */
    @Test
    public void testValidMultiMessaging() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);

        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);

        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");

        Command msg1 = new MessageCommand(0, "User0", "java", "this is message 1");
        Broadcast expected = Broadcast.okay(msg1, recipients);
        assertEquals(expected, msg1.updateServerModel(model), "broadcast msg1");

        Command msg2 = new MessageCommand(0, "User0", "java", "this is message 2");
        Broadcast expected2 = Broadcast.okay(msg2, recipients);
        assertEquals(expected2, msg2.updateServerModel(model), "broadcast msg2");

        Command msg3 = new MessageCommand(0, "User0", "java", "this is message 3");
        Broadcast expected3 = Broadcast.okay(msg3, recipients);
        assertEquals(expected3, msg3.updateServerModel(model), "broadcast msg3");

        Command join2 = new JoinCommand(2, "User2", "java");
        join2.updateServerModel(model);
        recipients.add("User2");

        Command msg4 = new MessageCommand(1, "User1", "java", "Hello new User2");
        Broadcast expected4 = Broadcast.okay(msg4, recipients);
        assertEquals(expected4, msg4.updateServerModel(model), "broadcast msg4");

    }

    /**
     * My test -> Tests user trying to join private channel and checking size of the
     * channel
     */
    public void testUserJoiningPrivateChannel() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command join = new JoinCommand(1, "User1", "java");
        ServerResponse s = ServerResponse.JOIN_PRIVATE_CHANNEL;
        Broadcast expected = Broadcast.error(join, s);
        assertEquals(expected, join.updateServerModel(model), "user joining private chan");
        assertEquals(1, model.getUsersInChannel("java").size(), "num. users in channel");
    }

    @Test
    /**
     * My test -> Tests owner adding non-existent user then checking size of channel
     */
    public void testInvitingInValidUser() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command inviteValid = new InviteCommand(0, "User0", "java", "User2");
        ServerResponse s = ServerResponse.NO_SUCH_USER;
        Broadcast expected = Broadcast.error(inviteValid, s);
        assertEquals(expected, inviteValid.updateServerModel(model), "user can't be added");
        assertEquals(1, model.getUsersInChannel("java").size(), "num. users in channel");
    }

    @Test
    /**
     * My test -> Test to check if owner deletes himself from the channel then
     * channel is deleted
     * from server state. ALso checks if owner owns multiple channel only the
     * channel he deletes himself
     * from is deleted. The other channels remain the same.
     */
    public void testOwnerKickingHimself() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command invite = new InviteCommand(0, "User0", "java", "User1");
        invite.updateServerModel(model);

        Command create2 = new CreateCommand(0, "User0", "java2", true);
        create2.updateServerModel(model);

        Command inviteValid2 = new InviteCommand(0, "User0", "java2", "User1");
        inviteValid2.updateServerModel(model);

        Command kick = new KickCommand(0, "User0", "java", "User0");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.okay(kick, recipients);
        assertEquals(expected, kick.updateServerModel(model));

        assertFalse(
                model.getChannels().contains("java"),
                "Owner kicked himself hence deleted the channel"
        );

        assertTrue(
                model.getChannels().contains("java2"),
                "Owner kicked himself hence deleted the channel"
        );

        assertEquals(2, model.getUsersInChannel("java2").size(), "2 users still in channel 2");

        assertTrue(model.getUsersInChannel("java2").contains("User0"), "User 0 still in channel2");
        assertTrue(model.getUsersInChannel("java2").contains("User1"), "User1 still in channel2");
    }

    /**
     * My test -> Tests user trying to kick Owner out of channel.
     */
    @Test
    public void testUserKickingOwner() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command inviteValid = new InviteCommand(0, "User0", "java", "User1");
        inviteValid.updateServerModel(model);

        Command kick = new KickCommand(1, "User1", "java", "User0");
        ServerResponse s = ServerResponse.USER_NOT_OWNER;
        Broadcast expected = Broadcast.error(kick, s);
        assertEquals(expected, kick.updateServerModel(model), "user can't kick owner");
        assertEquals(2, model.getUsersInChannel("java").size(), "num. users in channel");

    }

    /**
     * My Test -> Test owner of multiple channels adding users and then kicking them
     * out.
     */
    public void testOwnerMultipleInvitesAndKicks() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command create2 = new CreateCommand(0, "User0", "java2", true);
        create2.updateServerModel(model);

        model.registerUser(2);

        Command inviteValid = new InviteCommand(0, "User0", "java", "User1");
        inviteValid.updateServerModel(model);
        Command inviteValid2 = new InviteCommand(0, "User0", "java", "User2");
        inviteValid2.updateServerModel(model);

        Command inviteValid3 = new InviteCommand(0, "User0", "java2", "User1");
        inviteValid3.updateServerModel(model);
        Command inviteValid4 = new InviteCommand(0, "User0", "java2", "User2");
        inviteValid4.updateServerModel(model);

        assertEquals(3, model.getUsersInChannel("java").size(), "3 users in channel 1");
        assertEquals(3, model.getUsersInChannel("java2").size(), "3 users in channel 1");

        Command kick = new KickCommand(0, "User0", "java", "User1");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User0");
        recipients.add("User1");
        recipients.add("User2");
        Broadcast expected = Broadcast.okay(kick, recipients);
        assertEquals(expected, kick.updateServerModel(model));

        assertEquals(2, model.getUsersInChannel("java").size(), "2 users left in channel 1");
        assertTrue(model.getUsersInChannel("java").contains("User0"), "User0 still in channel");
        assertFalse(
                model.getUsersInChannel("java").contains("User1"), "User1 still in channel bad"
        );
        assertTrue(model.getUsersInChannel("java").contains("User2"), "User2 still in channel");

    }

}
