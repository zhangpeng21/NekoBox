package io.nekohasekai.sagernet.fmt;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import io.nekohasekai.sagernet.ktx.NetsKt;
import moe.matsuri.nb4a.utils.JavaUtil;

public abstract class AbstractBean extends Serializable {

    public String serverAddress;
    public Integer serverPort;

    public String name;

    //

    public String customOutboundJson;
    public String customConfigJson;

    //
    public transient String finalAddress;
    public transient int finalPort;

    public String displayName() {
        if (JavaUtil.isNotBlank(name)) {
            return name;
        } else {
            return displayAddress();
        }
    }

    public String displayAddress() {
        return NetsKt.wrapIPV6Host(serverAddress) + ":" + serverPort;
    }

    public String network() {
        return "tcp,udp";
    }

    public boolean canICMPing() {
        return true;
    }

    public boolean canTCPing() {
        return true;
    }

    public boolean canMapping() {
        return true;
    }

    @Override
    public void initializeDefaultValues() {
        if (JavaUtil.isNullOrBlank(serverAddress)) {
            serverAddress = "127.0.0.1";
        } else if (serverAddress.startsWith("[") && serverAddress.endsWith("]")) {
            serverAddress = NetsKt.unwrapIPV6Host(serverAddress);
        }
        if (serverPort == null) {
            serverPort = 1080;
        }
        if (name == null) name = "";

        finalAddress = serverAddress;
        finalPort = serverPort;

        if (customOutboundJson == null) customOutboundJson = "";
        if (customConfigJson == null) customConfigJson = "";
    }


    private transient boolean serializeWithoutName;

    @Override
    public void serializeToBuffer(@NonNull ByteBufferOutput output) {
        serialize(output);

        output.writeInt(1);
        if (!serializeWithoutName) {
            output.writeString(name);
        }
        output.writeString(customOutboundJson);
        output.writeString(customConfigJson);
    }

    @Override
    public void deserializeFromBuffer(@NonNull ByteBufferInput input) {
        deserialize(input);

        int extraVersion = input.readInt();

        name = input.readString();
        customOutboundJson = input.readString();
        customConfigJson = input.readString();
    }

    public void serialize(ByteBufferOutput output) {
        output.writeString(serverAddress);
        output.writeInt(serverPort);
    }

    public void deserialize(ByteBufferInput input) {
        serverAddress = input.readString();
        serverPort = input.readInt();
    }

    @NotNull
    @Override
    public abstract AbstractBean clone();

    // Deep-Opt: Cache the serialized bytes used for equality/hash computation.
    // During subscription deduplication, every proxy's hashCode() is called at least
    // once by LinkedHashSet.add() and again by indexOf().  Without caching each call
    // invokes KryoConverters.serialize() → full field traversal → byte array allocation.
    // For 500 proxies this means 1000+ serialize calls just for dedup.
    // The cache is invalidated (set to null) whenever serializeWithoutName changes
    // (i.e. when a mutation is signalled), and also kept transient so it is never
    // persisted to the DB or Parcel.
    private transient volatile byte[] _cachedBytes = null;

    private byte[] cachedBytes() {
        byte[] b = _cachedBytes;
        if (b == null) {
            serializeWithoutName = true;
            try {
                b = KryoConverters.serialize(this);
                _cachedBytes = b;
            } finally {
                serializeWithoutName = false;
            }
        }
        return b;
    }

    /** Call after any mutation to invalidate the equality/hash cache. */
    public void invalidateCache() {
        _cachedBytes = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(cachedBytes(), ((AbstractBean) o).cachedBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(cachedBytes());
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + JavaUtil.gson.toJson(this);
    }
}
