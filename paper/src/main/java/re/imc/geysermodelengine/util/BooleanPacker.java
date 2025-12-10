package re.imc.geysermodelengine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BooleanPacker {

    private static final int MAX_BOOLEANS = 24;

    public static int booleansToInt(List<Boolean> booleans) {
        int result = 0;
        int i = 1;

        for (boolean b : booleans) {
            if (b) {
                result += i;
            }
            i *= 2;
        }

        return result;
    }

    public static int mapBooleansToInt(Map<String, Boolean> booleanMap) {
        int result = 0;
        int i = 1;

        List<String> keys = new ArrayList<>(booleanMap.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            if (booleanMap.get(key)) {
                result += i;
            }
            i *= 2;
        }
        return result;
    }

    public static List<Integer> booleansToInts(List<Boolean> booleans) {
        List<Integer> results = new ArrayList<>();
        int result = 0;
        int i = 1;
        int i1 = 1;

        for (boolean b : booleans) {
            if (b) {
                result += i;
            }
            if (i1 % MAX_BOOLEANS == 0 || i1 == booleans.size()) {
                results.add(result);
                result = 0;
                i = 1;
            } else {
                i *= 2;
            }
            i1++;
        }

        return results;
    }

    public static List<Integer> mapBooleansToInts(Map<String, Boolean> booleanMap) {
        List<String> keys = new ArrayList<>(booleanMap.keySet());
        List<Boolean> booleans = new ArrayList<>();

        Collections.sort(keys);

        for (String key : keys) {
            booleans.add(booleanMap.get(key));
        }
        return booleansToInts(booleans);
    }
}
