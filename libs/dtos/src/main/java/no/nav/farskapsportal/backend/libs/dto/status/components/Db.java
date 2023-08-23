package no.nav.farskapsportal.backend.libs.dto.status.components;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Db extends Statuskomponent {

  DbDetails details;

  enum Database {
    POSTGRES_SQL("PostgreSQL");
    private String type;

    Database(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  private class DbDetails {

    Database database;
    String validationQuery;
  }
}
