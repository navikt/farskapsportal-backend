# farskapsportal-api
Støttefunksjoner for farskapsportal. Gjør oppslag mot PDL for å hente nødvendige personopplysninger 
i forbindelse med erklæring av farskap. Inneholder også grenesnitt mot Skatt og Joark for lagring av 
ferdigstilte farskapserklæringer.

### hemmeligheter
Hemmeligheter lagres som kubernetes secrets i GCP namespace farskapsportal. Følgende er i bruk:

##### Secret name: farskapsportal-api-secret
 - APIKEY_PDLAPI_FP: Api gateway-nøkkel for kommunikasjon mellom farskapsportal-api i GCP og pdl-api 
 i FSS-sonen. Originalen ligger i Vault, dev-sti:
 https://vault.adeo.no/ui/vault/secrets/apikey/show/apigw/dev/pdl-api/farskapsportal-api_q1
 
 - APIKEY_STS_FP: Api gateway-nøkkel for kommunikasjon mellom farskapsportal-api i GCP og 
 security-token-service (STS) i FSS-sonen. Originalen ligger i Vault, dev-sti: 
https://vault.adeo.no/ui/vault/secrets/apikey/show/apigw/dev/security-token-service-token/farskapsportal-api_q1

 - SRVFARSKAPSPORTAL_PWD: Passord til farskapsportalssystembruker. Denne brukes ved henting av token 
 STS. Originalen ligger i Vault, dev-sti:  
 https://vault.adeo.no/ui/vault/secrets/serviceuser/show/dev/srvfarskapsportal
 
##### Secret name farskapsportal-api-aud
 - AUD_FP: Angir gyldig publikum for farskapsportal-apis OIDC-token.

