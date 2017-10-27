// Copyright (c) Microsoft. All rights reserved.

package webservice.test.v1.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.Alarms;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.IAlarms;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.storage.StorageClient;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.models.AlarmServiceModel;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.runtime.IServicesConfig;
import com.microsoft.azure.iotsolutions.devicetelemetry.webservice.runtime.Config;
import com.microsoft.azure.iotsolutions.devicetelemetry.webservice.v1.controllers.AlarmsByRuleController;
import helpers.UnitTest;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import play.Logger;
import play.mvc.Result;
import com.microsoft.azure.documentdb.*;

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlarmsByRuleControllerTest {
    private static final Logger.ALogger log = Logger.of(AlarmsByRuleControllerTest.class);
    private AlarmsByRuleController controller;

    private final String docSchemaKey = "doc.schema";
    private final String docSchemaValue = "alarm";

    private final String docSchemaVersionKey = "doc.schemaVersion";
    private final int docSchemaVersionValue = 1;

    private final String createdKey = "created";
    private final String modifiedKey = "modified";
    private final String descriptionKey = "description";
    private final String statusKey = "status";
    private final String deviceIdKey = "device.id";

    private final String ruleIdKey = "rule.id";
    private final String ruleSeverityKey = "rule.severity";
    private final String ruleDescriptionKey = "rule.description";

    @Before
    public void setUp() {
        // setup before every test
        try {
            IServicesConfig servicesConfig = new Config().getServicesConfig();
            StorageClient client = new StorageClient(servicesConfig);
            String dbName = servicesConfig.getAlarmsStorageConfig().getDocumentDbDatabase();
            String collName = servicesConfig.getAlarmsStorageConfig().getDocumentDbCollection();
            client.createCollectionIfNotExists(dbName, collName);
            ArrayList<AlarmServiceModel> sampleAlarms = getSampleAlarms();
            ObjectMapper mapper = new ObjectMapper();
            for (AlarmServiceModel sampleAlarm : sampleAlarms) {
                client.upsertDocument(
                    dbName,
                    collName,
                    alarmToDocument(sampleAlarm)
                );
            }
            Alarms rule = new Alarms(servicesConfig, client);
            controller = new AlarmsByRuleController(rule);
        } catch (Exception ex) {
            log.error("Exception setting up test", ex);
        }
    }

    private Document alarmToDocument(AlarmServiceModel alarm) {

        Document document = new Document();

        // TODO: make inserts idempotent
        document.setId(UUID.randomUUID().toString());
        document.set(docSchemaKey, docSchemaValue);
        document.set(docSchemaVersionKey, docSchemaVersionValue);
        document.set(createdKey, alarm.getDateCreated().getMillis());
        document.set(modifiedKey, alarm.getDateModified().getMillis());
        document.set(statusKey, alarm.getStatus());
        document.set(descriptionKey, alarm.getDescription());
        document.set(deviceIdKey, alarm.getDeviceId());
        document.set(ruleIdKey, alarm.getRuleId());
        document.set(ruleSeverityKey, alarm.getRuleSeverity());
        document.set(ruleDescriptionKey, alarm.getRuleDescription());

        // The logic used to generate the alarm (future proofing for ML)
        document.set("logic", "1Device-1Rule-1Message");

        return document;
    }

    /**
     * Get sample alarm to return to client.
     * TODO: remove after storage dependency is added
     *
     * @return sample alarm
     */
    private AlarmServiceModel getSampleAlarm() {
        return new AlarmServiceModel(
            "6l1log0f7h2yt6p",
            "1234",
            DateTime.parse("2017-02-22T22:22:22-08:00").toInstant().getMillis(),
            DateTime.parse("2017-02-22T22:22:22-08:00").toInstant().getMillis(),
            "Temperature on device x > 75 deg F",
            "group-Id",
            "device-id",
            "open",
            "1234",
            "critical",
            "HVAC temp > 75"
        );
    }

    /**
     * Sample alarms that will be added to the testalarms storage collection
     *
     * @return sample alarm list
     */
    private ArrayList<AlarmServiceModel> getSampleAlarms() {
        ArrayList<AlarmServiceModel> list = new ArrayList<AlarmServiceModel>();
        AlarmServiceModel alarm1 = new AlarmServiceModel(
            null,
            "1",
            DateTime.parse("2017-07-22T22:22:22-08:00").toInstant().getMillis(),
            DateTime.parse("2017-07-22T22:22:22-08:00").toInstant().getMillis(),
            "Temperature on device x > 75 deg F",
            "group-Id",
            "device-id",
            "open",
            "1",
            "critical",
            "HVAC temp > 50"
        );
        AlarmServiceModel alarm2 = new AlarmServiceModel(
            null,
            "2",
            DateTime.parse("2017-06-22T22:22:22-08:00").toInstant().getMillis(),
            DateTime.parse("2017-07-22T22:22:22-08:00").toInstant().getMillis(),
            "Temperature on device x > 75 deg F",
            "group-Id",
            "device-id",
            "acknowledged",
            "2",
            "critical",
            "HVAC temp > 60");
        AlarmServiceModel alarm3 = new AlarmServiceModel(
            null,
            "3",
            DateTime.parse("2017-05-22T22:22:22-08:00").toInstant().getMillis(),
            DateTime.parse("2017-06-22T22:22:22-08:00").toInstant().getMillis(),
            "Temperature on device x > 75 deg F",
            "group-Id",
            "device-id",
            "open",
            "3",
            "info",
            "HVAC temp > 70");
        AlarmServiceModel alarm4 = new AlarmServiceModel(
            null,
            "4",
            DateTime.parse("2017-04-22T22:22:22-08:00").toInstant().getMillis(),
            DateTime.parse("2017-06-22T22:22:22-08:00").toInstant().getMillis(),
            "Temperature on device x > 75 deg F",
            "group-Id",
            "device-id",
            "closed",
            "4",
            "warning",
            "HVAC temp > 80");
        list.add(alarm1);
        list.add(alarm2);
        list.add(alarm3);
        list.add(alarm4);
        return list;
    }

    @After
    public void tearDown() {
        // something after every test
    }

    @Test(timeout = 5000)
    @Category({UnitTest.class})
    public void provideAlarmsByRuleByIdResult() throws Exception {
        ArrayList<AlarmServiceModel> alarmResult = new ArrayList<AlarmServiceModel>() {{
            add(new AlarmServiceModel());
            add(new AlarmServiceModel());
        }};

        IAlarms alarms = mock(IAlarms.class);
        AlarmsByRuleController controller = new AlarmsByRuleController(alarms);
        when(alarms.getListByRuleId(
            "1", DateTime.now(), DateTime.now(), "asc", 0, 100, new String[0]))
            .thenReturn(alarmResult);

        // Act
        Result response = controller.get("", null, null, null, 0, 0, null);

        // Assert
        assertThat(response.body().isKnownEmpty(), is(false));
    }

    @Test(timeout = 5000)
    @Category({UnitTest.class})
    public void provideAlarmsByRuleListResult() throws Exception {
        ArrayList<AlarmServiceModel> alarmResult = new ArrayList<AlarmServiceModel>() {{
            add(new AlarmServiceModel());
            add(new AlarmServiceModel());
        }};

        IAlarms alarms = mock(IAlarms.class);
        AlarmsByRuleController controller = new AlarmsByRuleController(alarms);
        when(alarms.getList(
            DateTime.now(), DateTime.now(), "asc", 0, 100, new String[0]))
            .thenReturn(alarmResult);

        // Act
        Result response = controller.list(null, null, null, 0, 0, null);

        // Assert
        assertThat(response.body().isKnownEmpty(), is(false));
    }
}
