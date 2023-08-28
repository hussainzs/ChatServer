package org.cis120;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * These tests are provided for testing client connection and disconnection,
 * and nickname changes. You can and should use these tests as a model for
 * your own testing, but write your tests in ServerModelTest.java.
 *
 * Note that "assert" commands used for testing can have a String as
 * their last argument, denoting what the "assert" command is testing.
 * 
 * Remember to check:
 * https://junit.org/junit5/docs/5.0.1/api/org/junit/jupiter/api/Assertions.html
 * for assert method documention!
 */
public class Task3Test {
    private ServerModel model;

    /**
     * Before each test, we initialize model to be
     * a new ServerModel (with all new, empty state)
     */
    @BeforeEach
    public void setUp() {
        model = new ServerModel();
    }

    @Test
    public void testEmptyOnInit() {
        assertTrue(model.getRegisteredUsers().isEmpty(), "No registered users");
    }

    // The questions to ask when writing a test for ServerModel:
    // 1. does the method return what is expected?
    // 2. was the state updated as expected?
    @Test
    public void testRegisterSingleUser() {
        // Recall: registerUser returns a 'connected' Broadcast with the nickname of the
        // registered
        // user! We expect the nickname to be "User0"
        Broadcast expected = Broadcast.connected("User0");

        // test: does model.registerUser(0) return what we expect?
        assertEquals(expected, model.registerUser(0), "Broadcast");

        // Recall: getRegisteredUsers returns a Collection of the nicknames of all
        // registered users!
        // We expect this collection to have one element, which is "User0"
        Collection<String> registeredUsers = model.getRegisteredUsers();

        // test: does the collection contain only one element?
        assertEquals(1, registeredUsers.size(), "Num. registered users");

        // test: does the collection contain the nickname "User0"
        assertTrue(registeredUsers.contains("User0"), "User0 is registered");
    }

    @Test
    public void testRegisterMultipleUsers() {
        Broadcast expected0 = Broadcast.connected("User0");
        assertEquals(expected0, model.registerUser(0), "Broadcast for User0");
        Broadcast expected1 = Broadcast.connected("User1");
        assertEquals(expected1, model.registerUser(1), "Broadcast for User1");
        Broadcast expected2 = Broadcast.connected("User2");
        assertEquals(expected2, model.registerUser(2), "Broadcast for User2");

        Collection<String> registeredUsers = model.getRegisteredUsers();
        assertEquals(3, registeredUsers.size(), "Num. registered users");
        assertTrue(registeredUsers.contains("User0"), "User0 is registered");
        assertTrue(registeredUsers.contains("User1"), "User1 is registered");
        assertTrue(registeredUsers.contains("User2"), "User2 is registered");
    }

    @Test
    public void testDeregisterSingleUser() {
        model.registerUser(0);
        model.deregisterUser(0);
        assertTrue(model.getRegisteredUsers().isEmpty(), "No registered users");
    }

    @Test
    public void testDeregisterOneOfManyUsers() {
        model.registerUser(0);
        model.registerUser(1);
        model.deregisterUser(0);
        assertFalse(model.getRegisteredUsers().isEmpty(), "Registered users still exist");
        assertFalse(model.getRegisteredUsers().contains("User0"), "User0 does not exist");
        assertTrue(model.getRegisteredUsers().contains("User1"), "User1 still exists");
    }

    @Test
    public void testDeregisterOneOfTwoUsers() {
        model.registerUser(0);
        model.registerUser(1);
        model.deregisterUser(1);
        assertFalse(model.getRegisteredUsers().isEmpty(), "Registered users still exist");
        assertTrue(model.getRegisteredUsers().contains("User0"), "User0 still exists");
    }

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
        create.updateServerModel(model);

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

    @Test
    public void testNickNotInChannels() {
        model.registerUser(0);
        Command command = new NicknameCommand(0, "User0", "cis120");
        Set<String> recipients = Collections.singleton("cis120");
        Broadcast expected = Broadcast.okay(command, recipients);
        assertEquals(expected, command.updateServerModel(model), "Broadcast");
        Collection<String> users = model.getRegisteredUsers();
        assertFalse(users.contains("User0"), "Old nick not registered");
        assertTrue(users.contains("cis120"), "New nick registered");
    }

    @Test
    public void testNickCollision() {
        model.registerUser(0);
        model.registerUser(1);
        Command command = new NicknameCommand(0, "User0", "User1");
        Broadcast expected = Broadcast.error(command, ServerResponse.NAME_ALREADY_IN_USE);
        assertEquals(expected, command.updateServerModel(model), "Broadcast");
        Collection<String> users = model.getRegisteredUsers();
        assertTrue(users.contains("User0"), "Old nick still registered");
        assertTrue(users.contains("User1"), "Other user still registered");
    }

    /**
     * My test -> Tests user, also an owner of a channel, changing its nickname.
     * Also tests create, join, and if server internal state keeps track of updated
     * name
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

    @Test
    public void testNickCollisionOnConnect() {
        model.registerUser(0);
        Command command = new NicknameCommand(0, "User0", "User1");
        command.updateServerModel(model);
        Broadcast expected = Broadcast.connected("User0");
        assertEquals(expected, model.registerUser(1), "Broadcast");
        Collection<String> users = model.getRegisteredUsers();
        assertEquals(2, users.size(), "Num. registered users");
        assertTrue(users.contains("User0"), "User0 registered");
        assertTrue(users.contains("User1"), "User1 registered");
        assertEquals(0, model.getUserId("User1"), "User1 has ID 0");
        assertEquals(1, model.getUserId("User0"), "User0 has ID 1");
    }

    @Test
    public void testEncapsulationGetRegisteredUsers() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        Collection<String> usersBefore = model.getRegisteredUsers();
        try {
            usersBefore.remove("User0");
            usersBefore.remove("User1");
        } catch (UnsupportedOperationException uox) {
            // Ok to return a Collections.unmodifiableSet
        }
        Collection<String> usersAfter = model.getRegisteredUsers();
        assertTrue(usersAfter.contains("User0"), "User0 not removed");
        assertTrue(usersAfter.contains("User1"), "User1 not removed");
    }

}
