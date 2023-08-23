package no.nav.farskapsportal.backend.libs.felles.util;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;

public class Utils {

  public static <T> Collector<T, ?, T> toSingletonOrThrow(RuntimeException exception) {
    return Collectors.collectingAndThen(
        Collectors.toList(),
        list -> {
          if (list.size() != 1) {
            throw exception;
          }
          return list.get(0);
        });
  }

  public static String getMeldingsidSkatt(Farskapserklaering farskapserklaering, byte[] pades) {
    var crc32 = new CRC32();
    var outputstream = new ByteArrayOutputStream();
    outputstream.writeBytes(pades);
    crc32.update(outputstream.toByteArray());

    var signeringstidspunktMor =
        farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt();
    var zonedDateTime = signeringstidspunktMor.atZone(ZoneId.systemDefault());
    var epoch = signeringstidspunktMor.toEpochSecond(zonedDateTime.getOffset());

    return String.valueOf(crc32.getValue()) + epoch;
  }
}
