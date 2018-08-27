package app.intra.util;

import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class DummyDnsPacket {
    public static final int HEADER_SIZE = 12;
    public static final int QUESTION_MIN_SIZE = 5;
    public static final int RESPONSE_SIZE = 16;
    public static byte[] generate(String url) throws ProtocolException {
        String[] pieces = url.toLowerCase()
                .split("\\x2E");
        int nameSize = 0;
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].length() == 0)
                continue;
            nameSize += 1 + pieces[i].length();
        }
        byte[] bytes = new byte[HEADER_SIZE
                + QUESTION_MIN_SIZE + nameSize
                + RESPONSE_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putShort((short)0);
        buffer.put((byte)-127); // type response
        buffer.put((byte)-128); //
        buffer.putShort((short)1); // questions
        buffer.putShort((short)1); // answers
        buffer.putShort((short)0); // authorities
        buffer.putShort((short)0); // additional

        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].length() == 0)
                continue;
            if (pieces[i].length() > 255)
                throw new ProtocolException("Invalid url length");
            char[] characters = pieces[i].toCharArray();
            if (pieces[i].length() != characters.length)
                throw new ProtocolException("Umm, something weird with the characters, encoding?");
            buffer.put((byte)characters.length);
            for (int j = 0; j < characters.length; j++) {
                int c = (int)characters[j];
                if (c < 0 || c > 255)
                    throw new ProtocolException("Invalid character in url");
                buffer.put((byte)c);
            }
        }
        buffer.put((byte)0);
        buffer.putShort((short)1);
        buffer.putShort((short)1);
        buffer.put((byte)-64); // the first 2 bits need to be set to 1
        buffer.put((byte)12); // the offset is always 12
        buffer.putShort((short)1);
        buffer.putShort((short)1);
        buffer.putInt(60); // ttl 60s, just in case we decide to unblock it
        buffer.putShort((short)4); //ip size
        // Now the ipv4 address
        buffer.put((byte)127);
        buffer.put((byte)0);
        buffer.put((byte)0);
        buffer.put((byte)1);
        return bytes;
    }
}
