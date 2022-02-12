package com.galenrhodes.kwikjson;

import java.io.IOException;

public class KwikJSONException extends IOException {
    public KwikJSONException() {
        super();
    }

    public KwikJSONException(String message) {
        super(message);
    }

    public KwikJSONException(String format, Object... args) {
        super(String.format(format, args));
    }

    public KwikJSONException(String message, Throwable cause) {
        super(message, cause);
    }

    public KwikJSONException(Throwable cause) {
        super(cause);
    }
}
