package cz.cesnet.shongo.domain.cesnet;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.api.jade.recording.*;
import cz.cesnet.shongo.connector.test.AbstractConnectorTest;
import junit.framework.Assert;
import org.joda.time.Duration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Class for testing connectors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CesnetConnectorTest extends AbstractConnectorTest
{
    private static final String MULTIPOINT_ROOM_NAME = "zzz-shongo-test-room";

    private static final String RECORDING_FOLDER_NAME = "zzz-shongo-test-folder";

    public CesnetConnectorTest()
    {
        super("../../../shongo-deployment", DEFAULT_CONFIGURATION_FILE_NAMES);
    }

    @Test
    public void testAdobeConnectDevices() throws Exception
    {
        Connector connect = addConnector("connect-test");
        testAdobeConnect(connect);
    }

    @Test
    public void testCiscoMcuDevices() throws Exception
    {
        Connector mcu1 = addConnector("mcu1");
        Connector mcu2 = addConnector("mcu2");
        Connector mcu3 = addConnector("mcu3");
        testCiscoMcu(mcu1, "950087099");
        testCiscoMcu(mcu2, "950083099");
        testCiscoMcu(mcu3, "950083099");
    }

    @Test
    public void testTcsDevices() throws Exception
    {
        Connector mcu1 = addConnector("mcu1");
        //Connector mcu2 = addConnector("mcu2");
        Connector tcs1 = addConnector("tcs1");
        //Connector tcs2 = addConnector("tcs2");

        testTcs(tcs1, mcu1, "950087099");
        //testTcs(tcs2, mcu1, "950087099");
        //testTcs(tcs2, mcu2, "950083099");
    }

    private void testAdobeConnect(Connector connector)
    {
        printTestBegin(connector, "Adobe Connect");

        Set<Technology> technologies = new HashSet<Technology>();
        technologies.add(Technology.ADOBE_CONNECT);
        Alias alias = new Alias(AliasType.ADOBE_CONNECT_URI, connector.getAddress() + "/" + MULTIPOINT_ROOM_NAME);
        testMultipoint(connector, technologies, alias);

        printTestEnd(connector);
    }

    private void testCiscoMcu(Connector connector, String number)
    {
        printTestBegin(connector, "Cisco MCU");

        Set<Technology> technologies = new HashSet<Technology>();
        technologies.add(Technology.H323);
        technologies.add(Technology.SIP);
        Alias alias = new Alias(AliasType.H323_E164, number);
        testMultipoint(connector, technologies, alias);

        printTestEnd(connector);
    }

    public void testTcs(Connector tcs, Connector mcu, String number)
    {
        String roomId = null;
        String recordingFolderId = null;
        try {
            Alias roomAlias = new Alias(AliasType.H323_E164, number);

            // Create room on MCU
            Room room = new Room();
            room.setDescription("Srom Test");
            room.setLicenseCount(1);
            room.addTechnology(Technology.H323);
            room.addAlias(new Alias(AliasType.ROOM_NAME, MULTIPOINT_ROOM_NAME));
            room.addAlias(roomAlias);
            roomId = performCommand(mcu, new CreateRoom(room));
            Assert.assertNotNull(roomId);
            dump(roomId);

            // Create recording folder
            recordingFolderId = performCommand(tcs,
                    new CreateRecordingFolder(new RecordingFolder(RECORDING_FOLDER_NAME)));
            Assert.assertNotNull(recordingFolderId);
            dump(recordingFolderId);

            // Start recording
            String recordingId = performCommand(tcs,
                    new StartRecording(recordingFolderId, roomAlias, new RecordingSettings()));
            Assert.assertNotNull(recordingId);
            dump(recordingId);

            // TODO: wait for recording started
            sleep(Duration.standardSeconds(10));
            waitForUserCheck("Recording started...");

            // Stop recording
            performCommand(tcs, new StopRecording(recordingId));

            // TODO: wait for recording to be prepared on TCS
            waitForUserCheck("Recording stopped...");

            // TODO: Move recording to recording folder
            performCommand(tcs, new CheckRecordings());

            waitForUserCheck("Checking recordings...");
        }
        finally {
            if (recordingFolderId != null) {
                performCommand(tcs, new DeleteRecordingFolder(recordingFolderId));
            }
            if (roomId != null) {
                performCommand(mcu, new DeleteRoom(roomId));
            }
        }
    }

    private void testMultipoint(Connector connector, Set<Technology> technologies, Alias alias)
    {
        printTestBegin(connector, "Multipoint");

        Room room;
        String roomId;

        // Create room
        room = new Room();
        room.setDescription("Srom Test");
        room.setLicenseCount(1);
        room.setTechnologies(technologies);
        room.addAlias(new Alias(AliasType.ROOM_NAME, MULTIPOINT_ROOM_NAME));
        room.addAlias(alias);
        roomId = performCommand(connector, new CreateRoom(room));
        Assert.assertNotNull("Room shall be created", roomId);
        room = performCommand(connector, new GetRoom(roomId));
        Assert.assertNotNull("Room shall be created", room);
        Alias roomAliasName = room.getAlias(AliasType.ROOM_NAME);
        Assert.assertNotNull("Room  shall have alias " + AliasType.ROOM_NAME, roomAliasName);
        Assert.assertEquals(MULTIPOINT_ROOM_NAME, roomAliasName.getValue());

        // Delete room
        performCommand(connector, new DeleteRoom(roomId));
        room = performCommand(connector, new GetRoom(roomId));
        Assert.assertNull("Room  shall not exist", room);

        printTestEnd(connector);
    }
}
