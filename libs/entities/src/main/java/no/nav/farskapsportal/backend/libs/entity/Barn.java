package no.nav.farskapsportal.backend.libs.entity;

import java.io.Serializable;
import java.time.LocalDate;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Barn implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private LocalDate termindato;

  @NaturalId private String foedselsnummer;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + (termindato == null ? 0 : termindato.hashCode())
            + (foedselsnummer == null ? 0 : foedselsnummer.hashCode());

    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final Barn other = (Barn) obj;

    if (termindato == null) {
      if (other.termindato != null) return false;
    } else if (!termindato.equals(other.termindato)) return false;

    if (foedselsnummer == null) {
      if (other.foedselsnummer != null) return false;
    } else if (!foedselsnummer.equals(other.foedselsnummer)) return false;

    return foedselsnummer == null
        ? termindato.equals(other.termindato)
        : foedselsnummer.equals(other.foedselsnummer);
  }

  @Override
  public String toString() {
    if (termindato != null) {
      return "Barn knyttet til termindato: " + termindato.toString();
    } else  if (foedselsnummer != null){
      return "Barn med fødselsnummer som starter med: " + foedselsnummer.substring(0,6);
    }
    return "Objektet mangler data";
  }
}
