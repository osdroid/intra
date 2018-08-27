package app.intra.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockedSites {
    private static Site[] list = null;
    public enum Category {
        UNKNOWN,
        ANNOYING,
        PROCRASTINATION,
        NEWS
    }
    public static class Site implements Comparable<Site> {
        public final char[] url;
        public final Category category;
        public static String reverseUrl(String original) {
            String[] pieces = original.split("\\x2E");
            StringBuilder builder = new StringBuilder();
            for (int i = pieces.length - 1; i >= 0; i--) {
                if (pieces[i].length() == 0)
                    continue;
                builder.append(".");
                builder.append(pieces[i]);
            }
            return builder.toString();
        }
        public Site(String url, Category category) {
            this.url = reverseUrl(url).toLowerCase().toCharArray();
            this.category = category;
        }
        @Override
        public int compareTo(Site other) {
            int lim = Math.min(url.length, other.url.length);
            for (int i = 0; i < lim; i++) {
                if (url[i] == '*' || other.url[i] == '*')
                    return 0;
                if (url[i] != other.url[i])
                    return url[i] - other.url[i];
            }
            return url.length - other.url.length;
        }
    }
    public static void loadSites(boolean forceReload) {
        if (list != null && !forceReload)
            return;
        List<Site> sites = new ArrayList<>();
        sites.add(new Site("*.advertising.com",
                Category.ANNOYING));
        sites.add(new Site("*.facebook.com",
                Category.ANNOYING));
        sites.add(new Site("*.facebook.net",
                Category.ANNOYING));
        sites.add(new Site("*.criteo.com",
                Category.ANNOYING));
        sites.add(new Site("*.doubleclick.net",
                Category.ANNOYING));
        sites.add(new Site("*.qq.com",
                Category.ANNOYING));

        list = sites.toArray(new Site[0]);
        Arrays.sort(list);
    }
    public static Category getUrlCategory(String url) {
        int foundIdx = Arrays.binarySearch(
                list, new Site(url, Category.UNKNOWN));
        if (foundIdx < 0)
            return Category.UNKNOWN;
        return list[foundIdx].category;
    }
}
