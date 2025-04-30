package io.dropwizard.metrics5.collectd;
 
 import javax.crypto.BadPaddingException;
 import javax.crypto.Cipher;
 import javax.crypto.IllegalBlockSizeException;
 import javax.crypto.Mac;
 import javax.crypto.NoSuchPaddingException;
 import javax.crypto.ShortBufferException;
 import javax.crypto.spec.IvParameterSpec;
 import javax.crypto.spec.SecretKeySpec;
 import java.io.IOException;
 import java.nio.BufferOverflowException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.charset.StandardCharsets;
 import java.security.InvalidKeyException;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.security.spec.InvalidParameterSpecException;
 import java.util.Arrays;
 
 class PacketWriter {
 
     private static final int TYPE_HOST = 0;
     private static final int TYPE_TIME = 1;
     private static final int TYPE_PLUGIN = 2;
     private static final int TYPE_PLUGIN_INSTANCE = 3;
     private static final int TYPE_TYPE = 4;
     private static final int TYPE_TYPE_INSTANCE = 5;
     private static final int TYPE_VALUES = 6;
     private static final int TYPE_INTERVAL = 7;
     private static final int TYPE_SIGN_SHA256 = 0x0200;
     private static final int TYPE_ENCR_AES256 = 0x0210;
 
     private static final int UINT16_LEN = 2;
     private static final int UINT32_LEN = UINT16_LEN * 2;
     private static final int UINT64_LEN = UINT32_LEN * 2;
     private static final int HEADER_LEN = UINT16_LEN * 2;
     private static final int BUFFER_SIZE = 1024;
 
     private static final int VALUE_COUNT_LEN = UINT16_LEN;
     private static final int NUMBER_LEN = HEADER_LEN + UINT64_LEN;
     private static final int SIGNATURE_LEN = 36;      // 2b Type + 2b Length + 32b Hash
     private static final int ENCRYPT_DATA_LEN = 22;   // 16b IV + 2b Type + 2b Length + 2b Username length
     private static final int IV_LENGTH = 16;
     private static final int SHA1_LENGTH = 20;
 
     private static final int VALUE_LEN = 9;
     private static final byte DATA_TYPE_GAUGE = (byte) 1;
     private static final byte NULL = (byte) '\0';
     private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
     private static final String AES_CYPHER = "AES_256/OFB/NoPadding";
     private static final String AES = "AES";
     private static final String SHA_256_ALGORITHM = "SHA-256";
     private static final String SHA_1_ALGORITHM = "SHA1";
 
     private final Sender sender;
 
     private final SecurityLevel securityLevel;
     private final byte[] username;
     private final byte[] password;
 
     PacketWriter(Sender sender, String username, String password, SecurityLevel securityLevel) {
         this.sender = sender;
         this.securityLevel = securityLevel;
         this.username = username != null ? username.getBytes(StandardCharsets.UTF_8) : null;
         this.password = password != null ? password.getBytes(StandardCharsets.UTF_8) : null;
     }
 
     void write(MetaData metaData, Number... values) throws BufferOverflowException, IOException {
         final ByteBuffer packet = ByteBuffer.allocate(BUFFER_SIZE);
         write(packet, metaData);
         write(packet, values);
         packet.flip();
 
         switch (securityLevel) {
             case NONE:
                 sender.send(packet);
                 break;
             case SIGN:
                 sender.send(signPacket(packet));
                 break;
             case ENCRYPT:
                 sender.send(encryptPacket(packet));
                 break;
             default:
                 throw new IllegalArgumentException("Unsupported security level: " + securityLevel);
         }
     }
 
 
     private void write(ByteBuffer buffer, MetaData metaData) {
         writeString(buffer, TYPE_HOST, metaData.getHost());
         writeNumber(buffer, TYPE_TIME, metaData.getTimestamp());
         writeString(buffer, TYPE_PLUGIN, metaData.getPlugin());
         writeString(buffer, TYPE_PLUGIN_INSTANCE, metaData.getPluginInstance());
         writeString(buffer, TYPE_TYPE, metaData.getType());
         writeString(buffer, TYPE_TYPE_INSTANCE, metaData.getTypeInstance());
         writeNumber(buffer, TYPE_INTERVAL, metaData.getPeriod());
     }
 
     private void write(ByteBuffer buffer, Number... values) {
         final int numValues = values.length;
         final int length = HEADER_LEN + VALUE_COUNT_LEN + numValues * VALUE_LEN;
         writeHeader(buffer, TYPE_VALUES, length);
         buffer.putShort((short) numValues);
         buffer.put(nCopies(numValues, DATA_TYPE_GAUGE));
         buffer.order(ByteOrder.LITTLE_ENDIAN);
         for (Number value : values) {
             buffer.putDouble(value.doubleValue());
         }
         buffer.order(ByteOrder.BIG_ENDIAN);
     }
 
     private byte[] nCopies(int n, byte value) {
         final byte[] array = new byte[n];
         Arrays.fill(array, value);
         return array;
     }
 
     private void writeString(ByteBuffer buffer, int type, String val) {
         if (val == null || val.length() == 0) {
             return;
         }
         int len = HEADER_LEN + val.length() + 1;
         writeHeader(buffer, type, len);
         buffer.put(val.getBytes(StandardCharsets.US_ASCII)).put(NULL);
     }
 
     private void writeNumber(ByteBuffer buffer, int type, long val) {
         writeHeader(buffer, type, NUMBER_LEN);
         buffer.putLong(val);
     }
 
     private void writeHeader(ByteBuffer buffer, int type, int len) {
         buffer.putShort((short) type);
         buffer.putShort((short) len);
     }
 
 
/** A server can verify the authenticity of the packet if it is signs. */
private ByteBuffer signPacket(ByteBuffer packet) {
    try {
        // Calculate the HMAC-SHA256 signature
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(new SecretKeySpec(password, HMAC_SHA256_ALGORITHM));
        mac.update(packet.array(), packet.arrayOffset(), packet.limit());
        byte[] signature = mac.doFinal();

        // Create a new ByteBuffer for the signed packet
        ByteBuffer signedPacket = ByteBuffer.allocate(packet.limit() + SIGNATURE_LEN);
        signedPacket.put(packet);
        signedPacket.put(signature);
        signedPacket.flip();

        return signedPacket;
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
        // Handle the exception accordingly
        e.printStackTrace();
        return null;
    }
}
 

}