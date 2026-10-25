# Integrazioni con i servizi streaming

Stato aggiornato alla versione 1.0.0. **Nessuna integrazione è stata provata su un dispositivo reale**:
le voci qui sotto distinguono ciò che si basa su documentazione pubblica da ciò che è un tentativo.

## Cosa fa STREAMHUB quando premi GUARDA

1. Cerca l'app del servizio tra i package candidati (diversi per Android TV e Fire TV).
2. Se il package non corrisponde, cerca un'app con la stessa **etichetta** nel launcher (es. "RaiPlay").
3. Se esiste un link ufficiale al singolo contenuto, prova prima quello; poi apre l'app.
4. Se l'app non c'è, mostra la schermata "non installata" e, solo per package pubblicati negli store,
   offre di aprire lo store (`market://` su Android TV, `amzn://apps/android?p=` su Fire TV).

## Apertura del singolo titolo

TMDb fornisce **dove** un titolo è disponibile (dati JustWatch), ma **non** fornisce deep link alla
riproduzione: la documentazione TMDb lo dichiara esplicitamente. Per questo, per tutti i servizi
tranne YouTube, STREAMHUB apre l'app ufficiale e non il titolo specifico. Nessuno schema URL è stato inventato.

| Servizio | Package Android TV | Package Fire TV | Riconoscimento per etichetta | Apertura titolo |
|---|---|---|---|---|
| Netflix | `com.netflix.ninja` (store) | `com.netflix.ninja` (store) | "Netflix" | No (solo app) |
| Prime Video | `com.amazon.amazonvideo.livingroom` (store) | `com.amazon.firebat`, `com.amazon.avod.thirdpartyclient` (**non verificati**) | "Prime Video" | No |
| Disney+ | `com.disney.disneyplus` (store) | `com.disney.disneyplus` (store) | "Disney+" | No |
| Apple TV | `com.apple.atve.androidtv.appletv` (store) | `com.apple.atve.amazon.appletv` (store) | "Apple TV" | No |
| Paramount+ | `com.cbs.ott` (**non verificato** per l'Italia) | idem | "Paramount+" | No |
| NOW | — (**non verificato**) | — | "NOW", "NOW TV" | No |
| DAZN | `com.dazn` (store) | `com.dazn` (store) | "DAZN" | No |
| RaiPlay | — (**non verificato**) | — | "RaiPlay" | No |
| Mediaset Infinity | — (**non verificato**) | — | "Mediaset Infinity", "Infinity" | No |
| YouTube | `com.google.android.youtube.tv` (store) | `com.amazon.firetv.youtube` (store) | "YouTube" | Sì, solo **trailer**: `https://www.youtube.com/watch?v=ID` (URL pubblico standard, ID fornito da TMDb) |

"(store)" = identificativo pubblico negli store, da confermare sul tuo dispositivo.
I package **non verificati** non vengono mai proposti nello store.

## Verificate (documentazione pubblica)

- Disponibilità per paese, tipi di offerta (abbonamento, gratis, con pubblicità, noleggio, acquisto) e loghi provider: endpoint TMDb `watch/providers`, con attribuzione JustWatch obbligatoria (presente in Home, Scheda e Impostazioni).
- Riconoscimento Fire TV: feature di sistema `amazon.hardware.fire_tv` (documentazione Amazon).
- Apertura app: API Android `getLeanbackLaunchIntentForPackage` / `getLaunchIntentForPackage`, con `<queries>` dichiarate nel manifest (Android 11+).

## Non verificate

- Package Fire TV di Prime Video, package di Paramount+ in Italia, package di NOW, RaiPlay, Mediaset Infinity.
- Comportamento dell'URL YouTube sull'app YouTube di Fire TV.
- Corrispondenza tra nomi dei provider TMDb e servizi: regole in `ServiceMatcher` (testate con nomi noti, ma TMDb può cambiare i nomi).

## Come aggiungere un servizio

1. Aggiungi una voce in `integrations/StreamingServices.kt` (id, nome, package candidati, etichette).
2. Aggiungi una regola in `ServiceMatcher.match`.
3. Se hai package nuovi, aggiungili in `<queries>` nel manifest.
4. Se il servizio documenta un link ufficiale ai contenuti, crea un adattatore come `YouTubeAdapter`.

La Home, la Scheda e la schermata Servizi si aggiornano da sole.

## Come verificare un package sul tuo dispositivo

```
adb shell pm list packages | grep -i netflix
```
