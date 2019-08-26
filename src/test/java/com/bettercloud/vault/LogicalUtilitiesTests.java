package com.bettercloud.vault;

import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.api.LogicalUtilities;
import com.bettercloud.vault.json.JsonObject;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;


public class LogicalUtilitiesTests {

    @Test
    public void addQualifierToPathTests() {
        ArrayList<String> stringList = new ArrayList<>();
        stringList.add("");
        String qualifierOutput = LogicalUtilities.addQualifierToPath(stringList, "test");
        Assert.assertEquals("/test/", qualifierOutput);

        stringList.clear();
        stringList.add("before");
        String qualifierOutput2 = LogicalUtilities.addQualifierToPath(stringList, "test");
        Assert.assertEquals("before/test/", qualifierOutput2);

        stringList.clear();
        stringList.add("before1");
        stringList.add("before2");
        String qualifierOutput3 = LogicalUtilities.addQualifierToPath(stringList, "test");
        Assert.assertEquals("before1/test/before2", qualifierOutput3);
    }

    @Test
    public void adjustPathForReadOrWriteTests() {
        String readOutputV2 = LogicalUtilities.adjustPathForReadOrWrite("test", Logical.logicalOperations.readV2);
        Assert.assertEquals(readOutputV2, "test/data/");

        String readOutputV2WithSlash = LogicalUtilities.adjustPathForReadOrWrite("test/", Logical.logicalOperations.readV2);
        Assert.assertEquals(readOutputV2WithSlash, "test/data//");

        String writeOutputV2 = LogicalUtilities.adjustPathForReadOrWrite("test", Logical.logicalOperations.writeV2);
        Assert.assertEquals(writeOutputV2, "test/data/");

        String writeOutputV2WithSlash = LogicalUtilities.adjustPathForReadOrWrite("test/", Logical.logicalOperations.writeV2);
        Assert.assertEquals(writeOutputV2WithSlash, "test/data//");

        String readOutputV1 = LogicalUtilities.adjustPathForReadOrWrite("test", Logical.logicalOperations.readV1);
        Assert.assertEquals(readOutputV1, "test");

        String writeOutputV1 = LogicalUtilities.adjustPathForReadOrWrite("test", Logical.logicalOperations.writeV1);
        Assert.assertEquals(writeOutputV1, "test");
    }

    @Test
    public void adjustPathForListTests() {
        String listOutputV2 = LogicalUtilities.adjustPathForList("test", Logical.logicalOperations.listV2);
        Assert.assertEquals(listOutputV2, "test/metadata/?list=true");

        String listOutputV2WithSlash = LogicalUtilities.adjustPathForList("test/", Logical.logicalOperations.listV2);
        Assert.assertEquals(listOutputV2WithSlash, "test/metadata//?list=true");

        String listOutputV1 = LogicalUtilities.adjustPathForList("test", Logical.logicalOperations.listV1);
        Assert.assertEquals(listOutputV1, "test?list=true");
    }

    @Test
    public void adjustPathForDeleteTests() {
        String deleteOutputV2 = LogicalUtilities.adjustPathForDelete("test", Logical.logicalOperations.deleteV2);
        Assert.assertEquals(deleteOutputV2, "test/metadata/");

        String deleteOutputV2WithSlash = LogicalUtilities.adjustPathForDelete("test/", Logical.logicalOperations.deleteV2);
        Assert.assertEquals(deleteOutputV2WithSlash, "test/metadata//");

        String deleteOutputV1 = LogicalUtilities.adjustPathForDelete("test", Logical.logicalOperations.deleteV1);
        Assert.assertEquals(deleteOutputV1, "test");
    }

    @Test
    public void adjustPathForVersionDeleteTests() {
        String versionDeleteOutput = LogicalUtilities.adjustPathForVersionDelete("test");
        Assert.assertEquals(versionDeleteOutput, "test/delete/");

        String versionDeleteOutputWithSlash = LogicalUtilities.adjustPathForVersionDelete("test/");
        Assert.assertEquals(versionDeleteOutputWithSlash, "test/delete//");
    }

    @Test
    public void adjustPathForVersionUnDeleteTests() {
        String versionDeleteOutput = LogicalUtilities.adjustPathForVersionUnDelete("test");
        Assert.assertEquals(versionDeleteOutput, "test/undelete/");

        String versionDeleteOutputWithSlash = LogicalUtilities.adjustPathForVersionUnDelete("test/");
        Assert.assertEquals(versionDeleteOutputWithSlash, "test/undelete//");
    }

    @Test
    public void adjustPathForVersionDestroyTests() {
        String versionDeleteOutput = LogicalUtilities.adjustPathForVersionDestroy("test");
        Assert.assertEquals(versionDeleteOutput, "test/destroy/");

        String versionDeleteOutputWithSlash = LogicalUtilities.adjustPathForVersionDestroy("test/");
        Assert.assertEquals(versionDeleteOutputWithSlash, "test/destroy//");
    }

    @Test
    public void jsonObjectToWriteFromEngineVersionTests() {
        JsonObject jsonObjectV2 = new JsonObject().add("test", "test");
        JsonObject jsonObjectFromEngineVersionV2 = LogicalUtilities.jsonObjectToWriteFromEngineVersion(Logical.logicalOperations.writeV2, jsonObjectV2);
        Assert.assertEquals(jsonObjectFromEngineVersionV2.get("data"), jsonObjectV2);

        JsonObject jsonObjectV1 = new JsonObject().add("test", "test");
        JsonObject jsonObjectFromEngineVersionV1 = LogicalUtilities.jsonObjectToWriteFromEngineVersion(Logical.logicalOperations.writeV1, jsonObjectV1);
        Assert.assertNull(jsonObjectFromEngineVersionV1.get("data"));
    }
}
