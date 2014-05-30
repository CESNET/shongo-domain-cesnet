package cz.cesnet.shongo.domain.cesnet;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.test.AbstractConnectorTest;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Debugging tests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DebuggingTest extends AbstractConnectorTest
{
    private static final String ROOM_NAME = "zzz-shongo-test-room";

    public DebuggingTest()
    {
        super("../../../shongo-deployment", DEFAULT_CONFIGURATION_FILE_NAMES);
    }

    @Test
    public void testMcuRoomNotRegistered() throws Exception
    {
        Connector mcu = addConnector("mcu3");

        // Create room
        Room room = new Room();
        room.setDescription("Srom Test");
        room.setLicenseCount(1);
        room.addTechnology(Technology.H323);
        room.addTechnology(Technology.SIP);
        room.addAlias(new Alias(AliasType.ROOM_NAME, ROOM_NAME));
        room.addAlias(new Alias(AliasType.H323_E164, "950083099"));
        H323RoomSetting roomSetting = new H323RoomSetting();
        roomSetting.setRegisterWithGatekeeper(false);
        room.addRoomSetting(roomSetting);
        String roomId = performCommand(mcu, new CreateRoom(room));
        Assert.assertNotNull(roomId);
        room = performCommand(mcu, new GetRoom(roomId));
        Assert.assertNotNull(room);

        waitForUserCheck("Room Created");

        // Delete room
        performCommand(mcu, new DeleteRoom(roomId));
        room = performCommand(mcu, new GetRoom(roomId));
        Assert.assertNull(room);
    }
}
