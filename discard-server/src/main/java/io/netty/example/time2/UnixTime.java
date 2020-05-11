package io.netty.example.time2;

import java.util.Date;

public class UnixTime {
    private final long value;

    public UnixTime() {
        this(System.currentTimeMillis() / 1000L + 2208988800L);
    }

    public UnixTime(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "转换出来的时间是："+ new Date((getValue() - 2208988800L) * 1000L).toString();
    }
}
