package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Entity
@Validated
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Farskapserklaering implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  @NonNull
  @OneToOne(cascade = CascadeType.ALL)
  private Barn barn;

  @NonNull
  @ManyToOne(cascade = CascadeType.ALL)
  private Forelder mor;

  @NonNull
  @ManyToOne(cascade = CascadeType.ALL)
  private Forelder far;

  @NonNull
  @OneToOne(cascade = CascadeType.ALL)
  private SignertDokument signertErklaering;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + (barn == null ? 0 : barn.hashCode())
            + (mor == null ? 0 : mor.hashCode())
            + (far == null ? 0 : far.hashCode());

    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final Farskapserklaering other = (Farskapserklaering) obj;

    if (!barn.equals(other.barn)) return false;
    if (!mor.equals(other.mor)) return false;
    if (!far.equals(other.far)) return false;

    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder
        .append("Farskapserklaering gjelder barn med termindato ")
        .append(barn.getTermindato().toString())
        .append("\n")
        .append("Mor: ")
        .append(mor.getFornavn())
        .append(" ")
        .append(mor.getEtternavn())
        .append("\n")
        .append("Far: ")
        .append(far.getFornavn())
        .append(" ")
        .append(far.getEtternavn());
    return builder.toString();
  }
}
