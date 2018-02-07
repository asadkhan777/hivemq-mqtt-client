package org.mqttbee.mqtt3.codec.decoder;

import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mqttbee.annotations.Nullable;
import org.mqttbee.mqtt3.message.Mqtt3MessageType;
import org.mqttbee.mqtt3.message.pubrec.Mqtt3PubRecImpl;

import static org.junit.jupiter.api.Assertions.*;

class Mqtt3PubRecDecoderTest {


    private static final byte[] WELLFORMED_PUBREC_BEGIN = {
            //   type, flags
            0b0101_0000,
            //remaining length
            0b0000_0010
    };
    private static final byte[] MALFORMED_PUBREC_BEGIN_WRONG_FLAGS = {
            //   type, flags
            0b0101_0100,
            //remaining length
            0b0000_0010
    };

    private static final byte[] MALFORMED_PUBREC_BEGIN_TOO_LONG_LENGTH = {
            //   type, flags
            0b0101_0100,
            //remaining length
            0b0000_0001
    };
    private static final byte[] ENDING_TOO_LONG_MALFORMED = {0x01};
    private static final byte[] MAX_PACKET_ID = {(byte) 0b1111_1111, (byte) 0b1111_1111};
    private static final byte[] MIN_PACKET_ID = {0x00, 0x00};
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new Mqtt3Decoder(new Mqtt3PubRecTestMessageDecoders()));
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.close();
    }


    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void decode_SUCESS(boolean useMaxPacketId) {

        final byte[] encoded = Bytes.concat(WELLFORMED_PUBREC_BEGIN, useMaxPacketId ? MAX_PACKET_ID : MIN_PACKET_ID);
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);
        final Mqtt3PubRecImpl pubRec = channel.readInbound();
        assertNotNull(pubRec);
        assertEquals(useMaxPacketId ? 65535 : 0, pubRec.getPacketId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2"})
    void decode_ERROR_CASES(int errorcase) throws Exception {
        final byte[] encoded;
        switch (errorcase) {
            case 1:
                encoded = Bytes.concat(MALFORMED_PUBREC_BEGIN_WRONG_FLAGS, MAX_PACKET_ID);
                break;
            case 2:
                encoded =
                        Bytes.concat(MALFORMED_PUBREC_BEGIN_TOO_LONG_LENGTH, MIN_PACKET_ID, ENDING_TOO_LONG_MALFORMED);
                break;
            default:
                throw new Exception();
        }

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);
        final Mqtt3PubRecImpl pubRec = channel.readInbound();
        assertFalse(channel.isOpen());
        assertNull(pubRec);
    }

    private static class Mqtt3PubRecTestMessageDecoders implements Mqtt3MessageDecoders {
        @Nullable
        @Override
        public Mqtt3MessageDecoder get(final int code) {
            if (code == Mqtt3MessageType.PUBREC.getCode()) {
                return new Mqtt3PubRecDecoder();
            }
            return null;
        }
    }

}