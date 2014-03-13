package com.donvigo.androidmanifestparser.manifest;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Created by vgaidarji on 13.03.14.
 */
@Root(strict = false)
public class ApplicationEntry{
    @ElementList(entry = "activity", inline = true)
    private List<ActivityEntry> activities;

    public List<ActivityEntry> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityEntry> activities) {
        this.activities = activities;
    }
}
