package de.dytanic.cloudnet.common.encrypt;

import de.dytanic.cloudnet.common.StringUtil;
import org.junit.Assert;
import org.junit.Test;

public final class EncryptToTest {

    @Test
    public void testEncryptToSHA256() throws Exception
    {
        Assert.assertNotNull(EncryptTo.encryptToSHA256(StringUtil.generateRandomString(32)));
        Assert.assertNotNull(EncryptTo.encryptToSHA1(StringUtil.generateRandomString(32)));
    }
}