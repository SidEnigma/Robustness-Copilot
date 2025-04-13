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
 
     /**
      * Signs the provided packet, so a CollectD server can verify that its authenticity.
      * Wire format:
      * <pre>
      * +-------------------------------+-------------------------------+
      * ! Type (0x0200)                 ! Length                        !
      * +-------------------------------+-------------------------------+
      * ! Signature (SHA2(username + packet))                           \
      * +-------------------------------+-------------------------------+
      * ! Username                      ! Packet                        \
      * +---------------------------------------------------------------+
      * </pre>
      *
      * @see <a href="https://collectd.org/wiki/index.php/Binary_protocol#Signature_part">
      * Binary protocol - CollectD | Signature part</a>
      */
     private ByteBuffer signPacket(ByteBuffer packet) {
         final byte[] signature = sign(password, (ByteBuffer) ByteBuffer.allocate(packet.remaining() + username.length)
                 .put(username)
                 .put(packet)
                 .flip());
         return (ByteBuffer) ByteBuffer.allocate(BUFFER_SIZE)
                 .putShort((short) TYPE_SIGN_SHA256)
                 .putShort((short) (username.length + SIGNATURE_LEN))
                 .put(signature)
                 .put(username)
                 .put((ByteBuffer) packet.flip())
                 .flip();
     }
 
 
/** Encrypts the supplied packet, so that it cannot be listened to during a transfer to a CollectD server. */

private ByteBuffer encryptPacket(ByteBuffer packet) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException, ShortBufferException {
    // Generate a random IV (Initialization Vector)
    byte[] iv = new byte[IV_LENGTH];
    SecureRandom random = new SecureRandom();
    random.nextBytes(iv);

    // Create the AES cipher with the specified mode and padding
    Cipher cipher = Cipher.getInstance(AES_CYPHER);
    SecretKeySpec secretKeySpec = new SecretKeySpec(password, AES);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

    // Encrypt the packet
    byte[] encryptedData = cipher.doFinal(packet.array());

    // Calculate the SHA-1 hash of the encrypted data
    MessageDigest sha1Digest = MessageDigest.getInstance(SHA_1_ALGORITHM);
    byte[] sha1Hash = sha1Digest.digest(encryptedData);

    // Create the encrypted packet
    ByteBuffer encryptedPacket = ByteBuffer.allocate(BUFFER_SIZE);
    encryptedPacket.putShort((short) TYPE_ENCR_AES256);
    encryptedPacket.putShort((short) (ENCRYPT_DATA_LEN + encryptedData.length));
    encryptedPacket.put(iv);
    encryptedPacket.put(sha1Hash);
    encryptedPacket.put(encryptedData);
    encryptedPacket.flip();

    return encryptedPacket;
}
 

}