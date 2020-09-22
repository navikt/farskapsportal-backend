package no.nav.farskapsportal.util;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Utils {
    public static <T> Collector<T, ?, T> toSingletonOrThrow(RuntimeException exception) {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw exception;
                    }
                    return list.get(0);
                }
        );
    }
}
