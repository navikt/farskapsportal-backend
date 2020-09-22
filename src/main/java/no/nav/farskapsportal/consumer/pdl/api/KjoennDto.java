package no.nav.farskapsportal.consumer.pdl.api;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KjoennDto implements PdlDto {
    KjoennTypeDto kjoenn;
    FolkeregistermetadataDto folkeregistermetadata;
    MetadataDto metadata;
}
