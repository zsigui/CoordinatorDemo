package com.luna.powersaver.gp.utils.chiper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Zlib数据压缩工具类
 * Created by zsigui on 17-1-17.
 */

public class Zlib {

    private static final int BUFFER_LENGTH = 4024;

    public static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bs = new byte[BUFFER_LENGTH];
        int len;
        while (!deflater.finished()) {
            len = deflater.deflate(bs);
            baos.write(bs, 0, len);
        }
        deflater.end();
        byte[] result = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void compress(byte[] data, OutputStream os) {
        try {
            DeflaterOutputStream dos = new DeflaterOutputStream(os);
            dos.write(data, 0, data.length);
            dos.finish();
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] decompress(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.reset();
        inflater.setInput(data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        try {
            byte[] bs = new byte[BUFFER_LENGTH];
            int len;
            while (!inflater.finished()) {
                len = inflater.inflate(bs);
                baos.write(bs, 0, len);
            }
            inflater.end();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        byte[] result = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] decompress(InputStream is) {
        try {
            InflaterInputStream iis = new InflaterInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bs = new byte[BUFFER_LENGTH];
            int len = -1;
            while ((len = iis.read(bs)) != -1) {
                baos.write(bs, 0, len);
            }
            iis.close();
            is.close();
            byte[] result = baos.toByteArray();
            baos.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
