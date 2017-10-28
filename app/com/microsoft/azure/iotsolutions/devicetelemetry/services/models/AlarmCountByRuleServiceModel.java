// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.devicetelemetry.services.models;

import org.joda.time.DateTime;

public final class AlarmCountByRuleServiceModel {
    private final int count;
    private final String status;
    private final DateTime dateCreated;
    private final RuleServiceModel rule;

    public AlarmCountByRuleServiceModel(
        final int count,
        final String status,
        final DateTime dateCreated,
        final RuleServiceModel rule) {

        this.count = count;
        this.status = status;
        this.dateCreated = dateCreated;
        this.rule = rule;
    }

    public int getCount() {
        return count;
    }

    public String getStatus() {
        return this.status;
    }

    public DateTime getDateCreated() {
        return this.dateCreated;
    }

    public RuleServiceModel getRule() {
        return this.rule;
    }

}
