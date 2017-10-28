// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.devicetelemetry.services;

import com.google.inject.Inject;
import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.helpers.QueryBuilder;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.models.AlarmCountByRuleServiceModel;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.models.AlarmServiceModel;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.models.RuleServiceModel;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.runtime.IServicesConfig;
import com.microsoft.azure.iotsolutions.devicetelemetry.services.storage.IStorageClient;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class Alarms implements IAlarms {

    private final IRules rulesService;
    private final IStorageClient storageClient;
    private String databaseName;
    private String collectionId;

    @Inject
    public Alarms(IServicesConfig servicesConfig, IRules rulesService, IStorageClient storageClient) throws Exception {
        this.rulesService = rulesService;
        this.storageClient = storageClient;
        this.databaseName = servicesConfig.getAlarmsStorageConfig().getDocumentDbDatabase();
        this.collectionId = servicesConfig.getAlarmsStorageConfig().getDocumentDbCollection();
    }

    @Override
    public AlarmServiceModel get(String id) throws Exception {
        return new AlarmServiceModel(this.getDocumentById(id));
    }

    @Override
    public CompletionStage<List<AlarmCountByRuleServiceModel>> getAlarmCountByRuleList(
        DateTime from,
        DateTime to,
        String order,
        int skip,
        int limit,
        String[] devices
    ) throws Exception {

        ArrayList<AlarmCountByRuleServiceModel> alarmByRuleList = new ArrayList<>();

        // get list of rules
        return this.rulesService.getListAsync(
            order,
            skip,
            limit,
            null).thenApply(rulesList -> {

            // get open alarm count and most recent alarm for each rule
            for (RuleServiceModel rule : rulesList) {

                // get open/acknowledged alarm count for rule
                String[] statusList = {"open", "acknowledged"};
                String sqlQuery = QueryBuilder.getCountSQL(
                    "alarm",
                    rule.getId(), "rule.id",
                    from, "created",
                    to, "created",
                    devices, "device.id",
                    statusList, "status");

                Document doc = new Document();

                try {
                    ArrayList<Document> resultList = this.storageClient.queryDocuments(
                        this.databaseName,
                        this.collectionId,
                        null,
                        sqlQuery,
                        skip);
                    if (resultList.size() > 0) {
                        doc = resultList.get(0);
                    } else {
                        // There are no alarms for this time period,
                        // skip and go to next rule
                        continue;
                    }
                } catch (java.lang.Exception e) {
                    throw new CompletionException(
                        new ExternalDependencyException(
                            "Could not retrieve alarm count for rule id "
                                + rule.getId(), e));
                }

                int count = doc.getInt("_aggregate");

                // get most recent alarm for rule
                AlarmServiceModel recentAlarm = new AlarmServiceModel();
                try {
                    ArrayList<AlarmServiceModel> resultList = getListByRuleId(
                        rule.getId(),
                        from,
                        to,
                        "asc",
                        0,
                        1,
                        devices
                    );
                    if (resultList.size() > 0) {
                        recentAlarm = resultList.get(0);
                    } else {
                        // There was no alarm found for this time period,
                        // skip and go to next rule
                        continue;
                    }
                } catch (java.lang.Exception e) {
                    throw new CompletionException(
                        new ExternalDependencyException(
                            "Could not retrieve most recent alarm for rule id "
                                + rule.getId(), e));
                }

                // Add alarm by rule to list
                alarmByRuleList.add(
                    new AlarmCountByRuleServiceModel(
                        count,
                        recentAlarm.getStatus(),
                        recentAlarm.getDateCreated(),
                        rule));
            }

            return alarmByRuleList;
        });
    }

    @Override
    public ArrayList<AlarmServiceModel> getListByRuleId(String id, DateTime from, DateTime to, String order, int skip,
                                                        int limit, String[] devices) throws Exception {
        String sqlQuery = QueryBuilder.getDocumentsSQL(
            "alarm",
            id, "rule.id",
            from, "created",
            to, "created",
            order, "created",
            skip,
            limit,
            devices, "device.id");
        ArrayList<Document> docs = this.storageClient.queryDocuments(
            this.databaseName,
            this.collectionId,
            null,
            sqlQuery,
            skip);

        ArrayList<AlarmServiceModel> alarms = new ArrayList<AlarmServiceModel>();
        for (Document doc : docs) {
            alarms.add(new AlarmServiceModel(doc));
        }

        return alarms;
    }

    @Override
    public ArrayList<AlarmServiceModel> getList(DateTime from, DateTime to, String order, int skip,
                                                int limit, String[] devices) throws Exception {
        String sqlQuery = QueryBuilder.getDocumentsSQL(
            "alarm",
            null, null,
            from, "created",
            to, "created",
            order, "created",
            skip,
            limit,
            devices, "device.id");
        ArrayList<Document> docs = this.storageClient.queryDocuments(
            this.databaseName,
            this.collectionId,
            null,
            sqlQuery,
            skip);

        ArrayList<AlarmServiceModel> alarms = new ArrayList<AlarmServiceModel>();
        for (Document doc : docs) {
            alarms.add(new AlarmServiceModel(doc));
        }

        return alarms;
    }

    public AlarmServiceModel update(String id, String status) throws Exception {
        Document document = getDocumentById(id);
        document.set("status", status);

        document = this.storageClient.upsertDocument(
            this.databaseName,
            this.collectionId,
            document
        );

        return new AlarmServiceModel(document);
    }

    private Document getDocumentById(String id) throws Exception {
        // Retrieve the document using the DocumentClient.
        ArrayList<Document> documentList = this.storageClient.queryDocuments(
            this.databaseName,
            this.collectionId,
            null,
            "SELECT * FROM c WHERE c.id='" + id + "'",
            0);

        if (documentList.size() > 0) {
            return documentList.get(0);
        } else {
            return null;
        }
    }

}
