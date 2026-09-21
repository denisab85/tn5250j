package org.tn5250j.framework.tn5250;

import org.tn5250j.Session5250;
import org.tn5250j.SessionConfig;
import org.tn5250j.TN5250jConstants;
import org.tn5250j.encoding.CharMappings;
import org.tn5250j.encoding.ICodePage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public final class Tn5250TestHarness {

    private final ICodePage codePage = CharMappings.getCodePage("37");
    private final Session5250 session;
    private final Screen5250 screen;
    private final tnvt vt;
    private final ByteArrayOutputStream outbound = new ByteArrayOutputStream();

    private Tn5250TestHarness() {
        Properties props = new Properties();
        props.setProperty(TN5250jConstants.SESSION_HOST, "test.example.invalid");
        SessionConfig config = new SessionConfig(null, "test-session");
        session = new Session5250(props, "test-config", "test-session", config);
        screen = session.getScreen();
        vt = new tnvt(session, screen, false, true);
        attachSessionVT();
        screen.setVT(vt);
        attachOutboundCapture();
        attachStructuredFieldParser();
    }

    static Tn5250TestHarness create() {
        return new Tn5250TestHarness();
    }

    Screen5250 screen() {
        return screen;
    }

    tnvt vt() {
        return vt;
    }

    Session5250 session() {
        return session;
    }

    byte[] outboundBytes() {
        return outbound.toByteArray();
    }

    void clearOutbound() {
        outbound.reset();
    }

    int outboundLength() {
        return outbound.size();
    }

    int outboundByte(int offset) {
        return outboundBytes()[offset] & 0xff;
    }

    void unlockKeyboard() {
        screen.getOIA().setKeyBoardLocked(false);
    }

    void markConnected() {
        try {
            Field connected = tnvt.class.getDeclaredField("connected");
            connected.setAccessible(true);
            connected.set(vt, true);
        } catch (Exception e) {
            throw new AssertionError("Unable to mark VT connected", e);
        }
    }

    void process(byte[] dataStream) {
        vt.processDataStream(dataStream);
    }

    byte ebcdic(char c) {
        return (byte) codePage.uni2ebcdic(c);
    }

    byte[] gds(int opcode, byte... data) {
        int length = data.length + 10;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(length >> 8);
        stream.write(length & 0xff);
        stream.write(0x12);
        stream.write(0xa0);
        stream.write(0);
        stream.write(0);
        stream.write(4);
        stream.write(0);
        stream.write(0);
        stream.write(opcode);
        stream.write(data, 0, data.length);
        stream.write(0xff);
        stream.write(0xef);
        return stream.toByteArray();
    }

    byte[] outputOnly(byte... commands) {
        return gds(2, commands);
    }

    byte[] putGet(byte... commands) {
        return gds(3, commands);
    }

    byte[] invite(byte... commands) {
        return gds(1, commands);
    }

    byte[] messageLightOn() {
        return gds(11);
    }

    byte[] wtdText(int row, int col, String text) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(TN5250jConstants.CMD_WRITE_TO_DISPLAY);
        data.write(0);
        data.write(0);
        data.write(0x11);
        data.write(row);
        data.write(col);
        for (int i = 0; i < text.length(); i++) {
            data.write(ebcdic(text.charAt(i)));
        }
        return data.toByteArray();
    }

    byte[] wtdInputField(int row, int col, int length) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(TN5250jConstants.CMD_WRITE_TO_DISPLAY);
        data.write(0);
        data.write(0);
        data.write(inputFieldOrders(row, col, length), 0, 9);
        return data.toByteArray();
    }

    byte[] inputFieldOrders(int row, int col, int length) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(0x11);
        data.write(row);
        data.write(col);
        data.write(0x1d);
        data.write(0x40);
        data.write(0);
        data.write(0x20);
        data.write(length >> 8);
        data.write(length & 0xff);
        return data.toByteArray();
    }

    byte[] sohWithDataIncludedForPfKeys(int pf17ToPf24, int pf9ToPf16, int pf1ToPf8) {
        return new byte[]{
                0x01,
                0x07,
                0x00,
                0x00,
                0x00,
                0x18,
                (byte) pf17ToPf24,
                (byte) pf9ToPf16,
                (byte) pf1ToPf8
        };
    }

    byte[] wtdCreateWindow(int row, int col, int depth, int width) {
        return concat(
                new byte[]{
                        TN5250jConstants.CMD_WRITE_TO_DISPLAY,
                        0,
                        0,
                        0x11,
                        (byte) row,
                        (byte) col,
                        0x15
                },
                wtdsfCreateWindowSegment(depth, width));
    }

    int guiAt(int row, int col) {
        return screen.planes.getWhichGUI(screen.getPos(row - 1, col - 1));
    }

    byte[] concat(byte[]... chunks) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (byte[] chunk : chunks) {
            data.write(chunk, 0, chunk.length);
        }
        return data.toByteArray();
    }

    String screenTextAt(int row, int col, int length) {
        int start = screen.getPos(row - 1, col - 1);
        return new String(screen.getScreenAsChars(), start, length);
    }

    int lastOutboundOpcode() {
        byte[] bytes = outboundBytes();
        return bytes[9] & 0xff;
    }

    int lastOutboundFlags() {
        byte[] bytes = outboundBytes();
        return bytes[7] & 0xff;
    }

    int lastOutboundAid() {
        byte[] bytes = outboundBytes();
        return bytes[12] & 0xff;
    }

    private void attachOutboundCapture() {
        try {
            Field bout = tnvt.class.getDeclaredField("bout");
            bout.setAccessible(true);
            bout.set(vt, new FlushingOutputStream(outbound));
        } catch (Exception e) {
            throw new AssertionError("Unable to attach outbound capture", e);
        }
    }

    private void attachSessionVT() {
        try {
            Field sessionVT = Session5250.class.getDeclaredField("vt");
            sessionVT.setAccessible(true);
            sessionVT.set(session, vt);
        } catch (Exception e) {
            throw new AssertionError("Unable to attach session VT", e);
        }
    }

    private void attachStructuredFieldParser() {
        try {
            Field sfParser = tnvt.class.getDeclaredField("sfParser");
            sfParser.setAccessible(true);
            sfParser.set(vt, new WTDSFParser(vt));
        } catch (Exception e) {
            throw new AssertionError("Unable to attach structured field parser", e);
        }
    }

    private byte[] wtdsfCreateWindowSegment(int depth, int width) {
        return new byte[]{
                0x00,
                0x09,
                (byte) 0xd9,
                0x51,
                (byte) 0x80,
                0x00,
                0x00,
                (byte) depth,
                (byte) width
        };
    }

    private static final class FlushingOutputStream extends java.io.BufferedOutputStream {
        FlushingOutputStream(ByteArrayOutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
