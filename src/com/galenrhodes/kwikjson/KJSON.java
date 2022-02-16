package com.galenrhodes.kwikjson;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class KJSON {
    public static final ResourceBundle msgs  = ResourceBundle.getBundle("com.galenrhodes.kwikjson.kwikjsonmessages");
    public static final KwikProperties props = new KwikProperties("kwikjson.properties");

    public static final char APOS           = props.getChar("p.apos");
    public static final char QUOTE          = props.getChar("p.quote");
    public static final char PERIOD         = props.getChar("p.char_period");
    public static final char PLUS           = props.getChar("p.char_plus");
    public static final char MINUS          = props.getChar("p.char_minus");
    public static final char BS             = props.getChar("p.char_bs");
    public static final char FS             = props.getChar("p.char_fs");
    public static final char CH_B           = props.getChar("p.char_b");
    public static final char CH_F           = props.getChar("p.char_f");
    public static final char CH_N           = props.getChar("p.char_n");
    public static final char CH_R           = props.getChar("p.char_r");
    public static final char CH_T           = props.getChar("p.char_t");
    public static final char CH_U           = props.getChar("p.char_u");
    public static final char UC_E           = props.getChar("p.char_uc_e");
    public static final char LC_E           = props.getChar("p.char_lc_e");
    public static final char ZERO           = props.getChar("p.char_zero");
    public static final char LIST_OPEN      = props.getChar("p.list_open");
    public static final char LIST_CLOSE     = props.getChar("p.list_close");
    public static final char MAP_OPEN       = props.getChar("p.map_open");
    public static final char MAP_CLOSE      = props.getChar("p.map_close");
    public static final char MAP_SEPARATOR  = props.getChar("p.map_kv_separator");
    public static final char LIST_SEPARATOR = props.getChar("p.list_item_separator");

    public static final String MSG_BAD_CHAR = msgs.getString("msg.err.unexpected_char");

    private final Reader reader;
    private final char[] buffer;
    private       int    bPtr;
    private       int    bTop;

    private KJSON(Reader reader) {
        this.reader = reader;
        this.buffer = new char[65536];
        this.bTop   = 0;
        this.bPtr   = 0;
    }

    private List<Object> _parseList() throws IOException {
        CharHolder ch = getNextToken();
        if(ch.not(LIST_OPEN)) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        if(getNextToken(ch).is(LIST_CLOSE)) return Collections.emptyList();
        pushChar(ch);

        List<Object> list = new ArrayList<>();
        do {
            list.add(getObject());
            if(getNextToken(ch).is(LIST_CLOSE)) return list;
            if(ch.not(LIST_SEPARATOR)) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        } while(true);
    }

    private Map<String, Object> _parseMap() throws IOException {
        CharHolder ch = getNextToken();
        if(ch.not(MAP_OPEN)) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        if(getNextToken(ch).is(MAP_CLOSE)) return Collections.emptyMap();
        pushChar(ch);

        Map<String, Object> map = new LinkedHashMap<>();
        do {
            String str = parseString();
            if(getNextToken(ch).not(MAP_SEPARATOR)) throw new KwikJSONException(MSG_BAD_CHAR, ch);
            map.put(str, getObject());
            if(getNextToken(ch).is(MAP_CLOSE)) return map;
            if(ch.not(LIST_SEPARATOR)) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        } while(true);
    }

    private void getAtLeast1Digit(StringBuilder sb, CharHolder ch) throws IOException {
        if(getNextChar(ch).notDigit()) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        ch.append(sb);
        getDigits(sb, ch);
    }

    private String getChars(int cc) throws IOException {
        char[] array = new char[cc];
        for(int i = 0; i < cc; i++) array[i] = (char)getNextChar(false);
        return String.valueOf(array);
    }

    private void getDigits(StringBuilder sb, CharHolder ch) throws IOException {
        while(getNextChar(ch).isDigit()) ch.append(sb);
    }

    private char getHexChar() throws IOException {
        String str = getChars(4);
        if(!Pattern.compile(props.getProperty("p.unicode.regexp")).matcher(str).find()) throw new KwikJSONException(msgs.getString("mgs.err.invalid_hex_seq"), str);
        return (char)Integer.parseInt(str, 16);
    }

    private CharHolder getNextChar(CharHolder ch) throws IOException {
        ch.set((char)getNextChar(false));
        return ch;
    }

    private CharHolder getNextChar() throws IOException {
        return new CharHolder((char)getNextChar(false));
    }

    private int getNextChar(boolean optional) throws IOException {
        if(bPtr == bTop) {
            bPtr = 0;
            do bTop = reader.read(buffer); while(bTop == 0);
            if(bTop < 0) {
                if(optional) return props.getInteger("p.eof");
                else throw new KwikJSONException(msgs.getString("msg.err.unexpected_eof"));
            }
        }
        return buffer[bPtr++];
    }

    private CharHolder getNextToken(CharHolder ch) throws IOException {
        ch.set((char)getNextToken(false));
        return ch;
    }

    private CharHolder getNextToken() throws IOException {
        return new CharHolder((char)getNextToken(false));
    }

    private int getNextToken(boolean optional) throws IOException {
        int i = getNextChar(optional);
        while(i >= 0 && Character.isWhitespace((char)i)) i = getNextChar(optional);
        return i;
    }

    private Object getObject() throws IOException {
        CharHolder ch = peekNextToken();
        if(ch.is(LIST_OPEN)) return _parseList();
        else if(ch.is(MAP_OPEN)) return _parseMap();
        else if(ch.is(QUOTE)) return parseString();
        else if(ch.is(CH_T)) return parseKeyword(props.getProperty("p.true"), true);
        else if(ch.is(CH_F)) return parseKeyword(props.getProperty("p.false"), false);
        else if(ch.is(CH_N)) return parseKeyword(props.getProperty("p.null"), null);
        else return parseNumber();
    }

    private Object parse() throws IOException {
        int ch = peekNextToken(true);
        if(ch < 0) return null;
        if(ch == MAP_OPEN) return _parseMap();
        if(ch == LIST_OPEN) return _parseList();
        throw new KwikJSONException(MSG_BAD_CHAR, (char)ch);
    }

    private Object parseKeyword(String exemplar, Object value) throws IOException {
        String str = getChars(exemplar.length());
        if(exemplar.equals(str)) return value;
        throw reportBadChar(str, exemplar);
    }

    private Number parseNumber() throws IOException {
        StringBuilder sb = new StringBuilder();
        BoolHolder    fp = new BoolHolder(false);
        CharHolder    ch = getNextToken();

        if(ch.not(MINUS) && ch.notDigit()) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        ch.append(sb);
        parseNumberMinusSign(sb, ch);

        BigInteger bigInteger = parseNumberLeadingZero(sb, ch, fp);
        if(bigInteger != null) return bigInteger;

        getDigits(sb, ch);
        parseNumberDecimal(sb, ch, fp);
        parseNumberExponent(sb, ch);
        pushChar(ch);
        return parseNumberConvert(sb, fp);
    }

    private Number parseNumberConvert(StringBuilder sb, BoolHolder fp) throws KwikJSONException {
        String str = sb.toString();
        try { return fp.cond(() -> new BigDecimal(str), () -> new BigInteger(str)); }
        catch(Exception e) { throw new KwikJSONException(msgs.getString("msg.err.malformed_number"), str); }
    }

    private void parseNumberDecimal(StringBuilder sb, CharHolder ch, BoolHolder fp) throws IOException {
        if(ch.is(PERIOD)) {
            if(fp.is()) throw new KwikJSONException(MSG_BAD_CHAR, ch);
            ch.append(sb);
            fp.set();
            getAtLeast1Digit(sb, ch);
        }
    }

    private void parseNumberExponent(StringBuilder sb, CharHolder ch) throws IOException {
        if(ch.is(LC_E) || ch.is(UC_E)) {
            sb.append(Character.toUpperCase(ch.get()));
            getNextChar(ch);
            if(ch.not(PLUS) && ch.not(MINUS) && ch.notDigit()) throw new KwikJSONException(MSG_BAD_CHAR, ch);
            ch.append(sb);
            getDigits(sb, ch);
        }
    }

    private BigInteger parseNumberLeadingZero(StringBuilder sb, CharHolder ch, BoolHolder fp) throws IOException {
        if(ch.is(ZERO)) {
            if(getNextChar(ch).not(PERIOD)) return pushChar(ch, BigInteger.ZERO);
            fp.set();
            ch.append(sb);
            if(getNextChar(ch).notDigit()) throw new KwikJSONException(MSG_BAD_CHAR, ch);
            ch.append(sb);
        }
        return null;
    }

    private void parseNumberMinusSign(StringBuilder sb, CharHolder ch) throws IOException {
        if(ch.is(MINUS)) {
            if(getNextChar(ch).notDigit()) throw new KwikJSONException(MSG_BAD_CHAR, ch);
            ch.append(sb);
        }
    }

    private String parseString() throws IOException {
        CharHolder ch = getNextToken();
        if(ch.not(QUOTE)) throw new KwikJSONException(MSG_BAD_CHAR, ch);
        StringBuilder sb = new StringBuilder();
        getNextChar(ch);
        while(ch.not(QUOTE)) {
            if(ch.is(BS)) {
                getNextChar(ch); //@f:0
                if(ch.is(FS))         ch.append(sb);
                else if(ch.is(CH_N))  sb.append(props.getChar("p.lf"));
                else if(ch.is(CH_R))  sb.append(props.getChar("p.cr"));
                else if(ch.is(CH_T))  sb.append(props.getChar("p.tab"));
                else if(ch.is(CH_F))  sb.append(props.getChar("p.ff"));
                else if(ch.is(CH_B))  sb.append(props.getChar("p.bs"));
                else if(ch.is(BS))    ch.append(sb);
                else if(ch.is(APOS))  ch.append(sb);
                else if(ch.is(QUOTE)) ch.append(sb);
                else if(ch.is(CH_U))  sb.append(getHexChar());
                else throw new KwikJSONException(msgs.getString("msg.err.invalid_char_esc_seq"), ch); //@f:1
            }
            else ch.append(sb);
            getNextChar(ch);
        }
        return sb.toString();
    }

    private int peekNextChar(boolean optional) throws IOException {
        int i = getNextChar(optional);
        if(i >= 0) bPtr--;
        return i;
    }

    private int peekNextToken(boolean optional) throws IOException {
        int i = getNextToken(optional);
        if(i >= 0) bPtr--;
        return i;
    }

    private CharHolder peekNextToken() throws IOException {
        return new CharHolder((char)peekNextToken(false));
    }

    private void pushChar(char ch) throws KwikJSONException {
        if(bPtr == 0) throw new KwikJSONException(msgs.getString("msg.err.push_back_failed"));
        buffer[--bPtr] = ch;
    }

    private void pushChar(CharHolder ch) throws KwikJSONException {
        pushChar(ch.get());
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T pushChar(CharHolder ch, T obj) throws KwikJSONException {
        pushChar(ch);
        return obj;
    }

    private IOException reportBadChar(String sample, String exemplar) {
        for(int i = 0, j = Math.min(sample.length(), exemplar.length()); i < j; i++) {
            if(sample.charAt(i) != exemplar.charAt(i)) return new KwikJSONException(MSG_BAD_CHAR, sample.charAt(i));
        }
        if(sample.length() < exemplar.length()) return new KwikJSONException(msgs.getString("msg.err.too_few_chars"), exemplar, sample);
        if(sample.length() > exemplar.length()) return new KwikJSONException(msgs.getString("msg.err.too_many_chars"), exemplar, sample);
        return new KwikJSONException(msgs.getString("msg.err.internal_inconsistency"));
    }

    public static Object parseJSON(Reader reader) throws IOException {
        return new KJSON(reader).parse();
    }

    public static Object parseJSON(String string) throws IOException {
        return parseJSON(new StringReader(string));
    }

    public static Object parseJSON(InputStream inputStream, Charset cs) throws IOException {
        return parseJSON(new InputStreamReader(inputStream, cs));
    }

    public static Object parseJSON(InputStream inputStream) throws IOException {
        return parseJSON(inputStream, StandardCharsets.UTF_8);
    }

    public static Object parseJSON(byte[] bytes, Charset cs) throws IOException {
        return parseJSON(new ByteArrayInputStream(bytes), cs);
    }

    public static Object parseJSON(byte[] bytes) throws IOException {
        return parseJSON(bytes, StandardCharsets.UTF_8);
    }

    public static Object parseJSON(URL url) throws IOException {
        if(props.getProperty("p.http.http").equals(url.getProtocol()) || props.getProperty("p.http.https").equals(url.getProtocol())) {
            HttpURLConnection uconn = (HttpURLConnection)url.openConnection();
            uconn.setRequestMethod(props.getProperty("p.http.method"));
            uconn.setRequestProperty(props.getProperty("p.http.accept.key"), props.getProperty("p.http.accept.value"));
            InputStream inputStream = uconn.getInputStream();
            if(inputStream != null) return parseFromURLConnection(uconn, inputStream);
            return parseFromURLConnection(uconn, uconn.getErrorStream());
        }
        else {
            URLConnection uconn = url.openConnection();
            return parseFromURLConnection(uconn, uconn.getInputStream());
        }
    }

    private static Object parseFromURLConnection(URLConnection uconn, InputStream inputStream) throws IOException {
        if(inputStream == null) throw new KwikJSONException(msgs.getString("msg.err.unexpected_eof"));
        Charset cs = U.getCharset(uconn);
        String  ct = props.getProperty("p.http.content_type.json");
        return ct.equals(uconn.getContentType()) ? parseJSON(inputStream, cs) : Collections.singletonList(new String(U.readStream(inputStream), cs));
    }
}
