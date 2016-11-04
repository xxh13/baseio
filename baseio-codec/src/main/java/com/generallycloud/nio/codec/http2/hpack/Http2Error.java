package com.generallycloud.nio.codec.http2.hpack;

public enum Http2Error {
	    NO_ERROR(0x0),
	    PROTOCOL_ERROR(0x1),
	    INTERNAL_ERROR(0x2),
	    FLOW_CONTROL_ERROR(0x3),
	    SETTINGS_TIMEOUT(0x4),
	    STREAM_CLOSED(0x5),
	    FRAME_SIZE_ERROR(0x6),
	    REFUSED_STREAM(0x7),
	    CANCEL(0x8),
	    COMPRESSION_ERROR(0x9),
	    CONNECT_ERROR(0xA),
	    ENHANCE_YOUR_CALM(0xB),
	    INADEQUATE_SECURITY(0xC),
	    HTTP_1_1_REQUIRED(0xD);

	    private final long code;
	    private static final Http2Error[] INT_TO_ENUM_MAP;
	    static {
	        Http2Error[] errors = Http2Error.values();
	        Http2Error[] map = new Http2Error[errors.length];
	        for (int i = 0; i < errors.length; ++i) {
	            Http2Error error = errors[i];
	            map[(int) error.code()] = error;
	        }
	        INT_TO_ENUM_MAP = map;
	    }

	    Http2Error(long code) {
	        this.code = code;
	    }

	    /**
	     * Gets the code for this error used on the wire.
	     */
	    public long code() {
	        return code;
	    }

	    public static Http2Error valueOf(long value) {
	        return value >= INT_TO_ENUM_MAP.length || value < 0 ? null : INT_TO_ENUM_MAP[(int) value];
	    }
	}
