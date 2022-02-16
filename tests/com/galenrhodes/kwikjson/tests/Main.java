package com.galenrhodes.kwikjson.tests;

import com.galenrhodes.kwikjson.KJSON;
import com.galenrhodes.kwikjson.KwikProperties;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings({ "unchecked", "SameParameterValue" })
public class Main {
    public static final ResourceBundle msgs  = ResourceBundle.getBundle("kwikjsontestmessages");
    public static final KwikProperties props = new KwikProperties("/kwikjsontest.properties");
    public static final String         F8    = props.getProperty("p.f8");
    public static final String         F5    = props.getProperty("p.f5");

    public static final String NULL  = msgs.getString("msg.null");
    public static final char   LF    = props.getChar("p.lf");
    public static final char   SPACE = props.getChar("p.f1");
    public static final char   QUOTE = props.getChar("p.quote");

    public static void main(String... args) {
        try {
            try(InputStream inputStream = Main.class.getResourceAsStream(props.getProperty("p.test_file"))) {
                Object o = KJSON.parseJSON(inputStream, StandardCharsets.UTF_8);
                printf(props.getProperty("p.f6"), msgs.getString("msg.root_type"), ((o == null) ? NULL : o.getClass().getName()));
                print(1, o);
            }
            System.exit(0);
        }
        catch(Exception e) {
            System.err.println(msgs.getString("msg.err"));
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static void print(int level, Object o) {
        //@f:0
        if(level <= 0)                   print(1, o);
        else if(o == null)               printf(props.getProperty("p.f2"), NULL);
        else if(o instanceof Map)        printMap(level, (Map<String, Object>)o);
        else if(o instanceof List)       printList(level, (List<Object>)o);
        else if(o instanceof BigDecimal) printf(props.getProperty("p.f6"), o.getClass().getName(), String.format(props.getProperty("p.f7"), o));
        else if(o instanceof String)     printf(props.getProperty("p.f6"), o.getClass().getName(), quote(o));
        else                             printf(props.getProperty("p.f6"), o.getClass().getName(), o);
        //@f:1
    }

    private static Object[] append(Object o, Object... args) {
        Object[] _args = new Object[args.length + 1];
        System.arraycopy(args, 0, _args, 0, args.length);
        _args[args.length] = o;
        return _args;
    }

    private static String buildIndent(int level) {
        return stringOf(new char[level * 4], SPACE);
    }

    private static int getLength(Collection<String> strings) {
        int kl = 0;
        for(String s : strings) kl = Math.max(kl, s.length());
        return kl;
    }

    private static <T> void print(int level, String msgKey, FooBar1<T> fb1, FooBar2<T> fb2, FooBar3 fb3, Collection<T> c) {
        printf(props.getProperty(level > 1 ? "p.f3" : "p.f4"), msgs.getString(msgKey), buildIndent(level));
        if(c.isEmpty()) printf(props.getProperty("p.f1"), msgs.getString("msg.empty"));
        else print(level, fb3.getFormat(), fb1, fb2, c);
    }

    private static <T> void print(int level, String format, FooBar1<T> fb1, FooBar2<T> fb2, Collection<T> c) {
        System.out.print(LF);
        for(T o : c) {
            printf(format, fb2.getArgs(o));
            print(level + 1, fb1.getObject(o));
        }
    }

    private static void printList(final int level, List<Object> list) {
        print(level, "msg.list", new FooBar1<Object>() {
                  @Override
                  public Object getObject(Object o) {
                      return o;
                  }
              },
              new FooBar2<Object>() {
                  @Override
                  public Object[] getArgs(Object o1) {
                      return new Object[] { buildIndent(level + 1) };
                  }
              },
              new FooBar3() {
                  @Override
                  public String getFormat() {
                      return F5;
                  }
              }, list);
    }

    private static void printMap(final int level, Map<String, Object> map) {
        final int l = (getLength(map.keySet()) + 2);
        print(level, "msg.map", new FooBar1<Map.Entry<String, Object>>() {
            @Override
            public Object getObject(Map.Entry<String, Object> e) {
                return e.getValue();
            }
        }, new FooBar2<Map.Entry<String, Object>>() {
            @Override
            public Object[] getArgs(Map.Entry<String, Object> e) {
                return new Object[] { buildIndent(level), quote(e.getKey()) };
            }
        }, new FooBar3() {
            @Override
            public String getFormat() {
                return String.format(F8, l, SPACE);
            }
        }, map.entrySet());
    }

    private static void printf(String format, Object... args) {
        System.out.printf(format, append(LF, args));
    }

    private static String quote(Object o) {
        return ((o == null) ? NULL : QUOTE + o.toString() + QUOTE);
    }

    @SuppressWarnings("SameParameterValue")
    private static String stringOf(char[] spc, char ch) {
        Arrays.fill(spc, ch);
        return String.valueOf(spc);
    }

    private interface FooBar1<T> {
        Object getObject(T t);
    }

    private interface FooBar2<T> {
        Object[] getArgs(T t);
    }

    private interface FooBar3 {
        String getFormat();
    }
}
