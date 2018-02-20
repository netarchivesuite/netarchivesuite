package dk.netarkivet.heritrix3.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class AcceptLanguageParser {

    public static final int S_START_SPC = 0;
    public static final int S_LANG = 1;
    public static final int S_COUNTRY = 2;
    public static final int S_COUNTRY_SPC = 3;
    public static final int S_SEMICOLON = 4;
    public static final int S_NAME = 5;
    public static final int S_NAME_SPC = 6;
    public static final int S_EQ = 7;
    public static final int S_VALUE = 8;

    public static class AcceptLanguage {
        public String language;
        public String country;
        public String locale;
        public float qvalue = 1.0f;
    }

    public static class AcceptLanguageComparator implements Comparator<AcceptLanguage> {
        @Override
        public int compare(AcceptLanguage o1, AcceptLanguage o2) {
            return Math.round(Math.signum(o2.qvalue - o1.qvalue));
        }
    }

    public static AcceptLanguageComparator acceptLanguageComparator = new AcceptLanguageComparator();

    public static List<AcceptLanguage> parseHeader(HttpServletRequest req) {
        return parseHeader(req.getHeader("Accept-Language"));
    }

    public static List<AcceptLanguage> parseHeader(String acceptLanguageStr) {
        List<AcceptLanguage> acceptLanguages = new ArrayList<>();
        char[] charArr;
        String name = null;
        if (acceptLanguageStr != null && acceptLanguageStr.length() > 0) {
            AcceptLanguage acceptLanguage = null;
            StringBuilder sb = new StringBuilder();
            charArr = acceptLanguageStr.toLowerCase().toCharArray();
            char c;
            int idx = 0;
            int len = charArr.length;
            int state = S_START_SPC;
            boolean bLoop = true;
            while (bLoop) {
                if (idx < len) {
                    switch (state) {
                    case S_START_SPC:
                        while (idx < len && charArr[idx] == ' ') {
                            ++idx;
                        }
                        state = S_LANG;
                        acceptLanguage = new AcceptLanguage();
                        sb.setLength(0);
                        break;
                    case S_LANG:
                        c = charArr[idx];
                        switch (c) {
                        case ',':
                            acceptLanguages.add(acceptLanguage);
                            acceptLanguage.language = sb.toString();
                            acceptLanguage.locale = acceptLanguage.language;
                            ++idx;
                            state = S_START_SPC;
                            break;
                        case '-':
                        case '_':
                            acceptLanguages.add(acceptLanguage);
                            acceptLanguage.language = sb.toString();
                            ++idx;
                            state = S_COUNTRY;
                            sb.setLength(0);
                            break;
                        case ' ':
                            acceptLanguages.add(acceptLanguage);
                            acceptLanguage.language = sb.toString();
                            acceptLanguage.locale = acceptLanguage.language;
                            ++idx;
                            state = S_COUNTRY_SPC;
                            break;
                        case ';':
                            acceptLanguages.add(acceptLanguage);
                            acceptLanguage.language = sb.toString();
                            acceptLanguage.locale = acceptLanguage.language;
                            ++idx;
                            state = S_SEMICOLON;
                            break;
                        default:
                            if (c >= 'a' && c <= 'z') {
                                sb.append(c);
                                ++idx;
                            }
                            else {
                                bLoop = false;
                            }
                            break;
                        }
                        break;
                    case S_COUNTRY:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                            if (sb.length() > 0) {
                                acceptLanguage.country = sb.toString();
                                acceptLanguage.locale = acceptLanguage.language + "-" + acceptLanguage.country;
                            }
                            else {
                                acceptLanguage.locale = acceptLanguage.language;
                            }
                            ++idx;
                            state = S_COUNTRY_SPC;
                            break;
                        case ',':
                            if (sb.length() > 0) {
                                acceptLanguage.country = sb.toString();
                                acceptLanguage.locale = acceptLanguage.language + "-" + acceptLanguage.country;
                            }
                            else {
                                acceptLanguage.locale = acceptLanguage.language;
                            }
                            ++idx;
                            state = S_START_SPC;
                            break;
                        case ';':
                            if (sb.length() > 0) {
                                acceptLanguage.country = sb.toString();
                                acceptLanguage.locale = acceptLanguage.language + "-" + acceptLanguage.country;
                            }
                            else {
                                acceptLanguage.locale = acceptLanguage.language;
                            }
                            ++idx;
                            state = S_SEMICOLON;
                            break;
                        default:
                            if (c >= 'a' && c <= 'z') {
                                sb.append(c);
                                ++idx;
                            }
                            else {
                                bLoop = false;
                            }
                            break;
                        }
                           break;
                    case S_COUNTRY_SPC:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                            ++idx;
                            break;
                        case ',':
                            ++idx;
                            state = S_START_SPC;
                            break;
                        case ';':
                            ++idx;
                            state = S_SEMICOLON;
                            break;
                        default:
                            bLoop = false;
                            break;
                        }
                        break;
                    case S_SEMICOLON:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                        case ';':
                            ++idx;
                            break;
                        case ',':
                            ++idx;
                            state = S_START_SPC;
                            break;
                        default:
                            if (c >= 'a' && c <= 'z') {
                                sb.setLength(0);
                                sb.append(c);
                                ++idx;
                                state = S_NAME;
                            }
                            else {
                                bLoop = false;
                            }
                            break;
                        }
                        break;
                    case S_NAME:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                            name = sb.toString();
                            ++idx;
                            state = S_NAME_SPC;
                            break;
                        case '=':
                            name = sb.toString();
                            ++idx;
                            state = S_EQ;
                            break;
                        default:
                            if (c >= 'a' && c <= 'z') {
                                sb.append(c);
                                ++idx;
                            }
                            else {
                                bLoop = false;
                            }
                            break;
                        }
                        break;
                    case S_NAME_SPC:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                            ++idx;
                            break;
                        case '=':
                            ++idx;
                            state = S_EQ;
                            break;
                        default:
                            bLoop = false;
                            break;
                        }
                        break;
                    case S_EQ:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                            ++idx;
                            break;
                        default:
                            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                                sb.setLength(0);
                                sb.append(c);
                                ++idx;
                                state = S_VALUE;
                            }
                            else {
                                bLoop = false;
                            }
                            break;
                        }
                        break;
                    case S_VALUE:
                        c = charArr[idx];
                        switch (c) {
                        case ' ':
                            if ("q".equals(name)) {
                                try {
                                    acceptLanguage.qvalue = Float.parseFloat(sb.toString());
                                }
                                catch (NumberFormatException e) {
                                    bLoop = false;
                                }
                            }
                            ++idx;
                            state = S_COUNTRY_SPC;
                            break;
                        case ';':
                            if ("q".equals(name)) {
                                try {
                                    acceptLanguage.qvalue = Float.parseFloat(sb.toString());
                                }
                                catch (NumberFormatException e) {
                                    bLoop = false;
                                }
                            }
                            ++idx;
                            state = S_SEMICOLON;
                            break;
                        case ',':
                            if ("q".equals(name)) {
                                try {
                                    acceptLanguage.qvalue = Float.parseFloat(sb.toString());
                                }
                                catch (NumberFormatException e) {
                                    bLoop = false;
                                }
                            }
                            ++idx;
                            state = S_START_SPC;
                            break;
                        default:
                            if (c == '.' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                                sb.append(c);
                                ++idx;
                            }
                            else {
                                bLoop = false;
                            }
                            break;
                        }
                        break;
                    default:
                        throw new IllegalStateException("Epic fail! (State=" + state + ")");
                    }
                }
                else {
                    bLoop = false;
                }
            }
            if (idx == len) {
                switch (state) {
                case S_LANG:
                    if (sb.length() > 0) {
                        acceptLanguages.add(acceptLanguage);
                        acceptLanguage.language = sb.toString();
                        acceptLanguage.locale = acceptLanguage.language;
                    }
                    break;
                case S_COUNTRY:
                    if (sb.length() > 0) {
                        acceptLanguage.country = sb.toString();
                        acceptLanguage.locale = acceptLanguage.language + "-" + acceptLanguage.country;
                    }
                    else {
                        acceptLanguage.locale = acceptLanguage.language;
                    }
                    break;
                case S_VALUE:
                    if ("q".equals(name)) {
                        try {
                            acceptLanguage.qvalue = Float.parseFloat(sb.toString());
                        }
                        catch (NumberFormatException e) {
                        }
                    }
                    break;
                }
            }
        }
        Collections.sort(acceptLanguages, acceptLanguageComparator);
        return acceptLanguages;
    }

}
