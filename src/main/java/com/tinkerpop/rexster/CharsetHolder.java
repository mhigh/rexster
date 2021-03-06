package com.tinkerpop.rexster;

import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class CharsetHolder implements Comparable<CharsetHolder> {
    private static Logger logger = Logger.getLogger(CharsetHolder.class);

    private String charset;
    private float quality = 0.0f;
    private int order;

    public CharsetHolder(String charset, float quality, int order) {

        if (charset == null) {
            throw new NullArgumentException("charset");
        }

        this.charset = charset;
        this.quality = quality;
        this.order = order;
    }

    public String getCharset() {
        return this.charset;
    }

    public float getQuality() {
        return this.quality;
    }

    public int getOrder() {
        return this.order;
    }

    public boolean isSupported() {
        boolean isSupportedCharset = true;
        try {
            isSupportedCharset = Charset.isSupported(this.charset);
        } catch (IllegalCharsetNameException icne) {
            isSupportedCharset = false;
        }

        return isSupportedCharset;
    }

    public static List<CharsetHolder> getAcceptableCharsets(String acceptCharsetHeaderValue) {
        ArrayList<CharsetHolder> charsetHolders = new ArrayList<CharsetHolder>();

        String[] charsetStrings = acceptCharsetHeaderValue.split(",");
        int order = 0;

        CharsetHolder asteriskCharset = null;

        for (String charsetString : charsetStrings) {
            try {
                String charsetValue;
                float qualityValue = 1f;

                String[] charsetComponents = charsetString.split(";");

                charsetValue = charsetComponents[0];
                if (charsetComponents.length == 2) {
                    String[] qualityPair = charsetComponents[1].trim().split("=");
                    if (qualityPair.length == 2) {
                        qualityValue = Float.parseFloat(qualityPair[1].trim());
                    }
                }

                if (charsetValue.equals("*")) {
                    asteriskCharset = new CharsetHolder("*", qualityValue, order);
                } else {
                    charsetHolders.add(new CharsetHolder(charsetValue, qualityValue, order));
                }

            } catch (Exception ex) {
                logger.warn("Charset from the request (or rexster.xml) is not valid: [" + charsetString + "].");
            }

            order++;
        }

        SortedMap<String, Charset> availableCharsets = Charset.availableCharsets();
        if (asteriskCharset == null) {
            for (Map.Entry<String, Charset> availableCharset : availableCharsets.entrySet()) {

                CharsetHolder otherCharsetHolder = new CharsetHolder(availableCharset.getKey(), 0, Integer.MAX_VALUE);
                ;
                if (availableCharset.getKey().equals("ISO-8859-1")) {
                    otherCharsetHolder = new CharsetHolder(availableCharset.getKey(), 1, Integer.MAX_VALUE);
                }

                if (!charsetHolders.contains(otherCharsetHolder)) {
                    charsetHolders.add(otherCharsetHolder);
                }
            }
        } else {
            for (Map.Entry<String, Charset> availableCharset : availableCharsets.entrySet()) {
                CharsetHolder otherCharsetHolder = new CharsetHolder(availableCharset.getKey(), asteriskCharset.getQuality(), 0);

                if (!charsetHolders.contains(otherCharsetHolder)) {
                    charsetHolders.add(otherCharsetHolder);
                }
            }
        }

        Collections.sort(charsetHolders);

        return charsetHolders;
    }

    public int compareTo(CharsetHolder charsetHolder) {
        int compare = this.quality == charsetHolder.getQuality() ? 0 : (this.quality > charsetHolder.getQuality() ? -1 : 1);
        if (compare == 0) {
            compare = this.order == charsetHolder.getOrder() ? 0 : (this.order > charsetHolder.getOrder() ? 1 : -1);
        }

        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CharsetHolder that = (CharsetHolder) o;

        if (!charset.equals(that.charset)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return charset.hashCode();
    }
}
