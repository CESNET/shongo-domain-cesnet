package cz.cesnet.shongo.domain.cesnet;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.connector.api.RecordingSettings;
import cz.cesnet.shongo.connector.api.jade.multipoint.CreateRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.DeleteRoom;
import cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom;
import cz.cesnet.shongo.connector.api.jade.recording.*;
import cz.cesnet.shongo.connector.test.AbstractConnectorTest;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.*;

/**
 * Class for testing connectors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CesnetConnectorTest extends AbstractConnectorTest
{
    private static final String ROOM_NAME = "zzz-shongo-test-room";

    private static final String RECORDING_FOLDER_NAME = "zzz-shongo-test-folder";

    private static final Duration RECORDING_WAIT_TIMEOUT = Duration.standardMinutes(3);

    public CesnetConnectorTest()
    {
        super("../../../shongo-deployment", DEFAULT_CONFIGURATION_FILE_NAMES);
    }

    /*
    @Test
    public void testClear()
    {
        Connector mcu = addConnector("mcu1");
        Connector tcs = addConnector("tcs1");
        performCommand(mcu, new DeleteRoom(ROOM_NAME));
        performCommand(tcs, new DeleteRecordingFolder(RECORDING_FOLDER_NAME));
        performCommand(tcs, new DeleteRecording("zzz-shongo-test-folder_test_8801A9A8-C170-44E1-A239-6585C4E51811"));
    }
    /**/

    @Test
    public void testConnectTest() throws Exception
    {
        Connector connect = addConnector("connect-test");
        testAdobeConnect(connect);
    }

    @Test
    public void testMcu1() throws Exception
    {
        Connector mcu1 = addConnector("mcu1");
        testCiscoMcu(mcu1, "950087099");
    }

    @Test
    public void testMcu2() throws Exception
    {
        Connector mcu2 = addConnector("mcu2");
        testCiscoMcu(mcu2, "950083099");
    }

    @Test
    public void testMcu3() throws Exception
    {
        Connector mcu3 = addConnector("mcu3");
        testCiscoMcu(mcu3, "950083099");
    }

    @Test
    public void testTcs1() throws Exception
    {
        Connector tcs1 = addConnector("tcs1");
        Connector mcu1 = addConnector("mcu1");
        Connector mcu2 = addConnector("mcu2");
        Connector mcu3 = addConnector("mcu3");

        testTcs(tcs1, mcu1, "950087099");
        testTcs(tcs1, mcu2, "950083099");
        testTcs(tcs1, mcu3, "950083099");
    }

    @Test
    public void testTcs2() throws Exception
    {
        Connector tcs2 = addConnector("tcs2");
        Connector mcu1 = addConnector("mcu1");
        Connector mcu2 = addConnector("mcu2");
        Connector mcu3 = addConnector("mcu3");

        testTcs(tcs2, mcu1, "950087099");
        testTcs(tcs2, mcu2, "950083099");
        testTcs(tcs2, mcu3, "950083099");
    }

    /**
     * Test Adobe Connect.
     *
     * @param connector
     * @throws Exception
     */
    private void testAdobeConnect(Connector connector) throws Exception
    {
        printTestBegin(connector, "Adobe Connect");

        try {
            Set<Technology> technologies = new HashSet<Technology>();
            technologies.add(Technology.ADOBE_CONNECT);
            Alias alias = new Alias(AliasType.ADOBE_CONNECT_URI, connector.getAddress() + "/" + ROOM_NAME);
            testMultipoint(connector, technologies, alias);
        }
        finally {
            printTestEnd(connector);
        }
    }

    /**
     * Test MCU.
     *
     * @param connector
     * @param number
     * @throws Exception
     */
    private void testCiscoMcu(Connector connector, String number) throws Exception
    {
        printTestBegin(connector, "Cisco MCU");

        try {
            Set<Technology> technologies = new HashSet<Technology>();
            technologies.add(Technology.H323);
            technologies.add(Technology.SIP);
            Alias alias = new Alias(AliasType.H323_E164, number);
            testMultipoint(connector, technologies, alias);
        }
        finally {
            printTestEnd(connector);
        }
    }

    /**
     * Test TCS.
     *
     * @param tcs
     * @param mcu
     * @param number
     * @throws Exception
     */
    public void testTcs(Connector tcs, Connector mcu, String number) throws Exception
    {
        printTestBegin(tcs, "TCS");

        String roomId = null;
        String recordingFolderId = null;
        String recordingId = null;
        try {
            Alias roomAlias = new Alias(AliasType.H323_E164, number);

            // Create room in MCU
            Room room = new Room();
            room.setDescription("Srom Test");
            room.setLicenseCount(1);
            room.addTechnology(Technology.H323);
            room.addAlias(new Alias(AliasType.ROOM_NAME, ROOM_NAME));
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
            recordingId = performCommand(tcs,
                    new StartRecording(recordingFolderId, roomAlias, new RecordingSettings()));
            Assert.assertNotNull(recordingId);
            dump(recordingId);

            // Wait for recording to become STARTED
            waitForRecordingDuration(tcs, recordingId, Duration.standardSeconds(10));

            // Stop recording
            performCommand(tcs, new StopRecording(recordingId));
            Recording recording = performCommand(tcs, new GetRecording(recordingId));
            Assert.assertNull(recording.getDownloadUrl());

            // Wait for recording to become PROCESSED
            waitForRecordingState(tcs, recordingId, Recording.State.PROCESSED);

            // Check recording
            performCommand(tcs, new CheckRecordings());
            recording = performCommand(tcs, new GetRecording(recordingId));
            Assert.assertNotNull(recording.getDownloadUrl());
        }
        finally {
            if (recordingFolderId != null) {
                performCommand(tcs, new DeleteRecordingFolder(recordingFolderId));
            }
            if (roomId != null) {
                performCommand(mcu, new DeleteRoom(roomId));
            }
            if (recordingId != null) {
                performCommand(tcs, new DeleteRecording(recordingId));
            }
            printTestEnd(tcs);
        }
    }

    /**
     * Test multipoint device.
     *
     * @param connector
     * @param technologies
     * @param alias
     */
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
        room.addAlias(new Alias(AliasType.ROOM_NAME, ROOM_NAME));
        room.addAlias(alias);
        roomId = performCommand(connector, new CreateRoom(room));
        Assert.assertNotNull("Room shall be created", roomId);
        room = performCommand(connector, new GetRoom(roomId));
        Assert.assertNotNull("Room shall be created", room);
        Alias roomAliasName = room.getAlias(AliasType.ROOM_NAME);
        Assert.assertNotNull("Room  shall have alias " + AliasType.ROOM_NAME, roomAliasName);
        Assert.assertEquals(ROOM_NAME, roomAliasName.getValue());

        // Delete room
        performCommand(connector, new DeleteRoom(roomId));
        room = performCommand(connector, new GetRoom(roomId));
        Assert.assertNull("Room  shall not exist", room);

        printTestEnd(connector);
    }

    /**
     * Wait for recording state.
     *
     * @param tcs
     * @param recordingId
     */
    private void waitForRecordingDuration(Connector tcs, String recordingId, Duration duration) throws Exception
    {
        DateTime waitTimeout = DateTime.now().plus(RECORDING_WAIT_TIMEOUT);
        Recording recording = performCommand(tcs, new GetRecording(recordingId));
        while (!recording.getDuration().isLongerThan(duration)) {
            logger.info("Waiting for recording to become longer than {}...", duration);
            sleep(Duration.standardSeconds(5));
            recording = performCommand(tcs, new GetRecording(recordingId));
            if (waitTimeout.isBeforeNow()) {
                throw new Exception("Waiting for recording " + recordingId + " duration " + duration + " expired.");
            }
        }
    }

    /**
     * Wait for recording state.
     *
     * @param tcs
     * @param recordingId
     * @param state
     */
    private void waitForRecordingState(Connector tcs, String recordingId, Recording.State state) throws Exception
    {
        DateTime waitTimeout = DateTime.now().plus(RECORDING_WAIT_TIMEOUT);
        Recording recording = performCommand(tcs, new GetRecording(recordingId));
        while (!state.equals(recording.getState())) {
            logger.info("Waiting for recording to become {}...", state);
            sleep(Duration.standardSeconds(10));
            recording = performCommand(tcs, new GetRecording(recordingId));
            if (waitTimeout.isBeforeNow()) {
                throw new Exception("Waiting for recording " + recordingId + " state " + state + " expired.");
            }
        }
    }
}
