# ARC Consolidator

Ce projet lit les événements bruts depuis les topics Kafka `iam.raw.*`, les consolide/normalise, puis publie les événements au format cible dans le topic `arc`.

## Structure initiale
- src/main/java/com/instantaccessprovider/arcconsolidator/
    - ArcEvent.java (modèle cible)
    - ArcEventMapper.java (mapping raw → arc)
    - ArcEventPublisher.java (Kafka publisher)
    - RawIamEventConsumer.java (Kafka consumer)
    - Application.java (Spring Boot entrypoint)
- src/main/resources/
    - application.yml

## Fonctionnement
- Consomme les topics `iam.raw.*` (ex : `iam.raw.activedirectory`)
- Transforme chaque événement brut en un événement ARC unifié
- Publie dans le topic `arc`

---

À compléter selon les besoins métier (enrichissement, gestion d’erreurs, tests, etc.)
