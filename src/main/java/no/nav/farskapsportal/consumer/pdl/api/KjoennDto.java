package no.nav.farskapsportal.consumer.pdl.api;

<<<<<<< HEAD
import lombok.Value;

@Value
=======
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
>>>>>>> main
public class KjoennDto implements PdlDto {
  KjoennTypeDto kjoenn;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
