package org.cis120;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.TreeSet;

/**
 * These tests are provided for testing the server's handling of invite-only
 * channels and kicking
 * users. You can and should use these tests as a model for your own testing,
 * but write your tests
 * in ServerModelTest.java.
 * 
 * Note that "assert" commands used for testing can have a String as their first
 * argument, denoting
 * what the "assert" command is testing.
 * 
 * Remember to check http://junit.sourceforge.net/javadoc/org/junit/Assert.html
 * for assert method
 * documention!
 */
public class Task5Test {
    private ServerModel model;

    /**
     * Before each test, we initialize model to be
     * a new ServerModel (with all new, empty state)
     */
    @BeforeEach
    public void setUp() {
        model = new ServerModel();

        model.registerUser(0); // add user with id = 0
        model.registerUser(1); // add user with id = 1

        // this command will create a channel called "java" with "User0" (with id = 0)
        // as the owner
        Command create = new CreateCommand(0, "User0", "java", true);

        // this line *actually* updates the model's state
        create.updateServerModel(model);
    }

    @Test
    public void testInviteByOwner() {
        Command invite = new InviteCommand(0, "User0", "java", "User1");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.names(invite, recipients, "User0");
        assertEquals(expected, invite.updateServerModel(model), "broadcast");

        assertEquals(2, model.getUsersInChannel("java").size(), "num. users in channel");
        assertTrue(model.getUsersInChannel("java").contains("User0"), "User0 in channel");
        assertTrue(model.getUsersInChannel("java").contains("User1"), "User1 in channel");
    }

    @Test
    public void testInviteByNonOwner() {
        model.registerUser(2);
        Command inviteValid = new InviteCommand(0, "User0", "java", "User1");
        inviteValid.updateServerModel(model);

        Command inviteInvalid = new InviteCommand(1, "User1", "java", "User2");
        Broadcast expected = Broadcast.error(inviteInvalid, ServerResponse.USER_NOT_OWNER);
        assertEquals(expected, inviteInvalid.updateServerModel(model), "error");

        assertEquals(2, model.getUsersInChannel("java").size(), "num. users in channel");
        assertTrue(model.getUsersInChannel("java").contains("User0"), "User0 in channel");
        assertTrue(model.getUsersInChannel("java").contains("User1"), "User1 in channel");
        assertFalse(model.getUsersInChannel("java").contains("User2"), "User2 not in channel");
    }

    /**
     * My test -> Tests user trying to join private channel and checking size of the
     * channel
     */
    public void testUserJoiningPrivateChannel() {
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

    @Test
    public void testKickOneChannel() {
        Command invite = new InviteCommand(0, "User0", "java", "User1");
        invite.updateServerModel(model);

        Command kick = new KickCommand(0, "User0", "java", "User1");
        Set<String> recipients = new TreeSet<>();
        recipients.add("User1");
        recipients.add("User0");
        Broadcast expected = Broadcast.okay(kick, recipients);
        assertEquals(expected, kick.updateServerModel(model));

        assertEquals(1, model.getUsersInChannel("java").size(), "num. users in channel");
        assertTrue(model.getUsersInChannel("java").contains("User0"), "User0 still in channel");
        assertFalse(model.getUsersInChannel("java").contains("User1"), "User1 still in channel");
    }

    /**
     * My test -> Tests user trying to kick Owner out of channel.
     */
    @Test
    public void testUserKickingOwner() {
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
