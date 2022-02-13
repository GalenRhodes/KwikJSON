package com.galenrhodes.kwikjson;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private List<Object> _parseList() throws IOException {
        char ch = getNextToken();
        if(ch != '[') throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
        ch = peekNextToken();
        if(ch == ']') return discardNext(Collections.emptyList());

        List<Object> list = new ArrayList<>();
        do {
            Object obj = getObject();
            list.add(obj);
            ch = peekNextToken();
            if(ch == ']') return discardNext(list);
            if(ch != ',') throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), (char)ch);
        }
        while(true);
    }

    private Map<String, Object> _parseMap() throws IOException {
        return Collections.emptyMap();
    }

    private <T> T discardNext(T obj) throws IOException {
        return discardNext(obj, false);
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T discardNext(T obj, boolean optional) throws IOException {
        getNextChar(optional);
        return obj;
    }

    private String getChars(int cc) throws IOException {
        char[] hex = new char[cc];
        for(int i = 0; i < cc; i++) hex[i] = getNextChar();
        return String.valueOf(hex);
    }

    private char getHexChar() throws IOException {
        String h = getChars(4);
        if(!Pattern.compile("[0-9a-fA-F]{4}").matcher(h).find()) throw new KwikJSONException(msgs.getString("mgs.err.invalid_hex_seq"), h);
        return (char)Integer.parseInt(h, 16);
    }

    private char getNextChar() throws IOException {
        return (char)getNextChar(false);
    }

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

    private Object getObject() throws IOException {
        char ch = peekNextToken(); //@f:0
        switch(ch) {
            case '[': return _parseList();
            case '{': return _parseMap();
            case '"': return parseString();
            case 't': return parseKeyword("true", true);
            case 'f': return parseKeyword("false", false);
            default:  return parseNumber(ch);
        }
        //@f:1
    }

    private boolean isTermChar(char ch) {
        return ((ch == ',') || (ch == ']') || (ch == '}') || Character.isWhitespace(ch));
    }

    private boolean notDigit(char ch) {
        return ((ch < '0') || (ch > '9'));
    }

    private boolean notTermChar(char ch) {
        return !isTermChar(ch);
    }

    private Object parse() throws IOException {
        int ch = peekNextToken(true);
        if(ch < 0) return null;
        if(ch == '{') return _parseMap();
        if(ch == '[') return _parseList();
        throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), (char)ch);
    }

    private List<Object> parseAssumedList() throws IOException {
        int ch = peekNextToken(true);
        if(ch < 0) return Collections.emptyList();
        if(ch == '{') return Collections.singletonList(_parseMap());
        if(ch == '[') return _parseList();
        throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), (char)ch);
    }

    private Map<String, Object> parseAssumedMap() throws IOException {
        int ch = peekNextToken(true);
        if(ch < 0) return Collections.emptyMap();
        if(ch == '{') return _parseMap();
        if(ch == '[') return Collections.singletonMap("root", _parseList());
        throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), (char)ch);
    }

    private Object parseKeyword(String exemplar, Object value) throws IOException {
        String str = getChars(exemplar.length());
        if(exemplar.equals(str)) return value;
        throw reportBadChar(str, exemplar);
    }

    private Number parseNumber(char ch) throws IOException {
        if((ch != '-') && notDigit(ch)) throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
        StringBuilder sb = new StringBuilder();
        boolean       fp = false;
        sb.append(ch);

        if(ch == '-') {
            if(notDigit(ch = getNextChar())) throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
            sb.append(ch);
        }

        if(ch == '0') {
            if((ch = getNextChar()) != '.') return pushChar(ch, BigInteger.ZERO);
            fp = true;
            sb.append(ch);
            if(notDigit(ch = getNextChar())) throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
            sb.append(ch);
        }

        ch = getDigits(sb);
        if(ch == '.') {
            if(fp)  throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
            fp = true;
            ch = getAtLeast1Digit(sb);
        }

        if(ch == 'e' || ch == 'E') {
            sb.append('e');
            ch = getNextChar();
            if((ch != '+') && (ch != '-') && notDigit(ch)) throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
            ch = getDigits(sb);
        }
        pushChar(ch);

        try {
            return fp ? new BigDecimal(sb.toString()) : new BigInteger(sb.toString());
        }
        catch(Exception e) {
            throw new KwikJSONException("Malformed number: %s", sb.toString());
        }
    }

    private char getAtLeast1Digit(StringBuilder sb) throws IOException {
        char ch = getNextChar();
        if(notDigit(ch)) throw new KwikJSONException(msgs.getString("msg.err.unexpected_char"), ch);
        sb.append(ch);
        return getDigits(sb);
    }

    private char getDigits(StringBuilder sb) throws IOException {
        char ch = getNextChar();
        while(isDigit(ch)) {
            sb.append(ch);
            ch = getNextChar();
        }
        return ch;
    }

    private boolean isDigit(char ch) {
        return ((ch >= '0') && (ch <= '9'));
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

    private int peekNextToken(boolean optional) throws IOException {
        int ch = getNextToken(optional);
        if(ch >= 0 && ch <= 0xffff) pushChar((char)ch);
        return ch;
    }

    private char peekNextToken() throws IOException {
        return (char)peekNextToken(false);
    }

    private void pushChar(char ch) throws KwikJSONException {
        if(bPtr == 0) throw new KwikJSONException(msgs.getString("msg.err.push_back_failed"));
        buffer[--bPtr] = ch;
    }

    private <T> T pushChar(char ch, T obj) throws KwikJSONException {
        pushChar(ch);
        return obj;
    }

    private IOException reportBadChar(String sample, String exemplar) {
        for(int i = 0, j = Math.min(sample.length(), exemplar.length()); i < j; i++) {
            if(sample.charAt(i) != exemplar.charAt(i)) return new KwikJSONException(msgs.getString("msg.err.unexpected_char"), sample.charAt(i));
        }
        if(sample.length() < exemplar.length()) return new KwikJSONException("Too few characters. Expected \"%s\", but only got \"%s\".", exemplar, sample);
        if(sample.length() > exemplar.length()) return new KwikJSONException("Too many characters. Expected only \"%s\", but got \"%s\".", exemplar, sample);
        return new KwikJSONException("Internal inconsistency.");
    }

    public static Object parseJSON(Reader reader) throws IOException {
        return new KJSON(reader).parse();
    }

    public static List<Object> parseJSONList(Reader reader) throws IOException {
        return new KJSON(reader).parseAssumedList();
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
        return new KJSON(reader).parseAssumedMap();
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
