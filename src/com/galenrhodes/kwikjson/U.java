package com.galenrhodes.kwikjson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class U {
    private U() { }

    public static Charset getCharset(URLConnection uconn) {
        try { return Charset.forName(uconn.getContentEncoding()); }
        catch(Exception e) { return StandardCharsets.UTF_8; }
    }

    public static byte[] readStream(InputStream inputStream) throws IOException {
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[65536];
            int    cc     = inputStream.read(buffer);

            while(cc >= 0) {
                if(cc > 0) outputStream.write(buffer, 0, cc);
                cc = inputStream.read(buffer);
            }

            inputStream.close();
            outputStream.flush();
            return outputStream.toByteArray();
        }
    }
}
