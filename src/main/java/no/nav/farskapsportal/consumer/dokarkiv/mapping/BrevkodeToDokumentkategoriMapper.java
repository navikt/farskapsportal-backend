package no.nav.farskapsportal.consumer.dokarkiv.mapping;

import static no.nav.farskapsportal.consumer.dokarkiv.api.Dokumentkategori.BREV;
import static no.nav.farskapsportal.consumer.dokarkiv.api.Dokumentkategori.INFOBREV;
import static no.nav.farskapsportal.consumer.dokarkiv.api.Dokumentkategori.VEDTAKSBREV;

import java.util.HashMap;
import java.util.Map;
import no.nav.farskapsportal.consumer.dokarkiv.api.Dokumentkategori;
import org.springframework.stereotype.Component;

@Component
public class BrevkodeToDokumentkategoriMapper {

  private Map<String, Dokumentkategori> brevkodemap;

  public BrevkodeToDokumentkategoriMapper() {
    initBrevkodemap();
  }

  private void initBrevkodemap() {
    brevkodemap = new HashMap<String, Dokumentkategori>();

    brevkodemap.put("BI01V03", VEDTAKSBREV);
    brevkodemap.put("BI01V02", VEDTAKSBREV);
    brevkodemap.put("BI01V01", VEDTAKSBREV);
    brevkodemap.put("BI01S41", VEDTAKSBREV);
    brevkodemap.put("BI01K50", VEDTAKSBREV);
    brevkodemap.put("BI01J50", VEDTAKSBREV);
    brevkodemap.put("BI01I50", VEDTAKSBREV);
    brevkodemap.put("BI01I01", VEDTAKSBREV);
    brevkodemap.put("BI01G50", VEDTAKSBREV);
    brevkodemap.put("BI01G04", VEDTAKSBREV);
    brevkodemap.put("BI01G02", VEDTAKSBREV);
    brevkodemap.put("BI01G01", VEDTAKSBREV);
    brevkodemap.put("BI01F50", VEDTAKSBREV);
    brevkodemap.put("BI01F02", VEDTAKSBREV);
    brevkodemap.put("BI01F01", VEDTAKSBREV);
    brevkodemap.put("BI01E50", VEDTAKSBREV);
    brevkodemap.put("BI01E02", VEDTAKSBREV);
    brevkodemap.put("BI01E01", VEDTAKSBREV);
    brevkodemap.put("BI01B50", VEDTAKSBREV);
    brevkodemap.put("BI01B21", VEDTAKSBREV);
    brevkodemap.put("BI01B20", VEDTAKSBREV);
    brevkodemap.put("BI01B05", VEDTAKSBREV);
    brevkodemap.put("BI01B03", VEDTAKSBREV);
    brevkodemap.put("BI01B02", VEDTAKSBREV);
    brevkodemap.put("BI01B01", VEDTAKSBREV);
    brevkodemap.put("BI01A50", VEDTAKSBREV);
    brevkodemap.put("BI01A08", VEDTAKSBREV);
    brevkodemap.put("BI01A06", VEDTAKSBREV);
    brevkodemap.put("BI01A02", VEDTAKSBREV);
    brevkodemap.put("BI01A01", VEDTAKSBREV);
    brevkodemap.put("BI01A03", INFOBREV);
    brevkodemap.put("BI01A07", INFOBREV);
    brevkodemap.put("BI01S01", INFOBREV);
    brevkodemap.put("BI01S03", INFOBREV);
    brevkodemap.put("BI01S04", INFOBREV);
    brevkodemap.put("BI01S05", INFOBREV);
    brevkodemap.put("BI01S06", INFOBREV);
    brevkodemap.put("BI01S07", INFOBREV);
    brevkodemap.put("BI01S08", INFOBREV);
    brevkodemap.put("BI01S09", INFOBREV);
    brevkodemap.put("BI01S11", INFOBREV);
    brevkodemap.put("BI01S12", INFOBREV);
    brevkodemap.put("BI01S13", INFOBREV);
    brevkodemap.put("BI01S14", INFOBREV);
    brevkodemap.put("BI01S15", INFOBREV);
    brevkodemap.put("BI01S16", INFOBREV);
    brevkodemap.put("BI01S17", INFOBREV);
    brevkodemap.put("BI01S18", INFOBREV);
    brevkodemap.put("BI01S19", INFOBREV);
    brevkodemap.put("BI01S20", INFOBREV);
    brevkodemap.put("BI01S21", INFOBREV);
    brevkodemap.put("BI01S25", INFOBREV);
    brevkodemap.put("BI01S26", INFOBREV);
    brevkodemap.put("BI01S27", INFOBREV);
    brevkodemap.put("BI01S28", INFOBREV);
    brevkodemap.put("BI01S29", INFOBREV);
    brevkodemap.put("BI01S30", INFOBREV);
    brevkodemap.put("BI01S31", INFOBREV);
    brevkodemap.put("BI01S32", INFOBREV);
    brevkodemap.put("BI01S33", INFOBREV);
    brevkodemap.put("BI01S34", INFOBREV);
    brevkodemap.put("BI01S35", INFOBREV);
    brevkodemap.put("BI01S36", INFOBREV);
    brevkodemap.put("BI01S37", INFOBREV);
    brevkodemap.put("BI01S38", INFOBREV);
    brevkodemap.put("BI01S39", INFOBREV);
    brevkodemap.put("BI01S42", INFOBREV);
    brevkodemap.put("BI01S43", INFOBREV);
    brevkodemap.put("BI01S44", INFOBREV);
    brevkodemap.put("BI01S45", INFOBREV);
    brevkodemap.put("BI01S46", INFOBREV);
    brevkodemap.put("BI01S47", INFOBREV);
    brevkodemap.put("BI01S48", INFOBREV);
    brevkodemap.put("BI01S49", INFOBREV);
    brevkodemap.put("BI01S50", INFOBREV);
    brevkodemap.put("BI01S51", INFOBREV);
    brevkodemap.put("BI01S53", INFOBREV);
    brevkodemap.put("BI01S54", INFOBREV);
    brevkodemap.put("BI01S55", INFOBREV);
    brevkodemap.put("BI01S56", INFOBREV);
    brevkodemap.put("BI01S57", INFOBREV);
    brevkodemap.put("BI01S58", INFOBREV);
    brevkodemap.put("BI01S59", INFOBREV);
    brevkodemap.put("BI01S60", INFOBREV);
    brevkodemap.put("BI01S62", INFOBREV);
    brevkodemap.put("BI01S63", INFOBREV);
    brevkodemap.put("BI01S64", INFOBREV);
    brevkodemap.put("BI01S65", INFOBREV);
    brevkodemap.put("BI01S68", INFOBREV);
    brevkodemap.put("BI01S70", INFOBREV);
    brevkodemap.put("BI01V04", INFOBREV);
  }

  public String toDokumentkategori(String brevkode) {
    return this.brevkodemap.getOrDefault(brevkode, BREV).getKode();
  }

  public Map<String, Dokumentkategori> getBrevkodemap() {
    return brevkodemap;
  }
}
