package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class NettyUtilsTest {

    @Test
    public void testNettyUtils() {
        ByteBuf byteBuf = Unpooled.buffer();

        int randomInt = ThreadLocalRandom.current().nextInt();
        long randomLong = ThreadLocalRandom.current().nextLong();
        String randomString = StringUtil.generateRandomString(10);

        NettyUtils.writeVarInt(byteBuf, randomInt);
        NettyUtils.writeVarLong(byteBuf, randomLong);
        NettyUtils.writeString(byteBuf, randomString);

        Assert.assertEquals(randomInt, NettyUtils.readVarInt(byteBuf));
        Assert.assertEquals(randomLong, NettyUtils.readVarLong(byteBuf));
        Assert.assertEquals(randomString, NettyUtils.readString(byteBuf));
    }
}