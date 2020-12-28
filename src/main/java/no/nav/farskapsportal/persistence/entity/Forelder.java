package no.nav.farskapsportal.persistence.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
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
public class Forelder implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @NaturalId private String foedselsnummer;

  private String fornavn;

  private String mellomnavn;

  private String etternavn;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "mor", cascade = CascadeType.ALL)
  private Set<Farskapserklaering> erklaeringerMor = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "far", cascade = CascadeType.ALL)
  private Set<Farskapserklaering> erklaeringerFar = new HashSet<>();

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (foedselsnummer == null ? 0 : foedselsnummer.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final Forelder other = (Forelder) obj;
    return foedselsnummer.equals(other.foedselsnummer);
  }

  @Override
  public String toString() {
    return "Forelder: " + fornavn + " " + (mellomnavn != null ? mellomnavn + " " : "") + etternavn;
  }
}
