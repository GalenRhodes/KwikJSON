package com.galenrhodes.kwikjson;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class KJSON {
    public static final int EOF = -1;

    private final ResourceBundle msgs = ResourceBundle.getBundle("/com/galenrhodes/kwikjson/kwikjsonmessages.properties");
    private final Reader         reader;
    private final char[]         buffer;
    private       int            bPtr;
    private       int            bTop;

    private KJSON(Reader reader) {
        this.reader = reader;
        this.buffer = new char[65536];
        this.bTop   = 0;
        this.bPtr   = 0;
    }

    private char getNextChar() throws IOException { return (char)getNextChar(false); }

    private int getNextChar(boolean optional) throws IOException {
        if(bPtr == bTop) {
            bPtr = 0;
            do bTop = reader.read(); while(bTop == 0);
            if(bTop < 0) {
                if(optional) return EOF;
                else throw new KwikJSONException(msgs.getString("msg.err.unexpected_eof"));
            }
        }
        return buffer[bPtr++];
    }

    private char getNextToken() throws IOException {
        return (char)getNextToken(false);
    }

    private int getNextToken(boolean optional) throws IOException {
        int ch = getNextChar(optional);
        while(ch >= 0 && ch <= 0xffff && Character.isWhitespace((char)ch)) ch = getNextChar(optional);
        return ch;
    }

    private Object parse() throws IOException {
        int ch = getNextChar(true);
        if(ch < 0) return null;
        pushChar((char)ch);
        if(ch == '{') return parseMap();
        if(ch == '[') return parseList();
        throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), (char)ch);
    }

    private List<Object> parseList() throws IOException {
        return Collections.emptyList();
    }

    private Map<String, Object> parseMap() throws IOException {
        return Collections.emptyMap();
    }

    private String parseString() throws IOException {
        char ch = getNextChar();
        if(ch != '"') throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
        StringBuilder sb = new StringBuilder();
        sb.append(ch);
        do {
            ch = getNextChar();
            if(ch == '\\') {
                ch = getNextChar();
                //@f:0
                switch(ch) {
                    case '/':  sb.append('/');             break;
                    case 'n':  sb.append('\n');            break;
                    case 'r':  sb.append('\r');            break;
                    case 't':  sb.append('\t');            break;
                    case 'f':  sb.append('\f');            break;
                    case 'b':  sb.append('\b');            break;
                    case '\\': sb.append('\\');            break;
                    case '\'': sb.append('\'');            break;
                    case '\"': sb.append('\"'); ch = '\\'; break;
                    case 'u':  sb.append(getHexChar());    break;
                    default: throw new KwikJSONException(msgs.getString("msg.err.invalid_char_esc_seq"), ch);
                }
                //@f:1
            }
            else {
                sb.append(ch);
            }
        }
        while(ch != '"');
        return sb.toString();
    }

    private char getHexChar() throws IOException {
        char[] hex = new char[4];
        for(int i = 0; i < 4; i++) hex[i] = getNextChar();
        String h = String.valueOf(hex);
        if(!Pattern.compile("[0-9a-fA-F]{4}").matcher(h).find()) throw new KwikJSONException(msgs.getString("mgs.err.invalid_hex_seq"), h);
        return (char)Integer.parseInt(h, 16);
    }

    private int peekNextToken(boolean optional) throws IOException {
        int ch = getNextToken(optional);
        if(ch >= 0 && ch <= 0xffff) pushChar((char)ch);
        return ch;
    }

    private int peekNextToken() throws IOException {
        return peekNextToken(false);
    }

    private void pushChar(char ch) throws KwikJSONException {
        if(bPtr == 0) throw new KwikJSONException(msgs.getString("msg.err.push_back_failed"));
        buffer[--bPtr] = ch;
    }

    public static Object parseJSON(Reader reader) throws IOException {
        return new KJSON(reader).parse();
    }

    public static List<Object> parseJSONList(Reader reader) throws IOException {
        return new KJSON(reader).parseList();
    }

    public static List<Object> parseJSONList(InputStream inputStream, Charset cs) throws IOException {
        return parseJSONList(new InputStreamReader(inputStream, cs));
    }

    public static List<Object> parseJSONList(InputStream inputStream) throws IOException {
        return parseJSONList(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public static List<Object> parseJSONList(String string) throws IOException {
        return parseJSONList(new StringReader(string));
    }

    public static Map<String, Object> parseJSONMap(Reader reader) throws IOException {
        return new KJSON(reader).parseMap();
    }

    public static Map<String, Object> parseJSONMap(InputStream inputStream, Charset cs) throws IOException {
        return parseJSONMap(new InputStreamReader(inputStream, cs));
    }

    public static Map<String, Object> parseJSONMap(InputStream inputStream) throws IOException {
        return parseJSONMap(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public static Map<String, Object> parseJSONMap(String string) throws IOException {
        return parseJSONMap(new StringReader(string));
    }
}
