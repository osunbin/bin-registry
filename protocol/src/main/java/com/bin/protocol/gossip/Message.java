package com.bin.protocol.gossip;

import com.bin.protocol.gossip.common.Address;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class Message  {





    private Map<String, String> headers = new HashMap<>();

    private  Object data;


    public Message() {

    }

    public Message(Builder builder) {
        this.headers = builder.headers();
        this.data = builder.data();
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers =headers;
    }

    public void setHeader(String key,String value) {
        this.headers.put(key,value);
    }



    public static Message fromData(String data) {
        return withData(data).build();
    }

    public static Builder withData(String data) {
        return builder().data(data);
    }


    public static Builder withData(Object data) {
        return builder().data(data);
    }


    public static Message fromHeaders(Map<String, String> headers) {
        return withHeaders(headers).build();
    }


    public static Builder withHeaders(Map<String, String> headers) {
        return builder().headers(headers);
    }



    public static Message from(Message message) {
        return with(message).build();
    }


    public static Builder with(Message message) {
        return withData(message.data).headers(message.headers);
    }


    public static Builder builder() {
        return new Builder();
    }


    public Map<String, String> headers() {
        return headers;
    }


    public String header(String name) {
        return headers.get(name);
    }




    public Object data() {
        // noinspection unchecked
        return  data;
    }



    @Override
    public String toString() {
        return new StringJoiner(", ", Message.class.getSimpleName() + "[", "]")
                .add("headers=" + headers)
                .add("data=" + data)
                .toString();
    }


    public static class Builder {

        private final Map<String, String> headers = new HashMap<>();
        private Object data;

        private Builder() {}

        private Object data() {
            return this.data;
        }

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        private Map<String, String> headers() {
            return this.headers;
        }


        public Builder headers(Map<String, String> headers) {
            headers.forEach(this::header);
            return this;
        }


        public Builder header(String key, String value) {
            Objects.requireNonNull(key);
            headers.put(key, value);
            return this;
        }


        public Message build() {
            return new Message(this);
        }
    }

}

