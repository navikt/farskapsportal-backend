package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;
import org.springframework.validation.annotation.Validated;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Barn implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  private LocalDate termindato;

  @NaturalId private String foedselsnummer;

  @OneToOne(mappedBy = "barn", cascade = CascadeType.ALL)
  private Farskapserklaering farskapserklaering;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + (termindato == null ? 0 : termindato.hashCode())
            + (foedselsnummer == null ? 0 : foedselsnummer.hashCode())
            + (farskapserklaering == null
                ? 0
                : farskapserklaering.getMor().getFoedselsnummer().hashCode())
            + (farskapserklaering == null
                ? 0
                : farskapserklaering.getFar().getFoedselsnummer().hashCode());

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

    if (farskapserklaering == null) {
      if (other.farskapserklaering != null) return false;
    } else if (other.farskapserklaering == null) return false;
    else if (!farskapserklaering
            .getMor()
            .getFoedselsnummer()
            .equals(other.farskapserklaering.getMor().getFoedselsnummer())
        || !farskapserklaering
            .getFar()
            .getFoedselsnummer()
            .equals(other.farskapserklaering.getFar().getFoedselsnummer())) return false;

    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Barn knyttet til termindato: ").append(termindato);
    return builder.toString();
  }
}
